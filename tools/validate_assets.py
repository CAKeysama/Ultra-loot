#!/usr/bin/env python3
"""Validates Ultra Loot block/item assets.

Checks, for the ids declared in generate_assets.py:
  * every block has a blockstate with all facing x active variants, correct y rotation, existing models
  * every block and item has an item model
  * every model's parent chain resolves and every face/layer texture resolves to an existing PNG
  * textures are 16x16 RGBA (or a vertical animation strip with a .mcmeta)
  * element geometry is within Minecraft's limits (coords -16..32, legal rotation angles, uv 0..16)
  * the generator output on disk is up to date (generate_assets.py --check)

Usage: python tools/validate_assets.py   (exit code 1 on any error)
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import generate_assets as gen  # noqa: E402

ASSETS = gen.ASSETS
NS = gen.NS
# Vanilla parents we rely on; they carry no texture references of their own that we must satisfy.
VANILLA_PARENTS = {"minecraft:block/block", "minecraft:item/generated", "minecraft:item/handheld"}
FACE_NAMES = {"north", "south", "east", "west", "up", "down"}
ROTATION_ANGLES = {-45.0, -22.5, 0.0, 22.5, 45.0}

errors: list[str] = []
warnings: list[str] = []


def err(msg: str) -> None:
    errors.append(msg)


def split_id(ref: str) -> tuple[str, str]:
    return tuple(ref.split(":", 1)) if ":" in ref else ("minecraft", ref)  # type: ignore[return-value]


def model_path(ref: str) -> Path | None:
    ns, path = split_id(ref)
    return ASSETS / "models" / f"{path}.json" if ns == NS else None


_model_cache: dict[str, dict] = {}


def load_model(ref: str) -> dict | None:
    if ref in _model_cache:
        return _model_cache[ref]
    p = model_path(ref)
    if p is None or not p.is_file():
        return None
    try:
        data = json.loads(p.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        err(f"{p.relative_to(gen.ROOT)}: invalid JSON ({e})")
        return None
    _model_cache[ref] = data
    return data


def resolve_model(ref: str) -> tuple[dict[str, str], list[dict], list[str]] | None:
    """Returns merged textures (child wins), effective elements and the parent chain."""
    chain: list[str] = []
    textures: dict[str, str] = {}
    elements: list[dict] | None = None
    cur: str | None = ref
    while cur is not None:
        if cur in chain:
            err(f"model {ref}: parent cycle via {cur}")
            return None
        chain.append(cur)
        if cur in VANILLA_PARENTS:
            break
        data = load_model(cur)
        if data is None:
            err(f"model {ref}: missing model {cur}" if cur != ref else f"missing model {ref}")
            return None
        for k, v in data.get("textures", {}).items():
            textures.setdefault(k, v)
        if elements is None and "elements" in data:
            elements = data["elements"]
        cur = data.get("parent")
    return textures, elements or [], chain


def resolve_texture(var_or_ref: str, textures: dict[str, str], ctx: str) -> str | None:
    seen = set()
    v = var_or_ref
    while v.startswith("#"):
        key = v[1:]
        if key in seen or key not in textures:
            err(f"{ctx}: texture variable {v} does not resolve")
            return None
        seen.add(key)
        v = textures[key]
    return v


def check_texture(ref: str, ctx: str, used: set[Path]) -> None:
    ns, path = split_id(ref)
    if ns != NS:
        return
    p = ASSETS / "textures" / f"{path}.png"
    if not p.is_file():
        err(f"{ctx}: texture {ref} -> missing {p.relative_to(gen.ROOT)}")
        return
    used.add(p)
    with Image.open(p) as im:
        w, h = im.size
        if im.mode != "RGBA":
            err(f"{p.relative_to(gen.ROOT)}: mode {im.mode}, expected RGBA")
        animated = p.with_suffix(".png.mcmeta").is_file()
        if not (w == 16 and (h == 16 or (animated and h % 16 == 0))):
            err(f"{p.relative_to(gen.ROOT)}: size {w}x{h}, expected 16x16")


def check_elements(ref: str, elements: list[dict]) -> None:
    for i, el in enumerate(elements):
        ctx = f"model {ref} element {i}"
        frm, to = el.get("from"), el.get("to")
        if not (isinstance(frm, list) and isinstance(to, list) and len(frm) == 3 and len(to) == 3):
            err(f"{ctx}: bad from/to")
            continue
        for a, b in zip(frm, to):
            if not (-16 <= a <= 32 and -16 <= b <= 32) or a > b:
                err(f"{ctx}: coordinates out of range {frm} -> {to}")
        rot = el.get("rotation")
        if rot is not None:
            if float(rot.get("angle", 0)) not in ROTATION_ANGLES or rot.get("axis") not in ("x", "y", "z"):
                err(f"{ctx}: illegal rotation {rot}")
        faces = el.get("faces", {})
        if not faces:
            err(f"{ctx}: no faces")
        for name, f in faces.items():
            if name not in FACE_NAMES:
                err(f"{ctx}: unknown face {name}")
            uv = f.get("uv")
            if uv is not None and (len(uv) != 4 or any(not 0 <= c <= 16 for c in uv)):
                err(f"{ctx} face {name}: uv out of range {uv}")
            if "texture" not in f:
                err(f"{ctx} face {name}: no texture")
        # Faces of elements reaching outside the block must carry explicit UVs (auto-UV would wrap).
        outside = any(c < 0 or c > 16 for c in frm + to)
        if outside and any("uv" not in f for f in faces.values()):
            err(f"{ctx}: element extends outside 0..16 but has faces without explicit uv")


def check_concrete_model(ref: str, used: set[Path], checked: set[str]) -> None:
    if ref in checked:
        return
    checked.add(ref)
    resolved = resolve_model(ref)
    if resolved is None:
        return
    textures, elements, chain = resolved
    check_elements(ref, elements)
    if elements and "particle" not in textures:
        err(f"model {ref}: no particle texture")
    for key, value in textures.items():
        target = resolve_texture(value, textures, f"model {ref} texture '{key}'")
        if target is not None:
            check_texture(target, f"model {ref} texture '{key}'", used)
    for i, el in enumerate(elements):
        for name, f in el.get("faces", {}).items():
            target = resolve_texture(f.get("texture", ""), textures, f"model {ref} element {i} face {name}")
            if target is not None:
                check_texture(target, f"model {ref} element {i} face {name}", used)
    if chain[-1] == "minecraft:item/generated" and "layer0" not in textures:
        err(f"model {ref}: generated item without layer0")


def main() -> int:
    used: set[Path] = set()
    checked: set[str] = set()
    expected_variants = {
        f"active={a},facing={f}": y for a in ("false", "true") for f, y in gen.FACINGS
    }

    for bid in gen.BLOCK_IDS:
        bs_path = ASSETS / "blockstates" / f"{bid}.json"
        if not bs_path.is_file():
            err(f"block {bid}: missing blockstate")
        else:
            variants = json.loads(bs_path.read_text(encoding="utf-8")).get("variants", {})
            if set(variants) != set(expected_variants):
                err(f"block {bid}: variants {sorted(variants)} != {sorted(expected_variants)}")
            for key, v in variants.items():
                if key in expected_variants and v.get("y", 0) != expected_variants[key]:
                    err(f"block {bid} [{key}]: y={v.get('y', 0)}, expected {expected_variants[key]}")
                want = f"{NS}:block/{bid}_on" if key.startswith("active=true") else f"{NS}:block/{bid}"
                if v.get("model") != want:
                    err(f"block {bid} [{key}]: model {v.get('model')}, expected {want}")
                check_concrete_model(v.get("model", ""), used, checked)
        item_ref = f"{NS}:item/{bid}"
        item = load_model(item_ref)
        if item is None:
            err(f"block item {bid}: missing item model")
        elif item.get("parent") != f"{NS}:block/{bid}":
            err(f"block item {bid}: parent {item.get('parent')}, expected {NS}:block/{bid}")
        else:
            check_concrete_model(item_ref, used, checked)

    for iid in gen.ITEM_IDS:
        ref = f"{NS}:item/{iid}"
        if load_model(ref) is None:
            err(f"item {iid}: missing item model")
            continue
        check_concrete_model(ref, used, checked)

    mob = load_model(f"{NS}:item/mob_card") or {}
    layers = mob.get("textures", {})
    if layers.get("layer0") != f"{NS}:item/mob_card" or layers.get("layer1") != f"{NS}:item/mob_card_overlay":
        err("item mob_card: expected layer0=mob_card, layer1=mob_card_overlay (tintindex 1)")

    for p in sorted((ASSETS / "textures").rglob("*.png")):
        if "gui" in p.relative_to(ASSETS / "textures").parts:
            continue
        if p not in used:
            warnings.append(f"unreferenced texture {p.relative_to(gen.ROOT)}")

    stale = [p for p, data in gen.build().items() if not p.is_file() or p.read_bytes() != data]
    for p in stale:
        err(f"stale generated file {p.relative_to(gen.ROOT)} (run tools/generate_assets.py)")

    for w in warnings:
        print(f"WARN  {w}")
    for e in errors:
        print(f"ERROR {e}")
    print(
        f"checked {len(gen.BLOCK_IDS)} blocks, {len(gen.ITEM_IDS)} items, {len(checked)} models, "
        f"{len(used)} textures: {len(errors)} errors, {len(warnings)} warnings"
    )
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
