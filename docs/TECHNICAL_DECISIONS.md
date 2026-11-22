# Ultra Loot — Technical Decisions

Status: verified on 2026-11-19 against the NeoForge 21.1.267 sources (incl. patched Minecraft 1.21.1) in
`C:\Users\conta\.mcsrc`, the vanilla data in `build/moddev/artifacts/neoforge-21.1.267-client-extra-aka-minecraft-resources.jar`,
docs.neoforged.net (1.21.1) and the source code of other mods. Each claim below was checked in one of those places.

## Corrections for the lead

1. **Fishing treasure never drops (bug in `loot/LootEngine#buildParams`, FISHING case).** Vanilla
   `gameplay/fishing.json` gates the treasure entry with `entity_properties` on `this` →
   `type_specific: {type: fishing_hook, in_open_water: true}`. `EntityPredicate#matches` returns `false` for a null
   entity, so without `THIS_ENTITY` you only ever get fish and junk. Fix: pass a detached, ownerless hook,
   `EntityType.FISHING_BOBBER.create(level)` (its `openWater` field starts as `true` and only changes in `tick()`),
   as `THIS_ENTITY`. Optionally pass the FakePlayer as `ATTACKING_ENTITY` (NeoForge allows it). Do **not** use
   `new FishingHook(Player, Level, int, int)`, because `setOwner` writes `player.fishing = this`. Vanilla also passes
   `withLuck(hook.luck + player.getLuck())`. If leaving treasure out is a deliberate gameplay choice, record it under
   Gameplay decisions instead.
2. **GameTests will be filtered out.** `build.gradle` sets `neoforge.enabledGameTestNamespaces=ultraloot`, and
   `GameTestRegistry.register(Method, Set)` filters by the test's *template namespace*
   (`GameTestHooks#getTemplateNamespace`). `@GameTestHolder` defaults that namespace to `"minecraft"`. Use
   `@GameTestHolder(UltraLoot.MOD_ID)`. Templates are loaded from `data/ultraloot/structure/<name>.nbt` (1.21 uses the
   singular `structure`, see `StructureTemplateManager.RESOURCE_LISTER`). The default name is
   `<classname>.<methodname>` in lowercase. You can also use `@PrefixGameTestTemplate(false)` +
   `@GameTest(template = "empty")` with one shared nbt. No `.nbt` exists yet, and `runGameTestServer` crashes when it
   finds no tests.
3. **`SimpleJsonResourceReloadListener` on 1.21.1 takes `(Gson gson, String directory)`.** 1.21.2+ tutorials use
   `(Codec, FileToIdConverter)` and do not apply here. The current code is correct. `AddReloadListenerEvent` is
   posted on `NeoForge.EVENT_BUS`. `@EventBusSubscriber(modid = …)` without `bus` defaults to the game bus, so
   `LootProfileManager` is correct. If you want `neoforge:conditions` evaluated, parse with `makeConditionalOps()`
   (inherited from `ContextAwareReloadListener`).
4. **`c:experience` facts.** NeoForge ships `Tags.Fluids.EXPERIENCE` as an **empty** tag (`data/c/tags/fluid/experience.json`).
   Its javadoc defines the standard as 20 mB per XP point. EnderIO (`ExperienceUtil.EXP_TO_FLUID = 20`), Cyclic
   (`ExpItemGain.FLUID_PER_EXP = 20`) and Industrial Foregoing (`SewerTile`: `orb.value * 20`) all use 20.
   Create: Enchantment Industry (1.21.1) uses its own fluid at about 1 mB/point (a Bottle o' Enchanting empties to
   10 mB) and does **not** tag it `c:experience`, so it is correctly incompatible. Ultra Loot registers no XP fluid,
   so the fluid view can only be fill-only (matches `ExperienceFluidAdapter`). Draining XP as fluid would need a
   tagged fluid chosen at runtime.
5. **`LootParams.Builder#create(set)`**: NeoForge disabled the "parameters not allowed" check (`if (false && …)`).
   Only *missing required* params throw `IllegalArgumentException`. `ReloadableServerRegistries.Holder#getLootTable`
   never throws and returns `LootTable.EMPTY` for unknown keys (already handled). Keep using the non-`Raw`
   `getRandomItems(...)` overloads: the `Raw` ones are `@Deprecated` because they skip Global Loot Modifiers.
6. **Data map file path**: `data/<map namespace>/data_maps/entity_type/<map path>.json`. The registry namespace is
   omitted for `minecraft`, see `DataMapLoader#getFolderLocation`. For `ultraloot:mob_simulation` that is
   `data/ultraloot/data_maps/entity_type/mob_simulation.json`. Read it with
   `type.builtInRegistryHolder().getData(TYPE)` or `BuiltInRegistries.ENTITY_TYPE.getData(TYPE, key)`. Both return
   `null` when there is no entry.
7. **Version context (not a blocker):** NeoForge's 26.1 release post (2026-03-24) says "There are strong indications
   that 26.1 will be the next stable modding version, replacing 1.21.1", and calls this "a personal educated guess,
   not an official statement". NeoForge has also declined to call any version "LTS". 1.21.1 is still the right
   target today, but the doc records this as a porting risk (see below).
8. Naming reminders: 1.21.1 uses `ResourceLocation` (renamed `Identifier` only in 1.21.11) and
   `registryOrThrow(...).getHolderOrThrow(...)` (renamed `lookupOrThrow`/`getOrThrow` in 1.21.2+).
   `EntityType#create(Level)` is `@Nullable`.

## Chosen versions

| Component | Version | Notes |
|---|---|---|
| Minecraft | 1.21.1 | release, no snapshots |
| NeoForge | 21.1.267 | newest `21.1.x` on maven.neoforged.net (checked 2026-11-19) |
| Java | 21 | required by MC 1.21.x; found by Gradle toolchain detection / foojay (no machine-specific path in the repo since 0.1.1) |
| ModDevGradle | 2.0.148 | newest `2.0.x` |
| Gradle wrapper | 9.2.1 | |
| Parchment | 2024.11.17 for 1.21.1 | parameter names only |
| JUnit | 5.11.3 (+ `net.neoforged:testframework:21.1.267`) | MDG `unitTest` |

**Why 1.21.1.** It is the NeoForge version with the largest mod and modpack ecosystem. The 2024 retrospective
reports "over 4000 mods are available for NeoForge on 1.21.1… This is our first version with wide adoption". The
2025 retrospective says "18 months after 1.21's release, it still remains the most popular NeoForge version, having
accumulated over 16000 mods". It is also still supported: "we will continue to support 1.21.1". Big packs (All of
NeoForge, The Great Tangle, and others) combine Mekanism, Ender IO and Create on 1.21.1, which is the audience for
a tech mod. NeoForge maven's newest line is 26.3.0.x, but newer lines have far fewer of the tech mods Ultra Loot
should sit next to.

## Alternatives considered

| Option | Rejected because |
|---|---|
| Newer MC lines (1.21.2–1.21.11, 26.x) | Smaller tech-mod ecosystem today, and frequent breaking API churn (reload listener codecs, `Identifier` rename, registry lookup renames, GameTest rewrite). 26.1 may become the next stable target, so revisit when the major tech mods have moved (porting risk, see limitations). |
| Forge (MinecraftForge) 1.21.1 | Most 1.21.x tech mods (Mekanism, EnderIO, Create 6, AE2) ship for NeoForge. Forge's capability and networking APIs differ, so we would have to support two codepaths for one audience. |
| Fabric / Quilt | No standard energy/item/fluid capability system shared with Mekanism/EnderIO (Fabric Transfer API / Team Reborn Energy are a separate ecosystem). Data maps, `ModConfigSpec` and GLMs are NeoForge-specific. |
| Multiloader (Architectury / MultiLoader template) | Doubles build and test surface for no target audience gain. Ultra Loot depends on NeoForge-only features (capabilities, data maps, GLM-aware loot). |
| Forge 1.20.1 | Old (Java 17, pre-components NBT item data, no data maps). Its ecosystem is large but frozen. |

## APIs used

| Concern | API (exact names, NeoForge 21.1.267) |
|---|---|
| Energy | `net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK` → `BlockCapability<IEnergyStorage, @Nullable Direction>`; `net.neoforged.neoforge.energy.IEnergyStorage` / `EnergyStorage` |
| Items | `Capabilities.ItemHandler.BLOCK` → `BlockCapability<IItemHandler, @Nullable Direction>`; `net.neoforged.neoforge.items.IItemHandler`, `ItemStackHandler` |
| Fluids | `Capabilities.FluidHandler.BLOCK` → `BlockCapability<IFluidHandler, @Nullable Direction>`; `net.neoforged.neoforge.fluids.capability.IFluidHandler`, `FluidStack`, `Tags.Fluids.EXPERIENCE` (`c:experience`) |
| Custom XP capability | `BlockCapability.createSided(ResourceLocation, Class<T>)` → `BlockCapability<IExperienceStorage, @Nullable Direction>` id `ultraloot:experience` (`BlockCapability.create(name, type, contextClass)` is the general form) |
| Capability registration | `RegisterCapabilitiesEvent` (mod bus): `registerBlockEntity(cap, BlockEntityType, ICapabilityProvider)`, `registerBlock(...)`, `registerItem(...)` |
| Neighbour lookups | `BlockCapabilityCache.create(cap, ServerLevel, BlockPos, context[, BooleanSupplier isValid, Runnable onInvalidate])`, `#getCapability()`; `ILevelExtension#getCapability(cap, pos, ctx)`; invalidate with `Level#invalidateCapabilities(BlockPos)` whenever a capability appears, disappears or changes (e.g. side config) |
| Ticking | `EntityBlock#getTicker(Level, BlockState, BlockEntityType)` → `BlockEntityTicker` |
| Loot lookup | `MinecraftServer#reloadableRegistries()` → `ReloadableServerRegistries.Holder#getLootTable(ResourceKey<LootTable>)` (returns `LootTable.EMPTY` if absent), `#getKeys(Registries.LOOT_TABLE)` |
| Loot rolling | `LootParams.Builder(ServerLevel)` `.withParameter/.withOptionalParameter/.withLuck/.create(LootContextParamSet)`; `LootTable#getRandomItems(LootParams)` (applies GLMs) |
| Contexts | `LootContextParamSets.CHEST` (req ORIGIN; opt THIS_ENTITY, ATTACKING_ENTITY), `FISHING` (req ORIGIN, TOOL; opt THIS_ENTITY, ATTACKING_ENTITY), `ARCHAEOLOGY` (req ORIGIN; opt THIS_ENTITY), `PIGLIN_BARTER` (req THIS_ENTITY), `ENTITY` (req THIS_ENTITY, ORIGIN, DAMAGE_SOURCE; opt ATTACKING_ENTITY, DIRECT_ATTACKING_ENTITY, LAST_DAMAGE_PLAYER), `EMPTY` |
| Mob simulation | `EntityType#create(Level)` (detached, never `addFreshEntity`); `LivingEntity#getLootTable()`; `LivingEntity#getExperienceReward(ServerLevel, Entity)`; `net.neoforged.neoforge.common.util.FakePlayerFactory.get(ServerLevel, GameProfile)` → `FakePlayer`; `DamageSources#playerAttack(Player)`; Looting via `registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.LOOTING)` + `ItemStack#enchant(Holder<Enchantment>, int)`. `EnchantedCountIncreaseFunction` reads the level from `ATTACKING_ENTITY`'s slot items |
| Per-mob costs | `net.neoforged.neoforge.registries.datamaps.DataMapType.builder(id, Registries.ENTITY_TYPE, codec)[.synced(codec, mandatory)].build()`; `RegisterDataMapTypesEvent#register` (mod bus) |
| Loot profiles | `SimpleJsonResourceReloadListener(Gson, "ultraloot/loot_profiles")` → `data/<ns>/ultraloot/loot_profiles/*.json`; `AddReloadListenerEvent#addListener` (game bus) |
| Networking | `RegisterPayloadHandlersEvent#registrar(version)` (mod bus) → `PayloadRegistrar#playToServer/playToClient(CustomPacketPayload.Type, StreamCodec, IPayloadHandler)`; handlers run on the main thread by default (`HandlerThread.MAIN`) |
| Config | `net.neoforged.neoforge.common.ModConfigSpec` (+ `.Builder`), `ModContainer#registerConfig(ModConfig.Type.SERVER, spec)` |
| Tests | MDG `neoForge.unitTest { enable(); testedMod = mods.ultraloot }` + JUnit 5; GameTests: `@GameTestHolder(MOD_ID)`, `@PrefixGameTestTemplate`, `@GameTest`, `RegisterGameTestsEvent`, `./gradlew runGameTestServer` |

## Dependencies

- **Required:** none besides NeoForge 21.1.x and Minecraft 1.21.1.
- **Optional integrations:** none at runtime. There are no compile or runtime dependencies on other mods.
  Interop goes only through NeoForge capabilities (FE, items, fluids), the `c:experience` fluid tag, the
  `ultraloot:experience` capability (other mods can query it by id), data maps and datapacks.

## Design decisions (technical)

- Server-authoritative: menus sync display data via `ContainerData` / payloads. Every C2S payload is validated
  (player distance, menu ownership, value ranges) before it is applied.
- Simulate-then-commit for every transfer: `IItemHandler#insertItem(..., simulate=true)`,
  `IEnergyStorage#extractEnergy(..., true)` and `FluidAction.SIMULATE` all run before execute, so items, FE and XP
  can never be lost or duplicated. Output that does not fit stays in an internal buffer that is persisted.
- No per-tick world scans: neighbour handlers are reached through `BlockCapabilityCache`. Machines work on cycle
  intervals from `config/Balance`.
- One XP unit internally (XP points, `long`/`int`). Fluid conversion uses a configurable 20 mB/point, the NeoForge
  standard.
- Loot is rolled through vanilla loot tables with a real context, so datapacks and Global Loot Modifiers apply.
  There are no hard-coded drop lists.
- Mob simulation uses detached entities. One cached instance per machine and type (`SimulatedEntityCache`),
  repositioned at the machine. It is never added to the level, so no spawn/death events, AI ticks or world side
  effects happen.
- Per-mob costs come from a data map (`ultraloot:mob_simulation`) so packs can rebalance without code. Missing
  entries fall back to `Balance` defaults.
- Datapack loot profiles (`data/<ns>/ultraloot/loot_profiles/*.json`) let packs add or override which tables
  machines can roll.
- Server-type config (`ModConfig.Type.SERVER`): it is per world (`<world>/serverconfig`) and synced to clients.

### Gameplay decisions

Decided by the lead under the brief's criteria (simplicity, balance, predictability). Numbers live in `config/Balance`.

| # | Open point | Decision | Why |
|---|---|---|---|
| G1 | Project name | **Ultra Loot**, mod id `ultraloot` ("LootCore" was the obsolete codename, renamed by the owner) | owner request |
| G2 | When FE is paid | Whole E_final paid at the end of a *successful* operation; progress only advances while the buffer holds one operation's cost | "no FE for operations that can't complete" without partial states |
| G3 | Insertion | In-memory simulation of every target, round robin per stack starting one target later each stack, spill-over, all-or-nothing | transactional, predictable distribution |
| G4 | Commit divergence (modded inventory rejects after simulating OK) | Persisted overflow buffer, machine pauses until flushed | never void or drop items |
| G5 | Item Target face meaning (Loot Generator) | Inventories touching the machine are used only through Item Target faces; non-adjacent inventories in range are always eligible | prevents filling adjacent batteries/machines; keeps range scanning simple |
| G6 | Which inventories | "Storage only" (tag `ultraloot:storage_targets`, default) or "Any inventory" (minus `ultraloot:scan_blacklist`); block-entity inventories in loaded chunks, cube range | avoids stuffing furnaces/hoppers by default; never loads chunks |
| G7 | Loot sources | Datapack loot profiles + auto-discovered `chests/*` tables of every namespace; tables with `exploration_map` rejected | other mods' chest loot works out of the box; avoids structure-search lag |
| G8 | Context-dependent tables | Context adapters: chest, fishing (detached bobber), archaeology, barter (detached piglin), empty | never run a table without the parameters it needs |
| G9 | Quantity Card | Adds independent rolls (x2/x3/x4); never multiplies stack sizes | keeps loot-table rules |
| G10 | Loot Card | Level = luck (Loot Gen) / Looting (Mob Gen) 1-3; any Loot Card unlocks filters All/Resources/Food/Equipment (remove-only) | honest: filters can't create loot; luck only matters where tables use quality |
| G11 | Range Card on the Mob Loot Generator | Collects XP orbs within 2/4/6 blocks | gives the slot a purpose and a vanilla XP input path |
| G12 | Card compatibility | All 15 cards shared by every tier of both families | simplest; family-specific effects are documented on the tooltip |
| G13 | Card replacement | Shift-click a higher level replaces the installed one and returns the old card; equal/lower is not swapped | "higher level replaces lower" |
| G14 | Energy floor | E_final >= 25% of E_base, >= 1 FE | no free operations with Efficiency |
| G15 | Mob simulation | Detached mob instance + FakePlayer killer with an iron sword (Looting from the card), counts as player kill | player-only drops and Looting behave like vanilla; no entity in the world |
| G16 | Mob XP cost | Data map `ultraloot:mob_simulation` (xp_cost, energy_multiplier); default 8 XP, 1.0x; cost x kills per operation | datapack-tunable, valuable mobs cost more |
| G17 | XP input | XP slot (Bottle o' Enchanting = 7, Experience Capsule filled by the player's own action), `ultraloot:experience` capability, `c:experience` fluids at 20 mB/point (fill only), orb collection | no Mekanism dependency; the machine never drains a player |
| G18 | Mob Card acquisition | Use a Blank Mob Card on a living mob (bosses/blacklist excluded); creative tab has presets | works for every modded mob |
| G19 | Output inventory | 27 slots (chest-sized), take-only for players, extract-only for automation, active push from Item Output faces | brief; 54 left as a constant change (`MOB_OUTPUT_SLOTS`) |
| G20 | Mob tier progression | kills/op 1,1,2,2 · XP tank 1k/4k/12k/40k · XP input 50/150/400/1000 · E_base 1.2k/2.4k/4.5k/9k · 300-tick base cycle | tier speed already scales throughput; kills add value at T3 |
| G21 | Breaking a machine | Drops cards and inventories, releases stored XP as orbs, FE is lost | no item/XP loss, simple |
| G22 | Side config storage | Per relative side (front/back/left/right/top/bottom) | survives placing the machine in any orientation |
| G23 | GUI sync | Server snapshot (CompoundTag) every 5 ticks, sent only when changed; C2S actions validated twice | minimal traffic, server-authoritative |
| G24 | ACTIVE blockstate | 40-tick hysteresis | avoids block-update/light spam when FE hovers around the cost |
| G25 | Loot Generator has no Side tab in the brief | Added a Side Configuration tab to both families | the brief requires per-face config for both machines |
| G26 | Cost above the FE buffer / XP tank (0.1.1) | Dedicated statuses `ENERGY_BUFFER_TOO_SMALL` / `XP_TANK_TOO_SMALL`; nothing consumed, progress frozen. Buffer sizing unchanged (owner decision, docs/BALANCE.md P1-01) | keeps G2 (pay once, on success) and every number; replaces a misleading "Not enough energy" |
| G27 | Buried Treasure cost (0.1.1) | `energy_multiplier` 2.0 → 3.5 | cheapest diamond source in the mod; floor = cheapest Tier I source (docs/BALANCE.md B-01) |

## Known technical limitations

- **Ticking only in entity-ticking chunks.** `LevelChunk#isTicking` requires `FullChunkStatus.BLOCK_TICKING` *and*
  `ServerLevel#areEntitiesLoaded`. Machines stop outside simulation distance or in lazy chunks. Ultra Loot does not
  force-load chunks; players use a chunk-loader mod.
- **Loot tables need the right context.** Tables are rolled with their declared param set. A missing required param
  throws, which is caught and the profile is treated as unusable. Conditions whose entity is absent evaluate to
  `false` (e.g. fishing treasure needs `THIS_ENTITY` = a fishing hook with `isOpenWaterFishing()`; barter needs a
  Piglin instance). Tables that need block context (`BLOCK`, `SHEARING`, …) are out of scope.
- **Mob simulation ≠ real kills.** Only the loot table (plus GLMs) is rolled. Not included: `dropCustomDeathLoot`
  (equipment drops, wither-skeleton/creeper-head style hard-coded drops), `LivingDropsEvent`,
  `LivingExperienceDropEvent` and the other death-event hooks of other mods. Entities are not `finalizeSpawn`ed, so
  variants and sizes are the constructor defaults (slime size, sheep colour → white wool, etc.). Modded entities whose
  constructors need world state may fail `EntityType#create`; they are treated as unsupported.
- **Looting is enchantment-based (data-driven).** The `Enchantment` holder must come from the level's registry
  access. Datapacks that redefine `minecraft:looting` change the result.
- **FakePlayer is shared.** `FakePlayerFactory` caches one instance per (level, GameProfile). The main-hand item must
  always be reset (done in a `finally`). Never keep a strong reference across level unloads.
- **XP fluid output** is impossible without a fluid in `c:experience` (the tag is empty in NeoForge itself). Mods
  with a different ratio or no tag (e.g. Create: Enchantment Industry, ~1 mB/point) are not compatible.
- **Server config** values are only available after the config loads (server start). Reading them during
  registration or in static initialisers throws.
- **Porting risk:** NeoForge maintainers expect 26.1 to replace 1.21.1 as the stable modding version. Porting will
  touch reload listeners (codec-based), `ResourceLocation`→`Identifier`, registry lookup names and GameTests.

## References (consulted)

- NeoForge sources 21.1.267 (local `C:\Users\conta\.mcsrc`): `capabilities/*`, `registries/datamaps/*`,
  `registries/DataMapLoader`, `network/registration/PayloadRegistrar`, `gametest/GameTestHooks`, `common/Tags`,
  `common/util/FakePlayerFactory`, Minecraft `LootParams`, `LootTable`, `LootContextParamSets`,
  `ReloadableServerRegistries`, `SimpleJsonResourceReloadListener`, `LevelChunk`, `FishingHook`,
  `EntityPredicate`, `EnchantedCountIncreaseFunction`, `GameTestRegistry`, `StructureTemplateManager`
- Vanilla data: `neoforge-21.1.267-client-extra-aka-minecraft-resources.jar` → `data/minecraft/loot_table/gameplay/fishing.json`
- https://docs.neoforged.net/docs/1.21.1/inventories/capabilities/
- https://docs.neoforged.net/docs/1.21.1/resources/server/datamaps/
- https://docs.neoforged.net/docs/1.21.1/networking/payload/
- https://docs.neoforged.net/docs/1.21.1/misc/config/
- https://docs.neoforged.net/docs/1.21.1/misc/gametest/
- https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/ and `/glm/`
- https://github.com/neoforged/ModDevGradle (README, `unitTest` block)
- https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml (latest 21.1.267; newest 26.3.0.113)
- https://neoforged.net/news/2024-retrospection/
- https://neoforged.net/news/2025-retrospection/
- https://neoforged.net/news/26.1release/
- https://github.com/Team-EnderIO/EnderIO (branch 1.21.1) `enderio/src/main/java/com/enderio/enderio/foundation/util/ExperienceUtil.java`
- https://github.com/Lothrazar/Cyclic `src/main/java/com/lothrazar/cyclic/block/expcollect/ExpItemGain.java`
- https://github.com/InnovativeOnlineIndustries/Industrial-Foregoing `.../agriculturehusbandry/tile/SewerTile.java`
- https://github.com/DragonsPlusMinecraft/CreateEnchantmentIndustry (branch 1.21.1/6.0.0-dev) `recipe/emptying/experience_bottle.json`
- https://www.curseforge.com/minecraft/modpacks/all-of-neoforge, https://moddex.gg/modpack/the-great-tangle (1.21.1 packs combining Create, Mekanism, Ender IO)
