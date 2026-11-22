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
12. **GUI validation** is partly manual; see docs/TEST_REPORT.md for what was checked in a client and what was not.
13. **Recipe book.** Fixed in 0.2.0: recipes unlock through recipe advancements when a key ingredient is picked up.
14. **Mobs without loot.** Since 0.2.0 a mob whose native table exists but rolls nothing (for example the goat) is valid:
    it can be imprinted and the machine runs, producing nothing. Only mobs with no resolvable table are refused.
15. **Moving XP orbs.** The Range card collects orbs every 20 ticks; orbs drifting quickly through the radius between
    passes can be missed.
16. **Not yet validated by hand.** The GUI has been compiled and smoke-loaded but not checked in-game by a person;
    no test with third-party pipes, generators or XP fluid mods has been run.
17. **XP tank does not grow.** The FE buffer grows to one operation (resolved in 0.2.0, docs/BALANCE.md 7.2), but
    the XP tank keeps its tier size: an expensive mob with a Quantity Card can exceed a Tier I tank (red status
    "Cost exceeds XP tank"). An operation above 100,000,000 FE is refused ("Cost exceeds FE buffer"); no shipped
    profile or mob reaches it.
18. **Tier FE efficiency.** Resolved in 0.2.0: FE per roll / kill is non-increasing with tier (docs/BALANCE.md 7.1).
19. **Card wear is per kill, not per operation.** A Quantity Card wears the card faster in real time (more kills per
    operation). Wear numbers are placeholders until playtested.
20. **Mekanism is a dev-only test dependency.** Ultra Loot has no compile or runtime dependency on any other mod; the
    Mekanism 10.7.19 compatibility run uses a separate dev-only run (`runClientMekanism`); results in docs/TEST_REPORT.md.
