package settlement.room.main.placement;

import game.GAME;
import settlement.main.SETT;
import settlement.room.main.furnisher.FurnisherItem;
import snake2d.util.sets.IntegerStack;

final class UtilHistory {
	
	private static final int historyAmount = 1024;
	private HistoryI[] histories = new HistoryI[historyAmount];
	private IntegerStack hStack = new IntegerStack(historyAmount);
	private short historyFirst = -1;
	private final RoomPlacer p;
	
	private short tick;
	private int currentTick;
	
	// GOUL: redo. What popHistory undoes is kept here, newest last, one group per undo
	private final short[] redoAction = new short[historyAmount];
	private final short[] redoX = new short[historyAmount];
	private final short[] redoY = new short[historyAmount];
	private final short[] redoGroup = new short[historyAmount];
	private int redoAm;
	private short redoTick;
	private boolean redoing;
	
	UtilHistory(RoomPlacer p) {
		for (int i = 0; i < historyAmount; i++) {
			histories[i] = new HistoryI(i);
			hStack.push(i);
		}
		this.p = p;
	}
	
	public void clear() {
		redoAm = 0; // GOUL
		while(historyFirst != -1) {
			hStack.push(historyFirst);
			historyFirst = histories[historyFirst].next;
		}
		
		
		
		historyFirst = -1;
	}
	
	private void init() {
		if (!redoing) // GOUL: a new change drops what was undone
			redoAm = 0;
		if (currentTick != GAME.updateI()) {
			tick++;
			currentTick = GAME.updateI();
		}
	}
	
	public void placeDoor(int x1, int y1, int delta) {
		init();
		if (hStack.isEmpty()) {
			clearHistory(-1, historyFirst, 10);
		}
		int hi = hStack.pop();
		HistoryI h = histories[hi];
		h.x = (short) x1;
		h.y = (short) y1;
		h.action = (short) delta == 1 ? HistoryI.actionDoor : HistoryI.actionDoorRemove;
		h.next = historyFirst;
		h.tick = tick;
		historyFirst = (short) hi;
		
	}
	
	public void placeItem(FurnisherItem it, int x1, int y1, int delta) {
		init();
		if (hStack.isEmpty()) {
			clearHistory(-1, historyFirst, 10);
		}
		int hi = hStack.pop();
		HistoryI h = histories[hi];
		
		
		if (delta < 0) {
			h.action = (short) (it.index()*delta);
		}else {
			h.action = (short) (1+ it.index());
			x1 += it.firstX();
			y1 += it.firstY();
		}
		h.x = (short) x1;
		h.y = (short) y1;
		h.next = historyFirst;
		h.tick = tick;
		historyFirst = (short) hi;
	}
	
	public void placeEmbryo(int x1, int y1, int delta) {
		init();
		if (hStack.isEmpty()) {
			clearHistory(-1, historyFirst, 64);
		}
		int hi = hStack.pop();
		HistoryI h = histories[hi];
		h.x = (short) x1;
		h.y = (short) y1;
		h.action = delta == -1 ? HistoryI.actionShrink : HistoryI.actionExpand;
		h.next = historyFirst;
		h.tick = tick;
		historyFirst = (short) hi;
		
	}
	
	private int clearHistory(int previous, int current, int amount) {
		if (current == -1)
			return amount;
		if (clearHistory(current, histories[current].next, amount) > 0) {
			hStack.push(current);
			histories[current].next = -1;
			if (previous == -1) {
				historyFirst = -1;
			}else {
				histories[previous].next = -1;
			}
			
			return amount-1;
		}
		return 0;
	}
	
	public boolean hasHistory() {
		return historyFirst != -1;
	}
	
	public void popHistory() {
		
		short hi = historyFirst;
		int t = histories[hi].tick;
		redoTick++; // GOUL
		while(hi != -1 && histories[hi].tick == t) {
			HistoryI h = histories[hi];
			hStack.push(hi);
			redoPush(h); // GOUL
			hi = h.next;
			historyFirst = hi;
			if (h.action < 256) {
				if (h.action <= 0) {
					FurnisherItem it = p.blueprint().constructor().item(-h.action);
					if (p.placability.itemProblem(h.x, h.y, it.group, it, p.instance) != null){
						clear();
						return;
					}
					
					for (int ry = 0; ry < it.height(); ry++) {
						for (int rx = 0; rx < it.width(); rx++) {
							if (p.placability.itemPlacable(h.x+rx, h.y+ry, rx, ry, it, p.instance) != null || !p.instance.is(h.x+rx, h.y+ry)) {
								clear();
								return;
							}
						}
					}
					SETT.ROOMS().fData.itemSet(h.x, h.y, it, p.instance);
				}else {
					SETT.ROOMS().fData.itemClear(h.x, h.y, p.instance);
				}
			}else if (h.action == HistoryI.actionExpand) {
				p.instance.clear(h.x, h.y);
			}else if (h.action == HistoryI.actionShrink){
				p.instance.set(h.x, h.y);
			}else if(h.action == HistoryI.actionDoor) {
				p.door.removeWithoutHistory(h.x, h.y);
			}else if (h.action == HistoryI.actionDoorRemove) {
				p.door.placeWithoutHistory(h.x, h.y);
				
			}
		}
		
		
	}
	
	// GOUL: redo
	private void redoPush(HistoryI h) {
		if (redoAm >= historyAmount)
			return;
		redoAction[redoAm] = h.action;
		redoX[redoAm] = h.x;
		redoY[redoAm] = h.y;
		redoGroup[redoAm] = redoTick;
		redoAm++;
	}
	
	public boolean hasRedo() {
		return redoAm > 0;
	}
	
	/**
	 * Does again what the last popHistory undid, oldest entry first, and records it as a new history step.
	 */
	public void redo() {
		if (redoAm == 0)
			return;
		redoing = true;
		short g = redoGroup[redoAm-1];
		while (redoAm > 0 && redoGroup[redoAm-1] == g) {
			redoAm--;
			if (!redo(redoAction[redoAm], redoX[redoAm], redoY[redoAm])) {
				redoAm = 0;
				break;
			}
		}
		redoing = false;
	}
	
	private boolean redo(short action, int x, int y) {
		if (action < 256) {
			if (action <= 0) {
				// an item was removed, x y is its top left
				FurnisherItem it = p.blueprint().constructor().item(-action);
				int mx = x + it.firstX();
				int my = y + it.firstY();
				if (SETT.ROOMS().fData.item.get(mx, my) != it || !p.instance.is(mx, my))
					return false;
				SETT.ROOMS().fData.itemClear(mx, my, p.instance);
				placeItem(it, x, y, -1);
			}else {
				// an item was placed, x y is its first tile
				FurnisherItem it = p.blueprint().constructor().item(action-1);
				int x1 = x - it.firstX();
				int y1 = y - it.firstY();
				if (p.placability.itemProblem(x1, y1, it.group, it, p.instance) != null)
					return false;
				for (int ry = 0; ry < it.height(); ry++) {
					for (int rx = 0; rx < it.width(); rx++) {
						if (p.placability.itemPlacable(x1+rx, y1+ry, rx, ry, it, p.instance) != null || !p.instance.is(x1+rx, y1+ry))
							return false;
					}
				}
				SETT.ROOMS().fData.itemSet(x1, y1, it, p.instance);
				placeItem(it, x1, y1, 1);
			}
		}else if (action == HistoryI.actionExpand) {
			p.instance.set(x, y);
			placeEmbryo(x, y, 1);
		}else if (action == HistoryI.actionShrink) {
			p.instance.clear(x, y);
			placeEmbryo(x, y, -1);
		}else if (action == HistoryI.actionDoor) {
			p.door.placeWithoutHistory(x, y);
			placeDoor(x, y, 1);
		}else if (action == HistoryI.actionDoorRemove) {
			p.door.removeWithoutHistory(x, y);
			placeDoor(x, y, -1);
		}
		return true;
	}
	
	private static class HistoryI {
		
		private static short actionExpand = 256+1;
		private static short actionShrink = 256+2;
		private static short actionDoor = (short) (actionShrink+1);
		private static short actionDoorRemove = (short) (actionDoor+16*2);
		
		private short next;
		private short x,y;
		private short action;
		private short tick;
		
		HistoryI(int index){
		}
	}
	
}
