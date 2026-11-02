# Room Builder Plus: move/copy items, redo and hotkeys in the room builder

**Game version:** 0.71.44 · **Patch:** [`patches/room-builder-plus.patch`](../../patches/room-builder-plus.patch)

## Problem

When planning a big room (e.g. a large laboratory) and you notice that 20 workstations are one tile off,
the only fix today is to remove them all and place them again one by one. The same goes for repeating a
good layout across a room. The Room Copier and Blueprints only work on whole rooms.

## What it adds

A **Selection** row under the **items** header of the room construction panel: Move, Copy, Mirror ⇆, Mirror ⇅.
The mirror buttons are greyed out until items are carried. The header's own -, +, rotate buttons (the tool buttons
the game shows when placing an item type) also show while items are carried and then act on the whole selection;
they're greyed out or hidden when the selection can't be resized or turned, like for a single item. It works the same for new
rooms and refurbishing, since both use the same placement code:

- **Move items** (hand icon): drag a rectangle over items. They're lifted and follow the mouse.
  Click to drop. Right click puts them back and returns to selecting. A second right click returns to normal item placing.
- **Copy items** (copy icon): drag a rectangle over items. Copies follow the mouse, and each click stamps a copy.
  Right click returns to selecting.

- **All of a type:** hold Ctrl when releasing a drag to act on every item of the dragged-over types in the room,
  not just the ones in the rectangle. Works for Move, Copy and the game's **Remove Item** tool (e.g. drag over one
  workstation with Ctrl to move, copy or remove all 20). The hover box shows the hint while dragging. A Ctrl-remove
  is one undo step.
- **Rotate and mirror** while moving or copying: the game's rotate key (R) or the header's rotate button turns the
  selection 90° clockwise around the cursor; H / V or the Mirror buttons mirror it left-right / up-down.
- **Resize** while moving or copying: the game's grow / shrink keys (E / Q, or Ctrl + wheel) or the header's - / +
  buttons give every carried item the next size of its type, centred where it was (e.g. Ctrl+X three workbenches, E, drop).
  Greyed out with the reason on hover when an item has no such size. Items keep their centre, so bigger items can end
  up overlapping each other: those tiles turn red and the hover says "Items overlap each other." until they're
  shrunk back or spread out.
- **Inner walls** (pillars made with Shrink Room): a wall the room encloses on every side, diagonals included, is
  picked up like an item, whole (a 1x1, 1x2 or 4x4 pillar alike), but only with **Alt**: start with Ctrl+Alt+X /
  Ctrl+Alt+C (or Alt+click the button), or hold Alt while dragging. Plain Ctrl+X / Ctrl+C leave walls alone, so a
  drag across a room doesn't grab its pillars by accident. Drag over any of a wall's tiles, alone or with items;
  with Ctrl on release, all inner walls come along. Walls follow the selection's rotate and mirror, keep their size on resize,
  and show as striped tiles. A wall can only go on room floor without items. Walls that touch the outer wall, even at
  a corner, aren't inner walls and can't be picked up.
- When the carried items are all of one type, that type is highlighted in the item list and becomes the current
  item, so placing goes on with it after the drop.

The selection is held at its tile closest to where the drag ended. Dragging past the items (e.g. down to the end of a
long room over a single workstation) snaps them to the cursor by their nearest corner instead of leaving them far away.
The hover box shows how many items are carried and the offset from where they were picked up.

### Hotkeys

New settlement hotkeys, only active while the room construction panel is open. All can be rebound by hovering
the button and pressing the assign hotkey key (Ctrl+H), or in the key settings.

| Default | Key code | Action |
|---|---|---|
| Ctrl+Z | `ROOM_UNDO` | Undo, the same as the undo button in the bottom bar. While items are lifted it puts them back first |
| Ctrl+Y | `ROOM_REDO` | Redo, also a new button (right arrow) next to undo |
| Ctrl+X | `ROOM_ITEMS_MOVE` | Move items (cut + paste in one go) |
| Ctrl+C | `ROOM_ITEMS_COPY` | Copy items |
| Ctrl+Alt+X / Ctrl+Alt+C | (same keys, Alt held) | Move / copy items and inner walls. Alt isn't a binding of its own: the game resolves Ctrl+Alt+X to Ctrl+X, and the tool checks Alt when it starts |
| R | (game's `ROTATE`) | Rotate the carried selection |
| H | `ROOM_ITEMS_FLIP_H` | Mirror the carried selection left-right |
| V | `ROOM_ITEMS_FLIP_V` | Mirror the carried selection up-down |

### How rotating and mirroring pick items

The game has no mirrored items, only up to 4 rotations of each one (`FurnisherItemGroup.getRot` turns the tile grid
clockwise, reusing the same tile objects). So each item is swapped for the variant of its group whose tile grid equals
the turned or mirrored grid, trying the variant the turn should give first (so 1x1 and symmetric items still change
facing). Since the result is always a real game item, a mirrored workstation is just a rotated one that looks the same.
If an item has no matching variant (an asymmetric item mirrored), the whole selection can't be mirrored: the Mirror
(or Rotate) button is greyed out while that selection is carried, and its hover names the item. The key does nothing
and the hover box says the same.

### Redo

`UtilHistory.popHistory` now keeps what it undoes in a redo list, one group per undo. `redo()` replays the
group oldest entry first, using the same calls as undo in the other direction (`itemSet`/`itemClear`,
`instance.set`/`clear`, door place/remove), and records it as a normal history step, so it can be undone again.
Any new change clears the redo list, like in any editor. If a redo entry no longer fits (shouldn't happen, the
room is back in the state it was in after the undo), redo stops and the list is cleared.

### Vanilla fix: the Remove toggle next to items

Two bugs made the item tool buttons (-, +, rotate, Remove) disappear and the Remove mode get stuck:

1. `SItems` only shows the item tool buttons while the item placer is the current tool. Holding Left Shift or
   clicking Remove switches to the "remove item" tool, so the whole group vanished, including the button to turn it off.
   Now they also show while removing, the way `SShape` already does it for the area tool.
2. `ToolPlacer.buttUndo`: clicking Remove off called `selectedSet(true)` instead of `false`. The button stayed
   selected internally, so the next Left Shift press latched remove mode on, and right click then undid changes
   instead of leaving it.

## Rules

- A drop is checked with the normal item rules (`UtilPlacability.itemProblem`), item by item, and placed only if **all** of them fit.
  Otherwise nothing changes and the reason is shown.
- One drop = **one undo step**: all history entries are written in the same tick.
- Max counts per item type are respected (copies included).
- **Refurbishing:** a built item that's moved is refunded like a removed item and has to be built again,
  the same result as removing and placing it by hand. Dropping at the same spot changes nothing.
- Lifted items are always put back before anything else touches the room: undo, construct, remove items/area,
  switching tool, closing the panel (`RoomPlacer`/`Config` call `mover.restore()`).
- **Walls:** moving one is the same as expanding the room over its old spot and shrinking it at the new one
  (`PlacerArea`): the floor under the new wall is refunded, the old spot gets a clearing job, and items next to the
  new wall that no longer fit (`itemPlacable` + `itemProblem`, as `PlacerArea.validateItems` does) are removed and refunded.
  A wall whose spot can't become room (`PLACEMENT.placable`, e.g. a built wall while "build on walls" is off) can't be
  moved, only copied: the hover says why. Copying a wall only cuts the new spot.
- Only rooms with an area (the panel that has the items list).
- Up to 256 items and 128 wall tiles per selection (the room history holds 1024 entries, 2 per moved item or wall tile).

## Code changes (additive except the 6 lines noted)

| File | Change |
|---|---|
| `settlement/room/main/placement/PlacerItemMove.java` | **New.** Selector (`PlacableMulti`) + floating selection (`PlacableFixed`), lift/drop/restore, history; inner walls found by a flood fill from outside the room's box |
| `settlement/room/main/placement/RoomPlacer.java` | `mover` field; `mover.restore()` at the top of `init`, `create`, `popHistory`, `removeAllItems`, `removeArea`; `hasRedo`/`redo` |
| `view/sett/ui/room/construction/Config.java` | Right click while moving cancels instead of undoing; restore on tool switch and on panel close |
| `view/sett/ui/room/construction/SItems.java` | The two buttons + Ctrl+X/Ctrl+C keys; item tool buttons kept while removing (3 conditions) |
| `settlement/room/main/placement/UtilHistory.java` | Redo list, `hasRedo`, `redo` (additive) |
| `view/sett/ui/room/construction/SFrame.java` | Undo button wrapped with the Ctrl+Z key (2 lines changed), redo button + Ctrl+Y |
| `view/tool/ToolPlacer.java` | Remove toggle fix (1 line changed) |
| `settlement/room/main/placement/PlacerItemArea.java` | Remove Item tool: Ctrl when releasing removes all items of the removed types (additive) |

All edits are marked `// GOUL`. New UI strings are plain fields with English text, not yet in `Dic.txt`: in `SItems`,
`SFrame` and `PlacerItemArea` they can't be `¤¤` fields, since those classes are in the dictionary and `D.ts` would
replace them with their missing key. `PlacerItemMove` is new, so its `¤¤` defaults are kept. To integrate, add the
entries to `Dic.txt` and turn the fields into `¤¤` fields.

## Test checklist

Enable **Goul - Room Builder Plus** in the launcher, then in a game:

New room:
- [ ] Plan a lab with ~10 workstations. Move: drag over 3 of them, move 1 tile, click → they move. Ctrl+Z (or the undo button) once → all 3 back.
- [ ] Move onto an invalid spot (wall, outside the room, overlapping) → red, hover shows why, click does nothing.
- [ ] Pick up, then right click → items back in place. Right click again → normal item placing.
- [ ] Pick up, then click another item type in the list → items back in place.
- [ ] Pick up, then press **construct** → room is built with the items in their original place.
- [ ] In a long room, drag from one workstation down to the far end → the workstation jumps to the cursor, its nearest corner under it.
- [ ] Copy: select a row, stamp it 3 times. Undo once → only the last stamp disappears.
- [ ] Ctrl+drag over one workstation of a lab with ~10 → all workstations are picked up, other items stay.
- [ ] Remove Item (Shift or the button), Ctrl+drag over one workstation → all workstations removed, other items stay. Ctrl+Z → all back.
- [ ] Carry a selection of different items, press R 4 times → back to the start; each turn looks right (items face the right way).
- [ ] Carry, press H, then V → mirrored; drop → one undo step. An item that can't be mirrored shows the red message and nothing changes.
- [ ] Copy, rotate, stamp twice → both copies rotated.
- [ ] Ctrl+X three workbenches, E → all three get the next size, centred; drop → one undo step. At the biggest size, + is greyed with a reason.
- [ ] Ctrl+X items placed end to end, E → the overlapping tiles are red, "Items overlap each other.", click does nothing; Q → fine again.
- [ ] Carry items of one type → that type is highlighted in the list; right click twice → placing that type.
- [ ] Copy past the max amount of an item → "Max amount of an item is reached."
- [ ] Ctrl+X, drag over a 2x2 pillar → not selected (hover says hold Alt). Hold Alt while dragging → selected.
- [ ] Shrink Room a 2x2 pillar in the middle of a room. Ctrl+Alt+X it, move it 3 tiles, drop → the pillar moves, the old spot is floor. Ctrl+Z → back. Ctrl+Y → moved again.
- [ ] Drag over a pillar and two items next to it, press R → pillar and items turn together; H → mirrored.
- [ ] Drop a pillar onto an item or outside the room → red, "Walls must go on room floor without items."
- [ ] Copy a 1x2 pillar, stamp it twice → two new pillars, the original stays; undo once → only the last one goes.
- [ ] Drop a pillar next to an item that can't touch walls → that item is removed; Ctrl+Z → pillar back, item back.
- [ ] A notch cut into the outer wall, or a wall touching it at a corner → can't be picked up.

Refurbish:
- [ ] Refurbish a built lab, move built workstations → they become construction sites again, resources partly refunded.
- [ ] Pick up built items, close the panel (Esc / exit) → room is rebuilt with items where they were, still built.
- [ ] Pick up and drop at the same spot → nothing changes, stays built.
- [ ] Refurbish a built room with a pillar, move it → it works, or the hover says why the old spot can't become room.

Hotkeys and Remove fix:
- [ ] Ctrl+Z undoes a step, Ctrl+X starts moving, Ctrl+C starts copying. Outside the construction panel they do nothing.
- [ ] Hover the Move button, press Ctrl+H, press another key → the new key works (and is kept after a restart).
- [ ] Pick up items, Ctrl+Z → items back, selecting again.
- [ ] Place items, expand the area, add a door, then Ctrl+Z three times and Ctrl+Y three times → back to the same room. The redo button greys out at the end.
- [ ] Move a selection, Ctrl+Z, Ctrl+Y → moved again (one step). Ctrl+Z, then place an item → redo greys out.
- [ ] While placing items, hold Left Shift → the -, +, rotate and Remove buttons stay visible.
- [ ] Click Remove on, then off, then tap Left Shift → back to placing, not stuck in remove mode.

Watch the log (`%APPDATA%\songsofsyx\logs`) for `item mover:` lines and exceptions.
