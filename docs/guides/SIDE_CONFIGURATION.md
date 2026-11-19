# Side configuration guide

Each of the six faces of a machine has its own role, set in the **Side Configuration** tab. Faces are named
relative to the machine's front (Front, Back, Left, Right, Top, Bottom — Left/Right as seen when looking at the
front), so the configuration keeps its meaning however the machine is placed.

In the tab the faces are shown as an unfolded cube. Left-click a face to go to the next role, right-click for the
previous one. Changes are validated by the server and applied immediately (pipes/cables reconnect).

| Role | Loot Generator | Mob Loot Generator |
|---|---|---|
| None | nothing connects | nothing connects |
| Energy Input | accepts FE | accepts FE |
| Item Target | an inventory touching this face may receive loot | — |
| XP Input | — | accepts XP (capability, `c:experience` fluids, bottles/capsules via hoppers) |
| Item Output | — | pushes output to the adjacent inventory; pipes may extract |

Defaults:

| Face | Loot Generator | Mob Loot Generator |
|---|---|---|
| Front | Item Target | None |
| Back | Energy Input | Energy Input |
| Left | Item Target | XP Input |
| Right | Item Target | None |
| Top | Item Target | XP Input |
| Bottom | Energy Input | Item Output |

Notes:
- Inventories that are not touching the Loot Generator are used if they are in range, regardless of faces.
- Internal inventories are never reachable through a face that doesn't have the matching role: upgrade slots and
  the Mob Card slot are never exposed to automation, and the output can only be extracted, never inserted into.
