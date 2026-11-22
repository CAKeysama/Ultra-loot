#!/usr/bin/env python3
"""Card Restorer asset generator (pure Python: zlib + struct, no Pillow).

Writes, deterministically, every asset of the `ultraloot:card_restorer` block:
  textures/block/card_restorer_{front,front_on,side,top,bottom}.png   16x16 RGBA
  textures/gui/card_restorer.png                                      256x256 GUI sheet (panel + sprites)
  models/block/card_restorer_base.json, card_restorer.json, card_restorer_on.json
  blockstates/card_restorer.json, models/item/card_restorer.json

The block art follows tools/generate_assets.py (same graphite palette, frame layout and model geometry) with a copper
accent; the GUI panel follows tools/generate_gui_textures.py (same panel recipe). Both of those need Pillow, so the
pieces used here are re-implemented below. Atlas coordinates of the GUI sprites must match CardRestorerScreen.java.

Usage (from the repo root):
    python tools/generate_restorer_assets.py           # write changed files
    python tools/generate_restorer_assets.py --check   # exit 1 if anything is stale or fails validation
"""
from __future__ import annotations

import json
import random
import struct
import sys
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "ultraloot"
NS = "ultraloot"
BID = "card_restorer"
FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))

# --------------------------------------------------------------------------------------------
# PNG codec (RGBA8, non-interlaced)
# --------------------------------------------------------------------------------------------


def png_bytes(w: int, h: int, px: list) -> bytes:
    raw = bytearray()
    for y in range(h):
        raw.append(0)  # filter: none
        for x in range(w):
            raw.extend(px[y * w + x])
    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
            + chunk(b"IEND", b""))


def png_header(data: bytes) -> tuple[int, int, int, int]:
    """(width, height, bit depth, colour type) of a PNG."""
    if data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
        raise ValueError("not a PNG")
    w, h, depth, ctype = struct.unpack(">IIBB", data[16:26])
    return w, h, depth, ctype


class Canvas:
    def __init__(self, w: int = 16, h: int = 16, fill=(0, 0, 0, 0)):
        self.w, self.h = w, h
        self.px = [tuple(fill)] * (w * h)

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y * self.w + x] = c if len(c) == 4 else (c[0], c[1], c[2], 255)

    def get(self, x, y):
        return self.px[y * self.w + x]

    def fill(self, x0, y0, x1, y1, c):
        """Inclusive rectangle."""
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, c)

    def rect(self, x, y, w, h, c):
        self.fill(x, y, x + w - 1, y + h - 1, c)

    def hline(self, x, y, w, c):
        self.rect(x, y, w, 1, c)

    def vline(self, x, y, h, c):
        self.rect(x, y, 1, h, c)

    def blit(self, art, key, ox=0, oy=0):
        width = len(art[0])
        for y, row in enumerate(art):
            if len(row) != width:
                raise ValueError(f"ragged pixel art row {y}: {row!r}")
            for x, ch in enumerate(row):
                if ch not in " .":
                    self.set(ox + x, oy + y, key[ch])

    def png(self) -> bytes:
        return png_bytes(self.w, self.h, self.px)


# --------------------------------------------------------------------------------------------
# Palette (same as generate_assets.py)
# --------------------------------------------------------------------------------------------


def hexc(s: str, a: int = 255):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


WHITE, BLACK = hexc("FFFFFF"), hexc("000000")


def ramp(base: str) -> dict:
    b = hexc(base)
    return {"d2": mix(b, BLACK, 0.58), "d1": mix(b, BLACK, 0.32), "b": b, "l1": mix(b, WHITE, 0.38), "l2": mix(b, WHITE, 0.72)}


S = [hexc(c) for c in ("14171B", "1E2227", "2B2F36", "353A42", "40454E", "4A505A", "5C636E", "737B87")]
COPPER = ramp("D9824A")  # restorer accent (copper, matches the recipe); GUI accent 0xFFD9824A
SOUL = ramp("3FD0C9")    # mob card colour
SPARK = ramp("7FE9FF")   # repair spark (processing cyan)

# --------------------------------------------------------------------------------------------
# Block textures (frame layout of generate_assets.machine_frame, copper trim, no tier pips)
# --------------------------------------------------------------------------------------------


def corner_plates(c: Canvas) -> None:
    for cx, cy in ((0, 0), (14, 0), (0, 14), (14, 14)):
        c.set(cx, cy, COPPER["l1"]); c.set(cx + 1, cy, COPPER["b"])
        c.set(cx, cy + 1, COPPER["b"]); c.set(cx + 1, cy + 1, COPPER["d1"])


def frame(light) -> Canvas:
    c = Canvas(fill=S[3])
    for x in range(16):
        c.set(x, 0, S[6]); c.set(x, 1, COPPER["b"]); c.set(x, 14, S[2]); c.set(x, 15, S[1])
    c.set(2, 1, COPPER["l1"]); c.set(13, 1, COPPER["d1"])
    for y in range(2, 14):
        c.set(0, y, S[5]); c.set(1, y, S[4]); c.set(14, y, S[3]); c.set(15, y, S[2])
    for i in range(2, 14):
        c.set(i, 13, S[4]); c.set(13, i, S[4]); c.set(i, 2, S[2]); c.set(2, i, S[2])
    for y in (4, 10):
        c.set(1, y, S[7]); c.set(1, y + 1, S[2]); c.set(14, y, S[6]); c.set(14, y + 1, S[1])
    corner_plates(c)
    # Single status light in place of tier pips.
    c.set(7, 14, light); c.set(8, 14, light)
    return c


# Front motif (12x12 at 2,2): a Mob Card held in a clamp over a small anvil; the spark lights up while repairing.
RESTORER_FRONT = [
    "            ",
    " kKKKKKKKKk ",
    " KhCCCCCChK ",
    " KhCwwwwChK ",
    " KhCwssw.hK ",
    " KhCwwwwChK ",
    " KhCCCCCChK ",
    " kKKKKKKKKk ",
    "  aAAAAAAa  ",
    "   dAAAAd   ",
    "   aaAAaa   ",
    "  DDDDDDDD  ",
]


def front(on: bool) -> Canvas:
    c = frame(COPPER["l2"] if on else COPPER["d1"])
    key = {
        "K": S[6], "k": S[5], "h": S[1],
        "C": SOUL["b"] if on else SOUL["d1"],
        "w": mix(S[1], SOUL["d1"], 0.5) if on else S[1],
        "s": SPARK["l2"] if on else S[4],
        "A": COPPER["l1"] if on else COPPER["b"], "a": COPPER["d1"], "d": COPPER["d2"], "D": S[1],
    }
    c.blit(RESTORER_FRONT, key, 2, 2)
    if on:
        c.set(9, 6, SPARK["b"])
    return c


SIDE_PANEL = [
    "            ",
    " KKKKKKKKKK ",
    " hhhhhhhhhh ",
    "            ",
    " KKKKKKKKKK ",
    " hhhhhhhhhh ",
    "            ",
    " KKKKKKKKKK ",
    " hhhhhhhhhh ",
    "        LA  ",
    " mmmm   Aa  ",
    "            ",
]


def side() -> Canvas:
    c = frame(COPPER["b"])
    c.blit(SIDE_PANEL, {"K": mix(S[0], COPPER["d2"], 0.45), "h": S[4], "m": S[4],
                        "L": COPPER["l1"], "A": COPPER["b"], "a": COPPER["d1"]}, 2, 2)
    return c


TOP_CENTER = [
    "..aa..",
    ".aAAa.",
    "aAKKAa",
    "aAKKAa",
    ".aAAa.",
    "..aa..",
]


def top() -> Canvas:
    c = Canvas(fill=S[4])
    for i in range(16):
        c.set(i, 0, S[2]); c.set(i, 15, S[2]); c.set(0, i, S[2]); c.set(15, i, S[2])
    for i in range(1, 15):
        c.set(i, 1, S[6]); c.set(1, i, S[6]); c.set(i, 14, S[3]); c.set(14, i, S[3])
    for i in range(3, 13):
        c.set(i, 3, S[2]); c.set(i, 12, S[5]); c.set(3, i, S[2]); c.set(12, i, S[5])
    corner_plates(c)
    c.blit(TOP_CENTER, {"a": COPPER["d1"], "A": COPPER["b"], "K": S[0]}, 5, 5)
    return c


def bottom() -> Canvas:
    c = Canvas(fill=S[2])
    for i in range(16):
        c.set(i, 0, S[1]); c.set(i, 15, S[1]); c.set(0, i, S[1]); c.set(15, i, S[1])
    for i in range(2, 14):
        c.set(i, i, S[1]); c.set(15 - i, i, S[1])
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        c.set(x, y, S[5])
    c.fill(7, 7, 8, 8, COPPER["d2"])
    c.set(7, 7, COPPER["d1"])
    return c


# --------------------------------------------------------------------------------------------
# Models (geometry of generate_assets.frame_elements + a raised clamp ledge)
# --------------------------------------------------------------------------------------------


def face(texture, cull=None, uv=None):
    f = {}
    if uv is not None:
        f["uv"] = uv
    f["texture"] = texture
    if cull is not None:
        f["cullface"] = cull
    return f


def frame_elements() -> list:
    els = [
        {"from": [0, 0, 0], "to": [16, 2, 16], "faces": {
            "down": face("#bottom", "down"), "up": face("#bottom"),
            "north": face("#front", "north"), "south": face("#side", "south"),
            "west": face("#side", "west"), "east": face("#side", "east")}},
        {"from": [1, 2, 1], "to": [15, 14, 15], "faces": {
            "north": face("#front"), "south": face("#side"), "west": face("#side"), "east": face("#side")}},
    ]
    for x0, z0 in ((0, 0), (14, 0), (0, 14), (14, 14)):
        els.append({"from": [x0, 2, z0], "to": [x0 + 2, 14, z0 + 2], "faces": {
            "north": face("#front", "north") if z0 == 0 else face("#side"),
            "south": face("#side", "south") if z0 == 14 else face("#side"),
            "west": face("#side", "west") if x0 == 0 else face("#side"),
            "east": face("#side", "east") if x0 == 14 else face("#side")}})
    els.append({"from": [0, 14, 0], "to": [16, 16, 16], "faces": {
        "down": face("#bottom"), "up": face("#top", "up"),
        "north": face("#front", "north"), "south": face("#side", "south"),
        "west": face("#side", "west"), "east": face("#side", "east")}})
    return els


def detail_box(frm, to, u0, v0, u1, v1) -> dict:
    return {"from": frm, "to": to, "faces": {
        "north": face("#front", uv=[u0, v0, u1, v1]),
        "up": face("#front", uv=[u0, v0, u1, v0 + 1]),
        "down": face("#front", uv=[u0, v1 - 1, u1, v1]),
        "west": face("#front", uv=[u1 - 1, v0, u1, v1]),
        "east": face("#front", uv=[u0, v0, u0 + 1, v1])}}


def base_model() -> dict:
    # Raised anvil top (texture row 10, x 4..11): model x is mirrored on the north face (x = 16 - u).
    els = frame_elements() + [detail_box([4, 5, 0.5], [12, 6, 1], 4, 10, 12, 11)]
    return {"parent": "minecraft:block/block", "textures": {"particle": "#side"}, "elements": els}


def tex(path: str) -> str:
    return f"{NS}:{path}"


def child_model(on: bool) -> dict:
    return {"parent": tex(f"block/{BID}_base"), "textures": {
        "particle": tex(f"block/{BID}_side"),
        "front": tex(f"block/{BID}_front_on" if on else f"block/{BID}_front"),
        "side": tex(f"block/{BID}_side"),
        "top": tex(f"block/{BID}_top"),
        "bottom": tex(f"block/{BID}_bottom"),
    }}


def blockstate() -> dict:
    variants = {}
    for active in ("false", "true"):
        for facing, y in FACINGS:
            v = {"model": tex(f"block/{BID}_on" if active == "true" else f"block/{BID}")}
            if y:
                v["y"] = y
            variants[f"active={active},facing={facing}"] = v
    return {"variants": variants}


# --------------------------------------------------------------------------------------------
# GUI sheet (panel recipe of generate_gui_textures.panel, copper accent) + restorer sprites
# --------------------------------------------------------------------------------------------
OUTLINE = (16, 18, 22, 255)
BODY = (58, 63, 72, 255)
BODY_HI = (98, 106, 118, 255)
BODY_LO = (30, 33, 38, 255)
RECESS = (27, 30, 35, 255)
RECESS_DOT = (34, 38, 44, 255)
RECESS_DARK = (14, 16, 19, 255)
RECESS_LIGHT = (90, 97, 108, 255)
SLOT_FILL = (38, 42, 49, 255)
ACCENT = (217, 130, 74)
PANEL_W, PANEL_H = 176, 202
RECESS_BOX = (5, 17, 166, 98)
PLAYER_INV = (8, 120)


def rgba(rgb, a=255):
    return (rgb[0], rgb[1], rgb[2], a)


def shade(rgb, f):
    return tuple(max(0, min(255, int(c * f))) for c in rgb[:3])


def gmix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def inset(c, x, y, w, h, fill, dark=RECESS_DARK, light=RECESS_LIGHT):
    c.rect(x, y, w, h, fill)
    c.hline(x, y, w - 1, dark)
    c.vline(x, y, h - 1, dark)
    c.hline(x + 1, y + h - 1, w - 1, light)
    c.vline(x + w - 1, y + 1, h - 1, light)
    c.set(x + w - 1, y, rgba(gmix(dark, light, 0.5)))
    c.set(x, y + h - 1, rgba(gmix(dark, light, 0.5)))


def panel(c: Canvas, accent, seed) -> None:
    rnd = random.Random(seed)
    w, h = PANEL_W, PANEL_H
    for y in range(h):
        for x in range(w):
            f = 1.0 + (rnd.random() - 0.5) * 0.05 + (0.015 if (y // 2) % 2 == 0 else 0)
            c.set(x, y, rgba(shade(BODY, f)))
    c.hline(1, 0, w - 2, OUTLINE); c.hline(1, h - 1, w - 2, OUTLINE)
    c.vline(0, 1, h - 2, OUTLINE); c.vline(w - 1, 1, h - 2, OUTLINE)
    for (x, y) in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        c.set(x, y, (0, 0, 0, 0))
    c.hline(1, 1, w - 3, BODY_HI); c.vline(1, 1, h - 3, BODY_HI)
    c.hline(2, h - 2, w - 3, BODY_LO); c.vline(w - 2, 2, h - 3, BODY_LO)
    for y in range(3, 15):
        for x in range(3, w - 3):
            r, g, b, a = c.get(x, y)
            c.set(x, y, (int(r * 0.9), int(g * 0.9), int(b * 0.92), 255))
    c.hline(3, 15, w - 6, rgba(shade(accent, 0.75)))
    c.hline(3, 16, w - 6, BODY_LO)
    for (px, py) in ((3, 3), (w - 6, 3)):
        c.rect(px, py, 3, 3, rgba(accent))
        c.set(px + 2, py + 2, rgba(shade(accent, 0.6)))
        c.set(px, py, rgba(gmix(accent, (255, 255, 255), 0.5)))
    for (rx, ry) in ((3, h - 5), (w - 5, h - 5)):
        c.set(rx, ry, BODY_HI)
        c.set(rx + 1, ry, rgba(shade(BODY_HI, 1.2)))
        c.set(rx, ry + 1, BODY_LO)
        c.set(rx + 1, ry + 1, OUTLINE)
    x0, y0, rw, rh = RECESS_BOX
    inset(c, x0, y0, rw, rh, RECESS)
    for y in range(y0 + 4, y0 + rh - 2, 6):
        for x in range(x0 + 4, x0 + rw - 2, 6):
            c.set(x, y, RECESS_DOT)
    for y in (y0 + 8, y0 + rh - 9):
        c.hline(2, y, 2, rgba(shade(accent, 0.8)))
        c.hline(w - 4, y, 2, rgba(shade(accent, 0.8)))
    ix, iy = PLAYER_INV
    for row in range(3):
        for col in range(9):
            inset(c, ix - 1 + col * 18, iy - 1 + row * 18, 18, 18, SLOT_FILL)
    for col in range(9):
        inset(c, ix - 1 + col * 18, iy - 1 + 58, 18, 18, SLOT_FILL)
    c.hline(ix + 2, iy + 54 + 1, 9 * 18 - 6, rgba(shade(accent, 0.45)))


def horizontal_fill(c, ox, oy, w, h, light, mid, dark):
    for y in range(h):
        t = y / (h - 1)
        base = gmix(light, mid, t * 2) if t < 0.5 else gmix(mid, dark, (t - 0.5) * 2)
        for x in range(w):
            col = base
            if (x + y) % 6 in (0, 1):
                col = gmix(base, (255, 255, 255), 0.18)
            c.set(ox + x, oy + y, rgba(col))


ARROW = [
    "........................",
    "................o.......",
    "................oo......",
    "................ofo.....",
    "................offo....",
    "ooooooooooooooooofffo...",
    "offffffffffffffffffffo..",
    "offffffffffffffffffffffo",
    "offffffffffffffffffffffo",
    "offffffffffffffffffffo..",
    "ooooooooooooooooofffo...",
    "................offo....",
    "................ofo.....",
    "................oo......",
    "................o.......",
    "........................",
]


def arrow(c, ox, oy, fill, hi):
    for y, row in enumerate(ARROW):
        for x, ch in enumerate(row):
            if ch == "o":
                c.set(ox + x, oy + y, OUTLINE)
            elif ch == "f":
                col = hi if y in (6, 7) else fill
                c.set(ox + x, oy + y, rgba(col))


def gui_sheet() -> Canvas:
    c = Canvas(256, 256)
    panel(c, ACCENT, 13)
    # Integrity fills 128x7: intact (green), decaying (amber), corrupted (red).
    horizontal_fill(c, 0, 204, 128, 7, (190, 250, 200), (91, 224, 138), (40, 140, 80))
    horizontal_fill(c, 0, 212, 128, 7, (255, 222, 150), (255, 177, 59), (170, 110, 20))
    horizontal_fill(c, 0, 220, 128, 7, (255, 170, 160), (255, 90, 90), (160, 40, 40))
    # Arrows 24x16: empty, processing (cyan), warning (amber).
    arrow(c, 130, 204, (36, 40, 46), (44, 48, 55))
    arrow(c, 156, 204, (63, 216, 240), (170, 240, 255))
    arrow(c, 182, 204, (255, 177, 59), (255, 222, 150))
    return c


# --------------------------------------------------------------------------------------------
# Output, check and validation
# --------------------------------------------------------------------------------------------


def build() -> dict:
    out = {}

    def png(rel, canvas):
        out[ASSETS / "textures" / f"{rel}.png"] = canvas.png()

    def js(rel, data):
        out[ASSETS / f"{rel}.json"] = (json.dumps(data, indent=2) + "\n").encode("utf-8")

    png(f"block/{BID}_front", front(False))
    png(f"block/{BID}_front_on", front(True))
    png(f"block/{BID}_side", side())
    png(f"block/{BID}_top", top())
    png(f"block/{BID}_bottom", bottom())
    png(f"gui/{BID}", gui_sheet())
    js(f"models/block/{BID}_base", base_model())
    js(f"models/block/{BID}", child_model(False))
    js(f"models/block/{BID}_on", child_model(True))
    js(f"blockstates/{BID}", blockstate())
    js(f"models/item/{BID}", {"parent": tex(f"block/{BID}")})
    return out


def validate(out: dict) -> list:
    """Structural checks mirroring tools/validate_assets.py for the restorer (works without Pillow)."""
    errors = []

    def model(ref):
        p = ASSETS / "models" / f"{ref.split(':', 1)[1]}.json"
        return json.loads(out.get(p, p.read_bytes() if p.is_file() else b"null").decode("utf-8"))

    bs = json.loads(out[ASSETS / "blockstates" / f"{BID}.json"])
    expected = {f"active={a},facing={f}": y for a in ("false", "true") for f, y in FACINGS}
    if set(bs["variants"]) != set(expected):
        errors.append("blockstate variants mismatch")
    for key, v in bs["variants"].items():
        if v.get("y", 0) != expected.get(key):
            errors.append(f"blockstate {key}: wrong y")
        textures, elements, cur = {}, None, v["model"]
        while cur and cur != "minecraft:block/block":
            data = model(cur)
            if data is None:
                errors.append(f"missing model {cur}")
                break
            for k, t in data.get("textures", {}).items():
                textures.setdefault(k, t)
            if elements is None and "elements" in data:
                elements = data["elements"]
            cur = data.get("parent")
        for el in elements or []:
            for a, b in zip(el["from"], el["to"]):
                if not (-16 <= a <= 32 and -16 <= b <= 32) or a > b:
                    errors.append(f"{v['model']}: element out of range")
            for name, f in el["faces"].items():
                ref = f["texture"]
                while ref.startswith("#"):
                    if ref[1:] not in textures:
                        errors.append(f"{v['model']} face {name}: {ref} unresolved")
                        break
                    ref = textures[ref[1:]]
                for c in f.get("uv", []):
                    if not 0 <= c <= 16:
                        errors.append(f"{v['model']} face {name}: uv out of range")
        for k, ref in textures.items():
            if ref.startswith("#"):
                continue
            p = ASSETS / "textures" / f"{ref.split(':', 1)[1]}.png"
            data = out.get(p) or (p.read_bytes() if p.is_file() else None)
            if data is None:
                errors.append(f"{v['model']}: missing texture {ref}")
            elif png_header(data) != (16, 16, 8, 6):
                errors.append(f"{ref}: expected 16x16 RGBA, got {png_header(data)}")
    item = model(f"{NS}:item/{BID}")
    if item.get("parent") != tex(f"block/{BID}"):
        errors.append("item model parent mismatch")
    gui = out[ASSETS / "textures" / "gui" / f"{BID}.png"]
    if png_header(gui) != (256, 256, 8, 6):
        errors.append("gui sheet must be 256x256 RGBA")
    return errors


def main(argv) -> int:
    check = "--check" in argv
    out = build()
    stale = [p for p, data in sorted(out.items()) if not p.is_file() or p.read_bytes() != data]
    if not check:
        for p in stale:
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_bytes(out[p])
    errors = validate(out)
    for p in stale:
        print(("STALE " if check else "wrote ") + str(p.relative_to(ROOT)))
    for e in errors:
        print("ERROR " + e)
    print(f"card_restorer assets: {len(out)} files, {len(stale)} {'differ' if check else 'written'}, {len(errors)} errors")
    return 1 if errors or (check and stale) else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
