"""Generates Ultra Loot data-pack JSON into src/main/resources/data. Run from anywhere: python tools/generate_data.py"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "data")
NS = "ultraloot"
WRITTEN = []


def write(rel, obj):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")
    WRITTEN.append(rel)


def opt(tag):
    return {"id": tag, "required": False}


TIERS = ["t1", "t2", "t3", "t4"]
MACHINES = [f"{fam}_{t}" for fam in ("loot_generator", "mob_loot_generator") for t in TIERS]
CARD_TYPES = ["speed", "quantity", "range", "efficiency", "loot"]
CARDS = [f"{c}_card_{l}" for c in CARD_TYPES for l in (1, 2, 3)]

# ---------------------------------------------------------------------------------------------------------------
# 1. Loot profiles: (id, loot_table, context, category, icon, min_tier, energy_multiplier)
# Every vanilla chests/* table is covered explicitly so none of them is auto-discovered at Tier I / 1.0x.
# Exploration-map tables (shipwreck_map, underwater_ruin_small/big) are intentionally absent: the manager rejects them.
# ---------------------------------------------------------------------------------------------------------------
PROFILES = [
    # Tier I - early overworld
    ("spawn_bonus_chest", "minecraft:chests/spawn_bonus_chest", "chest", "overworld", "minecraft:oak_sapling", 1, 0.5),
    ("simple_dungeon", "minecraft:chests/simple_dungeon", "chest", "overworld", "minecraft:mossy_cobblestone", 1, 1.0),
    ("abandoned_mineshaft", "minecraft:chests/abandoned_mineshaft", "chest", "overworld", "minecraft:rail", 1, 1.0),
    ("igloo_chest", "minecraft:chests/igloo_chest", "chest", "overworld", "minecraft:snow_block", 1, 0.8),
    ("jungle_temple_dispenser", "minecraft:chests/jungle_temple_dispenser", "chest", "overworld", "minecraft:dispenser", 1, 0.5),
    ("shipwreck_supply", "minecraft:chests/shipwreck_supply", "chest", "ocean", "minecraft:oak_boat", 1, 0.9),
    ("fishing", "minecraft:gameplay/fishing", "fishing", "fishing", "minecraft:fishing_rod", 1, 0.8),
    ("archaeology_desert_well", "minecraft:archaeology/desert_well", "archaeology", "archaeology", "minecraft:suspicious_sand", 1, 0.6),
    ("archaeology_desert_pyramid", "minecraft:archaeology/desert_pyramid", "archaeology", "archaeology", "minecraft:suspicious_sand", 1, 0.6),
    ("archaeology_ocean_ruin_cold", "minecraft:archaeology/ocean_ruin_cold", "archaeology", "archaeology", "minecraft:suspicious_gravel", 1, 0.6),
    ("archaeology_ocean_ruin_warm", "minecraft:archaeology/ocean_ruin_warm", "archaeology", "archaeology", "minecraft:suspicious_sand", 1, 0.6),
    ("archaeology_trail_ruins_common", "minecraft:archaeology/trail_ruins_common", "archaeology", "archaeology", "minecraft:brush", 1, 0.6),
    ("farmers_crate", "ultraloot:generator/farmers_crate", "chest", "ultraloot", "minecraft:hay_block", 1, 0.8),
    # Tier II - temples, outposts, ocean treasure, trial chambers
    ("archaeology_trail_ruins_rare", "minecraft:archaeology/trail_ruins_rare", "archaeology", "archaeology", "minecraft:brush", 2, 1.0),
    ("desert_pyramid", "minecraft:chests/desert_pyramid", "chest", "overworld", "minecraft:chiseled_sandstone", 2, 1.5),
    ("jungle_temple", "minecraft:chests/jungle_temple", "chest", "overworld", "minecraft:mossy_cobblestone", 2, 1.4),
    ("pillager_outpost", "minecraft:chests/pillager_outpost", "chest", "overworld", "minecraft:crossbow", 2, 1.3),
    ("ruined_portal", "minecraft:chests/ruined_portal", "chest", "overworld", "minecraft:crying_obsidian", 2, 1.3),
    ("shipwreck_treasure", "minecraft:chests/shipwreck_treasure", "chest", "ocean", "minecraft:emerald", 2, 1.5),
    ("buried_treasure", "minecraft:chests/buried_treasure", "chest", "ocean", "minecraft:heart_of_the_sea", 2, 2.0),
    ("ancient_city_ice_box", "minecraft:chests/ancient_city_ice_box", "chest", "overworld", "minecraft:packed_ice", 2, 0.8),
    ("trial_chambers_entrance", "minecraft:chests/trial_chambers/entrance", "chest", "trial_chambers", "minecraft:trial_key", 2, 1.2),
    ("trial_chambers_corridor", "minecraft:chests/trial_chambers/corridor", "chest", "trial_chambers", "minecraft:tuff_bricks", 2, 1.0),
    ("trial_chambers_supply", "minecraft:chests/trial_chambers/supply", "chest", "trial_chambers", "minecraft:barrel", 2, 1.0),
    ("trial_chambers_intersection", "minecraft:chests/trial_chambers/intersection", "chest", "trial_chambers", "minecraft:diamond_pickaxe", 2, 1.6),
    ("trial_chambers_intersection_barrel", "minecraft:chests/trial_chambers/intersection_barrel", "chest", "trial_chambers", "minecraft:barrel", 2, 1.4),
    ("resource_cache", "ultraloot:generator/resource_cache", "chest", "ultraloot", "minecraft:raw_iron", 2, 1.5),
    # Tier III - stronghold, nether, mansion, trial vaults
    ("stronghold_corridor", "minecraft:chests/stronghold_corridor", "chest", "stronghold", "minecraft:stone_bricks", 3, 1.8),
    ("stronghold_crossing", "minecraft:chests/stronghold_crossing", "chest", "stronghold", "minecraft:cracked_stone_bricks", 3, 1.8),
    ("stronghold_library", "minecraft:chests/stronghold_library", "chest", "stronghold", "minecraft:bookshelf", 3, 2.2),
    ("nether_bridge", "minecraft:chests/nether_bridge", "chest", "nether", "minecraft:nether_bricks", 3, 2.0),
    ("bastion_bridge", "minecraft:chests/bastion_bridge", "chest", "nether", "minecraft:polished_blackstone_bricks", 3, 2.2),
    ("bastion_hoglin_stable", "minecraft:chests/bastion_hoglin_stable", "chest", "nether", "minecraft:crimson_fungus", 3, 2.2),
    ("bastion_other", "minecraft:chests/bastion_other", "chest", "nether", "minecraft:gilded_blackstone", 3, 2.2),
    ("piglin_bartering", "minecraft:gameplay/piglin_bartering", "barter", "nether", "minecraft:gold_ingot", 3, 1.2),
    ("woodland_mansion", "minecraft:chests/woodland_mansion", "chest", "overworld", "minecraft:dark_oak_planks", 3, 2.5),
    ("trial_chambers_reward", "minecraft:chests/trial_chambers/reward", "chest", "trial_chambers", "minecraft:vault", 3, 2.5),
    ("trial_chambers_reward_common", "minecraft:chests/trial_chambers/reward_common", "chest", "trial_chambers", "minecraft:wind_charge", 3, 1.8),
    ("trial_chambers_reward_rare", "minecraft:chests/trial_chambers/reward_rare", "chest", "trial_chambers", "minecraft:diamond_chestplate", 3, 2.5),
    ("nether_haul", "ultraloot:generator/nether_haul", "chest", "ultraloot", "minecraft:blaze_rod", 3, 2.0),
    ("arcane_vault", "ultraloot:generator/arcane_vault", "chest", "ultraloot", "minecraft:enchanted_book", 3, 2.8),
    # Tier IV - end game
    ("bastion_treasure", "minecraft:chests/bastion_treasure", "chest", "nether", "minecraft:netherite_scrap", 4, 3.0),
    ("end_city_treasure", "minecraft:chests/end_city_treasure", "chest", "end", "minecraft:purpur_block", 4, 3.5),
    ("ancient_city", "minecraft:chests/ancient_city", "chest", "overworld", "minecraft:sculk_catalyst", 4, 3.5),
    ("trial_chambers_reward_unique", "minecraft:chests/trial_chambers/reward_unique", "chest", "trial_chambers", "minecraft:trident", 4, 3.5),
    ("trial_chambers_reward_ominous", "minecraft:chests/trial_chambers/reward_ominous", "chest", "trial_chambers", "minecraft:ominous_trial_key", 4, 4.0),
    ("trial_chambers_reward_ominous_common", "minecraft:chests/trial_chambers/reward_ominous_common", "chest", "trial_chambers", "minecraft:ominous_bottle", 4, 2.5),
    ("trial_chambers_reward_ominous_rare", "minecraft:chests/trial_chambers/reward_ominous_rare", "chest", "trial_chambers", "minecraft:diamond_block", 4, 3.5),
    ("trial_chambers_reward_ominous_unique", "minecraft:chests/trial_chambers/reward_ominous_unique", "chest", "trial_chambers", "minecraft:heavy_core", 4, 5.0),
    ("void_hoard", "ultraloot:generator/void_hoard", "chest", "ultraloot", "minecraft:shulker_shell", 4, 3.5),
]

VILLAGES = {
    "armorer": ("minecraft:blast_furnace", 1.0), "butcher": ("minecraft:smoker", 0.8),
    "cartographer": ("minecraft:cartography_table", 0.8), "desert_house": ("minecraft:sandstone", 0.8),
    "fisher": ("minecraft:barrel", 0.8), "fletcher": ("minecraft:fletching_table", 0.8),
    "mason": ("minecraft:stonecutter", 0.8), "plains_house": ("minecraft:oak_log", 0.8),
    "savanna_house": ("minecraft:acacia_log", 0.8), "shepherd": ("minecraft:loom", 0.8),
    "snowy_house": ("minecraft:spruce_log", 0.8), "taiga_house": ("minecraft:spruce_planks", 0.8),
    "tannery": ("minecraft:cauldron", 0.8), "temple": ("minecraft:brewing_stand", 1.0),
    "toolsmith": ("minecraft:smithing_table", 1.2), "weaponsmith": ("minecraft:grindstone", 1.2),
}
for name, (icon, mult) in VILLAGES.items():
    PROFILES.append((f"village_{name}", f"minecraft:chests/village/village_{name}", "chest", "village", icon, 1, mult))

for pid, table, ctx, cat, icon, tier, mult in PROFILES:
    write(f"{NS}/ultraloot/loot_profiles/{pid}.json", {
        "loot_table": table, "context": ctx, "category": cat, "icon": icon,
        "min_tier": tier, "energy_multiplier": mult,
    })


# ---------------------------------------------------------------------------------------------------------------
# 2. Ultra Loot's own generator tables
# ---------------------------------------------------------------------------------------------------------------
def item(name, weight=1, lo=None, hi=None, extra=None):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    funcs = []
    if lo is not None:
        count = lo if hi is None or hi == lo else {"type": "minecraft:uniform", "min": float(lo), "max": float(hi)}
        funcs.append({"function": "minecraft:set_count", "count": count, "add": False})
    if extra:
        funcs.extend(extra)
    if funcs:
        e["functions"] = funcs
    return e


def empty(weight):
    return {"type": "minecraft:empty", "weight": weight}


def pool(rolls, entries, bonus=0.0):
    if isinstance(rolls, tuple):
        rolls = {"type": "minecraft:uniform", "min": float(rolls[0]), "max": float(rolls[1])}
    return {"rolls": rolls, "bonus_rolls": bonus, "entries": entries}


def table(name, pools):
    write(f"{NS}/loot_table/generator/{name}.json", {
        "type": "minecraft:chest", "pools": pools, "random_sequence": f"{NS}:generator/{name}",
    })


RANDOM_BOOK = [{"function": "minecraft:enchant_randomly", "options": "#minecraft:on_random_loot"}]

table("farmers_crate", [
    pool((3, 6), [
        item("minecraft:wheat", 20, 3, 9), item("minecraft:carrot", 15, 2, 6), item("minecraft:potato", 15, 2, 6),
        item("minecraft:beetroot", 10, 2, 5), item("minecraft:apple", 10, 1, 3), item("minecraft:bread", 10, 1, 4),
        item("minecraft:pumpkin", 5, 1, 2), item("minecraft:melon_slice", 8, 2, 6), item("minecraft:sugar_cane", 8, 1, 4),
        item("minecraft:egg", 6, 1, 3), item("minecraft:sweet_berries", 6, 2, 5), item("minecraft:cocoa_beans", 4, 1, 3),
    ]),
    pool((1, 3), [
        item("minecraft:wheat_seeds", 20, 2, 6), item("minecraft:beetroot_seeds", 10, 1, 4),
        item("minecraft:pumpkin_seeds", 8, 1, 3), item("minecraft:melon_seeds", 8, 1, 3),
        item("minecraft:bone_meal", 12, 2, 6),
    ]),
    pool(1, [
        empty(70), item("minecraft:golden_carrot", 15, 1, 2), item("minecraft:honey_bottle", 10, 1, 2),
        item("minecraft:golden_apple", 2), item("minecraft:torchflower_seeds", 2), item("minecraft:pitcher_pod", 1),
    ]),
])

table("resource_cache", [
    pool((3, 5), [
        item("minecraft:coal", 20, 3, 8), item("minecraft:raw_iron", 18, 2, 5), item("minecraft:raw_copper", 18, 3, 7),
        item("minecraft:iron_ingot", 12, 1, 4), item("minecraft:redstone", 15, 3, 8), item("minecraft:lapis_lazuli", 12, 2, 6),
        item("minecraft:raw_gold", 8, 1, 3), item("minecraft:gold_ingot", 5, 1, 2), item("minecraft:quartz", 6, 2, 5),
        item("minecraft:iron_nugget", 10, 4, 12), item("minecraft:gold_nugget", 8, 3, 9), item("minecraft:flint", 6, 1, 3),
    ]),
    pool(1, [
        empty(60), item("minecraft:amethyst_shard", 15, 1, 4), item("minecraft:emerald", 12, 1, 2),
        item("minecraft:diamond", 6), item("minecraft:iron_block", 4), item("minecraft:redstone_block", 3),
    ]),
])

table("nether_haul", [
    pool((3, 5), [
        item("minecraft:quartz", 20, 3, 9), item("minecraft:glowstone_dust", 15, 2, 6), item("minecraft:nether_wart", 12, 1, 4),
        item("minecraft:magma_cream", 8, 1, 3), item("minecraft:gold_nugget", 15, 4, 12), item("minecraft:soul_sand", 10, 2, 6),
        item("minecraft:blackstone", 10, 4, 10), item("minecraft:crimson_fungus", 5, 1, 2), item("minecraft:warped_fungus", 5, 1, 2),
        item("minecraft:shroomlight", 4, 1, 2),
    ]),
    pool(1, [
        empty(40), item("minecraft:blaze_rod", 20, 1, 2), item("minecraft:ghast_tear", 10), item("minecraft:crying_obsidian", 12, 1, 3),
        item("minecraft:gold_ingot", 12, 1, 3), item("minecraft:netherite_scrap", 1),
    ]),
])

table("arcane_vault", [
    pool((1, 2), [
        item("minecraft:book", 30, extra=RANDOM_BOOK),
        item("minecraft:book", 10, extra=[{"function": "minecraft:enchant_with_levels", "levels": {"type": "minecraft:uniform", "min": 20.0, "max": 30.0}, "options": "#minecraft:on_random_loot"}]),
    ]),
    pool((2, 4), [
        item("minecraft:experience_bottle", 25, 2, 6), item("minecraft:lapis_lazuli", 20, 3, 9),
        item("minecraft:amethyst_shard", 15, 2, 6), item("minecraft:book", 12, 1, 3), item("minecraft:paper", 10, 2, 6),
        item("minecraft:glowstone_dust", 8, 2, 5), item("minecraft:ender_pearl", 6, 1, 2), item("minecraft:emerald", 6, 1, 3),
    ]),
    pool(1, [
        empty(85), item("minecraft:golden_apple", 10), item("minecraft:ender_eye", 5),
    ]),
])

table("void_hoard", [
    pool((3, 5), [
        item("minecraft:chorus_fruit", 20, 2, 6), item("minecraft:popped_chorus_fruit", 12, 1, 4),
        item("minecraft:end_stone", 15, 4, 12), item("minecraft:purpur_block", 10, 2, 6), item("minecraft:ender_pearl", 15, 1, 4),
        item("minecraft:end_rod", 6, 1, 3), item("minecraft:obsidian", 8, 2, 4), item("minecraft:experience_bottle", 10, 2, 5),
    ]),
    pool(1, [
        empty(50), item("minecraft:shulker_shell", 18, 1, 2), item("minecraft:dragon_breath", 12, 1, 2),
        item("minecraft:diamond", 12, 1, 2), item("minecraft:ender_eye", 8, 1, 2),
    ]),
    pool(1, [
        empty(90),
        item("minecraft:diamond_sword", 4, extra=[{"function": "minecraft:enchant_with_levels", "levels": {"type": "minecraft:uniform", "min": 20.0, "max": 39.0}, "options": "#minecraft:on_random_loot"}]),
        item("minecraft:diamond_pickaxe", 4, extra=[{"function": "minecraft:enchant_with_levels", "levels": {"type": "minecraft:uniform", "min": 20.0, "max": 39.0}, "options": "#minecraft:on_random_loot"}]),
        item("minecraft:book", 2, extra=RANDOM_BOOK),
    ]),
])

# ---------------------------------------------------------------------------------------------------------------
# 3. Block loot tables: each machine drops itself
# ---------------------------------------------------------------------------------------------------------------
for m in MACHINES:
    write(f"{NS}/loot_table/blocks/{m}.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1.0, "bonus_rolls": 0.0,
            "conditions": [{"condition": "minecraft:survives_explosion"}],
            "entries": [{"type": "minecraft:item", "name": f"{NS}:{m}"}],
        }],
        "random_sequence": f"{NS}:blocks/{m}",
    })

# ---------------------------------------------------------------------------------------------------------------
# 4. Tags
# ---------------------------------------------------------------------------------------------------------------
def tag(rel, values):
    write(rel, {"replace": False, "values": values})


tag(f"{NS}/tags/block/storage_targets.json", [
    "minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel", "#minecraft:shulker_boxes",
    opt("#c:chests"), opt("#c:barrels"),
])
tag(f"{NS}/tags/block/scan_blacklist.json", [
    "minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker", "minecraft:hopper", "minecraft:brewing_stand",
    "minecraft:dispenser", "minecraft:dropper", "minecraft:crafter", "minecraft:jukebox", "minecraft:chiseled_bookshelf",
    "minecraft:lectern", "minecraft:decorated_pot", "minecraft:beacon", "minecraft:campfire", "minecraft:soul_campfire",
])
machine_ids = [f"{NS}:{m}" for m in MACHINES]
tag("minecraft/tags/block/mineable/pickaxe.json", machine_ids)
tag("minecraft/tags/block/needs_iron_tool.json", machine_ids)

tag(f"{NS}/tags/item/loot_filter/resources.json", [
    opt("#c:ingots"), opt("#c:gems"), opt("#c:nuggets"), opt("#c:dusts"), opt("#c:raw_materials"),
    opt("#c:ores"), opt("#c:storage_blocks"),
    "#minecraft:coal_ores", "#minecraft:iron_ores", "#minecraft:copper_ores", "#minecraft:gold_ores",
    "#minecraft:redstone_ores", "#minecraft:lapis_ores", "#minecraft:diamond_ores", "#minecraft:emerald_ores",
    "minecraft:coal", "minecraft:charcoal", "minecraft:redstone", "minecraft:lapis_lazuli", "minecraft:quartz",
    "minecraft:amethyst_shard", "minecraft:emerald", "minecraft:diamond", "minecraft:netherite_scrap",
    "minecraft:ancient_debris", "minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:copper_ingot",
    "minecraft:netherite_ingot", "minecraft:raw_iron", "minecraft:raw_gold", "minecraft:raw_copper",
    "minecraft:iron_nugget", "minecraft:gold_nugget", "minecraft:glowstone_dust", "minecraft:prismarine_shard",
    "minecraft:prismarine_crystals", "minecraft:flint", "minecraft:echo_shard",
    "minecraft:iron_block", "minecraft:gold_block", "minecraft:copper_block", "minecraft:diamond_block",
    "minecraft:emerald_block", "minecraft:lapis_block", "minecraft:redstone_block", "minecraft:coal_block",
    "minecraft:netherite_block", "minecraft:raw_iron_block", "minecraft:raw_gold_block", "minecraft:raw_copper_block",
])
tag(f"{NS}/tags/item/loot_filter/food.json", [
    opt("#c:foods"), opt("#c:seeds"), opt("#c:crops"),
    "minecraft:wheat", "minecraft:wheat_seeds", "minecraft:beetroot_seeds", "minecraft:melon_seeds",
    "minecraft:pumpkin_seeds", "minecraft:torchflower_seeds", "minecraft:pitcher_pod", "minecraft:sugar",
    "minecraft:sugar_cane", "minecraft:egg", "minecraft:cocoa_beans", "minecraft:pumpkin", "minecraft:melon",
    "minecraft:cake", "minecraft:milk_bucket", "minecraft:nether_wart", "minecraft:hay_block",
    "minecraft:brown_mushroom", "minecraft:red_mushroom", "minecraft:bamboo", "minecraft:cactus", "minecraft:honeycomb",
])
tag(f"{NS}/tags/item/loot_filter/equipment.json", [
    opt("#c:tools"), opt("#c:armors"),
    "minecraft:bow", "minecraft:crossbow", "minecraft:trident", "minecraft:shield", "minecraft:mace",
    "minecraft:leather_horse_armor", "minecraft:iron_horse_armor", "minecraft:golden_horse_armor",
    "minecraft:diamond_horse_armor", "minecraft:wolf_armor", "minecraft:saddle", "minecraft:elytra",
    "minecraft:turtle_helmet",
])
tag(f"{NS}/tags/item/upgrade_cards.json", [f"{NS}:{c}" for c in CARDS])

tag(f"{NS}/tags/entity_type/mob_card_blacklist.json", [
    "minecraft:ender_dragon", "minecraft:wither", "minecraft:player", "minecraft:armor_stand", "minecraft:giant",
    "minecraft:iron_golem", "minecraft:snow_golem", "minecraft:villager", "minecraft:wandering_trader",
    opt("#c:bosses"),
])

# ---------------------------------------------------------------------------------------------------------------
# 5. Mob simulation data map (unlisted mobs: 8 XP per kill, 1.0x energy).
# piglin, piglin_brute, pillager and illusioner are omitted: their 1.21.1 tables yield nothing in simulation
# (piglin drops are equipment; the pillager bottle needs a raid captain).
# ---------------------------------------------------------------------------------------------------------------
MOBS = {
    # passive / farm animals
    "chicken": (3, 0.6), "cow": (4, 0.7), "mooshroom": (5, 0.8), "pig": (3, 0.6), "sheep": (3, 0.6),
    "rabbit": (4, 0.8), "goat": (3, 0.6), "horse": (4, 0.7), "donkey": (4, 0.7), "mule": (4, 0.7),
    "llama": (4, 0.7), "trader_llama": (4, 0.7), "hoglin": (10, 1.0), "strider": (6, 0.9), "panda": (4, 0.7),
    "polar_bear": (5, 0.8), "turtle": (8, 1.0), "parrot": (3, 0.6), "cat": (3, 0.6), "ocelot": (3, 0.6),
    "fox": (3, 0.6),
    # water
    "cod": (2, 0.5), "salmon": (2, 0.5), "tropical_fish": (2, 0.5), "pufferfish": (3, 0.6), "dolphin": (6, 0.8),
    "squid": (4, 0.7), "glow_squid": (6, 0.9), "drowned": (8, 1.0),
    "guardian": (20, 1.6), "elder_guardian": (120, 4.0),
    # overworld hostiles
    "zombie": (6, 1.0), "husk": (6, 1.0), "zombie_villager": (6, 1.0), "zombie_horse": (6, 0.8),
    "skeleton": (6, 1.0), "stray": (8, 1.1), "bogged": (8, 1.1), "skeleton_horse": (6, 0.8),
    "spider": (6, 1.0), "cave_spider": (6, 1.0), "creeper": (8, 1.1), "slime": (6, 1.0), "silverfish": (4, 0.6),
    "phantom": (10, 1.2), "witch": (15, 1.4), "vindicator": (14, 1.3),
    "evoker": (60, 3.0), "ravager": (30, 2.0), "breeze": (30, 2.0),
    "warden": (250, 6.0),
    # nether
    "blaze": (20, 1.5), "ghast": (24, 1.6), "magma_cube": (10, 1.2), "wither_skeleton": (40, 2.5),
    "zombified_piglin": (10, 1.2), "zoglin": (10, 1.0),
    # end
    "enderman": (18, 1.5), "endermite": (4, 0.6), "shulker": (50, 2.5),
}
write(f"{NS}/data_maps/entity_type/mob_simulation.json", {
    "values": {f"minecraft:{k}": {"xp_cost": v[0], "energy_multiplier": v[1]} for k, v in MOBS.items()},
})


# ---------------------------------------------------------------------------------------------------------------
# 6. Recipes
# ---------------------------------------------------------------------------------------------------------------
def ing(x):
    if isinstance(x, list):
        return [ing(i) for i in x]
    return {"tag": x[1:]} if x.startswith("#") else {"item": x}


def shaped(name, pattern, key, result, count=1, category="misc"):
    write(f"{NS}/recipe/{name}.json", {
        "type": "minecraft:crafting_shaped", "category": category,
        "key": {k: ing(v) for k, v in key.items()}, "pattern": pattern,
        "result": {"id": result, "count": count},
    })


def shapeless(name, ingredients, result, count=1, category="misc"):
    write(f"{NS}/recipe/{name}.json", {
        "type": "minecraft:crafting_shapeless", "category": category,
        "ingredients": [ing(i) for i in ingredients], "result": {"id": result, "count": count},
    })


U = lambda p: f"{NS}:{p}"  # noqa: E731
M = lambda p: f"minecraft:{p}"  # noqa: E731
SOUL = [M("soul_sand"), M("soul_soil")]

# Components
shaped("machine_frame", ["IGI", "GRG", "IGI"], {"I": M("iron_ingot"), "G": M("glass"), "R": M("redstone")}, U("machine_frame"))
shaped("logic_circuit", ["CRC", "RIR", "CRC"], {"C": M("copper_ingot"), "R": M("redstone"), "I": M("iron_ingot")}, U("logic_circuit"), 2)
shaped("enhanced_circuit", ["GRG", "LDL", "GRG"], {"G": M("gold_ingot"), "R": M("redstone"), "L": U("logic_circuit"), "D": M("diamond")}, U("enhanced_circuit"))
shaped("refined_circuit", ["EAE", "QCQ", "ESE"], {"E": M("emerald"), "A": M("amethyst_shard"), "Q": M("quartz"), "C": U("enhanced_circuit"), "S": M("netherite_scrap")}, U("refined_circuit"))
shaped("stellar_circuit", ["DBD", "ENE", "DRD"], {"D": M("diamond"), "B": M("dragon_breath"), "E": M("end_crystal"), "N": M("nether_star"), "R": U("refined_circuit")}, U("stellar_circuit"))
shaped("loot_core", ["GLG", "ECE", "GLG"], {"G": M("gold_ingot"), "L": M("lapis_lazuli"), "E": M("ender_pearl"), "C": M("chest")}, U("loot_core"))
shaped("soul_core", ["FBF", "SRS", "FTF"], {"F": M("rotten_flesh"), "B": M("bone"), "S": SOUL, "R": M("redstone"), "T": M("string")}, U("soul_core"))
shaped("blank_card", ["PRP", "PNP"], {"P": M("paper"), "R": M("redstone"), "N": M("gold_nugget")}, U("blank_card"), 2)
shapeless("blank_mob_card", [U("blank_card"), SOUL, M("bone")], U("blank_mob_card"))
shaped("experience_capsule", [" I ", "GEG", "GRG"], {"I": M("iron_ingot"), "G": M("glass"), "E": M("emerald"), "R": M("redstone")}, U("experience_capsule"))

# Cards. Level I: blank card + category materials. Level II: card I + enhanced circuit. Level III: card II + refined circuit.
CARD_L1 = {
    "speed": (["SRS", "RBR", "SRS"], {"S": M("sugar"), "R": M("redstone")}),
    "quantity": ([" H ", "CBC", " R "], {"H": M("hopper"), "C": M("chest"), "R": M("redstone")}),
    "range": ([" E ", "RBR", " M "], {"E": M("ender_pearl"), "R": M("redstone"), "M": M("compass")}),
    "efficiency": (["CLC", "LBL", "CLC"], {"C": M("copper_ingot"), "L": M("lapis_lazuli")}),
    "loot": (["GFG", "LBL", "GLG"], {"G": M("gold_ingot"), "F": M("rabbit_foot"), "L": M("lapis_lazuli")}),
}
# (M = corners, A = edges) for levels II and III
CARD_UP = {
    "speed": ((M("sugar"), M("glowstone_dust")), (M("redstone_block"), M("blaze_powder"))),
    "quantity": ((M("iron_ingot"), M("chest")), (M("diamond"), M("hopper"))),
    "range": ((M("amethyst_shard"), M("ender_pearl")), (M("ender_eye"), M("spyglass"))),
    "efficiency": ((M("lapis_lazuli"), M("gold_ingot")), (M("lapis_block"), M("emerald"))),
    "loot": ((M("gold_ingot"), M("lapis_block")), (M("diamond"), M("rabbit_foot"))),
}
for card, (pattern, key) in CARD_L1.items():
    shaped(f"{card}_card_1", pattern, dict(key, B=U("blank_card")), U(f"{card}_card_1"))
    (m2, a2), (m3, a3) = CARD_UP[card]
    shaped(f"{card}_card_2", ["MAM", "AXA", "MCM"], {"M": m2, "A": a2, "X": U(f"{card}_card_1"), "C": U("enhanced_circuit")}, U(f"{card}_card_2"))
    shaped(f"{card}_card_3", ["MAM", "AXA", "MCM"], {"M": m3, "A": a3, "X": U(f"{card}_card_2"), "C": U("refined_circuit")}, U(f"{card}_card_3"))

# Machines
shaped("loot_generator_t1", ["ILI", "CFC", "IKI"], {"I": M("iron_ingot"), "L": U("logic_circuit"), "C": M("chest"), "F": U("machine_frame"), "K": U("loot_core")}, U("loot_generator_t1"))
shaped("mob_loot_generator_t1", ["ILI", "WFW", "IKI"], {"I": M("iron_ingot"), "L": U("logic_circuit"), "W": M("iron_sword"), "F": U("machine_frame"), "K": U("soul_core")}, U("mob_loot_generator_t1"))
for fam in ("loot_generator", "mob_loot_generator"):
    shaped(f"{fam}_t2", ["GEG", "DMD", "GGG"], {"G": M("gold_ingot"), "E": U("enhanced_circuit"), "D": M("diamond"), "M": U(f"{fam}_t1")}, U(f"{fam}_t2"))
    shaped(f"{fam}_t3", ["ERE", "EME", "ENE"], {"E": M("emerald"), "R": U("refined_circuit"), "M": U(f"{fam}_t2"), "N": M("netherite_ingot")}, U(f"{fam}_t3"))
    shaped(f"{fam}_t4", ["CSC", "DMD", "CNC"], {"C": M("shulker_shell"), "S": U("stellar_circuit"), "D": M("diamond_block"), "M": U(f"{fam}_t3"), "N": M("nether_star")}, U(f"{fam}_t4"))

if __name__ == "__main__":
    print(f"wrote {len(WRITTEN)} files")
