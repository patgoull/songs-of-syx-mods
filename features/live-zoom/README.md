# Live Zoom (test)

## Problem

The city view has 4 zoom levels (0 to 3, each half the scale of the one before; a tile is 64, 32, 16 then 8 px).
Levels 0 to 2 are drawn live. Level 3 is the semi-map: each tile is a small flat square and people are dots.
Zooming out past level 3 opens the minimap. So the furthest live view shows about 120 × 68 tiles at 1080p.

## What it changes

Level 3 is drawn live, like levels 0 to 2 (about 240 × 135 tiles at 1080p). Zooming out past it still opens the minimap.
The semi-map isn't used any more.

This is a throwaway test to see whether the live render holds up at that zoom. The risk is the engine's sprite buffer
(`snake2d/VboSpriteOpti`): it holds 131072 sprites per frame and silently drops the rest. Measured with the first
version of this test, at level 3 in the same city:

| Canvas | Average | Peak | Samples over 131072 |
|---|---|---|---|
| 1440×810 (window) | ~47k | 60,511 | 0 of 11 |
| 2560×1440 (full screen) | ~126k | 168,323 | 8 of 19 |

So the buffer is doubled to 262144 (about 6 MB more memory). Frame rate is the other thing to watch.

`VboSpriteOpti.flush()` uploads sprites in chunks of 4096 but drew each layer's range to the layer's end, not the
chunk's, so above 65536 sprites it read past the 65536-entry point index buffer (`ElementArrays.pointBind`) and drew
leftover buffer contents. Each draw is now clamped to its chunk.

The point light buffer (`snake2d/VboLightPoint`) has the same kind of limit: 30,000 lit tiles per frame (each light
adds one per tile it lights), and lights past it are dropped whole. At night at level 3 the same city asked for up to
66,785, so lights blinked in and out as the view moved. Levels 0 to 2 stay under 5,000. The buffer is raised to
131072 (about 6 MB). It's drawn in one call through the shared point index buffer, which had 65536 entries, so that is
raised to 131072 too (512 KB more).

## Code changes

| File | Change |
|---|---|
| `settlement/main/SETT.java` | `render()`: the `zoomout == 3` branch to `renderSemiMap` is turned off. Test only: logs last frame's sprite count every 3 seconds (`GOUL live-zoom:` lines in the log) |
| `snake2d/VboSpriteOpti.java` | Sprite buffer `1<<17` → `1<<18`. `flush()`: each draw is clamped to the uploaded chunk (2 lines) |
| `snake2d/VboLightPoint.java` | Light buffer `10000 * 3` → `1<<17`. Test only: logs the most lit tiles asked for in a frame every 3 seconds |
| `snake2d/ElementArrays.java` | Point index buffer `1<<16` → `1<<17` |

## Test checklist

Enable **Goul - Live Zoom (test)** in the launcher, or run `.\run.ps1`, then in a big city:
- [ ] Zoom out to the last level before the minimap: the city is drawn live (buildings, people, lights), not as squares
- [ ] Pan around the densest part: no missing or flickering sprites
- [ ] Frame rate is acceptable (compare with level 2)
- [ ] Night: lights and shadows look right, and no whole light blinks out while panning at level 3
- [ ] In `build/game.log`, the `light tiles peak` lines stay under 131072
- [ ] Overlays (e.g. a heat map from the minimap panel) and room placement still work at that level
- [ ] Zoom out once more: the minimap opens. Zoom back in from the minimap: back to the live view
- [ ] In `build/game.log`, the `GOUL live-zoom: zoom 3` lines: the sprite count stays under 262144
- [ ] Full screen at a high resolution: no holes in the ground or floors while panning, and no OpenGL errors in the log
- [ ] Zoom levels 0 to 2 and the UI look the same as without the mod (the chunk clamp touches every frame)
