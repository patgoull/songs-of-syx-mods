# Move Room (proof of concept)

## Problem

There's no way to move a built room. Today you copy it with the Room Copier (it must not overlap the original), dismantle
the old room, then dismantle its walls and roof with Dismantle Structure, tile by tile around any walls you want to keep.
Moving a room by one tile isn't possible at all, because the copy can't overlap the original.

## What it adds

**Move Room**, next to Room Copier in the bottom-left options menu (searchable, and a bindable hotkey `MOVE_ROOM`).
It's the Room Copier with one difference: when you place the copy, the original goes.

1. Click a room. It follows the cursor as with the copier, and rotates the same way. The room type can be swapped too.
2. Place it. The new spot may overlap the old one.
3. In that one click:
   - the old room is dismantled like Dismantle Room: 75% of its resources are dropped on its tiles, where builders pick
     them up for the new one
   - the room is planned at the new spot with the same settings (upgrade, workers' settings, etc.) and its walls and doorways
   - for indoor rooms with walls on (the copier's wall button), the old walls and roof are queued for Dismantle Structure,
     except those still needed: next to a room (the new one, or a neighbour sharing a wall) or next to a roofed tile
     further out (a corridor). Mountain walls, fences, and tiles that already have a job are left alone.
4. The tool goes back to step 1 for the next room.

The room stops working until the new one is built, the same as copy + dismantle by hand. Moving costs the 25% that
dismantling loses, plus the labour.

## Code changes

| File | Change |
|---|---|
| `settlement/room/main/copy/RoomMover.java` | **New.** Reads the room's tiles and items, dismantles it (`ROOMS().DELETE`), and afterwards queues Dismantle Structure on the old walls and roof nothing needs |
| `settlement/room/main/copy/Copier.java` | A second constructor taking a `RoomMover`. In move mode: the source tiles may be overlapped, the room is read and dismantled before the copy is built, items are read from the mover, then the old structure is dismantled and the tool goes back to picking a room. Two lines moved up (the room's upgrade and state are read before it's gone), the item lookup goes through `master()` |
| `settlement/room/main/copy/ROOM_COPY.java` | 6 lines: a second `Copier` and its `move()` getter |
| `view/sett/ui/bottom/Options.java` | 1 line: the menu entry |

Why the copier reads the room before building: the copy writes its items into the shared tile data, so where it
overlaps the original it would overwrite items the copier hasn't read yet. Reading the whole room first (into
`RoomMover`) and dismantling it before building avoids that, and lets the walls be decided on the cleared tiles.

## Test checklist

Enable **Goul - Move Room** in the launcher, then, in a save with built rooms:
- [ ] Options menu → Move Room → click an indoor room (a lab) → place it a few tiles away. The old room is gone, resources
      are dropped on its tiles, a construction site with the same items and doorways is planned, the old walls and roof
      get Dismantle Structure jobs.
- [ ] Move a room by 1 tile: the copy overlaps the old room and is allowed. Walls end up right around the new room.
- [ ] Rotate it (same key as the copier) before placing.
- [ ] A room sharing a wall with a neighbour: the shared wall stays, the neighbour stays enclosed.
- [ ] A room with a doorway into a roofed corridor: the corridor keeps its walls and roof.
- [ ] Turn off the walls button: the room moves without walls, and the old walls and roof stay.
- [ ] An outdoor room (a farm or a well): it moves, no structure is touched.
- [ ] A room under construction: it moves too.
- [ ] After the new site is built, the room has the old settings (e.g. a workshop's recipe).
- [ ] The throne can't be picked.
