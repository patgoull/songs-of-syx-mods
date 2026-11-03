# Songs of Syx mods

Quality-of-life suggestions for [Songs of Syx](https://store.steampowered.com/app/1162750/Songs_of_Syx/), written as
mods so they can be tried in game and, if wanted, merged into the game.

**For Jake:** each feature is a small, patch-style change to the game's own classes, in the game's code style.
Every change is marked `// GOUL`, and each feature comes with a ready-to-read diff against vanilla V71.44 in
[`patches/`](patches/). The diffs are mostly additive; the few changed vanilla lines are listed in each feature's README.

## Features

| Mod | What it does | Diff |
|---|---|---|
| [Room Builder Plus](features/room-builder-plus/README.md) | Move, copy, rotate and mirror many furniture items and inner walls (pillars) at once while planning or refurbishing a room, nudge a whole planned room a few tiles, redo, rebindable Ctrl+Z / Ctrl+Y / Ctrl+X / Ctrl+C / Ctrl+M, and fixes for the Remove Item toggle disappearing and getting stuck and for undo breaking in big rooms | [room-builder-plus.patch](patches/room-builder-plus.patch) |
| [Room Checklist](features/room-checklist/README.md) (proof of concept) | The room construction panel says whether the room can be constructed, and on hover lists every requirement met or not, instead of only the first problem | [room-checklist.patch](patches/room-checklist.patch) |

## Layout

- `features/<name>/`: one mod per feature. `src/` holds the new classes and the copied vanilla classes with their
  `// GOUL` edits (same package and name, so they replace the vanilla class), `_Info.txt` is the mod descriptor.
- `patches/<name>.patch`: the feature as a diff against the vanilla sources (`info/SongsOfSyx-sources.jar`).
- `docs/`: [modding guide](docs/MODDING_GUIDE.md) (verified against the V71.44 sources) and [ideas/roadmap](docs/STRATEGY.md).

## Building

Needs a JDK 21 and the game installed. Extract `info/SongsOfSyx-sources.jar` from the game folder into
`vanilla/71.44/` (only used to write the patches), then:

```powershell
.\build.ps1 room-builder-plus -Install   # compiles against SongsOfSyx.jar, installs to %APPDATA%\songsofsyx\mods
.\run.ps1 -Launcher                      # start the game; enable the mod in the launcher once
```
