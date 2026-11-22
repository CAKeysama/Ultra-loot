#!/usr/bin/env python3
"""Ultra Loot Guide item assets (pure Python, no Pillow): the 16x16 book texture and its item model.

The pixel map below uses the mod palette (graphite cover, gold spine and emblem bar, teal gem, cream pages).
Usage (from the repo root):
    python tools/generate_guide_assets.py           # write changed files
    python tools/generate_guide_assets.py --check   # exit 1 if a file on disk differs
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_restorer_assets import png_bytes  # noqa: E402  (shared RGBA PNG encoder)

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "ultraloot"

PALETTE = {
    ".": (0, 0, 0, 0),
    "K": (0x0E, 0x10, 0x13, 255),  # outline
    "C": (0x3A, 0x41, 0x4B, 255),  # cover
    "c": (0x4C, 0x55, 0x60, 255),  # cover highlight
    "G": (0xE8, 0xB2, 0x3A, 255),  # gold
    "g": (0xA8, 0x78, 0x1E, 255),  # gold shadow
    "P": (0xF4, 0xEC, 0xD8, 255),  # page edge
    "p": (0xC9, 0xBB, 0x97, 255),  # page shade
    "T": (0x3F, 0xD0, 0xC9, 255),  # teal gem
    "t": (0x23, 0x80, 0x7C, 255),  # teal shadow
}

BOOK = [
    "................",
    "...KKKKKKKKKK...",
    "..KgGcccccccCK..",
    "..KgGcCCCCCCCPK.",
    "..KgGcCCCTCCCPK.",
    "..KgGcCCTtTCCPK.",
    "..KgGcCTtTtTCPK.",
    "..KgGcCCTtTCCPK.",
    "..KgGcCCCTCCCPK.",
    "..KgGcCCCCCCCPK.",
    "..KgGcCGGGGGCPK.",
    "..KgGcCCCCCCCPK.",
    "..KgGcCCCCCCCpK.",
    "..KgGCCCCCCCCpK.",
    "...KKKKKKKKKKKK.",
    "................",
]


def texture() -> bytes:
    assert len(BOOK) == 16 and all(len(row) == 16 for row in BOOK), "guide_book pixel map must be 16x16"
    return png_bytes(16, 16, [PALETTE[ch] for row in BOOK for ch in row])


def files() -> dict[Path, bytes]:
    model = {"parent": "minecraft:item/generated", "textures": {"layer0": "ultraloot:item/guide_book"}}
    return {
        ASSETS / "textures" / "item" / "guide_book.png": texture(),
        ASSETS / "models" / "item" / "guide_book.json": (json.dumps(model, indent=2) + "\n").encode("utf-8"),
    }


def main(argv: list[str]) -> int:
    check = "--check" in argv
    stale = []
    for path, data in files().items():
        current = path.read_bytes() if path.exists() else None
        if path.suffix == ".json" and current is not None:
            current = current.replace(b"\r\n", b"\n")  # a Windows checkout may have CRLF
        if current == data:
            continue
        stale.append(path)
        if not check:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
    verb = "would change" if check else "written"
    print(f"guide assets: {len(stale)} {verb}")
    for path in stale:
        print("  " + str(path.relative_to(ROOT)))
    return 1 if check and stale else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
