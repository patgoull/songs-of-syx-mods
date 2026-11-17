# Faster Load

## Problem

Loading a 10k-people save (768×768 map) took 6.2 s on the updater thread. A Java Flight Recorder profile
(`.\run.ps1 -Profile`, then the sample timeline of `GameSaver.load`) split it into:

| Phase | Wall time | Note |
|---|---|---|
| reading the save and the rest | ~0.6 s | |
| `ENVIRONMENT.init` | 3.4 s | `SettEnvMap.init` (flood fill per chunk) ~2.5 s, `SettOrganics.init` ~0.6 s |
| `Minimap.putPixels` | 1.4 s | only ~0.45 s of it Java CPU |
| `PATHING.init` | 0.6 s | |

Two of these have a cause that can be removed without changing the result:

- **Minimap upload.** `Minimap.putPixels` uploads the minimap in 32×32 tiles: 24×24 = 576 calls to
  `TextureHolder.putPixelBatch`, each a `CORE.GlJob` the loading thread waits for in a `sleep(1)` loop. About 0.9 s is
  that waiting.
- **Organics walls.** `SettOrganics.init` updates every 32×32 chunk with a margin. Its `QuickSum` tests `wall.is` on a
  105×105 area per chunk (6.3M calls for 576 chunks, on a 590k-tile map), and `getDist` scans up to 31 tiles in four
  directions from every tile. `wall.is` goes through the room map, the terrain tile and its availability.

## Code changes

| File | Change |
|---|---|
| `snake2d/TextureHolder.java` | New `putPixelBatches`: `putPixelBatch` for many batches of the same size, uploaded in one `GlJob` (slices of one buffer). |
| `util/rendering/Minimap.java` | `putPixels` collects the tiles (same pixels, same `getC` random grit in the same order) and calls `putPixelBatches` once. Also used by the world map. |
| `init/paths/VirtualFolder.java` | For the game's roots (each enabled mod's `V71` folder, then the vanilla data zip), whether a folder exists in each root is checked once and remembered; file lookups skip roots that don't have the folder. Nothing under a missing folder can exist, and the roots don't change while the game runs. Only for `PATHS.i.paths`, not other roots (saves, settings) that can change. |
| `settlement/environment/EUpdater.java` | `flood2()` (the environment flood, per 16×16 chunk over its 48×48 bounds) keeps its open tiles in a binary max-heap of arrays over the bounds instead of the shared `Flooder`'s red-black tree of tile objects. Same float values and the same push checks; all costs are positive, so the result doesn't depend on the order tied tiles come out in. Neighbours that are already closed are skipped (the push ignored them), and each tile's `getCost()` is read once per flood instead of once per neighbour. Also used for chunk updates while playing. |
| `settlement/environment/SettEnvShape.java` | `getWallDIR()` is stored per tile while `SettEnvMap.init()` runs; the chunks overlap, so each tile's wall direction was worked out many times. |
| `settlement/environment/SettEnvMap.java` | `init()` turns that cache on and off (2 lines). |
| `settlement/environment/SettOrganics.java` | `QuickSum`, `update` and `getDist` use `wallIs()` instead of `wall.is`. Once `QuickSum` has counted the walls of the 104×104 square around the chunk being updated, `wallIs()` reads a tile's own count from it (the square holds every tile `getDist` reaches), so each tile's `wall.is` runs once per chunk update instead of up to ~125 times. This also runs while playing, when a changed chunk is updated. Before that, during `init()`, the walls of the whole map are read into a bitmap once, because the chunks overlap; it's dropped at the end of `init()`. Tiles outside both are asked with `wall.is` as before. |

The walls don't change during one chunk update or during `init()` (both run on one thread), so the result is the same.

Still the same design: each chunk floods its 3×3 chunk area, so every source is flooded ~9 times. One flood of the
whole map could take paths outside those boxes and give slightly different values, and the shared flooder and
`EUpdater` statics mean it can't simply run on several threads. That's a design decision for the dev.

The new flood was checked against vanilla in three test builds that ran both on every chunk and compared the result
tile by tile: 0 of ~1.5 million tiles differed each time.

## Measured

**Simulation** (it isn't only loading: the flood and the organics walls also run while playing, when something is built
or removed). `tools\ab.ps1 -B "Goul - Faster Load" -Mode sim -Runs 3`, the same 10k save at the game's max speed (250x),
which this city can't reach: the speed reached went from **26x to 31.5x (+21%)**, ranges 24.8-26.6 vs 30-32.1. In a
vanilla profile of that run the environment was 23% of the game update: `SettEnvMap` chunk floods 13%, `SettOrganics` 9.5%.

**Loading:** A/B with the autopilot (`tools\ab.ps1 -B "Goul - Faster Load" -Mode load -Runs 3`, same save, 3 runs each, no other
mods): Continue to the first game update **9.67 s → 5.97 s (-38%)**, every run within 0.1 s.

**Before the save steps** (the loading screen after Continue, while `GAME` builds all game data): with 6 code-only
mods enabled, about 1.5 s of it was Windows file checks (`Files.exists`). Every asset lookup (race sounds, icons, room
sprites...) first checks each mod's folder, which doesn't exist for a mod without assets; vanilla is in `data.zip`,
which is checked in memory. Continue → save load start: **3.8 s → 2.3 s**; file-check samples 75 → 3. The rest is
decoding images and unzipping. Without mods this part doesn't change; with many mods it saves more.

**The save steps:**

Same 10k save, wall time on the loading thread (first to last profiler sample):

| Phase | Vanilla | Faster Load |
|---|---|---|
| `GameSaver.load` (whole load) | 6.22 s | **4.50 s** |
| `ENVIRONMENT.init` | 3.32 s | 2.72 s |
| `SettOrganics.init` | 0.83 s | 0.28 s |
| `Minimap.putPixels` | ~1.4 s | one sample (under 20 ms) |
| `SettEnvMap.init` (unchanged) | 2.47 s | 2.44 s |
| `PATHING.init` (unchanged) | 0.52 s | 0.52 s |

With the environment flood and shape changes (from the check build, where vanilla and new floods ran side by side,
vanilla's share taken out):

| | Vanilla | Faster Load |
|---|---|---|
| Environment flood (`EUpdater` `flood2`) | 129 samples | 56 samples (2.3× faster) |
| `SettEnvMap.init` | 2.44 s | ~1.35 s |
| Loading step "Generating 1/22" (`ENVIRONMENT.init`) | 2.72 s (vanilla) | 1.71 s (measured) |
| Whole save load (`GameSaver.load`) | 6.22 s | 3.47 s (measured) |

## Test checklist

Enable **Goul - Faster Load** in the launcher, then:
- [ ] Load a big save: the minimap (top right and the full map) looks as before
- [ ] The world map minimap looks as before
- [ ] The organics overlay (environment) looks as before
- [ ] Mods with assets still work (their sprites, sounds and texts replace vanilla as before)
- [ ] The environment overlays (noise, space, water, urbanisation, roundness, squareness) look as before
- [ ] `.\run.ps1 -Profile`: `Minimap.putPixels` and `SettOrganics.init` take less time in the load timeline
