#!/usr/bin/env python3
"""Pixel-art images for the CurseForge description (pure Python, no Pillow).

Everything is drawn from the mod's own 16x16 textures plus a hand-made 5x7 pixel font in the style of the
game's UI text (own glyphs, not the game's font file). Output goes to curseforge/art/*.png; the description
links them from the GitHub repository (raw.githubusercontent.com/.../main/curseforge/art/...).

Usage (from the repo root):
    python tools/generate_curseforge_art.py           # write changed files
    python tools/generate_curseforge_art.py --check   # exit 1 if a file on disk differs
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_logo import read_png  # noqa: E402
from generate_restorer_assets import png_bytes  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/ultraloot/textures"
OUT = ROOT / "curseforge/art"
WIDTH = 960

GRAPHITE = (0x1E, 0x22, 0x28)
PANEL = (0x16, 0x19, 0x1E)
EDGE = (0x0E, 0x10, 0x13)
GOLD = (0xE8, 0xB2, 0x3A)
GOLD_TOP = (0xFF, 0xE3, 0x7A)
GOLD_BOTTOM = (0xE0, 0x8C, 0x14)
WHITE = (0xFF, 0xFF, 0xFF)
GRAY = (0xAA, 0xAA, 0xAA)
DARK_GRAY = (0x6E, 0x74, 0x7C)
AQUA = (0x55, 0xFF, 0xFF)
GREEN = (0x55, 0xFF, 0x55)
YELLOW = (0xFF, 0xFF, 0x55)
BLUE = (0x78, 0x9C, 0xFF)

# ---------------------------------------------------------------------------------------------
# Pixel font: 7 rows above the baseline, an optional 8th row is the descender. '#' = ink.
# ---------------------------------------------------------------------------------------------
GLYPHS = {
    "A": ".###./#...#/#...#/#####/#...#/#...#/#...#",
    "B": "####./#...#/#...#/####./#...#/#...#/####.",
    "C": ".###./#...#/#..../#..../#..../#...#/.###.",
    "D": "####./#...#/#...#/#...#/#...#/#...#/####.",
    "E": "#####/#..../#..../####./#..../#..../#####",
    "F": "#####/#..../#..../####./#..../#..../#....",
    "G": ".####/#..../#..../#..##/#...#/#...#/.###.",
    "H": "#...#/#...#/#...#/#####/#...#/#...#/#...#",
    "I": "###/.#./.#./.#./.#./.#./###",
    "J": "....#/....#/....#/....#/....#/#...#/.###.",
    "K": "#...#/#..#./#.#../##.../#.#../#..#./#...#",
    "L": "#..../#..../#..../#..../#..../#..../#####",
    "M": "#...#/##.##/#.#.#/#...#/#...#/#...#/#...#",
    "N": "#...#/##..#/#.#.#/#..##/#...#/#...#/#...#",
    "O": ".###./#...#/#...#/#...#/#...#/#...#/.###.",
    "P": "####./#...#/#...#/####./#..../#..../#....",
    "Q": ".###./#...#/#...#/#...#/#...#/#..#./.##.#",
    "R": "####./#...#/#...#/####./#...#/#...#/#...#",
    "S": ".####/#..../#..../.###./....#/....#/####.",
    "T": "#####/..#../..#../..#../..#../..#../..#..",
    "U": "#...#/#...#/#...#/#...#/#...#/#...#/.###.",
    "V": "#...#/#...#/#...#/#...#/.#.#./.#.#./..#..",
    "W": "#...#/#...#/#...#/#.#.#/#.#.#/##.##/#...#",
    "X": "#...#/#...#/.#.#./..#../.#.#./#...#/#...#",
    "Y": "#...#/#...#/.#.#./..#../..#../..#../..#..",
    "Z": "#####/....#/...#./..#../.#.../#..../#####",
    "a": "...../...../.###./....#/.####/#...#/.####",
    "b": "#..../#..../#.##./##..#/#...#/#...#/####.",
    "c": "...../...../.###./#...#/#..../#...#/.###.",
    "d": "....#/....#/.##.#/#..##/#...#/#...#/.####",
    "e": "...../...../.###./#...#/#####/#..../.####",
    "f": "..##/.#../####/.#../.#../.#../.#..",
    "g": "...../...../.####/#...#/#...#/.####/....#/####.",
    "h": "#..../#..../#.##./##..#/#...#/#...#/#...#",
    "i": "#/./#/#/#/#/#",
    "j": "....#/...../....#/....#/....#/....#/#...#/.###.",
    "k": "#.../#.../#..#/#.#./##../#.#./#..#",
    "l": "#./#./#./#./#./#./.#",
    "m": "...../...../##.#./#.#.#/#.#.#/#...#/#...#",
    "n": "...../...../####./#...#/#...#/#...#/#...#",
    "o": "...../...../.###./#...#/#...#/#...#/.###.",
    "p": "...../...../#.##./##..#/#...#/####./#..../#....",
    "q": "...../...../.##.#/#..##/#...#/.####/....#/....#",
    "r": "...../...../#.##./##..#/#..../#..../#....",
    "s": "...../...../.####/#..../.###./....#/####.",
    "t": "..../.#../####/.#../.#../.#../..##",
    "u": "...../...../#...#/#...#/#...#/#...#/.####",
    "v": "...../...../#...#/#...#/#...#/.#.#./..#..",
    "w": "...../...../#...#/#...#/#.#.#/#.#.#/.####",
    "x": "...../...../#...#/.#.#./..#../.#.#./#...#",
    "y": "...../...../#...#/#...#/#...#/.####/....#/####.",
    "z": "...../...../#####/...#./..#../.#.../#####",
    "0": ".###./#...#/#..##/#.#.#/##..#/#...#/.###.",
    "1": "..#../.##../..#../..#../..#../..#../#####",
    "2": ".###./#...#/....#/..##./.#.../#..../#####",
    "3": ".###./#...#/....#/..##./....#/#...#/.###.",
    "4": "...#./..##./.#.#./#..#./#####/...#./...#.",
    "5": "#####/#..../####./....#/....#/#...#/.###.",
    "6": "..##./.#.../#..../####./#...#/#...#/.###.",
    "7": "#####/#...#/....#/...#./..#../..#../..#..",
    "8": ".###./#...#/#...#/.###./#...#/#...#/.###.",
    "9": ".###./#...#/#...#/.####/....#/...#./.##..",
    " ": ".../.../.../.../.../.../...",
    ".": "././././././#",
    ",": "././././././#/#",
    ":": "././#/./././#",
    ";": "././#/./././#/#",
    "!": "#/#/#/#/#/./#",
    "?": ".###./#...#/....#/...#./..#../...../..#..",
    "'": "#/#/././././.",
    "\"": "#.#/#.#/.../.../.../.../...",
    "-": "..../..../..../####/..../..../....",
    "+": "...../..#../..#../#####/..#../..#../.....",
    "=": "...../...../#####/...../#####/...../.....",
    "/": "....#/...#./...#./..#../.#.../.#.../#....",
    "(": "..#/.#./#../#../#../.#./..#",
    ")": "#../.#./..#/..#/..#/.#./#..",
    "[": "###/#../#../#../#../#../###",
    "]": "###/..#/..#/..#/..#/..#/###",
    "&": ".##../#..#./.##../.##.#/#..#./#..#./.##.#",
    "%": "#...#/#..#./...#./..#../.#.../.#..#/#...#",
    "#": ".#.#./.#.#./#####/.#.#./#####/.#.#./.#.#.",
    ">": "#..../.#.../..#../...#./..#../.#.../#....",
    "<": "....#/...#./..#../.#.../..#../...#./....#",
    "|": "#/#/#/#/#/#/#",
    "·": "./././#/././.",
    "→": "...../..#../...#./#####/...#./..#../.....",
}
ALIASES = {"–": "-", "—": "-", "×": "x"}
# Accented letters: base glyph + a 2-row mark drawn above it (rows -2..-1 for capitals, rows 0..1 for lowercase).
ACCENTS = {"acute": "...#./..#..", "grave": ".#.../..#..", "circ": "..#../.#.#.", "tilde": ".##.#/#.##."}
ACCENTED = {
    "á": ("a", "acute"), "à": ("a", "grave"), "â": ("a", "circ"), "ã": ("a", "tilde"),
    "é": ("e", "acute"), "ê": ("e", "circ"), "ó": ("o", "acute"), "ô": ("o", "circ"), "õ": ("o", "tilde"),
    "ú": ("u", "acute"), "Á": ("A", "acute"), "Ã": ("A", "tilde"), "É": ("E", "acute"), "Ê": ("E", "circ"),
    "Ó": ("O", "acute"), "Õ": ("O", "tilde"), "Ú": ("U", "acute"),
}
SPECIAL = {
    "í": ".#/../#./#./#./#./#.",
    "ç": "...../...../.###./#...#/#..../#...#/.###./..#..",
    "Ç": ".###./#...#/#..../#..../#..../#...#/.###./..#..",
}


def glyph(ch: str) -> tuple[int, list[tuple[int, int]]]:
    """(advance width, ink cells as (x, row)); row may be negative for accents on capitals."""
    ch = ALIASES.get(ch, ch)
    if ch in SPECIAL:
        rows = SPECIAL[ch].split("/")
        return len(rows[0]), [(x, y) for y, r in enumerate(rows) for x, c in enumerate(r) if c == "#"]
    if ch in ACCENTED:
        base, mark = ACCENTED[ch]
        width, cells = glyph(base)
        top = -2 if base.isupper() else 0
        mark_rows = ACCENTS[mark].split("/")
        cells = list(cells) + [(x, top + y) for y, r in enumerate(mark_rows) for x, c in enumerate(r) if c == "#"]
        return width, cells
    rows = GLYPHS[ch].split("/")
    return len(rows[0]), [(x, y) for y, r in enumerate(rows) for x, c in enumerate(r) if c == "#"]


def text_width(s: str, scale: int) -> int:
    return (sum(glyph(c)[0] + 1 for c in s) - 1) * scale if s else 0


# ---------------------------------------------------------------------------------------------
# Canvas
# ---------------------------------------------------------------------------------------------
class Canvas:
    def __init__(self, w: int, h: int, color=(0, 0, 0, 0)):
        self.w, self.h = w, h
        self.px = [tuple(color) if len(color) == 4 else (*color, 255)] * (w * h)

    def put(self, x: int, y: int, c) -> None:
        if not (0 <= x < self.w and 0 <= y < self.h):
            return
        a = c[3] if len(c) == 4 else 255
        if a == 255:
            self.px[y * self.w + x] = (c[0], c[1], c[2], 255)
        elif a > 0:
            r, g, b, da = self.px[y * self.w + x]
            out_a = a + da * (255 - a) // 255
            if out_a == 0:
                return
            mix = lambda s, d: (s * a + d * da * (255 - a) // 255) // out_a  # noqa: E731
            self.px[y * self.w + x] = (mix(c[0], r), mix(c[1], g), mix(c[2], b), out_a)

    def rect(self, x: int, y: int, w: int, h: int, c) -> None:
        for yy in range(max(0, y), min(self.h, y + h)):
            for xx in range(max(0, x), min(self.w, x + w)):
                self.put(xx, yy, c)

    def blit(self, other: "Canvas", ox: int, oy: int) -> None:
        for y in range(other.h):
            for x in range(other.w):
                c = other.px[y * other.w + x]
                if c[3]:
                    self.put(ox + x, oy + y, c)

    def texture(self, tex: tuple, ox: int, oy: int, scale: int, shade: float = 1.0) -> None:
        w, h, pixels = tex
        for ty in range(h):
            for tx in range(w):
                c = pixels[ty * w + tx]
                if c[3]:
                    self.rect(ox + tx * scale, oy + ty * scale, scale, scale, shaded(c, shade))

    def text(self, s: str, x: int, y: int, scale: int, color, shadow: bool = True, gradient=None) -> int:
        """Draws s with its top-left at (x, y); gradient = (top, bottom) colours over the 7 cap rows."""
        for layer in ((1, True), (0, False)) if shadow else ((0, False),):
            off, is_shadow = layer
            cx = x
            for ch in s:
                width, cells = glyph(ch)
                for gx, gy in cells:
                    if gradient:
                        t = min(max(gy, 0), 6) / 6
                        col = tuple(round(a + (b - a) * t) for a, b in zip(gradient[0], gradient[1]))
                    else:
                        col = color
                    if is_shadow:
                        col = shaded(col, 0.25)
                    self.rect(cx + (gx + off) * scale, y + (gy + off) * scale, scale, scale, col)
                cx += (width + 1) * scale
        return cx - x - scale

    def png(self) -> bytes:
        return png_bytes(self.w, self.h, self.px)


def shaded(c, f: float):
    return (round(c[0] * f), round(c[1] * f), round(c[2] * f), c[3] if len(c) == 4 else 255)


_TEX_CACHE: dict[str, tuple] = {}


def tex(name: str) -> tuple:
    if name not in _TEX_CACHE:
        folder = "block" if (TEX / "block" / f"{name}.png").exists() else "item"
        _TEX_CACHE[name] = read_png(TEX / folder / f"{name}.png")
    return _TEX_CACHE[name]


# ---------------------------------------------------------------------------------------------
# Building blocks
# ---------------------------------------------------------------------------------------------
def iso_block(top: str, front: str, side: str, k: int) -> Canvas:
    """Isometric cube (32k x 32k) from three 16x16 textures; k = screen pixels per texel step."""
    # World (X, Y, Z) in texels -> screen: sx = (16 + X - Z) k, sy = ((X + Z) / 2 + 16 - Y) k.
    # Visible faces: top (Y = 16), front (Z = 16, lower left) and side (X = 16, lower right).
    faces = {"top": tex(top), "front": tex(front), "side": tex(side)}
    size = 32 * k
    c = Canvas(size, size)
    for sy in range(size):
        for sx in range(size):
            p, q = (sx + 0.5) / k - 16, (sy + 0.5) / k
            sample = None
            X, Z = q + p / 2, q - p / 2
            if 0 <= X < 16 and 0 <= Z < 16:
                sample = ("top", int(X), int(Z), 1.0)
            elif p < 0:
                X = p + 16
                Y = (X + 16) / 2 + 16 - q
                if 0 <= X < 16 and 0 <= Y < 16:
                    sample = ("front", int(X), int(16 - Y), 0.82)
            else:
                Z = 16 - p
                Y = (16 + Z) / 2 + 16 - q
                if 0 <= Z < 16 and 0 <= Y < 16:
                    sample = ("side", int(p), int(16 - Y), 0.62)
            if sample:
                face, u, v, shade = sample
                w, h, pixels = faces[face]
                u, v = min(u, 15), min(v, 15)
                col = pixels[v * w + u]
                if col[3]:
                    c.px[sy * size + sx] = shaded(col, shade)
    return c


def frame(c: Canvas, x: int, y: int, w: int, h: int, fill=GRAPHITE, gold: int = 4) -> None:
    c.rect(x, y, w, h, EDGE)
    c.rect(x + 2, y + 2, w - 4, h - 4, GOLD)
    c.rect(x + 2 + gold, y + 2 + gold, w - 4 - 2 * gold, h - 4 - 2 * gold, fill)


def tooltip(lines: list[tuple[str, tuple]], scale: int = 3) -> Canvas:
    """Item-tooltip style box: near-black fill, purple gradient border, one line per entry."""
    pad, gap = 4 * scale, 3 * scale
    line_h = 8 * scale
    w = max(text_width(s, scale) for s, _ in lines) + 2 * pad + 3 * scale
    h = len(lines) * line_h + (len(lines) - 1) * gap + 2 * pad + scale * 2
    c = Canvas(w + 2 * scale, h + 2 * scale)
    bg = (0x10, 0x00, 0x10, 240)
    s = scale
    c.rect(s, 0, w, h + 2 * s, bg)
    c.rect(0, s, w + 2 * s, h, bg)
    top, bottom = (0x50, 0x00, 0xFF), (0x28, 0x00, 0x7F)
    for yy in range(s, h + s):
        t = (yy - s) / max(1, h - 1)
        col = tuple(round(a + (b - a) * t) for a, b in zip(top, bottom))
        c.rect(s, yy, s, 1, col)
        c.rect(w, yy, s, 1, col)
    c.rect(s, s, w, s, top)
    c.rect(s, h, w, s, bottom)
    y = pad + s
    for i, (line, color) in enumerate(lines):
        c.text(line, pad + s, y, scale, color)
        y += line_h + gap + (s * 2 if i == 0 else 0)
    return c


def slot(c: Canvas, x: int, y: int, scale: int) -> None:
    """18x18 inventory slot (dark top-left edge, light bottom-right edge)."""
    c.rect(x, y, 18 * scale, 18 * scale, (0x37, 0x37, 0x37))
    c.rect(x + scale, y + scale, 17 * scale, 17 * scale, (0xFF, 0xFF, 0xFF))
    c.rect(x + scale, y + scale, 16 * scale, 16 * scale, (0x8B, 0x8B, 0x8B))


def background(c: Canvas, name: str, scale: int, dim: float) -> None:
    t = tex(name)
    step = 16 * scale
    for ty in range(0, c.h, step):
        for tx in range(0, c.w, step):
            c.texture(t, tx, ty, scale, dim)


# ---------------------------------------------------------------------------------------------
# Images
# ---------------------------------------------------------------------------------------------
def banner() -> Canvas:
    c = Canvas(WIDTH, 320, GRAPHITE)
    background(c, "loot_generator_bottom", 6, 0.28)
    for y in range(c.h):  # vignette towards top and bottom
        fade = abs(y - c.h / 2) / (c.h / 2)
        c.rect(0, y, c.w, 1, (*EDGE, int(170 * fade ** 2)))
    frame_edges(c)
    loot = iso_block("loot_generator_t4_top", "loot_generator_t4_front_on", "loot_generator_t4_side", 5)
    mob = iso_block("mob_loot_generator_t4_top", "mob_loot_generator_t4_front_on", "mob_loot_generator_t4_side", 5)
    c.blit(loot, 24, 88)
    c.blit(mob, WIDTH - 24 - mob.w, 88)
    title = "ULTRA LOOT"
    tw = text_width(title, 9)
    c.text(title, (WIDTH - tw) // 2, 76, 9, GOLD, gradient=(GOLD_TOP, GOLD_BOTTOM))
    sub = "Energy in. Loot out."
    c.text(sub, (WIDTH - text_width(sub, 4)) // 2, 168, 4, WHITE)
    badges = ["MC 1.21.1", "NeoForge", "Forge Energy"]
    widths = [text_width(b, 3) + 24 for b in badges]
    x = (WIDTH - sum(widths) - 16 * (len(badges) - 1)) // 2
    for b, w in zip(badges, widths):
        c.rect(x, 226, w, 36, EDGE)
        c.rect(x + 3, 229, w - 6, 30, (0x2A, 0x2F, 0x37))
        c.text(b, x + 12, 234, 3, YELLOW)
        x += w + 16
    return c


def frame_edges(c: Canvas) -> None:
    c.rect(0, 0, c.w, 2, EDGE)
    c.rect(0, c.h - 2, c.w, 2, EDGE)
    c.rect(0, 2, c.w, 4, GOLD)
    c.rect(0, c.h - 6, c.w, 4, GOLD)
    c.rect(0, 6, c.w, 2, EDGE)
    c.rect(0, c.h - 8, c.w, 2, EDGE)


def header(title: str, subtitle: str, icon: Canvas) -> Canvas:
    c = Canvas(WIDTH, 128, PANEL)
    c.rect(0, 0, WIDTH, 128, EDGE)
    c.rect(2, 2, WIDTH - 4, 124, PANEL)
    c.rect(2, 2, 10, 124, GOLD)
    c.rect(12, 2, 2, 124, EDGE)
    c.rect(14, 2, WIDTH - 16, 2, (0x2A, 0x2F, 0x37))
    c.blit(icon, 30 + (96 - icon.w) // 2, (128 - icon.h) // 2)
    c.text(title, 146, 24, 6, GOLD, gradient=(GOLD_TOP, GOLD_BOTTOM))
    c.text(subtitle, 148, 82, 3, GRAY)
    return c


def item_icon(name: str, scale: int = 6) -> Canvas:
    t = tex(name)
    c = Canvas(16 * scale, 16 * scale)
    c.texture(t, 0, 0, scale)
    return c


def machine(prefix: str, tier: int | None, k: int) -> Canvas:
    t = f"_t{tier}" if tier else ""
    return iso_block(f"{prefix}{t}_top", f"{prefix}{t}_front_on", f"{prefix}{t}_side", k)


def tiers() -> Canvas:
    c = Canvas(WIDTH, 250, PANEL)
    c.rect(0, 0, WIDTH, 250, EDGE)
    c.rect(2, 2, WIDTH - 4, 246, PANEL)
    numerals = ["I", "II", "III", "IV"]
    k, step = 3, 108
    for g, (prefix, label) in enumerate((("loot_generator", "Loot Generator"),
                                         ("mob_loot_generator", "Mob Loot Generator"))):
        gx = 36 + g * 468
        c.text(label, gx + (4 * step - 12 - text_width(label, 3)) // 2, 24, 3, WHITE)
        for i in range(4):
            block = machine(prefix, i + 1, k)
            bx = gx + i * step
            c.blit(block, bx, 70)
            n = numerals[i]
            c.text(n, bx + (block.w - text_width(n, 4)) // 2, 190, 4, GOLD, gradient=(GOLD_TOP, GOLD_BOTTOM))
    c.rect(WIDTH // 2 - 1, 24, 2, 200, (0x2A, 0x2F, 0x37))
    return c


def upgrades() -> Canvas:
    kinds = [("speed", "Speed"), ("quantity", "Quantity"), ("range", "Range"),
             ("efficiency", "Efficiency"), ("loot", "Loot")]
    scale, col_w = 4, 168
    c = Canvas(WIDTH, 360, PANEL)
    c.rect(0, 0, WIDTH, 360, EDGE)
    c.rect(2, 2, WIDTH - 4, 356, PANEL)
    x0 = 90
    for row, numeral in enumerate(["I", "II", "III"]):
        y = 74 + row * 92
        c.text(numeral, 36 + (24 - text_width(numeral, 4)) // 2, y + 22, 4, GOLD, gradient=(GOLD_TOP, GOLD_BOTTOM))
    for i, (kind, label) in enumerate(kinds):
        cx = x0 + i * col_w + col_w // 2
        c.text(label, cx - text_width(label, 3) // 2, 26, 3, WHITE)
        for row in range(3):
            sx, sy = cx - 9 * scale, 74 + row * 92
            slot(c, sx, sy, scale)
            c.texture(tex(f"{kind}_card_{row + 1}"), sx + scale, sy + scale, scale)
    return c


def panel(parts: list[Canvas], gap: int = 48) -> Canvas:
    """Dark strip with the parts laid out left to right, vertically centred, the row centred."""
    h = max(p.h for p in parts) + 56
    c = Canvas(WIDTH, h, PANEL)
    c.rect(0, 0, WIDTH, h, EDGE)
    c.rect(2, 2, WIDTH - 4, h - 4, PANEL)
    total = sum(p.w for p in parts) + gap * (len(parts) - 1)
    x = (WIDTH - total) // 2
    for p in parts:
        c.blit(p, x, (h - p.h) // 2)
        x += p.w + gap
    return c


def label(s: str, scale: int, color) -> Canvas:
    c = Canvas(text_width(s, scale) + scale, 9 * scale)
    c.text(s, 0, 0, scale, color)
    return c


def stack(parts: list[Canvas], gap: int = 12) -> Canvas:
    """Parts on top of each other, centred horizontally."""
    w = max(p.w for p in parts)
    c = Canvas(w, sum(p.h for p in parts) + gap * (len(parts) - 1))
    y = 0
    for p in parts:
        c.blit(p, (w - p.w) // 2, y)
        y += p.h + gap
    return c


def card_restorer_panel() -> Canvas:
    arrow = label("→", 6, GOLD)
    worn_icon = item_icon("mob_card", 5)
    rot = (0x6B, 0x4A, 0x2A)
    for i, col in enumerate(worn_icon.px):  # same card, faded towards a rotten brown with a few dead texels
        if col[3]:
            tx, ty = (i % worn_icon.w) // 5, (i // worn_icon.w) // 5
            f = 0.75 if (tx * 7 + ty * 13) % 5 == 0 else 0.5
            worn_icon.px[i] = tuple(round(c * (1 - f) + r * f) for c, r in zip(col[:3], rot)) + (col[3],)
    worn = stack([worn_icon, label("Worn card", 3, GRAY)])
    fixed = stack([item_icon("mob_card", 5), label("Good as new", 3, GREEN)])
    restorer = stack([machine("card_restorer", None, 4), label("+ FE", 3, YELLOW)])
    return panel([worn, arrow, restorer, arrow, fixed], gap=36)


def scene_machines() -> Canvas:
    """In-game shot of the three machines on Mekanism cables, cropped above the chat and below the toast."""
    w, h, pixels = read_png(ROOT / "curseforge/screenshots/01_machines.png")
    top, bottom = 72, 296
    c = Canvas(w + 8, bottom - top + 8, EDGE)
    for y in range(top, bottom):
        for x in range(w):
            c.px[(y - top + 4) * c.w + x + 4] = pixels[y * w + x]
    return c


def divider() -> Canvas:
    c = Canvas(WIDTH, 24)
    c.rect(0, 10, WIDTH, 4, EDGE)
    c.rect(0, 11, WIDTH, 2, GOLD)
    for x in range(WIDTH // 2 - 60, WIDTH // 2 + 61, 30):
        c.rect(x - 6, 6, 12, 12, EDGE)
        c.rect(x - 4, 8, 8, 8, GOLD)
    return c


IMAGES = {
    "banner": banner,
    "header_loot_generator": lambda: header("LOOT GENERATOR", "Loot tables, rolled straight into your chests",
                                            machine("loot_generator", 4, 3)),
    "header_mob_loot_generator": lambda: header("MOB LOOT GENERATOR", "Mob drops from a card. No mob is spawned.",
                                                machine("mob_loot_generator", 4, 3)),
    "header_card_restorer": lambda: header("CARD WEAR & RESTORER", "Cards wear out. Energy brings them back.",
                                           machine("card_restorer", None, 3)),
    "header_upgrades": lambda: header("UPGRADE CARDS", "Five kinds, three levels each", item_icon("speed_card_3")),
    "header_quality": lambda: header("QUALITY OF LIFE", "A guide book, a clear status line, six-face config",
                                     item_icon("guide_book")),
    "header_compat": lambda: header("COMPATIBILITY", "Any Forge Energy mod. Tested with Mekanism.",
                                    item_icon("machine_frame")),
    "header_portuguese": lambda: header("EM PORTUGUÊS", "Tradução completa e livro-guia em português",
                                        item_icon("blank_mob_card")),
    "panel_loot_generator": lambda: panel([machine("loot_generator", 4, 5), tooltip([
        ("Loot Generator IV", AQUA), ("66 loot profiles", GRAY), ("Up to 4 rolls per operation", GRAY),
        ("16 block range", GRAY), ("Never drops or voids items", GREEN)])], gap=64),
    "panel_mob_loot_generator": lambda: panel([machine("mob_loot_generator", 4, 5), tooltip([
        ("Mob Loot Generator IV", AQUA), ("Reward tiers I-III", GRAY), ("Paid with FE and XP", GRAY),
        ("27-slot output", GRAY), ("Uses the mob's own loot table", GREEN)])], gap=64),
    "panel_card_restorer": card_restorer_panel,
    "tiers": tiers,
    "upgrades": upgrades,
    "divider": divider,
    "scene_machines": scene_machines,
}


def main(argv: list[str]) -> int:
    check = "--check" in argv
    only = [a for a in argv if not a.startswith("--")]
    stale = []
    for name, make in IMAGES.items():
        if only and name not in only:
            continue
        data = make().png()
        path = OUT / f"{name}.png"
        if path.exists() and path.read_bytes() == data:
            continue
        stale.append(path)
        if not check:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
    print(f"curseforge art: {len(stale)} {'would change' if check else 'written'}")
    for path in stale:
        print("  " + str(path.relative_to(ROOT)))
    return 1 if check and stale else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
