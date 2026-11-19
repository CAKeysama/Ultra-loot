"""Generates the Ultra Loot GUI textures (original pixel art) into assets/ultraloot/textures/gui/.

Outputs (all 256x256 sheets, read by client/screen/GuiSprites.java):
  loot_generator.png      panel 176x202 at (0,0), treasure-gold accent
  mob_loot_generator.png  panel 176x202 at (0,0), soul-teal accent
  widgets.png             shared sprites (slot, tabs, bars, buttons, side overlay, icons)

Run from the repo root:  python tools/generate_gui_textures.py
The atlas coordinates below must stay in sync with GuiSprites.java.
"""
from __future__ import annotations

import math
import random
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/ultraloot/textures/gui"

# ---- Palette (graphite steel, see docs/ASSET_SPEC.md) -------------------------------------------------
OUTLINE = (16, 18, 22, 255)
BODY = (58, 63, 72, 255)
BODY_HI = (98, 106, 118, 255)
BODY_LO = (30, 33, 38, 255)
TAB_BODY = (46, 50, 58, 255)
RECESS = (27, 30, 35, 255)
RECESS_DOT = (34, 38, 44, 255)
RECESS_DARK = (14, 16, 19, 255)
RECESS_LIGHT = (90, 97, 108, 255)
SLOT_FILL = (38, 42, 49, 255)
BAR_BG = (18, 20, 24, 255)
BAR_TICK = (42, 46, 53, 255)

ACCENT_LOOT = (232, 178, 58)
ACCENT_MOB = (63, 208, 201)

PANEL_W, PANEL_H = 176, 202
RECESS_BOX = (5, 17, 166, 98)  # x, y, w, h -> content area 5..170 x 17..114
PLAYER_INV = (8, 120)


def rgba(rgb, a=255):
    return (rgb[0], rgb[1], rgb[2], a)


def shade(rgb, f):
    return tuple(max(0, min(255, int(c * f))) for c in rgb[:3])


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


class Canvas:
    def __init__(self, w=256, h=256):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.px = self.img.load()

    def set(self, x, y, c):
        if 0 <= x < self.img.width and 0 <= y < self.img.height:
            self.px[x, y] = c if len(c) == 4 else rgba(c)

    def rect(self, x, y, w, h, c):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.set(xx, yy, c)

    def hline(self, x, y, w, c):
        self.rect(x, y, w, 1, c)

    def vline(self, x, y, h, c):
        self.rect(x, y, 1, h, c)

    def inset(self, x, y, w, h, fill, dark=RECESS_DARK, light=RECESS_LIGHT):
        """Recessed box: dark top/left edge, light bottom/right edge."""
        self.rect(x, y, w, h, fill)
        self.hline(x, y, w - 1, dark)
        self.vline(x, y, h - 1, dark)
        self.hline(x + 1, y + h - 1, w - 1, light)
        self.vline(x + w - 1, y + 1, h - 1, light)
        self.set(x + w - 1, y, mix(dark, light, 0.5))
        self.set(x, y + h - 1, mix(dark, light, 0.5))

    def slot(self, x, y):
        self.inset(x, y, 18, 18, SLOT_FILL)

    def pattern(self, ox, oy, rows, palette):
        for j, row in enumerate(rows):
            if len(row) != len(rows[0]):
                raise ValueError(f"row {j} has width {len(row)}: {row!r}")
            for i, ch in enumerate(row):
                if ch != ".":
                    self.set(ox + i, oy + j, palette[ch])

    def save(self, name):
        OUT.mkdir(parents=True, exist_ok=True)
        self.img.save(OUT / name)
        print("wrote", OUT / name)


# ---- Panels --------------------------------------------------------------------------------------------

def panel(accent, seed):
    rnd = random.Random(seed)
    c = Canvas()
    w, h = PANEL_W, PANEL_H
    # Body with a faint brushed-steel grain.
    for y in range(h):
        for x in range(w):
            f = 1.0 + (rnd.random() - 0.5) * 0.05 + (0.015 if (y // 2) % 2 == 0 else 0)
            c.set(x, y, rgba(shade(BODY, f)))
    # Outline with clipped corners.
    c.hline(1, 0, w - 2, OUTLINE)
    c.hline(1, h - 1, w - 2, OUTLINE)
    c.vline(0, 1, h - 2, OUTLINE)
    c.vline(w - 1, 1, h - 2, OUTLINE)
    for (x, y) in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        c.set(x, y, (0, 0, 0, 0))
    # Bevel.
    c.hline(1, 1, w - 3, BODY_HI)
    c.vline(1, 1, h - 3, BODY_HI)
    c.hline(2, h - 2, w - 3, BODY_LO)
    c.vline(w - 2, 2, h - 3, BODY_LO)
    # Title band: slightly darker, closed by an accent rule.
    for y in range(3, 15):
        for x in range(3, w - 3):
            r, g, b, a = c.px[x, y]
            c.set(x, y, (int(r * 0.9), int(g * 0.9), int(b * 0.92), 255))
    c.hline(3, 15, w - 6, rgba(shade(accent, 0.75)))
    c.hline(3, 16, w - 6, BODY_LO)
    # Accent corner plates in the title band.
    for (px, py) in ((3, 3), (w - 6, 3)):
        c.rect(px, py, 3, 3, rgba(accent))
        c.set(px + 2, py + 2, rgba(shade(accent, 0.6)))
        c.set(px, py, rgba(mix(accent, (255, 255, 255), 0.5)))
    # Rivets at the bottom corners.
    for (rx, ry) in ((3, h - 5), (w - 5, h - 5)):
        c.set(rx, ry, BODY_HI)
        c.set(rx + 1, ry, rgba(shade(BODY_HI, 1.2)))
        c.set(rx, ry + 1, BODY_LO)
        c.set(rx + 1, ry + 1, OUTLINE)
    # Content recess with a faint technical dot grid.
    x0, y0, rw, rh = RECESS_BOX
    c.inset(x0, y0, rw, rh, RECESS)
    for y in range(y0 + 4, y0 + rh - 2, 6):
        for x in range(x0 + 4, x0 + rw - 2, 6):
            c.set(x, y, RECESS_DOT)
    # Accent ticks beside the recess.
    for y in (y0 + 8, y0 + rh - 9):
        c.hline(2, y, 2, rgba(shade(accent, 0.8)))
        c.hline(w - 4, y, 2, rgba(shade(accent, 0.8)))
    # Player inventory and hotbar frames.
    ix, iy = PLAYER_INV
    for row in range(3):
        for col in range(9):
            c.slot(ix - 1 + col * 18, iy - 1 + row * 18)
    for col in range(9):
        c.slot(ix - 1 + col * 18, iy - 1 + 58)
    # Accent groove between inventory and hotbar.
    c.hline(ix + 2, iy + 54 + 1, 9 * 18 - 6, rgba(shade(accent, 0.45)))
    return c


# ---- Widgets -------------------------------------------------------------------------------------------

def tab(c, ox, oy, w, h, selected):
    body = BODY if selected else TAB_BODY
    open_cols = 3 if selected else 0  # selected tab covers the panel's left border
    tab_w = w - open_cols
    c.rect(ox + 1, oy + 1, w - 1, h - 2, body)
    # Outline on top, left and bottom with clipped outer corners.
    c.hline(ox + 1, oy, tab_w - 1, OUTLINE)
    c.hline(ox + 1, oy + h - 1, tab_w - 1, OUTLINE)
    c.vline(ox, oy + 1, h - 2, OUTLINE)
    hi = BODY_HI if selected else shade(BODY_HI, 0.85)
    c.hline(ox + 1, oy + 1, w - 1, rgba(hi))
    c.vline(ox + 1, oy + 1, h - 3, rgba(hi))
    c.hline(ox + 2, oy + h - 2, w - 2, BODY_LO)
    if selected:
        # The column over the panel outline keeps the outline at the very top and bottom only.
        c.set(ox + tab_w, oy, OUTLINE)
        c.set(ox + tab_w, oy + h - 1, OUTLINE)
        for xx in range(ox + tab_w + 1, ox + w):
            c.set(xx, oy, (0, 0, 0, 0))
            c.set(xx, oy + h - 1, (0, 0, 0, 0))
        c.set(ox + w - 1, oy + h - 2, body)
    else:
        c.vline(ox + w - 1, oy + 1, h - 2, BODY_LO)


def bar_frame(c, ox, oy, w, h):
    c.inset(ox, oy, w, h, BAR_BG)
    for y in range(oy + h - 1 - 8, oy, -8):
        c.hline(ox + w - 4, y, 2, BAR_TICK)


def vertical_fill(c, ox, oy, w, h, top, bottom):
    for y in range(h):
        base = mix(top, bottom, y / (h - 1))
        for x in range(w):
            col = base
            if x == 0:
                col = mix(base, (255, 255, 255), 0.35)
            elif x == w - 1:
                col = shade(base, 0.7)
            if y % 4 == 3:
                col = shade(col, 0.78)
            c.set(ox + x, oy + y, rgba(col))


def horizontal_fill(c, ox, oy, w, h, light, mid, dark):
    for y in range(h):
        t = y / (h - 1)
        base = mix(light, mid, t * 2) if t < 0.5 else mix(mid, dark, (t - 0.5) * 2)
        for x in range(w):
            col = base
            if (x + y) % 6 in (0, 1):
                col = mix(base, (255, 255, 255), 0.18)
            c.set(ox + x, oy + y, rgba(col))


def button(c, ox, oy, w, h, state):
    if state == "normal":
        top, bottom, hi, lo, edge = (82, 89, 100), (62, 68, 77), (118, 126, 138), (40, 44, 51), OUTLINE
    elif state == "hover":
        top, bottom, hi, lo, edge = (98, 106, 118), (74, 81, 91), (168, 176, 188), (48, 52, 60), (196, 204, 214, 255)
    elif state == "disabled":
        top, bottom, hi, lo, edge = (48, 52, 58), (44, 48, 54), (58, 63, 70), (34, 37, 42), (24, 26, 30, 255)
    else:  # selected / pressed
        top, bottom, hi, lo, edge = (28, 31, 36), (36, 40, 46), (22, 24, 28), (84, 91, 102), OUTLINE
    for y in range(h):
        col = mix(top, bottom, y / (h - 1))
        c.hline(ox, oy + y, w, rgba(col))
    c.hline(ox + 1, oy + 1, w - 2, rgba(hi))
    c.vline(ox + 1, oy + 1, h - 2, rgba(hi))
    c.hline(ox + 1, oy + h - 2, w - 2, rgba(lo))
    c.vline(ox + w - 2, oy + 1, h - 2, rgba(lo))
    c.hline(ox + 1, oy, w - 2, edge)
    c.hline(ox + 1, oy + h - 1, w - 2, edge)
    c.vline(ox, oy + 1, h - 2, edge)
    c.vline(ox + w - 1, oy + 1, h - 2, edge)


def thumb(c, ox, oy, state):
    button(c, ox, oy, 8, 15, state)
    grip = {"normal": (150, 158, 170), "hover": (210, 216, 224), "disabled": (70, 75, 84)}[state]
    for y in (oy + 5, oy + 7, oy + 9):
        c.hline(ox + 2, y, 4, rgba(grip))


def side_overlay(c, ox, oy):
    """Frame drawn over the side mode colour: opaque outline, translucent bevel, open centre."""
    c.hline(ox + 1, oy, 16, OUTLINE)
    c.hline(ox + 1, oy + 17, 16, OUTLINE)
    c.vline(ox, oy + 1, 16, OUTLINE)
    c.vline(ox + 17, oy + 1, 16, OUTLINE)
    c.hline(ox + 1, oy + 1, 16, (255, 255, 255, 110))
    c.vline(ox + 1, oy + 2, 15, (255, 255, 255, 110))
    c.hline(ox + 2, oy + 16, 15, (0, 0, 0, 120))
    c.vline(ox + 16, oy + 2, 14, (0, 0, 0, 120))
    # Gloss corner and four screw dots.
    c.set(ox + 3, oy + 3, (255, 255, 255, 150))
    c.set(ox + 4, oy + 3, (255, 255, 255, 90))
    c.set(ox + 3, oy + 4, (255, 255, 255, 90))
    for (sx, sy) in ((3, 14), (14, 3), (14, 14)):
        c.set(ox + sx, oy + sy, (0, 0, 0, 90))


def side_hover(c, ox, oy):
    for i in range(1, 17):
        c.set(ox + i, oy, (240, 244, 250, 255))
        c.set(ox + i, oy + 17, (240, 244, 250, 255))
        c.set(ox, oy + i, (240, 244, 250, 255))
        c.set(ox + 17, oy + i, (240, 244, 250, 255))


O = OUTLINE

TAB_ICONS = {
    "overview": (["................",
                  "................",
                  ".....oooooo.....",
                  "...oowwwwwwoo...",
                  "..owwawwwwawwo..",
                  ".owwwwwwwwwwwwo.",
                  ".oawwwwwwwwrwao.",
                  "owwwwwwwwwrwwwwo",
                  "owwwwwwwwrwwwwwo",
                  "owawwwwwrwwwwawo",
                  "owwwwwwoddwwwwwo",
                  "owwwwwwoddwwwwwo",
                  ".oooooooooooooo.",
                  "................",
                  "................",
                  "................"],
                 {"o": O, "w": (216, 220, 226), "a": (232, 178, 58), "r": (224, 72, 72), "d": (43, 47, 54)}),
    "upgrades": (["................",
                  ".......oo.......",
                  "......ollo......",
                  ".....olggdo.....",
                  "....olggggdo....",
                  "...olggggggdo...",
                  "..oooolggdoooo..",
                  ".....olggdo.....",
                  ".....olggdo.....",
                  ".....olggdo.....",
                  ".....olggdo.....",
                  ".....oddddo.....",
                  ".....oooooo.....",
                  "..oooooooooooo..",
                  "..occcccccccco..",
                  "..oooooooooooo.."],
                 {"o": O, "l": (155, 240, 190), "g": (61, 220, 132), "d": (31, 138, 80), "c": (63, 190, 230)}),
    "loot": (["................",
              "................",
              "..oooooooooooo..",
              ".obbbbbbbbbbbbo.",
              ".obBBBBBBBBBBbo.",
              ".obbbbbbbbbbbbo.",
              ".oyyyyyoYoyyyyo.",
              ".obbbbboyobbbbo.",
              ".obbbbbbobbbbbo.",
              ".obBBBBBBBBBBbo.",
              ".obbbbbbbbbbbbo.",
              ".obbbbbbbbbbbbo.",
              ".oyyyyyyyyyyyyo.",
              "..oooooooooooo..",
              "................",
              "................"],
             {"o": O, "b": (138, 90, 43), "B": (94, 59, 26), "y": (232, 178, 58), "Y": (255, 224, 138)}),
    "scan": (["................",
              ".....oooooo.....",
              "...ooddddddoo...",
              "..oddggggggddo..",
              ".oddgddddddgddo.",
              ".odgddggggddgdo.",
              "odgddgddddgddgdo",
              "odgdgddllddgdgdo",
              "odgdgddllllllldo",
              "odgddgddddgddgdo",
              ".odgddggggddgdo.",
              ".oddgddddddgddo.",
              "..oddggggggddo..",
              "...ooddddddoo...",
              ".....oooooo.....",
              "................"],
             {"o": O, "d": (27, 77, 51), "g": (61, 220, 132), "l": (190, 255, 214)}),
    "sides": ([".......oo.......",
               ".....oottoo.....",
               "...oottttttoo...",
               ".oottttttttttoo.",
               "otttttttttttttto",
               "ollttttttttttrro",
               "ollllttttttrrrro",
               "ollllllttrrrrrro",
               "olllllloorrrrrro",
               "olllllloorrrrRro",
               "olllllloorrrRRro",
               "olllllloorrRRRro",
               ".oolllloorrrroo.",
               "...oolloorroo...",
               ".....oooooo.....",
               "................"],
              {"o": O, "t": (200, 206, 214), "l": (224, 72, 72), "r": (232, 178, 58), "R": (176, 128, 36)}),
    "stats": (["................",
               "................",
               ".w.........ooo..",
               ".w.........oco..",
               ".w.........oco..",
               ".w.....ooo.oco..",
               ".w.....ogo.oco..",
               ".w.....ogo.oco..",
               ".w.ooo.ogo.oco..",
               ".w.oao.ogo.oco..",
               ".w.oao.ogo.oco..",
               ".w.oao.ogo.oco..",
               ".w.oao.ogo.oco..",
               ".wwwwwwwwwwwwww.",
               ".oooooooooooooo.",
               "................"],
              {"o": O, "w": (200, 206, 214), "a": (232, 178, 58), "g": (61, 220, 132), "c": (63, 190, 230)}),
    "mob": (["................",
             "....oooooooo....",
             "...owwwwwwwwo...",
             "..owwwwwwwwwwo..",
             ".owwwwwwwwwwwwo.",
             ".owwwwwwwwwwwwo.",
             ".owooowwwwooowo.",
             ".owoeowwwwoeowo.",
             ".owooowwwwooowo.",
             ".owwwwwoowwwwwo.",
             "..owwwwwwwwwwo..",
             "...oswswswswo...",
             "...owswswswso...",
             "....oooooooo....",
             "................",
             "................"],
            {"o": O, "w": (230, 226, 214), "s": (168, 162, 150), "e": (63, 208, 201)}),
    "inventory": (["................",
                   ".oooo.oooo.oooo.",
                   ".olbo.olbo.olbo.",
                   ".obbo.obbo.obbo.",
                   ".oooo.oooo.oooo.",
                   "................",
                   ".oooo.oooo.oooo.",
                   ".olbo.otto.olbo.",
                   ".obbo.otto.obbo.",
                   ".oooo.oooo.oooo.",
                   "................",
                   ".oooo.oooo.oooo.",
                   ".olbo.olbo.olbo.",
                   ".obbo.obbo.obbo.",
                   ".oooo.oooo.oooo.",
                   "................"],
                  {"o": O, "l": (200, 206, 214), "b": (138, 144, 154), "t": (63, 208, 201)}),
}
TAB_ICON_ORDER = ["overview", "upgrades", "loot", "scan", "sides", "stats", "mob", "inventory"]

GHOST = (150, 156, 166, 120)
GHOST_ICONS = {
    "speed": ["................",
              "..........ggg...",
              ".........ggg....",
              "........ggg.....",
              ".......ggg......",
              "......ggg.......",
              ".....gggggggg...",
              "...gggggggg.....",
              ".......ggg......",
              "......ggg.......",
              ".....ggg........",
              "....ggg.........",
              "...ggg..........",
              "..gg............",
              "................",
              "................"],
    "quantity": ["................",
                 "................",
                 ".......gg.......",
                 ".......gg.......",
                 ".....gggggg.....",
                 ".....gggggg.....",
                 ".......gg.......",
                 ".......gg.......",
                 "................",
                 "..gggggggggggg..",
                 "................",
                 "..gggggggggggg..",
                 "................",
                 "..gggggggggggg..",
                 "................",
                 "................"],
    "efficiency": ["................",
                   "................",
                   "......gggg......",
                   "....gggggggg....",
                   "....g......g....",
                   "....g.gggg.g....",
                   "....g.gggg.g....",
                   "....g......g....",
                   "....g.gggg.g....",
                   "....g.gggg.g....",
                   "....g......g....",
                   "....g.gggg.g....",
                   "....g.gggg.g....",
                   "....g......g....",
                   "....gggggggg....",
                   "................"],
    "loot": ["................",
             "................",
             "....gggggggg....",
             "...g..g..g..g...",
             "..g...g..g...g..",
             ".gggggggggggggg.",
             "..g...g..g...g..",
             "...g...gg...g...",
             "....g..gg..g....",
             ".....g.gg.g.....",
             "......gggg......",
             ".......gg.......",
             "................",
             "................",
             "................",
             "................"],
    "xp": ["................",
           "......gggg......",
           ".......gg.......",
           ".......gg.......",
           "......gggg......",
           ".....g....g.....",
           "....g......g....",
           "...g........g...",
           "...g.gggggg.g...",
           "...g.gggggg.g...",
           "...g.gggggg.g...",
           "...g........g...",
           "....gggggggg....",
           "................",
           "................",
           "................"],
    "mob_card": ["................",
                 "..gggggggggggg..",
                 "..g..........g..",
                 "..g..gggggg..g..",
                 "..g.g......g.g..",
                 "..g.g.g..g.g.g..",
                 "..g.g......g.g..",
                 "..g.g.gggg.g.g..",
                 "..g.g......g.g..",
                 "..g..gggggg..g..",
                 "..g..........g..",
                 "..g.gggggggg.g..",
                 "..g..........g..",
                 "..g.gggggg...g..",
                 "..gggggggggggg..",
                 "................"],
}
GHOST_ORDER = ["speed", "quantity", "range", "efficiency", "loot", "xp", "mob_card"]

SMALL_ICONS = {
    "filter_all": (["............",
                    ".....ww.....",
                    ".w...ww...w.",
                    "..w..ww..w..",
                    "...w.ww.w...",
                    ".wwwwwwwwww.",
                    ".wwwwwwwwww.",
                    "...w.ww.w...",
                    "..w..ww..w..",
                    ".w...ww...w.",
                    ".....ww.....",
                    "............"],
                   {"w": (216, 220, 226)}),
    "filter_resources": (["............",
                          "............",
                          "............",
                          "...oooooo...",
                          "..oYYYYyyo..",
                          ".oYyyyyyydo.",
                          "oyyyyyyyyddo",
                          "oddddddddddo",
                          "oooooooooooo",
                          "............",
                          "............",
                          "............"],
                         {"o": O, "y": (232, 178, 58), "Y": (255, 230, 150), "d": (168, 122, 30)}),
    "filter_food": (["......b.....",
                     "......bgg...",
                     "..oooobooo..",
                     ".orrRrrrrro.",
                     "orRRrrrrrrro",
                     "orRrrrrrrrro",
                     "orrrrrrrrrro",
                     "orrrrrrrrrro",
                     ".orrrrrrrro.",
                     ".orrrrrrrro.",
                     "..ooo..ooo..",
                     "............"],
                    {"o": O, "r": (216, 58, 58), "R": (240, 128, 120), "g": (61, 200, 90), "b": (110, 70, 30)}),
    "filter_equipment": ([".........ooo",
                          "........osso",
                          ".......ossSo",
                          "......ossSo.",
                          ".....ossSo..",
                          "..o.ossSo...",
                          "..ooysSo....",
                          "...oyyo.....",
                          "..ohoyyo....",
                          ".oho..oo....",
                          "oho.........",
                          "oo.........."],
                         {"o": O, "s": (216, 220, 226), "S": (138, 144, 154), "y": (232, 178, 58), "h": (120, 78, 36)}),
    "lock": (["............",
              "....oooo....",
              "...oggggo...",
              "...og..go...",
              "...og..go...",
              "..oooooooo..",
              "..oyyyyyyo..",
              "..oyyddyyo..",
              "..oyyddyyo..",
              "..oyyyyyyo..",
              "..oooooooo..",
              "............"],
             {"o": O, "g": (200, 206, 214), "y": (232, 178, 58), "d": (70, 46, 10)}),
    "energy": (["......oooo..",
                ".....oyyo...",
                "....oyyo....",
                "...oyyo.....",
                "..oyyyyyyo..",
                "..ooooyyo...",
                ".....oyo....",
                "....oyo.....",
                "...oyo......",
                "...oo.......",
                "............",
                "............"],
               {"o": O, "y": (255, 210, 74)}),
}
SMALL_ORDER = ["filter_all", "filter_resources", "filter_food", "filter_equipment", "lock", "energy", "xp_orb"]


def ring_icon(c, ox, oy, color):
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if abs(d - 6.5) < 0.6 or abs(d - 3.4) < 0.6 or d < 1.1:
                c.set(ox + x, oy + y, color)


def orb_icon(c, ox, oy):
    for y in range(12):
        for x in range(12):
            d = math.hypot(x - 5.5, y - 5.5)
            if d < 4.6:
                col = (124, 219, 60) if d < 3.6 else (40, 110, 20)
                if math.hypot(x - 4, y - 4) < 1.3:
                    col = (220, 255, 170)
                c.set(ox + x, oy + y, rgba(col))
            elif d < 5.4:
                c.set(ox + x, oy + y, O)


def widgets():
    c = Canvas()
    c.slot(0, 0)                      # slot frame        (0,0) 18x18
    side_overlay(c, 20, 0)            # side overlay      (20,0) 18x18
    side_hover(c, 40, 0)              # side hover        (40,0) 18x18
    tab(c, 0, 20, 24, 22, False)      # tab               (0,20) 24x22
    tab(c, 26, 20, 30, 22, True)      # tab selected      (26,20) 30x22
    bar_frame(c, 0, 44, 14, 88)       # bar frame         (0,44) 14x88
    vertical_fill(c, 16, 45, 12, 86, (255, 120, 90), (176, 36, 36))   # energy fill (16,45) 12x86
    vertical_fill(c, 30, 45, 12, 86, (190, 255, 120), (52, 140, 24))  # xp fill     (30,45) 12x86
    horizontal_fill(c, 48, 44, 128, 7, (255, 226, 140), ACCENT_LOOT, (168, 122, 30))   # (48,44)
    horizontal_fill(c, 48, 52, 128, 7, (170, 248, 244), ACCENT_MOB, (31, 143, 138))    # (48,52)
    for i, state in enumerate(("normal", "hover", "disabled", "selected")):
        button(c, 0, 136 + i * 16, 128, 16, state)   # buttons (0,136+16i) 128x16
    for i, state in enumerate(("normal", "hover", "disabled")):
        thumb(c, 130 + i * 10, 136, state)            # thumbs (130+10i,136) 8x15
    for i, name in enumerate(TAB_ICON_ORDER):         # tab icons (16i,200) 16x16
        rows, pal = TAB_ICONS[name]
        c.pattern(i * 16, 200, rows, pal)
    for i, name in enumerate(GHOST_ORDER):            # ghost icons (16i,216) 16x16
        if name == "range":
            ring_icon(c, i * 16, 216, GHOST)
        else:
            c.pattern(i * 16, 216, GHOST_ICONS[name], {"g": GHOST})
    for i, name in enumerate(SMALL_ORDER):            # small icons (12i,232) 12x12
        if name == "xp_orb":
            orb_icon(c, i * 12, 232)
        else:
            rows, pal = SMALL_ICONS[name]
            c.pattern(i * 12, 232, rows, pal)
    return c


def main():
    panel(ACCENT_LOOT, 7).save("loot_generator.png")
    panel(ACCENT_MOB, 11).save("mob_loot_generator.png")
    widgets().save("widgets.png")


if __name__ == "__main__":
    main()
