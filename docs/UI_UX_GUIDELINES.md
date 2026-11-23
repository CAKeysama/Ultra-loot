# Ultra Loot: UI/UX guidelines

Applies to every Ultra Loot screen: Loot Generator, Mob Loot Generator and Card Restorer. Code lives in
`client/screen/` (`MachineScreen` base, `GuiDraw` colours and status indicators, `GuiSprites`, `GuiFormat`);
the textures come from `tools/generate_gui_textures.py`.

## 1. Principles

1. **Information first.** Every pixel shows machine state: bars, short labels, numbers. Explanations go into
   tooltips, never into permanent paragraphs.
2. **Compact, Mekanism-inspired layout.** 176-px-wide panel, vertical FE/XP bars on the left, status and progress
   at the top of the text column, key/value rows below. Secondary information sits behind side tabs.
3. **Never colour alone.** Every status has its own indicator shape and text; colour only reinforces it.
4. **One design language.** Same insets (`GuiDraw.inset`), row helper (`GuiDraw.row`), number formatting
   (`GuiFormat`), tab icons and tooltip structure (title line, then gray detail lines) on every screen.
5. **Localized.** Every visible string is a translation key (`tools/lang/*.py` → `en_us`, `pt_br`). No literal
   text, except numbers and units built by `GuiFormat`.
6. **Truncate, don't overflow.** Long text is cut with an ellipsis (`GuiDraw.trimmed`) and the full text goes into
   the tooltip.

## 2. Colour and status semantics

| Class | Colour (ARGB) | Indicator (7×7) | Meaning | Statuses |
|---|---|---|---|---|
| Processing | cyan `0xFF3FD8F0` | filled square | the machine is working right now | `RUNNING` |
| Ready / OK | green `0xFF5BE08A` | (values only) | a value is fine: supported mob, intact card, enough FE | — |
| Inactive | gray `0xFF8A919C` | hollow square | nothing configured yet | `NO_PROFILE`, `NO_MOB_CARD` (Restorer: idle) |
| Warning | amber `0xFFFFB13B` | `!` | waiting for a resource that arrives by itself | `NO_ENERGY`, `NO_XP`, `OUTPUT_FULL`, `TARGETS_FULL`, `NO_TARGETS` |
| Blocked | red `0xFFFF5A5A` | `×` | the player must change something | `INVALID_PROFILE`, `PROFILE_LOCKED`, `INVALID_MOB`, `ENERGY_BUFFER_TOO_SMALL`, `XP_TANK_TOO_SMALL`, `NO_LOOT_SOURCE` |

The source of truth is `GuiDraw.statusClass(MachineStatus)`. A new status must be added there, get a lang key
`ultraloot.status.<name>` and a row in this table.

Card integrity follows the same scale: green while intact, amber while decaying, red when corrupted. The text
(`Integrity 87%`) and the tooltip state line say the same thing in words.

## 3. Machine GUI inventory

| Screen | Tab | Contents |
|---|---|---|
| Loot Generator | Overview | FE bar, status, progress, selected profile, FE per operation, targets summary |
| | Upgrades | 4 upgrade slots + per-card effect rows |
| | Loot config | scrollable profile list (icon, name, min tier, FE multiplier), filter buttons (need a Loot Card) |
| | Inventory scan | scrollable list of nearby inventories with their state (target / full / not storage / blacklisted / face disabled / not picked); **All in range / Picked only** button (tooltip: targets, range, picked count); in Picked only mode each row has a checkbox and a click ticks / unticks it; Rescan button |
| | Side config | 6-face widget, click cycles the mode (None, Energy Input, Item Target) |
| | Statistics | operations, FE spent, items generated / filtered |
| Mob Loot Generator | Overview | FE bar, XP bar, status (+ loot-source reason), progress, mob, XP/op, FE/op, output free slots, XP item slot |
| | Upgrades | as above |
| | Mob config | card slot, mob name, supported state, **card integrity**, XP/op, kills/op, Looting, filter, 3D mob preview |
| | Inventory | 27-slot output |
| | Side config | None, Energy Input, XP Input (also accepts Mob Cards), Item Output |
| | Statistics | operations, FE, XP, items |
| Card Restorer | single view | FE bar, status, input card slot, card integrity bar, output slot, FE per wear point |

## 4. Tooltip inventory

| Element | Lines |
|---|---|
| FE bar (all machines) | "Energy"; stored / max FE; FE per operation |
| XP bar (Mob) | "Experience"; stored / max XP; XP per operation |
| Progress bar | progress %; time per operation |
| Status (cut off) | full status text |
| Status `NO_LOOT_SOURCE` (Mob) | status; reason (`ultraloot.gui.loot_problem.{no_loot_table, fallback_missing, supplemental_missing}`) |
| Mob name / preview | mob name; supported / not supported |
| Card integrity (Mob config) | integrity %; state (intact / decaying / corrupted); kills before decay; Rotten Flesh chance and FE cost %; repair hint |
| Kills / Looting rows | what the number means |
| Filter buttons | filter name; locked without a Loot Card |
| Profile rows | name; min tier; FE multiplier; loot table; locked reason |
| Scan rows | inventory name; position; state reason |
| Side widget | face; mode; click to cycle |
| Slots (empty) | slot name; what it accepts (ghost icon drawn in the slot) |
| Mob Card item | mob tier; XP per kill; FE cost % of base (wear included); integrity %; decaying/corrupted line; repair hint |

## 5. GUI validation checklist

Run in the dev client (`./gradlew runClient`). For each machine and tier I–IV:

- [ ] Opens without log errors; title shows the tier numeral in the tier colour.
- [ ] Every tab opens; no text overlaps or leaves the panel in `en_us` and `pt_br` (pt_br strings are longer).
- [ ] Every status listed in section 2 can be reached and shows the right colour **and** indicator shape.
- [ ] Red statuses: `XP_TANK_TOO_SMALL` (Tier I Mob Loot Generator, evoker card + Quantity Card III) and
      `ENERGY_BUFFER_TOO_SMALL` (only above `Balance.MAX_OPERATION_ENERGY`; with the dynamic buffer it is not
      reachable with shipped data: mark it BLOCKED in the report unless a datapack forces it).
- [ ] Bars match the numbers in their tooltips; FE bar max follows the dynamic capacity.
- [ ] Long names (modded mob, long profile) are cut with an ellipsis and complete in the tooltip.
- [ ] Shift-click moves cards, XP items and Mob Cards to the right slots; nothing can be put in a wrong slot.
- [ ] Mob Card scenarios: new card (no bar, "Integrity 100%"); worn but intact (green bar); decaying (amber bar,
      Rotten Flesh appears in the output, FE/op rises); corrupted (red bar, only Rotten Flesh, 2× FE); repaired in
      the Card Restorer (back to 100%, normal loot).
- [ ] Removing the card resets progress; card wear never changes progress by itself.
- [ ] Card Restorer: idle (gray), no energy (amber), running (cyan), output full (amber); FE is only spent while
      wear goes down.
- [ ] GUI scale 1–4 and a 1280×720 window: panel and tooltips stay on screen.
