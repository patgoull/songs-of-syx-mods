# Room Checklist (proof of concept)

## Problem

When a planned room can't be constructed, the construct button's hover shows one message: the first problem
`UtilPlacability.createProblem` finds. Fix it and the next one shows up. New players don't know what a room needs
until they hit each requirement in turn, and some messages ("Area must be connected!") don't say what to change.

## What it adds

Two lines at the bottom of the stats in the room construction panel (rooms with an area):

- "Ready to construct" (green tick) or "N requirements not met" (red cross).
- Below it, only when there are any, "1 warning" / "N warnings" (yellow).
- Hover it for the full list, each with a tick or a cross:
  - every item type the room needs, and every stat with a minimum (e.g. workers). A met one is just its name with a
    tick; one that's short says what's needed in red (e.g. "Workbench at least 1 needed")
  - the room's own rule, if it has one and it fails (`Furnisher.constructionProblem`, e.g. must be indoors)
  - can be reached from outside, all of the room is connected, has a doorway (when walls are built automatically),
    each with what to do when it fails
- Below, the warnings that don't block construction: the room's own warning, weak support (yellow tiles) and poor insulation.

If the game refuses construction for a reason the list doesn't cover, that reason is added as a failed line, so the
list never says ready when the construct button won't work.

Works alongside Room Builder Plus: they replace different classes.

## Code changes (additive only)

| File | Change |
|---|---|
| `settlement/room/main/placement/RoomChecklist.java` | **New.** Runs each check of `UtilPlacability.createProblem` separately and lists them in a `GBox`. The access and connection checks are copied from `UtilPlacability`, where they're private |
| `view/sett/ui/room/construction/SChecklist.java` | **New.** The status line and its hover. The checks run 4 times a second for the line, and on every hover frame for the list |
| `view/sett/ui/room/construction/SStats.java` | 3 lines: the `SChecklist` field and adding its line in `get()` |

To integrate, the checks could move into `UtilPlacability` itself (one method that reports all problems instead of
the first), and the construct button's hover could show the same list.

## Ideas for after the POC

- Click a failed line to highlight the tiles behind it (unreachable tiles, the cut-off part, the item type in the list).
- Show the list in the construct button's hover too.
- Dictionary entries for the texts (`RoomChecklist` and `SChecklist` are new classes, so their `¤¤` defaults are used).

## Test checklist

Enable **Goul - Room Checklist** in the launcher (with or without Room Builder Plus), then:
- [ ] Plan a lab with no items → red "N requirements not met"; hover lists the missing item types and stats as "at least N needed".
- [ ] Place the required items → those lines turn to ticks, the count goes down.
- [ ] Fill a small room's edge with items so it can't be entered → "Can be reached from outside" fails.
- [ ] Split a room in two with items → "All of the room is connected" fails.
- [ ] With auto walls on, a room with no doorway → "Has a doorway" fails.
- [ ] Everything met → green "Ready to construct", and the construct button works.
- [ ] A wide indoor room → yellow warnings line, hover shows weak support / insulation.
- [ ] Refurbish an existing room → the line shows and updates.
