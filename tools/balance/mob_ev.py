"""Expected loot per simulated kill for the mobs in Ultra Loot's mob_simulation data map.

Models MobLootSimulator: detached EntityType#create instance (adult, not on fire, default variant), killer and direct
attacker = FakePlayer (minecraft:player) holding an iron sword (Looting N on the L3 column), damage source = player
attack, LAST_DAMAGE_PLAYER set (killed_by_player passes), luck 0.

Usage: python -I mob_ev.py <minecraft-resources.jar> <mob_simulation.json> <out_dir>

Exactness notes:
- Counts are tracked as discrete distributions: set_count uses NumberProvider#getInt (uniform = integer inclusive,
  constant = round, binomial = exact); counts <= 0 make the stack empty; enchanted_count_increase adds
  round(level * getFloat()) to max(count, 0) (ItemStack#getCount is 0 for an empty stack) and then applies `limit`.
- Pool rolls use the mean of getInt; bonus_rolls * luck(0) = 0.
- Entry conditions are evaluated before weighted selection (vanilla LootPoolEntryContainer#expand): failing entries
  leave the pool; random entry conditions are handled by enumerating their pass/fail subsets exactly.
- Random conditions inside one condition list are treated as independent.
"""
import json, sys, zipfile, collections, itertools, math, os

JAR, DATAMAP, OUT = sys.argv[1], sys.argv[2], sys.argv[3]
z = zipfile.ZipFile(JAR)
NAMES = set(z.namelist())

def load_table(tid):
    ns, path = tid.split(":", 1) if ":" in tid else ("minecraft", tid)
    p = f"data/{ns}/loot_table/{path}.json"
    return json.loads(z.read(p)) if p in NAMES else None

VALUES = {
    "rotten_flesh": 1, "bone": 2, "arrow": 1, "string": 2, "spider_eye": 2, "gunpowder": 4, "ender_pearl": 12,
    "blaze_rod": 16, "breeze_rod": 16, "ghast_tear": 20, "magma_cream": 6, "slime_ball": 4, "phantom_membrane": 8,
    "prismarine_shard": 3, "prismarine_crystals": 4, "cod": 2, "salmon": 2, "tropical_fish": 3, "pufferfish": 3,
    "wet_sponge": 30, "tide_armor_trim_smithing_template": 40, "gold_nugget": 1, "gold_ingot": 9, "iron_ingot": 9,
    "emerald": 15, "diamond": 50, "totem_of_undying": 120, "wither_skeleton_skull": 100, "coal": 3,
    "sculk_catalyst": 40, "shulker_shell": 40, "feather": 1, "leather": 3, "beef": 2, "porkchop": 2, "chicken": 2,
    "mutton": 2, "rabbit": 2, "rabbit_hide": 1, "rabbit_foot": 10, "ink_sac": 2, "glow_ink_sac": 4, "white_wool": 2,
    "carrot": 1, "potato": 1, "poisonous_potato": 1, "redstone": 2, "glowstone_dust": 2, "sugar": 1,
    "glass_bottle": 1, "stick": 0.5, "saddle": 15, "seagrass": 0.5, "bowl": 0.5, "scute": 8, "turtle_scute": 8,
    "goat_horn": 10, "iron_nugget": 1, "glow_berries": 1, "experience_bottle": 7, "potion": 2, "nautilus_shell": 15,
    "trident": 60, "iron_sword": 5, "crossbow": 5, "bow": 3, "ominous_bottle": 10, "heart_of_the_sea": 100,
    "enchanted_book": 20,
}
UNVALUED = set()

def value_of(item):
    p = item.split(":")[-1]
    if p in VALUES: return float(VALUES[p])
    if p.startswith("music_disc_"): return 20.0
    if p.endswith("_spawn_egg"): return 0.0
    UNVALUED.add(item)
    return 1.0

# ---------------------------------------------------------------- number providers
def jround(x): return math.floor(x + 0.5)  # Java Math.round

def mean_float(p):
    if isinstance(p, (int, float)): return float(p)
    t = p.get("type", "minecraft:uniform").split(":")[-1]
    if t == "constant": return float(p["value"])
    if t == "uniform": return (mean_float(p["min"]) + mean_float(p["max"])) / 2
    if t == "binomial": return mean_float(p["n"]) * mean_float(p["p"])
    raise ValueError(f"unsupported number provider {p}")

def int_dist(p):
    """Distribution of NumberProvider#getInt."""
    if isinstance(p, (int, float)): return {jround(p): 1.0}
    t = p.get("type", "minecraft:uniform").split(":")[-1]
    if t == "constant": return {jround(p["value"]): 1.0}
    if t == "uniform":
        out = collections.Counter()
        for a, pa in int_dist(p["min"]).items():
            for b, pb in int_dist(p["max"]).items():
                if b < a: out[a] += pa * pb; continue
                for v in range(a, b + 1): out[v] += pa * pb / (b - a + 1)
        return dict(out)
    if t == "binomial":
        out = collections.Counter(); pr = mean_float(p["p"])
        for n, pn in int_dist(p["n"]).items():
            for k in range(n + 1): out[k] += pn * math.comb(n, k) * pr ** k * (1 - pr) ** (n - k)
        return dict(out)
    raise ValueError(f"unsupported number provider {p}")

def rounded_scaled_float_dist(p, level, steps=6000):
    """Distribution of Math.round(level * getFloat())."""
    if isinstance(p, (int, float)) or p.get("type", "").endswith("constant"):
        return {jround(level * mean_float(p)): 1.0}
    t = p.get("type", "minecraft:uniform").split(":")[-1]
    if t == "uniform":
        lo, hi = mean_float(p["min"]), mean_float(p["max"])
        out = collections.Counter()
        for i in range(steps):
            u = lo + (hi - lo) * (i + 0.5) / steps
            out[jround(level * u)] += 1 / steps
        return dict(out)
    return {jround(level * mean_float(p)): 1.0}

# ---------------------------------------------------------------- conditions
class Ctx:
    def __init__(self, mob, looting, size):
        self.mob, self.looting, self.size = mob, looting, size
        self.notes = set()

def level_value(v, level):
    if isinstance(v, (int, float)): return float(v)
    t = v.get("type", "").split(":")[-1]
    if t == "linear": return v["base"] + v["per_level_above_first"] * (level - 1)
    if t == "constant": return float(v["value"])
    raise ValueError(f"unsupported level-based value {v}")

def entity_pred(entity, pred, ctx):
    """Probability (0/1) that an entity_properties predicate matches."""
    victim = entity == "this"
    etype = ctx.mob if victim else "minecraft:player"
    ok = True
    for k, v in pred.items():
        if k == "flags":
            for f, want in v.items():
                actual = False  # not on fire, not baby, not sneaking/sprinting/swimming/flying
                if f == "is_on_fire": ctx.notes.add("is_on_fire predicate FAILS (victim not burning)")
                elif f == "is_baby": ctx.notes.add("is_baby evaluated as adult")
                else: ctx.notes.add(f"flag {f} assumed false")
                ok &= (actual == want)
        elif k == "type":
            if isinstance(v, str) and v.startswith("#"):
                ok = False
                ctx.notes.add(f"{entity} type tag {v} FAILS (killer is a FakePlayer)" if not victim else f"this type tag {v} assumed not matching")
            else:
                types = v if isinstance(v, list) else [v]
                ok &= etype in types
                if not victim: ctx.notes.add(f"{entity} type {v} FAILS (killer is a FakePlayer)")
        elif k == "type_specific" and v.get("type", "").endswith("slime"):
            s = v.get("size")
            if isinstance(s, (int, float)): ok &= ctx.size == s
            elif isinstance(s, dict): ok &= s.get("min", -1e9) <= ctx.size <= s.get("max", 1e9)
            ctx.notes.add(f"slime-size predicate {s} evaluated with size {ctx.size}")
        elif k == "equipment" and not victim:
            # FakePlayer holds a plain iron sword (+Looting on the L3 column); no smelts_loot / other enchantments.
            ctx.notes.add(f"{entity} equipment predicate FAILS (iron sword without smelts_loot)")
            ok = False
        else:
            ctx.notes.add(f"ASSUMED PASS: entity_properties {entity} {k}={json.dumps(v)}")
    return 1.0 if ok else 0.0

def cond_p(c, ctx):
    t = c["condition"].split(":")[-1]
    if t == "killed_by_player": return 1.0
    if t == "random_chance": return mean_float(c["chance"])
    if t == "random_chance_with_enchanted_bonus":
        return level_value(c["enchanted_chance"], ctx.looting) if ctx.looting > 0 else float(c["unenchanted_chance"])
    if t == "inverted": return 1.0 - cond_p(c["term"], ctx)
    if t == "all_of": return math.prod(cond_p(x, ctx) for x in c["terms"])
    if t == "any_of": return 1.0 - math.prod(1.0 - cond_p(x, ctx) for x in c["terms"])
    if t == "entity_properties": return entity_pred(c["entity"], c.get("predicate", {}), ctx)
    if t == "damage_source_properties":
        ctx.notes.add(f"damage_source_properties {json.dumps(c.get('predicate'))} FAILS (player melee attack)")
        return 0.0
    if t == "match_tool":
        ctx.notes.add(f"ASSUMED PASS: match_tool {json.dumps(c.get('predicate'))}")
        return 1.0
    ctx.notes.add(f"ASSUMED PASS: condition {t}")
    return 1.0

def conds_p(conds, ctx):
    return math.prod(cond_p(c, ctx) for c in (conds or []))

# ---------------------------------------------------------------- item counts
def mix(a, b, pa):
    out = collections.Counter()
    for k, v in a.items(): out[k] += v * pa
    for k, v in b.items(): out[k] += v * (1 - pa)
    return dict(out)

def apply_functions(fns, ctx, item):
    """Return (expected count, item name) after the function chain applied to a 1-count stack."""
    dist = {1: 1.0}
    for fn in fns or []:
        t = fn["function"].split(":")[-1]
        p = conds_p(fn.get("conditions"), ctx)
        if p == 0.0:
            if t == "furnace_smelt": ctx.notes.add("furnace_smelt not applied (not on fire, no smelts_loot weapon)")
            continue
        if t == "set_count":
            new = collections.Counter()
            for c, pc in dist.items():
                for v, pv in int_dist(fn["count"]).items():
                    base = max(c, 0) if fn.get("add") else 0
                    new[base + v] += pc * pv
            dist = mix(dict(new), dist, p)
        elif t in ("enchanted_count_increase", "looting_enchant"):
            if ctx.looting == 0: continue
            inc = rounded_scaled_float_dist(fn["count"], ctx.looting)
            lim = fn.get("limit", 0)
            new = collections.Counter()
            for c, pc in dist.items():
                for d, pd in inc.items():
                    n = max(c, 0) + d
                    if lim > 0: n = min(n, lim)
                    new[n] += pc * pd
            dist = mix(dict(new), dist, p)
        elif t == "furnace_smelt":
            ctx.notes.add("ASSUMED: furnace_smelt applied (unexpected)")
        elif t == "set_potion":
            ctx.notes.add(f"set_potion {fn.get('id')} on {item} (counted as plain {item.split(':')[-1]})")
        else:
            ctx.notes.add(f"function {t} ignored for count")
    return sum(max(c, 0) * pc for c, pc in dist.items())

# ---------------------------------------------------------------- tables
def table_ev(table, ctx, depth=0):
    out = collections.Counter()
    if table is None or depth > 8: return out
    for pool in table.get("pools", []):
        pp = conds_p(pool.get("conditions"), ctx)
        if pp == 0: continue
        rolls = sum(k * v for k, v in int_dist(pool.get("rolls", 1)).items())  # + bonus_rolls * luck(0)
        per_roll = pool_roll_ev(pool.get("entries", []), ctx, depth)
        for k, v in per_roll.items(): out[k] += v * rolls * pp
    for fn in table.get("functions", []) or []:
        ctx.notes.add(f"table-level function {fn['function']} ignored")
    return out

def entry_choices(e, ctx):
    """Expand one entry into (pass_probability, weight, producer) choices, vanilla-style."""
    ty = e.get("type", "").split(":")[-1]
    p = conds_p(e.get("conditions"), ctx)
    w = max(0, int(e.get("weight", 1)))  # quality * luck(0) = 0
    if ty in ("alternatives", "group", "sequence"):
        return [(p, 1, ("composite", e))]
    if ty == "tag" and e.get("expand"):
        ctx.notes.add(f"tag entry #{e['name']} counted as one entry (contents not expanded)")
    return [(p, w, ("leaf", e))]

def leaf_ev(e, ctx, depth):
    ty = e.get("type", "").split(":")[-1]
    out = collections.Counter()
    if ty == "item":
        out[e["name"]] += apply_functions(e.get("functions"), ctx, e["name"])
    elif ty == "tag":
        out["#" + e["name"]] += apply_functions(e.get("functions"), ctx, "#" + e["name"])
    elif ty == "loot_table":
        v = e["value"]
        sub = load_table(v) if isinstance(v, str) else v
        if isinstance(v, str): ctx.notes.add(f"nested loot_table {v} expanded")
        for fn in e.get("functions", []) or []:
            if conds_p(fn.get("conditions"), ctx) > 0:
                ctx.notes.add(f"function {fn['function']} on nested table ignored")
        for k, x in table_ev(sub, ctx, depth + 1).items(): out[k] += x
    elif ty == "empty":
        pass
    elif ty in ("alternatives", "group", "sequence"):
        children = e.get("children", [])
        if ty == "group":
            for ch in children:
                pc = conds_p(ch.get("conditions"), ctx)
                for k, x in leaf_ev(ch, ctx, depth).items(): out[k] += x * pc
        else:
            remaining = 1.0
            for ch in children:
                pc = conds_p(ch.get("conditions"), ctx)
                for k, x in leaf_ev(ch, ctx, depth).items(): out[k] += x * pc * remaining
                if ty == "alternatives": remaining *= (1 - pc)
                else: remaining *= pc
    else:
        ctx.notes.add(f"unknown entry type {ty}")
    return out

def pool_roll_ev(entries, ctx, depth):
    choices = [c for e in entries for c in entry_choices(e, ctx)]
    sure = [c for c in choices if c[0] == 1.0 and c[1] > 0]
    rnd = [c for c in choices if 0.0 < c[0] < 1.0 and c[1] > 0]
    evs = {id(c): leaf_ev(c[2][1], ctx, depth) for c in sure + rnd}
    out = collections.Counter()
    for mask in itertools.product((True, False), repeat=len(rnd)):
        prob = 1.0
        active = list(sure)
        for c, on in zip(rnd, mask):
            prob *= c[0] if on else (1 - c[0])
            if on: active.append(c)
        total = sum(c[1] for c in active)
        if total == 0 or prob == 0: continue
        for c in active:
            for k, x in evs[id(c)].items(): out[k] += prob * c[1] / total * x
    return out

# ---------------------------------------------------------------- main
EXTRA_ASSUMPTIONS = {
    "minecraft:sheep": ["Fresh EntityType#create sheep: white, not sheared -> getLootTable() = entities/sheep/white (1 white_wool + entities/sheep mutton pool)"],
    "minecraft:slime": ["Fresh slime instance has size 1 (MobLootSimulator only resizes magma cubes) -> slime_ball pool (size == 1) passes"],
    "minecraft:magma_cube": ["MobLootSimulator forces magma cube size 2 -> magma_cream entry (size >= 2) passes; froglights need a frog killer -> no drop"],
    "minecraft:creeper": ["Music-disc pool needs a skeleton attacker -> never drops"],
    "minecraft:turtle": ["Bowl pool needs lightning damage -> never drops; turtle_scute comes from baby growth, not the loot table"],
    "minecraft:goat": ["Loot table is empty (goat horns come from ramming, not the loot table)"],
    "minecraft:fox": ["Loot table is empty (fox drops only its held item, which a fresh instance does not have)"],
    "minecraft:ocelot": ["Loot table is empty"],
    "minecraft:drowned": ["Only the loot table is rolled: equipment drops (trident, nautilus shell held item) are not part of it"],
    "minecraft:zombified_piglin": ["Equipment (golden sword) drop is not part of the loot table"],
    "minecraft:skeleton": ["Equipment (bow) drop is not part of the loot table"],
    "minecraft:stray": ["Equipment (bow) drop is not part of the loot table"],
    "minecraft:bogged": ["Equipment (bow) drop is not part of the loot table"],
}

def main():
    mobs = json.load(open(DATAMAP, encoding="utf-8"))["values"]
    result, missing = {}, []
    for mob, cfg in mobs.items():
        path = mob.split(":", 1)[1]
        size = 2 if path == "magma_cube" else 1
        if path == "sheep":
            tid = "minecraft:entities/sheep/white"
        else:
            tid = f"minecraft:entities/{path}"
        table = load_table(tid)
        if table is None:
            missing.append(mob); continue
        rec = {"loot_table": tid}
        notes = set()
        for lv in (0, 3):
            ctx = Ctx(mob, lv, size)
            ev = table_ev(table, ctx)
            notes |= ctx.notes
            items = {k: round(v, 6) for k, v in sorted(ev.items(), key=lambda kv: -kv[1]) if v > 1e-12}
            rec[f"items_ev_looting{lv}"] = items
            rec[f"value_ev_looting{lv}"] = round(sum(v * value_of(k) for k, v in ev.items()), 6)
            if lv == 0: rec["item_count_ev_looting0"] = round(sum(ev.values()), 6)
            else: rec["item_count_ev_looting3"] = round(sum(ev.values()), 6)
        rec["xp_cost"] = cfg.get("xp_cost")
        rec["energy_multiplier"] = cfg.get("energy_multiplier")
        rec["assumptions"] = EXTRA_ASSUMPTIONS.get(mob, []) + sorted(notes)
        if not table.get("pools"): rec["assumptions"].append("empty loot table (no pools)")
        result[mob] = rec
    out = dict(result)
    out["unvalued_items"] = sorted(UNVALUED)
    out["missing_tables"] = missing
    out["model"] = ("killed_by_player=true; killer/direct attacker = FakePlayer (minecraft:player) with iron sword; "
                    "not on fire; damage_source_properties FAIL; luck 0; Looting 0 and 3 columns; exact count "
                    "distributions with negative counts clamped to 0; equipment drops not modelled (mod rolls only the loot table).")
    with open(os.path.join(OUT, "mob_ev.json"), "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2)

    rows = sorted(result.items(), key=lambda kv: -kv[1]["value_ev_looting0"])
    lines = ["# Ultra Loot: expected loot value per simulated kill", "",
             "Model: player kill by FakePlayer with iron sword, not on fire, luck 0. L0 = no Looting, L3 = Looting III. "
             "Values from the balance value table (unvalued items count 1.0: " + ", ".join(i.split(':')[-1] for i in sorted(UNVALUED)) + ").", "",
             "| mob | value/kill (L0) | value/kill (L3) | items/kill (L0) | main drops (L0 value share) | xp_cost | energy_multiplier | value per xp | value per energy mult |",
             "|---|---:|---:|---:|---|---:|---:|---:|---:|"]
    for mob, r in rows:
        contrib = sorted(((k, v * value_of(k)) for k, v in r["items_ev_looting0"].items()), key=lambda kv: -kv[1])[:3]
        main = ", ".join(f"{k.split(':')[-1]} {r['items_ev_looting0'][k]:.3f} (={c:.2f})" for k, c in contrib if c > 0) or "-"
        v0 = r["value_ev_looting0"]
        vpx = f"{v0 / r['xp_cost']:.3f}" if r["xp_cost"] else "-"
        vpe = f"{v0 / r['energy_multiplier']:.2f}" if r["energy_multiplier"] else "-"
        lines.append(f"| {mob.split(':')[1]} | {v0:.2f} | {r['value_ev_looting3']:.2f} | {r['item_count_ev_looting0']:.2f} | {main} | "
                     f"{r['xp_cost']} | {r['energy_multiplier']} | {vpx} | {vpe} |")
    if missing: lines += ["", "Missing tables: " + ", ".join(missing)]
    with open(os.path.join(OUT, "mob_ev.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")

    for mob, r in rows:
        print(f"{mob:28s} L0={r['value_ev_looting0']:8.2f} L3={r['value_ev_looting3']:8.2f} " +
              ", ".join(f"{k.split(':')[-1]}={v:.3f}" for k, v in list(r['items_ev_looting0'].items())[:6]))
    print("unvalued:", sorted(UNVALUED), "missing:", missing)

if __name__ == "__main__":
    main()
