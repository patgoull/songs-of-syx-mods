# Strategy: mods as suggestions for the dev

**Goal:** make improvements the dev (Jake) can *inspect and integrate*, not just mods players install.
That changes the usual modding priorities.

## 1. Core principle: write patches, not hacks

| | Hook/reflection mod (usual advice) | **Patch-style mod (our choice)** |
|---|---|---|
| How | Reach into private fields at runtime, inject UI | Copy the vanilla class into the same package, make a minimal marked edit |
| Compatible with other mods | Good | Weaker (two mods replacing the same class conflict) |
| Survives game updates | Sometimes | Must be rebased (mechanical with git) |
| **Dev can integrate it** | Has to rewrite it | **Reads the diff and pastes it** |

Verified in V71.44: mod jars are put on the classpath **before** `SongsOfSyx.jar`
(`snake2d/util/process/Proccesser.java`), so a same-named class in our jar replaces the vanilla one.
There's precedent too: `view/tool/PLACER_TYPE.java` ships with a `// modded` comment above the shape list.

Rules for every change:
- **Smallest possible diff.** Touch as few classes and lines as possible. Mark each edit with `// GOUL: <why>`.
- **Jake's style:** tabs, `snake2d.util.sets.*` collections, UI strings as `¤¤` fields + `D.ts(...)` so they're translatable,
  no Lombok or other libraries.
- **One feature = one patch.** He should be able to cherry-pick, so features never depend on each other.
- **Every feature ships with:** a `.patch` file against vanilla, a short README (problem, change, how to test), and ideally a GIF.
- **Bug fixes first.** Small, obviously correct fixes are the cheapest way to build trust.

## 2. Tooling: drop the template, keep its knowledge

The template's Maven setup is built for "normal" code mods: Lombok, shading, JUnit, and a wrapper that downloads Maven
(blocked by the sandbox here). We don't need any of that. Plan:

```
SongOfGoul/
├── vanilla/<version>/      ← extracted SongsOfSyx-sources.jar (local only, gitignored)
├── features/<feature>/
│   ├── src/...             ← replaced classes, same package paths as the game
│   ├── assets/...          ← optional config/sprite/text files
│   └── README.md
├── build.ps1               ← javac + jar, writes _Info.txt, installs to %APPDATA%\songsofsyx\mods
├── patches/                ← generated: one .patch per feature, diffed against vanilla/<version>
└── docs/
```

- `build.ps1 <feature>` (or `-All`) compiles with the JDK already installed (`javac 21`) against `SongsOfSyx.jar`,
  builds one mod per feature (plus an optional "bundle" mod for players), and regenerates the `.patch` files.
- On a game update: re-extract `vanilla/<new>`, 3-way apply the patches (`git apply -3`), fix any conflicts, rebuild.
  A check script lists which of our replaced classes changed upstream.
- IntelliJ (already installed) can open the folder as a plain project with `SongsOfSyx.jar` + sources as a library,
  so you get navigation and debugging without Maven.
- Copyright: replaced classes are Jake's code. Keep the repo private, or publish only patches, until he says otherwise.

## 3. Roadmap

1. **Tooling** (above) plus one trivial feature to prove the loop: build, install, launch, see it in-game.
2. **Build-tools polish pack** (first real target; findings below).
3. Share with Jake (Discord modding channel / Steam Workshop): one message per patch, tiny and self-explanatory.
4. Repeat on other systems once the loop is smooth.

## 4. Build system: findings in V71.44

Code map:
- `view/tool/`: the generic placement tool (drag shapes, size, undo, hover info). `PlacableMultiTool` = area dragging,
  `PlacableFixedTool` = fixed items (rotate/size), `PLACER_TYPE` = the shapes.
- `settlement/room/main/placement/`: room placement logic (area, furniture items, doors, walls, undo history: `UtilHistory`, 1024 steps).
- `view/sett/ui/room/construction/`: the room-construction panel (shape buttons `SShape`, items `SItems`, stats `SStats`, materials `SMaterial`).
- `settlement/room/main/copy/`: copy/blueprint ("prints") system.

Candidates, from smallest to largest:

| # | Type | What | Where | Size |
|---|---|---|---|---|
| A | Bug | Placing **canals**: the overlay/foundation toggle buttons are built but never shown. `getAdditionalButt()` fills `li` and then returns `super.getAdditionalButt()` (always `null`) instead of `li`. | `UIRoomPlacer.PlacerItemSingleArea` | 1 line |
| B | Polish | While dragging, the size readout (`12x5 (60)`) only appears when **both** width and height are > 1, so straight lines (roads, walls, 1-wide strips) show no length. Show `N` (and OK-tile count) for 1-wide shapes too. | `PlacableMulti.placeInfo` | ~5 lines |
| C | Augment | **New drag shapes** in the shape popup: e.g. diagonal line, plus/cross, diamond, ring with thickness. The button list is built from `PLACER_TYPE.all`, so adding shapes means touching only `PLACER_TYPE` (+ icons). | `view/tool/PLACER_TYPE.java` | small–medium |
| D | Augment | Shape picking only happens through a popup. Add a hotkey to cycle shapes and remember the last shape across tools (today it's remembered per tool via `placable.previous`). Hotkeys need `view.keyboard.KEYS` (more invasive). | `PlacableMultiTool`, `KEYS` | medium |
| E | Augment | Bigger ideas to scope later: symmetry/mirror placement, measure tool, live "rooms of this type nearby" coverage during placement, improvements to the blueprint copier. | various | large |

### User requests (2026-11-02), now top priority

**F. Select, move and duplicate furniture items while designing or refurbishing.** Done: `features/room-builder-plus`.
Not in the game. The only item tools are place, "Remove Item" (drag) and undo. The Room Copier and Blueprints work on *whole rooms* only.
- Building blocks already exist in `settlement.room.main.placement` (all package-private, so the new code lives in that package):
  `SETT.ROOMS().fData.item.get / itemX1Y1 / itemClear / itemSet`, `placability.itemProblem(...)` for validation,
  `history.placeItem(it, x, y, ±1)` for undo. History entries are grouped per game tick, so a 20-item move is **one Ctrl+Z**.
- Proposed UX: a "Select" tool next to Remove Item. Drag a rectangle to select items, then they follow the cursor as ghosts
  (green/red per item), **arrow keys nudge by 1 tile**, click to drop (only if all fit), right-click/Esc puts them back.
  Modifier = duplicate instead of move (design one row, stamp it 10 times). Rotating a selection is v2.
- Design decision for Jake: when refurbishing, moved *built* items are rebuilt (resources refunded, then re-spent), same as vanilla remove + place.
  No free teleporting of finished furniture.
- Size: one new class (~250 lines) + small hooks in `RoomPlacer` and `view/sett/ui/room/construction/SItems`.

**G. Pause a large design without losing it.**
Not in the game. The design in progress is a single temporary "embryo" room (`PLACEMENT`/`Instance`). Leaving the tool clears it
(`Config.deactivateAction` → `placer.init(null, 0)`), and it's never written to saves (`PLACEMENT.factory.save` is empty).
- Option 1 (small, recommended first): **"Save design as blueprint"** button in the construction panel. The embryo is a `Room`, and the
  existing blueprint system (`SavedPrints.push(Room, x, y)`) stores blueprints in the player profile across saves and games.
  Later: open Blueprints, place it, keep editing. Unverified: whether *planned* (not yet real) walls/doors are captured correctly.
- Option 2 (larger): keep the embryo alive when leaving the tool and resume it on return, and save it in the savegame.
  More invasive: the embryo blocks tiles, and only one can exist at a time.

Workaround that exists today for misaligned rooms: Room Copier copies a whole planned room to a shifted position. That doesn't help for 20 stations inside one room.

Not worth reporting: in `PlacableMultiTool.gui()` the `+`/`−` button condition (`any |= !usesSize`) looks inverted,
but no current tool's shape restrictions make it visible.
