# Room Builder Plus: move/copy items, redo and hotkeys in the room builder

**Game version:** 0.71.44 · **Patch:** [`patches/room-builder-plus.patch`](../../patches/room-builder-plus.patch)

## Problem

When planning a big room (e.g. a large laboratory) and you notice that 20 workstations are one tile off,
the only fix today is to remove them all and place them again one by one. The same goes for repeating a
good layout across a room. The Room Copier and Blueprints only work on whole rooms.

## What it adds

Two buttons next to the **items** header of the room construction panel. It works the same for new rooms and
refurbishing, since both use the same placement code:

- **Move items** (hand icon): drag a rectangle over items. They're lifted and follow the mouse.
  Click to drop. Right click puts them back and returns to selecting. A second right click returns to normal item placing.
- **Copy items** (copy icon): drag a rectangle over items. Copies follow the mouse, and each click stamps a copy.
  Right click returns to selecting.

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
- Only rooms with an area (the panel that has the items list). Rotating a selection isn't supported yet.
- Up to 256 items per selection (the room history holds 1024 entries, 2 per moved item).

## Code changes (additive except the 6 lines noted)

| File | Change |
|---|---|
| `settlement/room/main/placement/PlacerItemMove.java` | **New.** Selector (`PlacableMulti`) + floating selection (`PlacableFixed`), lift/drop/restore, history |
| `settlement/room/main/placement/RoomPlacer.java` | `mover` field; `mover.restore()` at the top of `init`, `create`, `popHistory`, `removeAllItems`, `removeArea`; `hasRedo`/`redo` |
| `view/sett/ui/room/construction/Config.java` | Right click while moving cancels instead of undoing; restore on tool switch and on panel close |
| `view/sett/ui/room/construction/SItems.java` | The two buttons + Ctrl+X/Ctrl+C keys; item tool buttons kept while removing (3 conditions) |
| `settlement/room/main/placement/UtilHistory.java` | Redo list, `hasRedo`, `redo` (additive) |
| `view/sett/ui/room/construction/SFrame.java` | Undo button wrapped with the Ctrl+Z key (2 lines changed), redo button + Ctrl+Y |
| `view/tool/ToolPlacer.java` | Remove toggle fix (1 line changed) |

All edits are marked `// GOUL`. New UI strings are plain defaults (not yet in `Dic.txt`).

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
- [ ] Copy past the max amount of an item → "Max amount of an item is reached."

Refurbish:
- [ ] Refurbish a built lab, move built workstations → they become construction sites again, resources partly refunded.
- [ ] Pick up built items, close the panel (Esc / exit) → room is rebuilt with items where they were, still built.
- [ ] Pick up and drop at the same spot → nothing changes, stays built.

Hotkeys and Remove fix:
- [ ] Ctrl+Z undoes a step, Ctrl+X starts moving, Ctrl+C starts copying. Outside the construction panel they do nothing.
- [ ] Hover the Move button, press Ctrl+H, press another key → the new key works (and is kept after a restart).
- [ ] Pick up items, Ctrl+Z → items back, selecting again.
- [ ] Place items, expand the area, add a door, then Ctrl+Z three times and Ctrl+Y three times → back to the same room. The redo button greys out at the end.
- [ ] Move a selection, Ctrl+Z, Ctrl+Y → moved again (one step). Ctrl+Z, then place an item → redo greys out.
- [ ] While placing items, hold Left Shift → the -, +, rotate and Remove buttons stay visible.
- [ ] Click Remove on, then off, then tap Left Shift → back to placing, not stuck in remove mode.

Watch the log (`%APPDATA%\songsofsyx\logs`) for `item mover:` lines and exceptions.
