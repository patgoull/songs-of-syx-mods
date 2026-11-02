package settlement.room.main.placement;

import init.sprite.SPRITES;
import settlement.main.SETT;
import settlement.room.main.MapDataF;
import settlement.room.main.construction.ConstructionData;
import settlement.room.main.furnisher.FurnisherItem;
import settlement.room.main.furnisher.FurnisherItemGroup;
import settlement.room.main.furnisher.FurnisherItemTile;
import snake2d.CORE;
import snake2d.LOG;
import snake2d.SPRITE_RENDERER;
import snake2d.util.color.COLOR;
import snake2d.util.datatypes.AREA;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.Coo;
import snake2d.util.datatypes.DIR;
import snake2d.util.misc.CLAMP;
import snake2d.util.sprite.SPRITE;
import util.gui.misc.GBox;
import util.text.D;
import view.main.VIEW;
import view.subview.GameWindow;
import view.tool.PLACABLE;
import view.tool.PLACER_TYPE;
import view.tool.PlacableFixed;
import view.tool.PlacableMessages;
import view.tool.PlacableMulti;
import view.tool.ToolConfig;

/**
 * GOUL: select furniture items of the room being planned (new or refurbished) and move or copy them all at once.
 *
 * 1. {@link #select(boolean, ToolConfig)} gives a rectangle selector.
 * 2. Once items are selected they follow the mouse. When moving, they are lifted out of the room first.
 * 3. A click drops them. A move drops once, a copy can be stamped again and again.
 *    Right click goes back to selecting and puts lifted items back.
 *
 * A drop is one step in the room history, so a single undo reverts it.
 * Built items that are moved are refunded and must be built again, same as removing and placing them by hand.
 */
public final class PlacerItemMove {

	private static CharSequence ¤¤select = "Select items";
	private static CharSequence ¤¤selectDesc = "Drag a rectangle over the items to pick up.";
	private static CharSequence ¤¤move = "Move items";
	private static CharSequence ¤¤copy = "Copy items";
	private static CharSequence ¤¤items = "items";
	private static CharSequence ¤¤offset = "Offset";
	private static CharSequence ¤¤cancel = "Right click to cancel.";
	private static CharSequence ¤¤maxReached = "Max amount of an item is reached.";

	static {
		D.ts(PlacerItemMove.class);
	}

	// two history entries per moved item, the history holds 1024
	private static final int MAX = 256;

	private final RoomPlacer p;
	private final FurnisherItem[] items = new FurnisherItem[MAX];
	private final int[] ox = new int[MAX];
	private final int[] oy = new int[MAX];
	private final boolean[] built = new boolean[MAX];
	private final int[] groupAm = new int[64];
	private int am = 0;

	private boolean copy;
	private boolean lifted;
	private ToolConfig config;
	private PLACABLE pending;

	// the selection follows the mouse, relative to where the mouse was when it was picked up
	private int anchorX, anchorY;
	private int halfW, halfH;
	private int offX, offY;
	private int[] grid = new int[0];

	private boolean dirty = true;
	private CharSequence problem;

	PlacerItemMove(RoomPlacer p){
		this.p = p;
	}

	/**
	 * @return the placable to activate to start selecting items
	 */
	public PLACABLE select(boolean copy, ToolConfig config) {
		restore();
		this.copy = copy;
		this.config = config;
		am = 0;
		pending = null;
		return selector;
	}

	public boolean isCurrent() {
		PLACABLE c = VIEW.s().tools.placer.getCurrent();
		return c != null && (c == selector || c == floater);
	}

	public boolean isCurrent(boolean copy) {
		return isCurrent() && this.copy == copy;
	}

	/**
	 * Goes from moving back to selecting.
	 * @return false if already selecting
	 */
	public boolean cancel() {
		if (VIEW.s().tools.placer.getCurrent() != floater)
			return false;
		restore();
		am = 0;
		VIEW.s().tools.place(selector, config);
		return true;
	}

	/**
	 * Puts lifted items back where they were picked up. Safe to call any time.
	 */
	public void restore() {
		if (!lifted)
			return;
		lifted = false;
		for (int k = 0; k < am; k++) {
			if (fits(ox[k], oy[k], items[k]))
				SETT.ROOMS().fData.itemSet(ox[k], oy[k], items[k], p.instance);
			else
				LOG.err("item mover: could not put back " + items[k].group.name() + " at " + ox[k] + " " + oy[k]);
		}
		am = 0;
		p.resources.updatee();
	}

	private boolean fits(int x1, int y1, FurnisherItem it) {
		for (int y = 0; y < it.height(); y++) {
			for (int x = 0; x < it.width(); x++) {
				if (it.get(x, y) == null)
					continue;
				if (!p.instance.is(x1+x, y1+y) || SETT.ROOMS().fData.tile.is(x1+x, y1+y))
					return false;
			}
		}
		return true;
	}

	private void pickUp(int mx, int my) {
		int x1 = Integer.MAX_VALUE;
		int y1 = Integer.MAX_VALUE;
		int x2 = Integer.MIN_VALUE;
		int y2 = Integer.MIN_VALUE;
		for (int k = 0; k < am; k++) {
			x1 = Math.min(x1, ox[k]);
			y1 = Math.min(y1, oy[k]);
			x2 = Math.max(x2, ox[k]+items[k].width()-1);
			y2 = Math.max(y2, oy[k]+items[k].height()-1);
		}
		// the selection is held at the tile of it closest to the mouse, so dragging past the items doesn't
		// leave them far from the cursor
		anchorX = CLAMP.i(mx, x1, x2);
		anchorY = CLAMP.i(my, y1, y2);
		halfW = Math.max(anchorX-x1, x2-anchorX);
		halfH = Math.max(anchorY-y1, y2-anchorY);

		int w = halfW*2+1;
		int h = halfH*2+1;
		if (grid.length < w*h)
			grid = new int[w*h];
		for (int i = 0; i < w*h; i++)
			grid[i] = -1;
		for (int k = 0; k < am; k++) {
			FurnisherItem it = items[k];
			for (int y = 0; y < it.height(); y++) {
				for (int x = 0; x < it.width(); x++) {
					if (it.get(x, y) != null)
						grid[rx(k)+x + (ry(k)+y)*w] = k;
				}
			}
		}

		if (!copy) {
			for (int k = 0; k < am; k++)
				SETT.ROOMS().fData.itemClear(ox[k]+items[k].firstX(), oy[k]+items[k].firstY(), p.instance);
			lifted = true;
			p.resources.updatee();
		}

		offX = 0;
		offY = 0;
		dirty = true;
	}

	// upper left of item k, relative to the floating box
	private int rx(int k) {
		return ox[k]-anchorX+halfW;
	}

	private int ry(int k) {
		return oy[k]-anchorY+halfH;
	}

	private int itemAt(int rx, int ry) {
		int w = halfW*2+1;
		if (rx < 0 || ry < 0 || rx >= w || ry >= halfH*2+1)
			return -1;
		int k = grid[rx+ry*w];
		return k < am ? k : -1;
	}

	private CharSequence problem() {
		if (!dirty)
			return problem;
		dirty = false;
		problem = null;

		if (copy) {
			for (int i = 0; i < groupAm.length; i++)
				groupAm[i] = 0;
			for (int k = 0; k < am; k++) {
				FurnisherItemGroup g = items[k].group;
				groupAm[g.index()]++;
				if (p.resources.groups(g) + groupAm[g.index()] > g.max) {
					problem = ¤¤maxReached;
					return problem;
				}
			}
		}

		for (int k = 0; k < am; k++) {
			CharSequence s = p.placability.itemProblem(ox[k]+offX, oy[k]+offY, items[k].group, items[k], p.instance);
			if (s != null) {
				problem = s;
				return problem;
			}
		}
		return problem;
	}

	private void drop() {

		if (!copy && offX == 0 && offY == 0) {
			restore();
			pending = selector;
			return;
		}

		MapDataF d = SETT.ROOMS().fData;

		if (!copy)
			for (int k = 0; k < am; k++)
				if (built[k])
					setBuilt(k, 0);

		int placed = 0;
		CharSequence fail = null;
		for (; placed < am; placed++) {
			FurnisherItem it = items[placed];
			int x = ox[placed]+offX;
			int y = oy[placed]+offY;
			fail = p.placability.itemProblem(x, y, it.group, it, p.instance);
			if (fail != null)
				break;
			d.itemSet(x, y, it, p.instance);
			p.resources.updatee();
		}

		if (fail != null) {
			for (int k = 0; k < placed; k++)
				d.itemClear(ox[k]+offX+items[k].firstX(), oy[k]+offY+items[k].firstY(), p.instance);
			if (!copy)
				for (int k = 0; k < am; k++)
					if (built[k])
						setBuilt(k, 1);
			p.resources.updatee();
			dirty = false;
			problem = fail;
			return;
		}

		if (!copy) {
			for (int k = 0; k < am; k++)
				p.history.placeItem(items[k], ox[k], oy[k], -1);
			for (int k = 0; k < am; k++)
				if (built[k])
					p.resources.removeItem(ox[k], oy[k], items[k]);
		}
		for (int k = 0; k < am; k++)
			p.history.placeItem(items[k], ox[k]+offX, oy[k]+offY, 1);

		if (copy) {
			dirty = true;
		}else {
			lifted = false;
			am = 0;
			pending = selector;
		}
	}

	private void setBuilt(int k, int value) {
		FurnisherItem it = items[k];
		for (int y = 0; y < it.height(); y++)
			for (int x = 0; x < it.width(); x++)
				if (it.get(x, y) != null)
					ConstructionData.dConstructed.set(p.instance, ox[k]+x, oy[k]+y, value);
	}

	private final Selector selector = new Selector();

	private final class Selector extends PlacableMulti {

		Selector(){
			super(¤¤select, ¤¤selectDesc, null);
		}

		@Override
		public CharSequence isPlacable(int tx, int ty, AREA area, PLACER_TYPE type) {
			if (!p.instance.is(tx, ty) || SETT.ROOMS().fData.item.get(tx, ty) == null)
				return PlacableMessages.¤¤ITEM_MUST;
			return null;
		}

		@Override
		public boolean expandsTo(int fromX, int fromY, int toX, int toY) {
			if (!p.instance.is(fromX, fromY) || !p.instance.is(toX, toY))
				return false;
			MapDataF d = SETT.ROOMS().fData;
			if (d.item.get(fromX, fromY) == null || d.item.get(toX, toY) == null)
				return false;
			COORDINATE c = d.itemMaster(fromX, fromY, Coo.TMP);
			int x = c.x();
			int y = c.y();
			return d.itemMaster(toX, toY, Coo.TMP).isSameAs(x, y);
		}

		@Override
		public boolean canBePlacedAs(PLACER_TYPE t) {
			return t == PLACER_TYPE.SQUARE;
		}

		@Override
		public void finishChecking(AREA placedArea) {
			am = 0;
		}

		@Override
		public void place(int tx, int ty, AREA area, PLACER_TYPE type) {
			MapDataF d = SETT.ROOMS().fData;
			if (am >= MAX || !d.isMaster.is(tx, ty))
				return;
			COORDINATE c = d.itemX1Y1(tx, ty, Coo.TMP);
			items[am] = d.item.get(tx, ty);
			ox[am] = c.x();
			oy[am] = c.y();
			built[am] = ConstructionData.dConstructed.is(tx, ty, 1);
			am++;
		}

		@Override
		public void finishPlacing(AREA placedArea) {
			if (am > 0)
				pending = floater;
		}

		@Override
		public void updateRegardless(GameWindow window, AREA selected) {
			if (pending == floater) {
				pending = null;
				pickUp(window.tile().x(), window.tile().y());
				VIEW.s().tools.place(floater, config);
			}
		}

		@Override
		public void hoverDesc(GBox box) {
			box.title(copy ? ¤¤copy : ¤¤move);
			box.text(desc);
		}

	};

	private final Floater floater = new Floater();

	private final class Floater extends PlacableFixed {

		@Override
		public CharSequence name() {
			return copy ? ¤¤copy : ¤¤move;
		}

		@Override
		public SPRITE getIcon() {
			return null;
		}

		@Override
		public PLACABLE getUndo() {
			return null;
		}

		@Override
		public int rotations() {
			return 1;
		}

		@Override
		public int sizes() {
			return 1;
		}

		@Override
		public int width() {
			return halfW*2+1;
		}

		@Override
		public int height() {
			return halfH*2+1;
		}

		@Override
		public void init(int cx, int cy) {
			int dx = cx-anchorX;
			int dy = cy-anchorY;
			if (dx != offX || dy != offY) {
				offX = dx;
				offY = dy;
				dirty = true;
			}
		}

		@Override
		public CharSequence placableWhole(int tx1, int ty1) {
			return problem();
		}

		@Override
		public CharSequence placable(int tx, int ty, int rx, int ry) {
			int k = itemAt(rx, ry);
			if (k < 0)
				return null;
			return p.placability.itemPlacable(tx, ty, rx-rx(k), ry-ry(k), items[k], p.instance);
		}

		@Override
		public void place(int tx, int ty, int rx, int ry) {

		}

		@Override
		public void afterPlaced(int tx1, int ty1) {
			drop();
		}

		@Override
		public void updateRegardless(GameWindow window) {
			// also when the undo button put the lifted items back
			if (pending == selector || am == 0) {
				pending = null;
				am = 0;
				VIEW.s().tools.place(selector, config);
			}
		}

		@Override
		public void renderPlaceHolder(SPRITE_RENDERER r, int mask, int x, int y, int tx, int ty, int rx, int ry, boolean isPlacable, boolean areaIsPlacable) {
			int k = itemAt(rx, ry);
			if (k < 0)
				return;
			FurnisherItem it = items[k];
			int ix = rx-rx(k);
			int iy = ry-ry(k);
			FurnisherItemTile t = it.get(ix, iy);
			if (t == null)
				return;

			if (t.mustBeReachable) {
				SPRITES.cons().BIG.filled.render(r, 0, x, y);
				COLOR c = CORE.renderer().colorGet();
				COLOR.unbind();
				int ri = -1;
				if (t.sprite() != null) {
					int d = t.sprite().getData(tx, ty, ix, iy, it, 0);
					ri = t.sprite.rotation(d, it)-1;
				}
				if (ri < 0)
					SPRITES.cons().ICO.arrows_inward.render(r, x, y);
				else
					SPRITES.cons().ICO.arrows_inwards.get(ri).render(r, x, y);
				c.bind();
			}else if (t.sprite() != null) {
				int d = t.sprite().getData(tx, ty, ix, iy, it, 0);
				t.sprite().renderPlaceholder(r, x, y, d, tx, ty, ix, iy, it);
			}else {
				int m = 0;
				for (DIR dir : DIR.ORTHO)
					if (it.get(ix+dir.x(), iy+dir.y()) != null)
						m |= dir.mask();
				SPRITES.cons().BIG.dashed.render(r, m, x, y);
			}
			it.group.blueprint.renderExtra(r, x, y, tx, ty, ix, iy, it);
		}

		@Override
		public void placeInfo(GBox b, int x1, int y1) {
			b.add(b.text().add(am).s().add(¤¤items));
			b.NL();
			b.add(b.text().lablify().add(¤¤offset));
			b.tab(4);
			b.add(b.text().add(offX).add(',').s().add(offY));
			b.NL();
			b.text(¤¤cancel);
		}

	};

}
