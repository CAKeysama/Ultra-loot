# Known limitations

Technical / design limits of the current version. Each one is intentional or caused by the platform.

1. **Ticking chunks only.** Machines work while their chunk is entity-ticking (inside simulation distance or
   force-loaded by another mod). They never load chunks and pause when the chunk unloads; progress, energy, XP,
   items and statistics are saved and resume on load.
2. **Loot needs context.** Profiles run tables with the parameter set they were written for. Conditions that need
   data the machine cannot provide simply fail. Fishing uses a detached open-water bobber, so treasure can roll;
   conditions based on biome/weather at the hook use the machine position.
   Tables with `minecraft:exploration_map` (directly or via nested table references) are rejected (repeated
   structure searches would lag the server). Global loot modifiers that *inject* such functions are not detected.
3. **Mob drops are loot-table drops.** Equipment a mob would spawn wearing, and drops that need a specific killer
   (e.g. music discs from creepers killed by skeletons), are not produced. Every simulated kill counts as a player
   kill with an iron sword (Looting from the Loot Card).
4. **Mob entity instances.** The simulator constructs one entity instance per machine for loot context and never
   adds it to the world. A modded entity that throws while being constructed outside normal spawning is reported as
   "cannot be simulated".
5. **Filters void items.** Items removed by a Loot Card filter are discarded before they exist (statistic "filtered").
6. **Overflow buffer.** If a modded inventory rejects items after accepting them in simulation, the Loot Generator
   keeps them in an internal buffer and pauses until they fit. Vanilla inventories never trigger this.
7. **Only block-entity inventories are scanned** (chests, barrels, shulker boxes, modded storage BEs). Block-only
   inventories (e.g. composter) are ignored. Range is a cube, distance = max(|dx|,|dy|,|dz|).
8. **XP fluid compatibility** relies on the common `c:experience` fluid tag at 20 mB per point (configurable).
   XP is stored as points, is input-only for automation and is never exposed as a fluid.
9. **Breaking a machine** drops its cards, inventories and items; stored FE is lost; stored XP is released as orbs.
10. **Energy input rate** is limited per transfer call (tier value), not strictly per tick.
11. **Minecraft version.** Built for 1.21.1 (the most widely used NeoForge version). NeoForge 26.1 is expected to
    become the next long-lived modding version; a port would be required.
12. **GUI validation** was done by code review and compilation; it could not be rendered in this environment
    (no graphical client available to the agents). See docs/TEST_REPORT.md.
13. **Recipe book.** Recipes have no unlock advancements, so the vanilla recipe book does not list them (crafting
    works normally; use JEI/EMI or the README recipe list).
14. **Mobs without loot.** Mobs whose loot table is empty (e.g. piglin: its loot is equipment) cannot be imprinted.
15. **Moving XP orbs.** The Range card collects orbs every 20 ticks; orbs drifting quickly through the radius between
    passes can be missed.
16. **Not yet validated by hand.** The GUI has been compiled and smoke-loaded but not checked in-game by a person;
    no test with third-party pipes, generators or XP fluid mods has been run.
