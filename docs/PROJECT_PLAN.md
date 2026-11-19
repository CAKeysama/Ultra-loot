# Ultra Loot — Project plan

Team: lead (Claude, orchestration + core Java + lang + docs) · architect · machine engineer · loot system engineer ·
UI & asset engineer (two instances: assets, GUI) · QA & release engineer. Definitions in `.claude/agents/`.

| # | Task | Owner | Depends on | State |
|---|---|---|---|---|
| 0.1 | Inspect repo/env, install JDK 21, choose platform | lead | — | COMPLETED |
| 0.2 | Technical research + TECHNICAL_DECISIONS.md | architect | 0.1 | COMPLETED |
| 1.1 | Gradle/MDG project, metadata, first build | lead | 0.1 | COMPLETED |
| 1.2 | CLAUDE.md, agent definitions, docs skeleton | lead | 1.1 | COMPLETED |
| 2.1 | Shared core: Balance, config, tiers, families, side config | lead | 1.1 | COMPLETED |
| 2.2 | Energy (FE) + XP storage/capability/fluid adapter | lead | 2.1 | COMPLETED |
| 2.3 | Upgrade cards, inventory, modifier service | lead | 2.1 | COMPLETED |
| 2.4 | Menus + payload sync | lead | 2.1 | COMPLETED |
| 3.1 | Loot Generator: block, BE, scanner, transactional insertion | lead | 2.* | COMPLETED |
| 3.2 | Loot engine, profiles, auto-discovery, filters | lead | 2.* | COMPLETED |
| 3.3 | Loot profiles / tables / tags / recipes / block loot (data) | loot-engineer | 3.2 | COMPLETED |
| 4.1 | Mob Loot Generator: BE, mob cards, simulator, XP intake, export | lead | 2.* | COMPLETED |
| 4.2 | Mob data map, mob card blacklist | loot-engineer | 4.1 | COMPLETED |
| 5.1 | GUI: screens, tabs, side config widget, GUI textures | ui-engineer-gui | 2.4 | COMPLETED |
| 6.1 | Block/item models + textures | ui-asset-engineer | 1.1 | COMPLETED |
| 6.2 | Lang en_us + pt_br | lead | 5.1, 3.3 | COMPLETED |
| 7.1 | Unit tests + GameTests + TEST_PLAN | qa-engineer | 3.1, 4.1 | COMPLETED |
| 7.2 | Integration, cross review, fixes | lead + qa + architect | all | COMPLETED (architect perf review not run: session limit) |
| 8.1 | Release candidate: JAR, README, guides, changelog, test report | lead + qa | 7.2 | COMPLETED (manual GUI checks pending) |

Note: the "machine engineer" role's scope (machine/, energy/, experience/, inventory/) was implemented directly by
the lead to keep the shared core consistent; the role definition stays available for follow-up work.
