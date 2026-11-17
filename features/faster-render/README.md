# Faster Render

## Problem

With a big city (10k people, 2560×1440, furthest zoom), the game spends most of a frame in Java building the sprite
list on the updater thread; the GPU needs under 1 ms. A Java Flight Recorder profile (`.\run.ps1 -Profile`, then
`python tools\jfr-hot.py <file> --thread Thread-2 --under settlement/main/SETT.render`) showed a cost that doesn't
change the picture:

| Share of `SETT.render` | Where | Why |
|---|---|---|
| 10.9% (`LOS_MAP.get`) of 18.6% (`PointRayTracer.init`) | every visible light, every frame | `LOS_MAP.get` is called on every ray step for both tiles (the first is the previous step's second) and up to 24 more times per tile in the corner pass. Each call goes through the room blueprint map before the tile map. |

## Code changes

| File | Change |
|---|---|
| `settlement/thing/pointlight/PointRayTracer.java` | A per-light cache of the tiles' `LOS` around the light (`tileDiameter+2` square). `init` starts a new light with `losNew`, the four `SETT.LIGHTS().los().get` calls go through `los()`, which looks each tile up once. Tiles outside the square (rays go one row past it) are read directly as before. Entries are invalidated with a counter, not cleared. |

Same results: the cache returns the objects `LOS_MAP.get` would (the map doesn't change during one light's `init`,
which runs on the updater thread).

## Measured

Same city, shares of `SETT.render` (scenes differ a bit between runs):

| | Vanilla | Light cache |
|---|---|---|
| `LOS_MAP.get` | 10.9% | 5.5% |
| `TileMap.LOS` | 6.4% | 3.1% |
| `PointRayTracer.init` | 18.6% | 17.1% |

The light lookups halved, about 5% of `SETT.render`. Most of the tracer's time is the corner pass itself
(`cornercheck`), so the gain on the whole tracer is smaller.

## Tried and dropped

- **Single-tile furniture wear** (`RoomSingleton.degrader()` building the room area wrapper for every tile drawn,
  ~6% of `SETT.render`, while `getDegrade` only reads the stored data). Skipping the wrapper saved most of that, but
  the edit relied on which tile `ini()` was last called with, which is easy to break. Left as an idea for the dev:
  `getDegrade` could read the data without the wrapper.

## Test checklist

Enable **Goul - Faster Render** in the launcher (with or without Live Zoom), then in a big city:
- [ ] At night, lights look as before: walls block light, light comes through doorways, corners are lit the same
- [ ] Build or remove a wall next to a torch: its light changes right away
- [ ] Profile again: `LOS_MAP.get` takes about half its old share of `SETT.render`
