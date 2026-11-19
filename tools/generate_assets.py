#!/usr/bin/env python3
"""Ultra Loot asset generator.

Deterministically (re)generates every block/item texture, block model, blockstate and item model
owned by the asset work package (see docs/ASSET_SPEC.md). All pixel art is original and is drawn
here from a fixed palette plus small hand-authored pixel grids; nothing is copied from other mods.

Usage (from the repo root):
    python tools/generate_assets.py           # write/overwrite all generated files
    python tools/generate_assets.py --check   # exit 1 if any file on disk differs from the generator

Model geometry is authored here as element lists (no Blockbench). Re-running reproduces
byte-identical PNG and JSON files.
"""
from __future__ import annotations

import io
import json
import math
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "ultraloot"
NS = "ultraloot"

Color = tuple[int, int, int, int]

# --------------------------------------------------------------------------------------------
# Registry ids (contract with the Java registries)
# --------------------------------------------------------------------------------------------

FAMILIES = ("loot_generator", "mob_loot_generator")
TIER_NUMBERS = (1, 2, 3, 4)
BLOCK_IDS = tuple(f"{fam}_t{t}" for fam in FAMILIES for t in TIER_NUMBERS)

CARD_CATEGORIES = ("speed", "quantity", "range", "efficiency", "loot")
CARD_LEVELS = (1, 2, 3)
CARD_IDS = tuple(f"{cat}_card_{lvl}" for cat in CARD_CATEGORIES for lvl in CARD_LEVELS)
CIRCUIT_IDS = ("logic_circuit", "enhanced_circuit", "refined_circuit", "stellar_circuit")
ITEM_IDS = CARD_IDS + (
    "blank_card",
    "mob_card",
    "blank_mob_card",
    "experience_capsule",
) + CIRCUIT_IDS + ("machine_frame", "loot_core", "soul_core")

# Item models with more than one layer: id -> ordered layer texture names.
LAYERED_ITEMS = {"mob_card": ("mob_card", "mob_card_overlay")}

FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))

# --------------------------------------------------------------------------------------------
# Palette
# --------------------------------------------------------------------------------------------


def hexc(s: str, a: int = 255) -> Color:
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def mix(a: Color, b: Color, t: float) -> Color:
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))  # type: ignore[return-value]


WHITE = hexc("FFFFFF")
BLACK = hexc("000000")
CLEAR: Color = (0, 0, 0, 0)


def ramp(base: str) -> dict[str, Color]:
    """Five-step shading ramp around a base colour."""
    b = hexc(base)
    return {
        "d2": mix(b, BLACK, 0.58),
        "d1": mix(b, BLACK, 0.32),
        "b": b,
        "l1": mix(b, WHITE, 0.38),
        "l2": mix(b, WHITE, 0.72),
    }


# Graphite steel body, darkest -> brightest (#2B2F36 .. #4A505A is the core range).
S = [hexc(c) for c in ("14171B", "1E2227", "2B2F36", "353A42", "40454E", "4A505A", "5C636E", "737B87")]

FAMILY_ACCENT = {"loot_generator": ramp("E8B23A"), "mob_loot_generator": ramp("3FD0C9")}
TIER = {1: ramp("C8C8C8"), 2: ramp("F2C94C"), 3: ramp("3DDC84"), 4: ramp("C9A0FF")}
T4_GLOW = hexc("F3E6FF")

CARD_COLOR = {
    "speed": ramp("3EC8F0"),
    "quantity": ramp("F08A2E"),
    "range": ramp("5BD25B"),
    "efficiency": ramp("F2D93A"),
    "loot": ramp("E04FC8"),
}
SOUL = FAMILY_ACCENT["mob_loot_generator"]
GOLD = FAMILY_ACCENT["loot_generator"]
CONTACT = ramp("E0B04A")

# --------------------------------------------------------------------------------------------
# Canvas helpers
# --------------------------------------------------------------------------------------------


class Canvas:
    def __init__(self, w: int = 16, h: int = 16, fill: Color = CLEAR):
        self.w, self.h = w, h
        self.img = Image.new("RGBA", (w, h), fill)
        self.px = self.img.load()

    def set(self, x: int, y: int, c: Color) -> None:
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[x, y] = c

    def get(self, x: int, y: int) -> Color:
        return self.px[x, y]

    def fill(self, x0: int, y0: int, x1: int, y1: int, c: Color) -> None:
        """Fill an inclusive rectangle."""
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, c)

    def blit(self, art: list[str], key: dict[str, Color], ox: int = 0, oy: int = 0) -> None:
        """Draw a pixel grid; ' ' and '.' leave the pixel untouched, every other char must be keyed."""
        width = len(art[0])
        for y, row in enumerate(art):
            if len(row) != width:
                raise ValueError(f"ragged pixel art row {y}: {row!r}")
            for x, ch in enumerate(row):
                if ch in " .":
                    continue
                self.set(ox + x, oy + y, key[ch])

    def png(self) -> bytes:
        buf = io.BytesIO()
        self.img.save(buf, format="PNG")
        return buf.getvalue()


# --------------------------------------------------------------------------------------------
# Machine block textures
# --------------------------------------------------------------------------------------------
# Face layout (texture space, y down), matched by the model elements:
#   rows 0-1   top cap   (row 1 = tier trim stripe)
#   rows 14-15 base plate (row 14 carries the tier pips)
#   cols 0-1 / 14-15 corner pillars, 2x2 tier corner plates at the four corners
#   x,y 2..13  recessed body panel (motif drawn into it)


def tier_pip_xs(tier: int) -> list[int]:
    width = 3 * tier - 1
    x0 = (16 - width) // 2
    xs: list[int] = []
    for i in range(tier):
        xs += [x0 + 3 * i, x0 + 3 * i + 1]
    return xs


def corner_plates(c: Canvas, tier: int) -> None:
    tr = TIER[tier]
    for cx, cy in ((0, 0), (14, 0), (0, 14), (14, 14)):
        c.set(cx, cy, T4_GLOW if tier == 4 else tr["l1"])
        c.set(cx + 1, cy, tr["b"])
        c.set(cx, cy + 1, tr["b"])
        c.set(cx + 1, cy + 1, tr["d1"])


def machine_frame(tier: int, pip_color: Color) -> Canvas:
    tr = TIER[tier]
    c = Canvas(fill=S[3])
    for x in range(16):
        c.set(x, 0, S[6])
        c.set(x, 1, tr["b"])
        c.set(x, 14, S[2])
        c.set(x, 15, S[1])
    c.set(2, 1, tr["l1"])
    c.set(13, 1, tr["d1"])
    for y in range(2, 14):
        c.set(0, y, S[5])
        c.set(1, y, S[4])
        c.set(14, y, S[3])
        c.set(15, y, S[2])
    # Recessed panel: shadow under the cap / beside the left pillar, light on the opposite edges.
    for i in range(2, 14):
        c.set(i, 13, S[4])
        c.set(13, i, S[4])
        c.set(i, 2, S[2])
        c.set(2, i, S[2])
    # Pillar rivets.
    for y in (4, 10):
        c.set(1, y, S[7])
        c.set(1, y + 1, S[2])
        c.set(14, y, S[6])
        c.set(14, y + 1, S[1])
    corner_plates(c, tier)
    for x in tier_pip_xs(tier):
        c.set(x, 14, pip_color)
    return c


LOOT_FRONT = [
    "            ",
    "  aAAAAAAa  ",
    " aLLLLLLLLa ",
    " AppppppppA ",
    " AppppppppA ",
    " aAAAmmAAAa ",
    " ApppMMpppA ",
    " ApppmmpppA ",
    " ApKGFFGKpA ",
    " AppggggppA ",
    " DaaaaaaaaD ",
    "            ",
]

MOB_FRONT = [
    "            ",
    " sSSSSSSSSs ",
    " SkkggggkkS ",
    " SkbkcckbkS ",
    " SkbcGGcbkS ",
    " SkbGLGGbkS ",
    " SkbGGGGbkS ",
    " SkbcGGcbkS ",
    " SkbkcckbkS ",
    " SkkggggkkS ",
    " sSSSSSSSSs ",
    "            ",
]

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

LOOT_TOP_CENTER = [
    "aAAAAa",
    "AKKKKA",
    "AaaaaA",
    "AKKKKA",
    "AaaaaA",
    "aAAAAa",
]

MOB_TOP_CENTER = [
    ".cccc.",
    "cKggKc",
    "cgLGgc",
    "cgGGgc",
    "cKggKc",
    ".cccc.",
]


def machine_front(family: str, tier: int, on: bool) -> Canvas:
    tr = TIER[tier]
    acc = FAMILY_ACCENT[family]
    c = machine_frame(tier, tr["l2"] if on else tr["d1"])
    if family == "loot_generator":
        panel = mix(S[2], acc["d2"], 0.35)
        if on:
            panel = mix(panel, acc["d1"], 0.3)
        key = {
            "a": acc["d1"], "A": mix(acc["b"], acc["l1"], 0.4) if on else acc["b"],
            "L": acc["l2"] if on else acc["l1"], "D": acc["d2"],
            "p": panel, "m": S[5], "M": S[6], "K": S[0],
            "G": acc["l1"] if on else mix(acc["d2"], S[1], 0.4),
            "F": acc["l2"] if on else mix(acc["d2"], S[1], 0.2),
            "g": mix(panel, acc["b"], 0.55) if on else panel,
        }
        c.blit(LOOT_FRONT, key, 2, 2)
    else:
        key = {
            "S": S[6], "s": S[5], "k": S[1], "b": S[6],
            "c": acc["b"] if on else acc["d2"],
            "G": acc["l1"] if on else acc["d1"],
            "L": acc["l2"] if on else acc["b"],
            "g": mix(S[1], acc["d1"], 0.6) if on else S[1],
        }
        c.blit(MOB_FRONT, key, 2, 2)
    return c


def machine_side(family: str, tier: int) -> Canvas:
    acc = FAMILY_ACCENT[family]
    c = machine_frame(tier, TIER[tier]["b"])
    key = {
        "K": mix(S[0], acc["d2"], 0.45), "h": S[4], "m": S[4],
        "L": acc["l1"], "A": acc["b"], "a": acc["d1"],
    }
    c.blit(SIDE_PANEL, key, 2, 2)
    return c


def machine_top(family: str, tier: int) -> Canvas:
    acc = FAMILY_ACCENT[family]
    c = Canvas(fill=S[4])
    for i in range(16):
        c.set(i, 0, S[2]); c.set(i, 15, S[2]); c.set(0, i, S[2]); c.set(15, i, S[2])
    for i in range(1, 15):
        c.set(i, 1, S[6]); c.set(1, i, S[6]); c.set(i, 14, S[3]); c.set(14, i, S[3])
    for i in range(3, 13):  # engraved groove ring
        c.set(i, 3, S[2]); c.set(i, 12, S[5]); c.set(3, i, S[2]); c.set(12, i, S[5])
    corner_plates(c, tier)
    if family == "loot_generator":
        key = {"a": acc["d1"], "A": acc["b"], "K": S[0]}
        c.blit(LOOT_TOP_CENTER, key, 5, 5)
    else:
        key = {"c": acc["d1"], "K": S[0], "g": acc["d2"], "G": acc["b"], "L": acc["l1"]}
        c.blit(MOB_TOP_CENTER, key, 5, 5)
    return c


def machine_bottom(family: str) -> Canvas:
    acc = FAMILY_ACCENT[family]
    c = Canvas(fill=S[2])
    for i in range(16):
        c.set(i, 0, S[1]); c.set(i, 15, S[1]); c.set(0, i, S[1]); c.set(15, i, S[1])
    for i in range(2, 14):  # cross bracing
        c.set(i, i, S[1]); c.set(15 - i, i, S[1])
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        c.set(x, y, S[5])
    c.fill(7, 7, 8, 8, acc["d2"])
    c.set(7, 7, acc["d1"])
    return c


def machine_crystal(on: bool) -> Canvas:
    tr = TIER[4]
    c = Canvas()
    facets = [tr["l1"], tr["b"], tr["b"], tr["d1"]]
    for y in range(8):
        for x in range(16):
            col = facets[x % 4]
            col = mix(col, WHITE, 0.35 - 0.05 * y) if y < 3 else mix(col, tr["d2"], 0.06 * (y - 3))
            c.set(x, y, col)
    top = [
        [tr["l1"], tr["l1"], tr["b"], tr["b"]],
        [tr["l1"], tr["l2"], tr["l1"], tr["b"]],
        [tr["b"], tr["l1"], tr["b"], tr["d1"]],
        [tr["b"], tr["b"], tr["d1"], tr["d1"]],
    ]
    for y in range(8, 16):
        for x in range(16):
            c.set(x, y, top[(y - 8) % 4][x % 4])
    if on:
        for y in range(16):
            for x in range(16):
                c.set(x, y, mix(c.get(x, y), T4_GLOW, 0.45))
        c.set(5, 11, WHITE)
        c.set(5, 1, WHITE)
    return c


# --------------------------------------------------------------------------------------------
# Item textures
# --------------------------------------------------------------------------------------------

CARD_BASE = [
    "................",
    "..OOOOOOOOOO....",
    "..OBBBBBBBBBO...",
    "..OCCCCCCCCCCO..",
    "..OHHHHHHHHHHO..",
    "..OHKKKKKKKKmO..",
    "..OHKWWWWWWKmO..",
    "..OHKwwwwwwKmO..",
    "..OHKWWWWWWKmO..",
    "..OHKwwwwwwKmO..",
    "..OHKKKKKKKKmO..",
    "..OHPPPPPPPPmO..",
    "..OHPPPPPPPPmO..",
    "..OssssssssssO..",
    "..OOOOOOOOOOOO..",
    "................",
]
CARD_WINDOW = (5, 6)  # top-left of the 6x4 screen interior
CARD_PIPS_Y = 11

CARD_GLYPHS = {
    "speed": ["##.##.", ".##.##", ".##.##", "##.##."],
    "quantity": ["..##..", "######", "######", "..##.."],
    "range": [".#..#.", "##++##", "##++##", ".#..#."],
    "efficiency": ["#####.", "#+#+##", "#+#+##", "#####."],
    "loot": [".##++.", "###+++", ".##++.", "..#+.."],
}

MOB_EMBLEM = [".####.", "#d##d#", "######", ".#d#d."]


def card_canvas(band_light: Color, band: Color, window_border: Color) -> Canvas:
    c = Canvas()
    key = {
        "O": S[0], "B": band_light, "C": band, "H": S[6], "m": S[4], "s": S[2],
        "K": window_border, "W": hexc("14181D"), "w": hexc("191E24"), "P": S[1],
    }
    c.blit(CARD_BASE, key)
    return c


def card_pips(c: Canvas, level: int, pal: dict[str, Color]) -> None:
    xs = {1: [7], 2: [5, 9], 3: [4, 7, 10]}[level]
    for x in xs:
        c.set(x, CARD_PIPS_Y, pal["l2"]); c.set(x + 1, CARD_PIPS_Y, pal["l2"])
        c.set(x, CARD_PIPS_Y + 1, pal["l1"]); c.set(x + 1, CARD_PIPS_Y + 1, pal["b"])


def upgrade_card(category: str, level: int) -> Canvas:
    pal = CARD_COLOR[category]
    c = card_canvas(pal["l1"], pal["b"], S[1])
    c.blit(CARD_GLYPHS[category], {"#": pal["l1"], "+": pal["b"]}, *CARD_WINDOW)
    card_pips(c, level, pal)
    return c


def blank_card() -> Canvas:
    c = card_canvas(S[7], S[6], S[1])
    wx, wy = CARD_WINDOW
    for x, y in ((wx + 1, wy + 2), (wx + 2, wy + 1), (wx + 3, wy)):
        c.set(x, y, S[4])
    return c


def mob_card_base(filled: bool) -> Canvas:
    c = card_canvas(SOUL["l1"], SOUL["b"], SOUL["d2"])
    wx, wy = CARD_WINDOW
    if filled:
        c.fill(4, CARD_PIPS_Y, 11, CARD_PIPS_Y, SOUL["l1"])
        c.fill(4, CARD_PIPS_Y + 1, 11, CARD_PIPS_Y + 1, SOUL["b"])
    else:
        # Empty soul window: dotted ghost outline of the slot, nothing captured.
        for y in range(wy, wy + 4):
            for x in range(wx, wx + 6):
                edge = x in (wx, wx + 5) or y in (wy, wy + 3)
                if edge and (x + y) % 2 == 0:
                    c.set(x, y, SOUL["d1"])
        c.fill(4, CARD_PIPS_Y, 11, CARD_PIPS_Y + 1, S[1])
        c.set(4, CARD_PIPS_Y + 1, SOUL["d2"])
    return c


def mob_card_overlay() -> Canvas:
    c = Canvas()
    c.blit(MOB_EMBLEM, {"#": hexc("E6E6E6"), "d": hexc("5E5E5E")}, *CARD_WINDOW)
    return c


EXPERIENCE_CAPSULE = [
    "................",
    ".....OOOOOO.....",
    "....OMMMMmsO....",
    "....OmmmmmsO....",
    "....OSSSSSSO....",
    "....OwggggGO....",
    "....OwggggGO....",
    "....OwXEEEeO....",
    "....OwExEEeO....",
    "....OWEEExeO....",
    "....OwEEEEeO....",
    "....OwEEEeeO....",
    "....OSSSSSSO....",
    "....OmmmmmsO....",
    ".....OOOOOO.....",
    "................",
]


def experience_capsule() -> Canvas:
    c = Canvas()
    key = {
        "O": S[0], "M": S[7], "m": S[5], "s": S[3], "S": S[2],
        "w": (225, 245, 255, 210), "g": (150, 190, 205, 120), "G": (100, 130, 145, 160),
        "E": hexc("7CD84A"), "e": hexc("3F8F24"), "x": hexc("B8F57A"), "X": hexc("EFFFD0"),
        "W": (240, 255, 240, 255),
    }
    c.blit(EXPERIENCE_CAPSULE, key)
    return c


def circuit(tier: int) -> Canvas:
    tr = TIER[tier]
    board, board_hi, board_lo, edge = hexc("24302B"), hexc("31413A"), hexc("1A231F"), hexc("0E1311")
    c = Canvas()
    c.fill(1, 2, 14, 13, edge)
    c.fill(2, 3, 13, 12, board)
    c.fill(2, 3, 13, 3, board_hi)
    c.fill(2, 3, 2, 12, board_hi)
    c.fill(2, 12, 13, 12, board_lo)
    c.fill(13, 3, 13, 12, board_lo)
    trace, via = tr["b"], (T4_GLOW if tier == 4 else tr["l1"])
    for x, y in ((4, 8), (3, 8), (3, 7), (3, 6), (3, 5),     # left
                 (11, 7), (12, 7), (12, 8), (12, 9),          # right
                 (7, 4), (6, 4),                              # top
                 (8, 11), (9, 11), (10, 11)):                 # bottom
        c.set(x, y, trace)
    for x, y in ((3, 4), (5, 4), (12, 10), (11, 11)):
        c.set(x, y, via)
    # Chip with pins and lit core.
    c.fill(6, 6, 9, 9, S[5])
    c.fill(7, 7, 8, 8, S[1])
    c.set(7, 7, T4_GLOW if tier == 4 else tr["l1"])
    c.set(8, 7, tr["b"]); c.set(7, 8, tr["b"]); c.set(8, 8, tr["d1"])
    for x, y in ((7, 5), (8, 10), (5, 8), (10, 7)):
        c.set(x, y, S[6])
    # Edge connector contacts.
    for x in (3, 5, 7, 9, 11):
        c.set(x, 13, CONTACT["l1"])
        c.set(x, 14, CONTACT["d1"])
    if tier == 4:
        for x, y in ((6, 5), (9, 10), (5, 9), (10, 6)):
            c.set(x, y, mix(tr["d1"], board, 0.3))
    return c


def machine_frame_item() -> Canvas:
    c = Canvas()
    c.fill(1, 1, 14, 14, S[0])
    c.fill(2, 2, 13, 13, S[5])
    c.fill(2, 2, 13, 2, S[6]); c.fill(2, 2, 2, 13, S[6])
    c.fill(2, 13, 13, 13, S[3]); c.fill(13, 2, 13, 13, S[3])
    c.fill(4, 4, 11, 11, S[1])  # inner lip
    c.fill(5, 5, 10, 10, CLEAR)
    for i in range(6):  # diagonal braces across the opening
        c.set(5 + i, 5 + i, S[4])
        c.set(10 - i, 5 + i, S[4])
    tr = TIER[1]
    for cx, cy in ((2, 2), (12, 2), (2, 12), (12, 12)):
        c.set(cx, cy, tr["l1"]); c.set(cx + 1, cy, tr["b"])
        c.set(cx, cy + 1, tr["b"]); c.set(cx + 1, cy + 1, tr["d1"])
    return c


def core_item(pal: dict[str, Color], kind: str) -> Canvas:
    c = Canvas()
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            if d > 7.3:
                continue
            if d > 6.4:
                c.set(x, y, S[0])
            elif d > 4.7:
                c.set(x, y, S[6] if dx + dy < -2 else (S[3] if dx + dy > 2 else S[4]))
            elif d > 3.9:
                c.set(x, y, S[1])
            else:
                q = math.hypot(x - 6.0, y - 6.0) / 4.4
                tone = "l2" if q < 0.25 else "l1" if q < 0.55 else "b" if q < 0.9 else "d1" if q < 1.2 else "d2"
                c.set(x, y, pal[tone])
    # Clamps holding the core in the ring.
    for x, y in ((7, 3), (8, 3), (7, 12), (8, 12), (3, 7), (3, 8), (12, 7), (12, 8)):
        c.set(x, y, S[6])
    if kind == "loot":  # faceted gem lines
        for y in range(16):
            for x in range(16):
                m = abs(x - cx) + abs(y - cy)
                if m == 3 and c.get(x, y) in pal.values():
                    c.set(x, y, mix(c.get(x, y), pal["d2"], 0.5))
        c.set(6, 6, WHITE)
    else:  # soul swirl
        for x, y in ((6, 5), (7, 5), (8, 6), (8, 7), (7, 8), (6, 8), (6, 9), (7, 10), (8, 10)):
            c.set(x, y, pal["l2"])
        c.set(9, 9, pal["d2"])
    return c


# --------------------------------------------------------------------------------------------
# Models and blockstates
# --------------------------------------------------------------------------------------------


def face(texture: str, cull: str | None = None, uv: list[float] | None = None) -> dict:
    f: dict = {}
    if uv is not None:
        f["uv"] = uv
    f["texture"] = texture
    if cull is not None:
        f["cullface"] = cull
    return f


def frame_elements() -> list[dict]:
    """Shared machine frame: base plate, recessed body, four corner pillars, top cap (16x16x16)."""
    els = [
        {"from": [0, 0, 0], "to": [16, 2, 16], "faces": {
            "down": face("#bottom", "down"), "up": face("#bottom"),
            "north": face("#front", "north"), "south": face("#side", "south"),
            "west": face("#side", "west"), "east": face("#side", "east")}},
        {"from": [1, 2, 1], "to": [15, 14, 15], "faces": {
            "north": face("#front"), "south": face("#side"),
            "west": face("#side"), "east": face("#side")}},
    ]
    for x0, z0 in ((0, 0), (14, 0), (0, 14), (14, 14)):
        faces = {
            "north": face("#front", "north") if z0 == 0 else face("#side"),
            "south": face("#side", "south") if z0 == 14 else face("#side"),
            "west": face("#side", "west") if x0 == 0 else face("#side"),
            "east": face("#side", "east") if x0 == 14 else face("#side"),
        }
        els.append({"from": [x0, 2, z0], "to": [x0 + 2, 14, z0 + 2], "faces": faces})
    els.append({"from": [0, 14, 0], "to": [16, 16, 16], "faces": {
        "down": face("#bottom"), "up": face("#top", "up"),
        "north": face("#front", "north"), "south": face("#side", "south"),
        "west": face("#side", "west"), "east": face("#side", "east")}})
    return els


def detail_box(frm: list[float], to: list[float], u0: int, v0: int, u1: int, v1: int) -> dict:
    """Small relief element on the front face; every face samples the matching front-texture pixels."""
    return {"from": frm, "to": to, "faces": {
        "north": face("#front", uv=[u0, v0, u1, v1]),
        "up": face("#front", uv=[u0, v0, u1, v0 + 1]),
        "down": face("#front", uv=[u0, v1 - 1, u1, v1]),
        "west": face("#front", uv=[u1 - 1, v0, u1, v1]),
        "east": face("#front", uv=[u0, v0, u0 + 1, v1])}}


def family_details(family: str) -> list[dict]:
    if family == "loot_generator":
        # Chest-lid seam ledge (texture row 7, x 3..12) and the latch plate (x 7..8, rows 7..9).
        return [
            detail_box([3, 8, 0.5], [13, 9, 1], 3, 7, 13, 8),
            detail_box([7, 6, 0.25], [9, 9, 0.5], 7, 7, 9, 10),
        ]
    # Raised cage bars over the soul core (texture columns 5 and 10, rows 5..10).
    return [
        detail_box([10, 5, 0.5], [11, 11, 1], 5, 5, 6, 11),
        detail_box([5, 5, 0.5], [6, 11, 1], 10, 5, 11, 11),
    ]


def crown_elements() -> list[dict]:
    """T4 only: steel pedestal on the cap with a 45-degree nether-star crystal."""
    return [
        {"from": [5, 16, 5], "to": [11, 17, 11], "faces": {
            "up": face("#top", uv=[5, 5, 11, 11]),
            "north": face("#side", uv=[5, 0, 11, 1]), "south": face("#side", uv=[5, 0, 11, 1]),
            "west": face("#side", uv=[5, 0, 11, 1]), "east": face("#side", uv=[5, 0, 11, 1])}},
        {"from": [6, 17, 6], "to": [10, 22, 10],
         "rotation": {"angle": 45, "axis": "y", "origin": [8, 17, 8]},
         "faces": {
             "up": face("#crystal", uv=[4, 10, 8, 14]),
             "north": face("#crystal", uv=[4, 0, 8, 5]), "south": face("#crystal", uv=[4, 0, 8, 5]),
             "west": face("#crystal", uv=[4, 0, 8, 5]), "east": face("#crystal", uv=[4, 0, 8, 5])}},
    ]


def parent_name(family: str, tier: int) -> str:
    return f"{family}_base_t4" if tier == 4 else f"{family}_base"


def parent_model(family: str, crowned: bool) -> dict:
    els = frame_elements() + family_details(family)
    if crowned:
        els += crown_elements()
    return {"parent": "minecraft:block/block", "textures": {"particle": "#side"}, "elements": els}


def tex(path: str) -> str:
    return f"{NS}:{path}"


def child_model(family: str, tier: int, on: bool) -> dict:
    bid = f"{family}_t{tier}"
    textures = {
        "particle": tex(f"block/{bid}_side"),
        "front": tex(f"block/{bid}_front_on" if on else f"block/{bid}_front"),
        "side": tex(f"block/{bid}_side"),
        "top": tex(f"block/{bid}_top"),
        "bottom": tex(f"block/{family}_bottom"),
    }
    if tier == 4:
        textures["crystal"] = tex("block/machine_crystal_on" if on else "block/machine_crystal")
    return {"parent": tex(f"block/{parent_name(family, tier)}"), "textures": textures}


def blockstate(bid: str) -> dict:
    variants: dict = {}
    for active in ("false", "true"):
        for facing, y in FACINGS:
            v: dict = {"model": tex(f"block/{bid}_on" if active == "true" else f"block/{bid}")}
            if y:
                v["y"] = y
            variants[f"active={active},facing={facing}"] = v
    return {"variants": variants}


# --------------------------------------------------------------------------------------------
# Output
# --------------------------------------------------------------------------------------------


def build() -> dict[Path, bytes]:
    out: dict[Path, bytes] = {}

    def png(rel: str, canvas: Canvas) -> None:
        out[ASSETS / "textures" / f"{rel}.png"] = canvas.png()

    def js(rel: str, data: dict) -> None:
        out[ASSETS / f"{rel}.json"] = (json.dumps(data, indent=2) + "\n").encode("utf-8")

    # Block textures
    for fam in FAMILIES:
        png(f"block/{fam}_bottom", machine_bottom(fam))
        for t in TIER_NUMBERS:
            bid = f"{fam}_t{t}"
            png(f"block/{bid}_front", machine_front(fam, t, False))
            png(f"block/{bid}_front_on", machine_front(fam, t, True))
            png(f"block/{bid}_side", machine_side(fam, t))
            png(f"block/{bid}_top", machine_top(fam, t))
    png("block/machine_crystal", machine_crystal(False))
    png("block/machine_crystal_on", machine_crystal(True))

    # Item textures
    for cat in CARD_CATEGORIES:
        for lvl in CARD_LEVELS:
            png(f"item/{cat}_card_{lvl}", upgrade_card(cat, lvl))
    png("item/blank_card", blank_card())
    png("item/mob_card", mob_card_base(True))
    png("item/mob_card_overlay", mob_card_overlay())
    png("item/blank_mob_card", mob_card_base(False))
    png("item/experience_capsule", experience_capsule())
    for i, cid in enumerate(CIRCUIT_IDS, start=1):
        png(f"item/{cid}", circuit(i))
    png("item/machine_frame", machine_frame_item())
    png("item/loot_core", core_item(GOLD, "loot"))
    png("item/soul_core", core_item(SOUL, "soul"))

    # Block models, blockstates, block item models
    for fam in FAMILIES:
        js(f"models/block/{fam}_base", parent_model(fam, False))
        js(f"models/block/{fam}_base_t4", parent_model(fam, True))
        for t in TIER_NUMBERS:
            bid = f"{fam}_t{t}"
            js(f"models/block/{bid}", child_model(fam, t, False))
            js(f"models/block/{bid}_on", child_model(fam, t, True))
            js(f"blockstates/{bid}", blockstate(bid))
            js(f"models/item/{bid}", {"parent": tex(f"block/{bid}")})

    # Generated item models
    for iid in ITEM_IDS:
        layers = LAYERED_ITEMS.get(iid, (iid,))
        js(f"models/item/{iid}", {
            "parent": "minecraft:item/generated",
            "textures": {f"layer{i}": tex(f"item/{name}") for i, name in enumerate(layers)},
        })
    return out


def main(argv: list[str]) -> int:
    check = "--check" in argv
    out = build()
    stale = []
    for path, data in sorted(out.items()):
        current = path.read_bytes() if path.exists() else None
        if current == data:
            continue
        if check:
            stale.append(path)
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    if check:
        for p in stale:
            print(f"STALE {p.relative_to(ROOT)}")
        print(f"{len(out)} generated files checked, {len(stale)} differ")
        return 1 if stale else 0
    print(f"{len(out)} files generated under {ASSETS.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
