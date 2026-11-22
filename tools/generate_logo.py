#!/usr/bin/env python3
"""Mod logo (pure Python, no Pillow): the Tier IV Loot Generator front, scaled up on the mod's graphite frame.

Written to src/main/resources/ultraloot_logo.png (mod list, `logoFile` in neoforge.mods.toml) and
curseforge/logo.png (CurseForge project avatar, 400x400).
Usage (from the repo root):
    python tools/generate_logo.py           # write changed files
    python tools/generate_logo.py --check   # exit 1 if a file on disk differs
"""
from __future__ import annotations

import struct
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_restorer_assets import png_bytes  # noqa: E402  (shared RGBA PNG encoder)

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "src/main/resources/assets/ultraloot/textures/block/loot_generator_t4_front_on.png"
SIZE = 400
SCALE = 20  # 16 px texture -> 320 px
BORDER = 6
BACKGROUND = (0x1E, 0x22, 0x28, 255)
EDGE = (0x0E, 0x10, 0x13, 255)
GOLD = (0xE8, 0xB2, 0x3A, 255)


def read_png(path: Path) -> tuple[int, int, list[tuple[int, int, int, int]]]:
    """Minimal decoder for 8-bit RGB/RGBA non-interlaced PNGs (what the asset tools write)."""
    data = path.read_bytes()
    pos, idat = 8, b""
    width = height = color_type = 0
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b"IHDR":
            width, height, depth, color_type = struct.unpack(">IIBB", body[:10])
            assert depth == 8 and color_type in (2, 6), f"unsupported PNG {path}"
        elif kind == b"IDAT":
            idat += body
    bpp = 4 if color_type == 6 else 3
    raw = zlib.decompress(idat)
    stride = width * bpp
    prev = bytearray(stride)
    pixels = []
    i = 0
    for _ in range(height):
        kind = raw[i]
        line = bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for k in range(stride):
            a = line[k - bpp] if k >= bpp else 0
            b = prev[k]
            c = prev[k - bpp] if k >= bpp else 0
            if kind == 1:
                line[k] = (line[k] + a) & 255
            elif kind == 2:
                line[k] = (line[k] + b) & 255
            elif kind == 3:
                line[k] = (line[k] + (a + b) // 2) & 255
            elif kind == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[k] = (line[k] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        for x in range(width):
            px = line[x * bpp:x * bpp + bpp]
            pixels.append((px[0], px[1], px[2], px[3] if bpp == 4 else 255))
        prev = line
    return width, height, pixels


def logo() -> bytes:
    width, height, texture = read_png(SOURCE)
    assert (width, height) == (16, 16), "logo source must be a 16x16 texture"
    offset = (SIZE - width * SCALE) // 2
    out = []
    for y in range(SIZE):
        for x in range(SIZE):
            edge = min(x, y, SIZE - 1 - x, SIZE - 1 - y)
            if edge < 2:
                color = EDGE
            elif edge < BORDER:
                color = GOLD
            else:
                color = BACKGROUND
            tx, ty = (x - offset) // SCALE, (y - offset) // SCALE
            if 0 <= tx < width and 0 <= ty < height and 0 <= x - offset and 0 <= y - offset:
                px = texture[ty * width + tx]
                if px[3] > 0:
                    color = px
            out.append(color)
    return png_bytes(SIZE, SIZE, out)


def main(argv: list[str]) -> int:
    check = "--check" in argv
    data = logo()
    stale = []
    for path in (ROOT / "src/main/resources/ultraloot_logo.png", ROOT / "curseforge/logo.png"):
        if path.exists() and path.read_bytes() == data:
            continue
        stale.append(path)
        if not check:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
    print(f"logo: {len(stale)} {'would change' if check else 'written'}")
    for path in stale:
        print("  " + str(path.relative_to(ROOT)))
    return 1 if check and stale else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
