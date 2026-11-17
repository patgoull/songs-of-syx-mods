# Faster Save

## Problem

Every save stops the game. In a 10k city (5.9 MB save file) a save, and so each autosave, froze the game for about
1.45 s. Timing each phase of `GameSaver.save` (a throwaway test build, three saves) gave:

| Phase | Time | What it is |
|---|---|---|
| `save(fp)`: every system writes its data | ~505 ms | 470 ms of it is 31 `SPRITES.loader().print` calls (one per `SettResource`). In the minified mode a save uses, each one draws the whole game behind the small "Saving" box (`VIEW.render`) and swaps the screen (`CORE.swapAndPoll`), which waits for the monitor (~400 ms in total). The data itself takes ~40 ms. |
| `fp.zip`: compress and write | ~810 ms | Deflate at the default level on the "zipper" thread, while the game thread waits for it. The `DeflaterOutputStream` writes to the `FileOutputStream` unbuffered, 512 bytes at a time. |
| the two `System.gc()` | ~100 ms | Two full collections (Parallel GC, ~50 ms each) |

## Code changes

| File | Change |
|---|---|
| `init/sprite/RLoadPrinter.java` | In the minified mode `print` draws at most every 100 ms. The text argument isn't shown in that mode; only the dots after "Saving" move (one step per 4 prints). `minify()` lets the first print through. It still draws and polls the window 10 times a second during a long save. |
| `snake2d/util/file/FilePutter.java` | `zip()`: a 64 KB `BufferedOutputStream` between the `DeflaterOutputStream` and the file. Same deflater, same flushes, so the same bytes. |

The save file is the same: neither change touches what is written.

## Measured

A/B with the autopilot (`tools\ab.ps1 -B "Goul - Faster Save" -Mode save -Runs 3`: the same 10k save loaded, then
saved three times into `build\auto`, never over the game's saves), median of 3 runs each:

| Save | Vanilla | Faster Save | Change | Vanilla range | Faster Save range |
|---|---|---|---|---|---|
| 1st | 1468 ms | 1004 ms | -32% | 1468-1470 | 996-1019 |
| 2nd | 1444 ms | 951 ms | -34% | 1437-1458 | 932-962 |
| 3rd | 1436 ms | 934 ms | -35% | 1433-1438 | 926-939 |

The longest gap between two frames in each step went down by the same amount, so the freeze is that much shorter.
What is left is mostly the compression (~0.8 s).

## Ideas for the dev (not in this mod)

- **Zip in the background.** The data is already copied into the `FilePutter` buffer before `zip` starts, and a new
  buffer is made for every save, so the game could keep running while the zipper thread compresses (~0.8 s of the
  pause). The next save, loading and quitting would have to wait for it to finish.
- **A faster compression level** (`new Deflater(Deflater.BEST_SPEED)`) would cut most of the 0.8 s for a larger file;
  loading reads any level.
- **The two `System.gc()`** calls after a save cost ~100 ms; the JVM collects on its own.
