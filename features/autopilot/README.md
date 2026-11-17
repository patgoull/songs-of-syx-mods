# Autopilot (test tool)

Not a suggestion for the game: a test tool so a test run needs no clicks. `.\run.ps1 -Auto` starts the game, loads a
save, plays a fixed tour of the city, logs a line per step, saves screenshots and quits.

```
.\build.ps1 autopilot -Install     # once, and after changing it
.\run.ps1 -Auto                    # or .\run.ps1 -Auto -Profile for a Java Flight Recorder profile too
```

Modes (`-Mode`): `tour` (default, below), `load` (only the startup and load timing, ~45 s), `sim` (the simulation at
the game's max speed, 250x, ~30 s), `save` (three saves into `build\auto`, timed), `look` (the middle of the map
at fixed sun positions of one day: sunrise, morning, noon, afternoon, sunset, twilight, night, paused, ~1 minute).
`-Mods "A","B"` runs with exactly those mods instead of the launcher's list.

`.\tools\look.ps1 -B "<mod>"` runs `look` without and with a mod and puts each pair of shots side by side in
`build\look\compare` (needs Python with Pillow), for mods that change how the game looks.
`-Focus <room key>` (e.g. `REFINER_SMELTER`, the room's file name) looks at the room of that type with the most
workers instead of the middle of the map, in any mode.

## Comparing two builds: `tools\ab.ps1`

```
.\tools\ab.ps1 -B "Goul - Faster Load" -LabelB faster-load -Mode load -Runs 3
.\tools\ab.ps1 -A "Goul - Live Zoom (test)" -B "Goul - Live Zoom (test)","Goul - Faster Render" -LabelB render
```

Runs A and B `-Runs` times each on the pinned save, alternating the order each round, keeps each log in
`build\ab\<label>-<n>.log` and writes `build\ab\report.md`: the median of every number, the change from A to B, and
each side's min-max (if the ranges overlap, the difference isn't real). `-A` defaults to no mods (vanilla).

Any code-only mod makes `load s` about 1 s longer without Faster Load (each asset lookup checks every mod's
folder on disk), and `startup s` about 0.2 s longer. So compare load times only between sides with the same number
of mods, or with Faster Load on both sides.

First result (2026-11-16, 10k city, 6 code-only mods off):

| Metric | vanilla | Faster Load | Change | vanilla range | Faster Load range |
|---|---|---|---|---|---|
| startup s (java start to Continue, included the logo then) | 6.93 | 6.89 | -0.6% | 6.88-7.00 | 6.85-6.89 |
| load s (Continue to the first game update) | 9.67 | 5.97 | -38.3% | 9.66-9.71 | 5.94-6.04 |

## What a run does

- `run.ps1` enables **Goul - Autopilot (test)** in `LauncherSettings.txt` for that run only. Only the `MODS` list is
  changed and put back afterwards (a copy waits in `build\auto\launcher-mods-backup.txt`, so a run that was cut off
  is put back by the next `run.ps1`). It passes `-Dgoul.autopilot=tour` and the save and output folder through `JAVA_TOOL_OPTIONS`.
- The "Gamatron presents" logo is skipped and the menu loads the save right away, as if Continue was clicked, and the "click to continue" screen after
  loading closes itself.
- The script `goul.autopilot.Autopilot` (`forceInit` only with the setting) runs the tour, timed in real seconds:

| Step | Seconds | Screenshot |
|---|---|---|
| settle (speed 1, zoom 1, at the middle of the map, where most of the test city is: under the mountain and outside) | 8 | |
| day, zoom 0, 1, 2, 3 | 5 each | yes |
| night (middle of the next night), zoom 3 | 6 | yes |
| night, zoom 2 | 5 | yes |
| night, zoom 3, one circle of 80 tiles around that point | 10 | yes (at the end) |
| night, zoom 3, speed 3 | 10 | |
| zoom 1, speed 250 (warm-up: the game ramps up one step per update) | 6 | |
| zoom 1, speed 250 | 15 | |

Each step keeps its zoom (the mouse wheel over the window would change it). After each step it logs, for example:

```
GOUL auto: night-pan-zoom3      fps  45.0, updates/s  45.1, worst gap  30.7 ms, game x   1.0, ms update  1.20 render 20.66 flush  0.36, zoom 3, night true
```

`fps` is the game's own counter; `game x` is game seconds per real second (the speed the game actually reached; it
slows itself down when it can't keep up, so at speed 250 it's the simulation score); `updates/s` and `worst gap` (the longest time between two updates) are measured by the
script; the `ms` values are the game's `CoreStats` averages. `run.ps1` prints these lines and the screenshot paths
after the run.

## Files

- `build\auto\NN-step.png`: the screenshots (the game's own Print Screen shot, moved there)
- `build\auto\save.txt`: the pinned save. The first run pins the newest save; `.\run.ps1 -Auto -Save <name>` changes it.
  Pinned so the city is the same in every run, even if the game autosaves during a test.
- `build\game.log`: the full log

## Code

| File | Change |
|---|---|
| `goul/autopilot/Autopilot.java` | New: the script, the tour, and a timing line (`GOUL auto: timing startup .. s, load .. s`) |
| `menu/Menu.java` | With `-Dgoul.autopilot`, skips the logo and loads `goul.autopilot.save` (or the newest save) right away, and notes the time |
| `view/interrupter/ILoadScreen.java` | With `-Dgoul.autopilot`, closes the "click to continue" screen |
| `goul/autopilot/AutoChoice.java` | New: popups that would stop the game until clicked take their default choice here, one update later as a click would come, with a `GOUL auto:` line in the log |
| `view/world/ui/battle/UIWBattlePrompt.java` | With `-Dgoul.autopilot`, battle popups don't open the world map or pause the game: auto-resolve a battle, accept the result without slaves or spoils, retreat from a siege, abandon a conquest. At 250x a battle comes up often, and the popup used to stop the game (game x 0.0) |
| `view/ui/message/Messages.java` | With `-Dgoul.autopilot`, messages go to the inbox without opening their window (which stops the game until closed); an offer from a faction is accepted |
| `view/ui/diplomacy/UIDipMessDeal.java` | `goulAccept()`: what the offer's Accept button does |
| `snake2d/Mouse.java` | With `-Dgoul.autopilot`, the real mouse is ignored (position, clicks, wheel), so bumping it doesn't change a run; the game's cursor stays on an empty part of the top bar (no tooltip in the screenshots, no edge scrolling) |

If something else stops the updates for 60 s, the script logs `GOUL auto: stuck` and quits the game, so a run never keeps the screen.

Without `-Dgoul.autopilot` none of this does anything, and the script isn't offered in the game. The tour moves the
time to the next night; it never saves, but an autosave during a run would keep that time (the pinned save isn't
touched).
