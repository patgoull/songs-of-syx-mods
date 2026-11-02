package settlement.room.main.placement;

import settlement.main.SETT;
import settlement.room.main.construction.ConstructionData;
import settlement.room.main.furnisher.FurnisherItem;
import settlement.room.main.furnisher.FurnisherStat;
import snake2d.SPRITE_RENDERER;
import snake2d.util.datatypes.AREA;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.Coo;
import snake2d.util.datatypes.DIR;
import snake2d.util.sprite.text.Str;
import util.GUTIL;
import util.data.AreaTmp;
import util.gui.misc.GBox;
import util.info.GFORMAT;
import util.text.D;
import view.keyboard.KEYS;
import view.tool.PLACABLE;
import view.tool.PLACER_TYPE;
import view.tool.PlacableMessages;
import view.tool.PlacableMulti;

class PlacerItemArea extends PlacerItemSingle{

	private static CharSequence ¤¤undo = "¤Remove Item";
	private static CharSequence sAllOfType = "Hold {0} when releasing: all items of these types"; // GOUL: plain text, not in the dictionary yet
	static {
		D.ts(PlacerItemArea.class);
	}
	
	private final PlacableMulti undo = new PlacableMulti(¤¤undo) {
		
		// GOUL: with the mod key (Ctrl) held when releasing, every item of the removed types goes
		private final boolean[] removed = new boolean[64];
		
		@Override
		public void finishChecking(AREA placedArea) {
			for (int i = 0; i < removed.length; i++)
				removed[i] = false;
		}
		
		@Override
		public void finishPlacing(AREA placedArea) {
			if (!KEYS.MAIN().MOD.isPressed())
				return;
			for (COORDINATE c : embryo.instance.body()) {
				if (!embryo.instance.is(c) || !SETT.ROOMS().fData.isMaster.is(c.x(), c.y()))
					continue;
				FurnisherItem it = SETT.ROOMS().fData.item.get(c.x(), c.y());
				if (it != null && removed[it.group.index()])
					removeItem(c.x(), c.y());
			}
		}
		
		@Override
		public void placeInfo(GBox b, int oktiles, AREA a) {
			super.placeInfo(b, oktiles, a);
			b.NL();
			b.text(Str.TMP.clear().add(sAllOfType).insert(0, KEYS.MAIN().MOD.repr()));
		}
		
		@Override
		public void place(int x1, int y1, AREA a, PLACER_TYPE t) {
			FurnisherItem it = SETT.ROOMS().fData.item.get(x1, y1); // GOUL
			if (it != null && embryo.instance.is(x1, y1)) // GOUL
				removed[it.group.index()] = true; // GOUL
			removeItem(x1, y1);
		}
		
		@Override
		public CharSequence isPlacable(int tx, int ty, AREA a, PLACER_TYPE t) {
			if (!area.is(tx, ty) || !SETT.ROOMS().fData.item.is(tx, ty)) {
				return PlacableMessages.¤¤ITEM_MUST;
			}
			return null;
		}
		
		@Override
		public boolean expandsTo(int fromX, int fromY, int toX, int toY) {
			if (!area.is(fromX, fromY) || !area.is(toX, toY))
				return false;
			FurnisherItem it = SETT.ROOMS().fData.item.get(fromX, fromY);
			FurnisherItem it2 = SETT.ROOMS().fData.item.get(toX, toY);
			if ( it != null && it2 != null) {
				COORDINATE c = SETT.ROOMS().fData.itemMaster(fromX, fromY, Coo.TMP);
				int x = c.x();
				int y = c.y();
				return SETT.ROOMS().fData.itemMaster(toX, toY, Coo.TMP).isSameAs(x, y);
			}
			return false;
		};
	};
	
	public void removeItem(int x1, int y1) {
		if (!embryo.instance.is(x1, y1))
			return;
		
		final FurnisherItem item = SETT.ROOMS().fData.item.get(x1, y1);
		if (item == null)
			return;
		
		COORDINATE c = SETT.ROOMS().fData.itemX1Y1(x1, y1, Coo.TMP);
		int x11 = c.x();
		int y11 = c.y();
		
		boolean constructed = ConstructionData.dConstructed.is(x1, y1, 1);
		for (int dy = 0; dy < item.height(); dy++) {
			for (int dx = 0; dx < item.width(); dx++) {
				if (item.get(dx, dy) != null) {
					if (!embryo.instance.is(x11+dx, y11+dy)) {
						debug(x1, y1);
					}
					
				}
			}
		}
		
		
		for (int dy = 0; dy < item.height(); dy++) {
			for (int dx = 0; dx < item.width(); dx++) {
				if (item.get(dx, dy) != null) {
					ConstructionData.dConstructed.set(embryo.instance, x11+dx, y11+dy, 0);
				}
			}
		}
		SETT.ROOMS().fData.itemClear(x1, y1, embryo.instance);
		
		embryo.history.placeItem(item, x11, y11, -1);
		
		if (constructed) {
			embryo.resources.removeItem(x1, y1, item);
		}
	}
	
	private void debug(int x1, int y1) {
		
		FurnisherItem item = SETT.ROOMS().fData.item.get(x1, y1);
		COORDINATE c = SETT.ROOMS().fData.itemX1Y1(x1, y1, Coo.TMP);
		int x11 = c.x();
		int y11 = c.y();
		
		System.err.println("so, here we go again... Item is " + item.group.blueprint.blue().key + " " + item.group.index() + " " + item.width() + " " + item.height());
		System.err.println("coo: " + x1 + " " + y1);
		System.err.println("embrio: " + embryo.instance.body());
		
		System.err.println("the x1y1: " + c);
		
		for (int dy = 0; dy < item.height(); dy++) {
			for (int dx = 0; dx < item.width(); dx++) {
				if (item.get(dx, dy) != null) {
					if (!embryo.instance.is(x11+dx, y11+dy)) {
						System.err.println(dx + " " + dy + " " + (x11+dx) + " " + (y11+dy));
					}
					
				}
			}
		}
		
		throw new RuntimeException("Game has crashed because there is some weirdness when deleting items. Please, try to remember how you furnished the room before you deleted the items, and try and recreate the crash, so that this bug can be found and squashed. Type it in the message, but even better, compose an email to: info@songsofsyx.com");
	}
	
	@Override
	public void renderPlaceHolder(SPRITE_RENDERER r, int mask, int x, int y, int tx, int ty, int rx, int ry,
			boolean isPlacable, boolean areaIsPlacable) {
		boolean b = embryo.autoWalls.getBool();
		embryo.autoWalls.set(false);
		super.renderPlaceHolder(r, mask, x, y, tx, ty, rx, ry, isPlacable, areaIsPlacable);
		embryo.autoWalls.set(b);
	}
	
	
	
	public PlacerItemArea(RoomPlacer embryo) {
		super(embryo);
		
	}
	
	@Override
	public void place(int tx, int ty, int rx, int ry) {
		
		
		
		
		if (rx == 0 && ry == 0) {
			
//			area.init(tx, ty, group.item(size(), rot()));
//			AREA atmp = RES.AREA();
//			if (atmp.area() > 0) {
//			for (COORDINATE c : atmp.body())
//				embryo.placerArea.place(c.x(), c.y(), atmp, null);
//			}
//			
			FurnisherItem it = group.item(size(), rot());
			SETT.ROOMS().fData.itemSet(tx, ty, it, embryo.instance);
			embryo.history.placeItem(it, tx, ty, 1);
		}
	}

	@Override
	public CharSequence placableWhole(int tx1, int ty1) {
		
		
		
//		area.init(tx1, ty1, group.item(size(), rot()));
//		AREA atmp = RES.AREA();
//		if (atmp.area() > 0) {
//			CharSequence s = embryo.placerArea.isPlacable(atmp, null);
//			if (s != null)
//				return s;
//			for (COORDINATE c : atmp.body()) {
//				if (atmp.is(c)) {
//					s = embryo.placerArea.isPlacable(c.x(), c.y(), atmp, null);
//					if (s != null)
//						return s;
//				}
//			}	
//		}
		return embryo.placability.itemProblem(tx1, ty1, group, group.item(size(), rot()), embryo.instance);
	}
	
	@Override
	public CharSequence placable(int tx, int ty, int rx, int ry) {
		return embryo.placability.itemPlacable(tx, ty, rx, ry, group.item(size(), rot()), embryo.instance);
	}
	
	@Override
	public void placeInfo(GBox box, int x1, int y1) {
		box.add(box.text().add(width()).add('x').add(height()));
		box.NL();
		for (int i = 0; i < group.blueprint.resources(); i++) {
			if (group.item(size(), rot()).cost2(i, embryo.instance.upgrade()) > 0) {
				box.setResource(group.blueprint.resource(i), group.item(size(), rot()).cost2(i, embryo.instance.upgrade()));
				box.space();
			}
		}
		
		for (FurnisherStat s : group.blueprint.stats()) {
			double am = embryo.resources.statIncr(group.item(size(), rot()), s);
			if (am != 0) {
				box.NL();
				box.add(box.text().lablify().add(s.name()));
				box.tab(7);
				box.add(GFORMAT.f0(box.text(), am));
			}
		}
	}

	
	
	@Override
	public PLACABLE getUndo() {
		return undo;
	}
	
	public AREA getTmpArea(int x1, int y1, FurnisherItem item) {
		
		AreaTmp a = GUTIL.AREA();
		a.clear();
		
		for (int y = 0; y < item.height(); y++) {
			for (int x = 0; x < item.width(); x++) {
				if (!item.is(x, y)) {
					continue;
				}
				
				int tx = x+x1;
				int ty = y+y1;
				
				if (!embryo.instance.is(tx, ty)) {
					a.set(tx, ty);
				}
				
			}
		}
		
		for (int y = -1; y <= item.height(); y++) {
			for (int x = -1; x <= item.width(); x++) {
				if (item.is(x, y)) {
					continue;
				}
				
				for (DIR d : DIR.ALL) {
					int dx = x + d.x();
					int dy = y+d.y();
					if (item.is(dx, dy) && item.get(dx, dy).mustBeReachable) {
						int tx = x+x1;
						int ty = y+y1;
						if (!embryo.instance.is(tx, ty)) {
							a.set(tx, ty);
						}
						break;
					}
				}
			}
		}
		
		return a;
		
	}
	
//	private class Area implements AREA {
//
//		private Rec body = new Rec();
//		
//		boolean init(int x1, int y1, FurnisherItem item) {
//			AreaTmp a = RES.AREA();
//			a.clear();
//			
//			for (int y = 0; y < item.height(); y++) {
//				for (int x = 0; x < item.width(); x++) {
//					if (!item.is(x, y)) {
//						continue;
//					}
//					
//					int tx = x+x1;
//					int ty = y+y1;
//					
//					if (!embryo.instance.is(tx, ty)) {
//						a.set(tx, ty);
//					}
//					
//				}
//			}
//			
//			for (int y = 0; y < item.height(); y++) {
//				for (int x = 0; x < item.width(); x++) {
//					if (!item.is(x, y)) {
//						continue;
//					}
//					if (!item.get(x, y).mustBeReachable) {
//						continue;
//					}
//					
//					for (DIR d : DIR.ORTHO) {
//						int tx = x+x1+d.x();
//						int ty = y+y1+d.y();
//						if (!item.is(x, y, d) && !embryo.instance.is(tx, ty)) {
//							a.set(tx, ty);
//							if (!item.is(x, y, d.next(1)) && !embryo.instance.is(tx, ty, d.next(1))) {
//								a.set(tx, ty, d.next(1));
//							}
//							if (!item.is(x, y, d.next(-1)) && !embryo.instance.is(tx, ty, d.next(-1))) {
//								a.set(tx, ty, d.next(-1));
//							}
//						}
//					}
//					
//				}
//			}
//			
//			if (a.area() == 0) {
//				body.set(embryo.instance);
//			}else {
//				body.set(a);
//				if (embryo.instance.area() > 0)
//					body.unify(embryo.instance.body());
//			}
//			return a.area() > 0;
//		}
//		
//		@Override
//		public RECTANGLE body() {
//			return body;
//		}
//
//		@Override
//		public boolean is(int tile) {
//			return embryo.instance.is(tile) || RES.AREA().is(tile);
//		}
//
//		@Override
//		public boolean is(int tx, int ty) {
//			return embryo.instance.is(tx, ty) || RES.AREA().is(tx, ty);
//		}
//
//		@Override
//		public int area() {
//			return embryo.instance.area() + RES.AREA().area();
//		}
//		
//	}


}
