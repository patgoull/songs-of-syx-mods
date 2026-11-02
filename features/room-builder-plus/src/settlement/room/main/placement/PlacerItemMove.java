package settlement.room.main.placement;

import init.sprite.SPRITES;
import settlement.main.SETT;
import settlement.room.main.MapDataF;
import settlement.room.main.construction.ConstructionData;
import settlement.room.main.furnisher.FurnisherItem;
import settlement.room.main.furnisher.FurnisherItemGroup;
import settlement.room.main.furnisher.FurnisherItemTile;
import snake2d.CORE;
import snake2d.KEYCODES;
import snake2d.LOG;
import snake2d.SPRITE_RENDERER;
import snake2d.util.color.COLOR;
import snake2d.util.datatypes.AREA;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.Coo;
import snake2d.util.datatypes.DIR;
import snake2d.util.datatypes.RECTANGLE;
import snake2d.util.misc.CLAMP;
import snake2d.util.sprite.SPRITE;
import snake2d.util.sprite.text.Str;
import util.gui.misc.GBox;
import util.text.D;
import view.keyboard.KEYS;
import view.main.VIEW;
import view.subview.GameWindow;
import view.tool.PLACABLE;
import view.tool.PLACER_TYPE;
import view.tool.PlacableFixed;
import view.tool.PlacableMessages;
import view.tool.PlacableMulti;
import view.tool.ToolConfig;

/**
 * GOUL: select furniture items and inner walls of the room being planned (new or refurbished) and move or copy them
 * all at once.
 *
 * 1. {@link #select(boolean, ToolConfig)} gives a rectangle selector. Holding the mod key (Ctrl) when releasing
 *    also selects every other item of the same types in the room, and every inner wall if one was selected.
 *    Inner walls are only picked up with Alt: held when the tool starts (Ctrl+Alt+X) or while dragging.
 * 2. Once items are selected they follow the mouse. When moving, they are lifted out of the room first.
 *    While carried, the selection can be rotated (the normal rotate key) and flipped.
 * 3. A click drops them. A move drops once, a copy can be stamped again and again.
 *    Right click goes back to selecting and puts lifted items back.
 *
 * Inner walls are the tiles left out of the room that the room encloses on every side, diagonals included
 * (pillars made with shrink room). Moving one is the same as expanding the room over the old spot and shrinking it
 * at the new one: the floor there is refunded, and items next to the new wall that no longer fit are removed.
 *
 * A drop is one step in the room history, so a single undo reverts it.
 * Built items that are moved are refunded and must be built again, same as removing and placing them by hand.
 */
public final class PlacerItemMove {

	private static CharSequence ¤¤select = "Select items";
	private static CharSequence ¤¤selectDesc = "Drag a rectangle over the items to pick up. Hold {0} when releasing to also select all items of the same types in the room. Hold Alt to also pick up inner walls (all of them with {0}).";
	private static CharSequence ¤¤wallsHint = "Hold Alt: inner walls too";
	private static CharSequence ¤¤move = "Move items";
	private static CharSequence ¤¤copy = "Copy items";
	private static CharSequence ¤¤items = "items";
	private static CharSequence ¤¤walls = "wall tiles";
	private static CharSequence ¤¤offset = "Offset";
	private static CharSequence ¤¤allOfType = "Hold {0} when releasing: all items of these types";
	private static CharSequence ¤¤rotate = "Rotate:";
	private static CharSequence ¤¤cancel = "Right click to cancel.";
	private static CharSequence ¤¤maxReached = "Max amount of an item is reached.";
	private static CharSequence ¤¤cantRotate = "Can't be rotated:";
	private static CharSequence ¤¤cantFlip = "Can't be mirrored:";
	private static CharSequence ¤¤cantGrow = "No bigger size:";
	private static CharSequence ¤¤cantShrink = "No smaller size:";
	private static CharSequence ¤¤size = "Size:";
	private static CharSequence ¤¤overlap = "Items overlap each other.";
	private static CharSequence ¤¤wallFloor = "Walls must go on room floor without items.";
	private static CharSequence ¤¤wallStuck = "The wall can't become room, copy it instead:";

	static {
		D.ts(PlacerItemMove.class);
	}

	// two history entries per moved item, the history holds 1024
	private static final int MAX = 256;
	// two history entries per moved wall tile
	private static final int MAXW = 128;

	private static final int ROTATE = 0;
	private static final int FLIP_X = 1;
	private static final int FLIP_Y = 2;
	private static final int GROW = 3;
	private static final int SHRINK = 4;

	private final RoomPlacer p;

	// the items as they were picked up: put back, history removals and refunds use these
	private final FurnisherItem[] items = new FurnisherItem[MAX];
	private final int[] ox = new int[MAX];
	private final int[] oy = new int[MAX];
	private final boolean[] built = new boolean[MAX];
	private int am = 0;

	// what gets placed at offset 0: the same items, unless the selection was rotated or flipped
	private final FurnisherItem[] pit = new FurnisherItem[MAX];
	private final int[] px = new int[MAX];
	private final int[] py = new int[MAX];
	private final FurnisherItem[] tmp = new FurnisherItem[MAX];

	// inner wall tiles, as picked up and where they go at offset 0
	private final int[] wx = new int[MAXW];
	private final int[] wy = new int[MAXW];
	private final int[] pwx = new int[MAXW];
	private final int[] pwy = new int[MAXW];
	// room data of the tiles a drop turns into walls, to put back if the drop fails
	private final int[] wData = new int[MAXW];
	private int wAm = 0;
	private boolean wLifted;
	private boolean wStuck;
	private final Str stuck = new Str(128);

	// items next to new walls that no longer fit, removed by a drop
	private final FurnisherItem[] nIt = new FurnisherItem[MAX];
	private final int[] nx = new int[MAX];
	private final int[] ny = new int[MAX];
	private int nAm = 0;

	// inner walls of the room: per tile of the room's box (one wider on each side), the wall it belongs to,
	// -1 for room and outside, -2 while not known yet
	private int[] walls = new int[0];
	private int[] stack = new int[0];
	private boolean[] wallSel = new boolean[0];
	private int wallsAm;
	private int scanX, scanY, scanW, scanH;
	private int scanArea = -1;
	private boolean scanDirty = true;

	private final int[] groupAm = new int[64];
	private final boolean[] groupSel = new boolean[64];

	private boolean copy;
	// inner walls are picked up too, the tool was started with Alt held
	private boolean withWalls;
	private boolean lifted;
	private ToolConfig config;
	private PLACABLE pending;

	// the selection follows the mouse, relative to where it was picked up
	private int anchorX, anchorY;
	private int halfW, halfH;
	private int offX, offY;
	// per tile of the floating box: the item (index) or wall tile (MAX + index) on it, -1 if none
	private int[] grid = new int[0];
	// tiles where two carried items overlap, after a resize
	private boolean[] gridOverlap = new boolean[0];
	private boolean overlaps;

	private boolean dirty = true;
	private CharSequence problem;
	private final Str note = new Str(128);
	// per transform (ROTATE, FLIP_X, FLIP_Y, GROW, SHRINK), the first carried item that has no such variant
	private final FurnisherItem[] blocked = new FurnisherItem[5];
	private final Str reason = new Str(128);
	private double noteUntil;

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
		withWalls = altDown();
		am = 0;
		wAm = 0;
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
	 * @return true while selected items follow the mouse
	 */
	public boolean isCarrying() {
		return VIEW.s().tools.placer.getCurrent() == floater && am + wAm > 0;
	}

	/**
	 * @return why the carried selection can't be mirrored, or null if it can
	 */
	public CharSequence cantFlip(boolean horizontal) {
		return cant(horizontal ? FLIP_X : FLIP_Y);
	}

	private CharSequence cant(int op) {
		if (!isCarrying() || blocked[op] == null)
			return null;
		CharSequence s = op == ROTATE ? ¤¤cantRotate : op == GROW ? ¤¤cantGrow : op == SHRINK ? ¤¤cantShrink : ¤¤cantFlip;
		return reason.clear().add(s).s().add(blocked[op].group.name());
	}

	/**
	 * @return the item type of the carried items, null if no item is carried or they're of different types
	 */
	public FurnisherItemGroup carriedGroup() {
		if (!isCarrying() || am == 0)
			return null;
		FurnisherItemGroup g = pit[0].group;
		for (int k = 1; k < am; k++)
			if (pit[k].group != g)
				return null;
		return g;
	}

	// gives every carried item the next size up or down of its type, keeping it centred
	private void resize(boolean bigger) {
		if (isCarrying())
			transform(bigger ? GROW : SHRINK);
	}

	/**
	 * Mirrors the carried selection around the mouse.
	 */
	public void flip(boolean horizontal) {
		if (isCarrying())
			transform(horizontal ? FLIP_X : FLIP_Y);
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
		wAm = 0;
		VIEW.s().tools.place(selector, config);
		return true;
	}

	/**
	 * Puts lifted items and walls back where they were picked up. Safe to call any time.
	 */
	public void restore() {
		// the room may have changed (undo, redo, expand), find the inner walls again before the next selection
		scanDirty = true;
		if (!lifted)
			return;
		lifted = false;
		if (wLifted)
			for (int k = 0; k < wAm; k++)
				p.instance.clear(wx[k], wy[k]);
		wLifted = false;
		for (int k = 0; k < am; k++) {
			if (fits(ox[k], oy[k], items[k]))
				SETT.ROOMS().fData.itemSet(ox[k], oy[k], items[k], p.instance);
			else
				LOG.err("item mover: could not put back " + items[k].group.name() + " at " + ox[k] + " " + oy[k]);
		}
		am = 0;
		wAm = 0;
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

	// what the area placer checks before it lets an item stay after the room changed
	private CharSequence fitsNow(int x1, int y1, FurnisherItem it) {
		for (int y = 0; y < it.height(); y++) {
			for (int x = 0; x < it.width(); x++) {
				CharSequence s = p.placability.itemPlacable(x1+x, y1+y, x, y, it, p.instance);
				if (s != null)
					return s;
			}
		}
		return p.placability.itemProblem(x1, y1, it.group, it, p.instance);
	}

	private void add(int tx, int ty) {
		MapDataF d = SETT.ROOMS().fData;
		COORDINATE c = d.itemX1Y1(tx, ty, Coo.TMP);
		items[am] = d.item.get(tx, ty);
		ox[am] = c.x();
		oy[am] = c.y();
		built[am] = ConstructionData.dConstructed.is(tx, ty, 1);
		am++;
	}

	// adds every other item of the room whose type is already selected, and every inner wall if one is
	private void addSameTypes() {
		if (wAm > 0)
			for (int w = 0; w < wallsAm; w++)
				addWall(w);

		for (int i = 0; i < groupSel.length; i++)
			groupSel[i] = false;
		for (int k = 0; k < am; k++)
			groupSel[items[k].group.index()] = true;

		MapDataF d = SETT.ROOMS().fData;
		int selected = am;
		for (COORDINATE c : p.instance.body()) {
			if (am >= MAX)
				return;
			if (!p.instance.is(c) || !d.isMaster.is(c.x(), c.y()))
				continue;
			FurnisherItem it = d.item.get(c.x(), c.y());
			if (it == null || !groupSel[it.group.index()])
				continue;
			int x = d.itemX1Y1(c.x(), c.y(), Coo.TMP).x();
			int y = d.itemX1Y1(c.x(), c.y(), Coo.TMP).y();
			boolean has = false;
			for (int k = 0; k < selected; k++) {
				if (ox[k] == x && oy[k] == y) {
					has = true;
					break;
				}
			}
			if (!has)
				add(c.x(), c.y());
		}
	}

	// finds the inner walls: what isn't room and can't be reached from outside the room's box without crossing room
	private void scan() {
		RECTANGLE b = p.instance.body();
		int x1 = b.x1()-1;
		int y1 = b.y1()-1;
		int w = b.width()+2;
		int h = b.height()+2;
		if (!scanDirty && scanArea == p.instance.area() && x1 == scanX && y1 == scanY && w == scanW && h == scanH)
			return;
		scanDirty = false;
		scanArea = p.instance.area();
		scanX = x1;
		scanY = y1;
		scanW = w;
		scanH = h;
		wallsAm = 0;
		if (scanArea <= 0 || w <= 2 || h <= 2) {
			scanW = 0;
			scanH = 0;
			return;
		}

		if (walls.length < w*h) {
			walls = new int[w*h];
			stack = new int[w*h];
		}
		for (int i = 0; i < w*h; i++)
			walls[i] = p.instance.is(x1+i%w, y1+i/w) ? -1 : -2;
		// the border of the box is outside the room. Diagonals count, so a wall that touches the outside at a
		// corner isn't an inner wall
		for (int i = 0; i < w*h; i++) {
			int x = i%w;
			int y = i/w;
			if ((x == 0 || y == 0 || x == w-1 || y == h-1) && walls[i] == -2)
				fill(i, -1);
		}
		for (int i = 0; i < w*h; i++)
			if (walls[i] == -2)
				fill(i, wallsAm++);
		if (wallSel.length < wallsAm)
			wallSel = new boolean[wallsAm];
	}

	private void fill(int start, int value) {
		int n = 0;
		stack[n++] = start;
		walls[start] = value;
		while (n > 0) {
			int i = stack[--n];
			int x = i%scanW;
			int y = i/scanW;
			for (int di = 0; di < DIR.ALL.size(); di++) {
				DIR d = DIR.ALL.get(di);
				int dx = x+d.x();
				int dy = y+d.y();
				if (dx < 0 || dy < 0 || dx >= scanW || dy >= scanH)
					continue;
				int j = dx+dy*scanW;
				if (walls[j] != -2)
					continue;
				walls[j] = value;
				stack[n++] = j;
			}
		}
	}

	private static boolean altDown() {
		return CORE.getInput().getKeyboard().isPressed(KEYCODES.KEY_LEFT_ALT) || CORE.getInput().getKeyboard().isPressed(KEYCODES.KEY_RIGHT_ALT);
	}

	// @return the inner wall on the tile, -1 if none or if walls aren't picked up
	private int wall(int tx, int ty) {
		if (!withWalls && !altDown())
			return -1;
		int x = tx-scanX;
		int y = ty-scanY;
		if (x < 0 || y < 0 || x >= scanW || y >= scanH)
			return -1;
		return walls[x+y*scanW];
	}

	private void addWall(int w) {
		if (wallSel[w])
			return;
		int n = 0;
		for (int i = 0; i < scanW*scanH; i++)
			if (walls[i] == w)
				n++;
		if (wAm + n > MAXW)
			return;
		wallSel[w] = true;
		for (int i = 0; i < scanW*scanH; i++) {
			if (walls[i] == w) {
				wx[wAm] = scanX+i%scanW;
				wy[wAm] = scanY+i/scanW;
				wAm++;
			}
		}
	}

	// @return why the wall tile can't become room
	private CharSequence wallStuck(int tx, int ty) {
		CharSequence s = PLACEMENT.placable(tx, ty, p.blueprint(), p.buildOnWalls.is());
		if (s != null)
			return s;
		return p.blueprint().constructor().placable(tx, ty, null, null);
	}

	// @return why a wall can't go on the tile
	private CharSequence wallProblem(int tx, int ty) {
		if (!p.instance.is(tx, ty) || SETT.ROOMS().fData.tile.is(tx, ty))
			return ¤¤wallFloor;
		return null;
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
			pit[k] = items[k];
			px[k] = ox[k];
			py[k] = oy[k];
		}
		for (int k = 0; k < wAm; k++) {
			x1 = Math.min(x1, wx[k]);
			y1 = Math.min(y1, wy[k]);
			x2 = Math.max(x2, wx[k]);
			y2 = Math.max(y2, wy[k]);
			pwx[k] = wx[k];
			pwy[k] = wy[k];
		}
		// the selection is held at the tile of it closest to the mouse, so dragging past the items doesn't
		// leave them far from the cursor
		anchorX = CLAMP.i(mx, x1, x2);
		anchorY = CLAMP.i(my, y1, y2);
		buildGrid();

		wStuck = false;
		if (!copy) {
			for (int k = 0; k < am; k++)
				SETT.ROOMS().fData.itemClear(ox[k]+items[k].firstX(), oy[k]+items[k].firstY(), p.instance);
			// the walls' spots become room while carried, so items can be dropped there
			for (int k = 0; k < wAm && !wStuck; k++) {
				CharSequence s = wallStuck(wx[k], wy[k]);
				if (s != null) {
					stuck.clear().add(¤¤wallStuck).s().add(s);
					wStuck = true;
				}
			}
			if (!wStuck) {
				for (int k = 0; k < wAm; k++)
					p.instance.set(wx[k], wy[k]);
				wLifted = wAm > 0;
			}
			lifted = true;
			p.resources.updatee();
		}

		offX = 0;
		offY = 0;
		noteUntil = 0;
		floater.sizeSet(floater.sizeBase());
		dirty = true;
	}

	// the floating box is centred on the anchor, big enough for the selection in any direction
	private void buildGrid() {
		halfW = 0;
		halfH = 0;
		for (int k = 0; k < am; k++) {
			halfW = Math.max(halfW, Math.max(anchorX-px[k], px[k]+pit[k].width()-1-anchorX));
			halfH = Math.max(halfH, Math.max(anchorY-py[k], py[k]+pit[k].height()-1-anchorY));
		}
		for (int k = 0; k < wAm; k++) {
			halfW = Math.max(halfW, Math.abs(pwx[k]-anchorX));
			halfH = Math.max(halfH, Math.abs(pwy[k]-anchorY));
		}

		for (int op = ROTATE; op <= SHRINK; op++) {
			blocked[op] = null;
			for (int k = 0; k < am && blocked[op] == null; k++)
				if (turned(pit[k], op) == null)
					blocked[op] = pit[k];
		}

		int w = halfW*2+1;
		int h = halfH*2+1;
		if (grid.length < w*h) {
			grid = new int[w*h];
			gridOverlap = new boolean[w*h];
		}
		for (int i = 0; i < w*h; i++) {
			grid[i] = -1;
			gridOverlap[i] = false;
		}
		overlaps = false;
		for (int k = 0; k < am; k++) {
			FurnisherItem it = pit[k];
			for (int y = 0; y < it.height(); y++) {
				for (int x = 0; x < it.width(); x++) {
					if (it.get(x, y) == null)
						continue;
					mark(rx(k)+x + (ry(k)+y)*w, k);
				}
			}
		}
		for (int k = 0; k < wAm; k++)
			mark(pwx[k]-anchorX+halfW + (pwy[k]-anchorY+halfH)*w, MAX+k);
	}

	private void mark(int i, int value) {
		if (grid[i] >= 0) {
			gridOverlap[i] = true;
			overlaps = true;
		}
		grid[i] = value;
	}

	// upper left of item k, relative to the floating box
	private int rx(int k) {
		return px[k]-anchorX+halfW;
	}

	private int ry(int k) {
		return py[k]-anchorY+halfH;
	}

	private int gridAt(int rx, int ry) {
		int w = halfW*2+1;
		if (rx < 0 || ry < 0 || rx >= w || ry >= halfH*2+1)
			return -1;
		return grid[rx+ry*w];
	}

	private int itemAt(int rx, int ry) {
		int k = gridAt(rx, ry);
		return k < am ? k : -1;
	}

	private int wallAt(int rx, int ry) {
		int k = gridAt(rx, ry);
		return k >= MAX ? k-MAX : -1;
	}

	/**
	 * Rotates (90° clockwise) or mirrors the selection around the anchor. Every item is swapped for the
	 * variant of its group whose tiles match the turned item. If one has no such variant, nothing changes.
	 * Walls turn with the selection and keep their size.
	 */
	private void transform(int op) {
		for (int k = 0; k < am; k++) {
			tmp[k] = turned(pit[k], op);
			if (tmp[k] == null) {
				blocked[op] = pit[k];
				note.clear().add(cant(op));
				noteUntil = VIEW.renderSecond() + 4;
				return;
			}
		}
		for (int k = 0; k < am; k++) {
			int x1 = px[k]-anchorX;
			int y1 = py[k]-anchorY;
			int w = pit[k].width();
			int h = pit[k].height();
			if (op == GROW || op == SHRINK) {
				px[k] += w/2 - tmp[k].width()/2;
				py[k] += h/2 - tmp[k].height()/2;
			}else if (op == ROTATE) {
				px[k] = anchorX-(y1+h-1);
				py[k] = anchorY+x1;
			}else if (op == FLIP_X) {
				px[k] = anchorX-(x1+w-1);
			}else {
				py[k] = anchorY-(y1+h-1);
			}
			pit[k] = tmp[k];
		}
		for (int k = 0; k < wAm; k++) {
			int x1 = pwx[k]-anchorX;
			int y1 = pwy[k]-anchorY;
			if (op == ROTATE) {
				pwx[k] = anchorX-y1;
				pwy[k] = anchorY+x1;
			}else if (op == FLIP_X) {
				pwx[k] = anchorX-x1;
			}else if (op == FLIP_Y) {
				pwy[k] = anchorY-y1;
			}
		}
		noteUntil = 0;
		buildGrid();
		dirty = true;
	}

	private static FurnisherItem turned(FurnisherItem it, int op) {
		FurnisherItemGroup g = it.group;
		if (op == GROW || op == SHRINK) {
			int s = it.variation() + (op == GROW ? 1 : -1);
			return s >= 0 && s < g.size() ? g.item(s, it.rotation) : null;
		}
		int n = g.rotations();
		int r = it.rotation;
		// rotation i is the first variant turned i times clockwise. Try the variant the turn should give first,
		// so items whose tiles look the same either way (1x1, symmetric) still turn to face the right way
		int a = ((-r) % n + n) % n;
		int b = ((2-r) % n + n) % n;
		int[] tries = op == ROTATE ? new int[] {(r+1) % n} : op == FLIP_X ? new int[] {a, b} : new int[] {b, a};
		for (int t : tries) {
			FurnisherItem c = g.item(it.variation(), t);
			if (matches(it, c, op))
				return c;
		}
		for (int t = 0; t < n; t++) {
			FurnisherItem c = g.item(it.variation(), t);
			if (matches(it, c, op))
				return c;
		}
		return null;
	}

	private static boolean matches(FurnisherItem it, FurnisherItem c, int op) {
		int w = it.width();
		int h = it.height();
		if (op == ROTATE ? (c.width() != h || c.height() != w) : (c.width() != w || c.height() != h))
			return false;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int cx = op == ROTATE ? h-1-y : op == FLIP_X ? w-1-x : x;
				int cy = op == ROTATE ? x : op == FLIP_Y ? h-1-y : y;
				if (c.get(cx, cy) != it.get(x, y))
					return false;
			}
		}
		return true;
	}

	private boolean unchanged() {
		if (offX != 0 || offY != 0)
			return false;
		for (int k = 0; k < am; k++)
			if (pit[k] != items[k] || px[k] != ox[k] || py[k] != oy[k])
				return false;
		for (int k = 0; k < wAm; k++)
			if (pwx[k] != wx[k] || pwy[k] != wy[k])
				return false;
		return true;
	}

	private CharSequence problem() {
		if (!dirty)
			return problem;
		dirty = false;
		problem = null;

		if (overlaps) {
			problem = ¤¤overlap;
			return problem;
		}

		if (!copy && wStuck) {
			problem = stuck;
			return problem;
		}

		if (copy) {
			for (int i = 0; i < groupAm.length; i++)
				groupAm[i] = 0;
			for (int k = 0; k < am; k++) {
				FurnisherItemGroup g = pit[k].group;
				groupAm[g.index()]++;
				if (p.resources.groups(g) + groupAm[g.index()] > g.max) {
					problem = ¤¤maxReached;
					return problem;
				}
			}
		}

		for (int k = 0; k < wAm; k++) {
			CharSequence s = wallProblem(pwx[k]+offX, pwy[k]+offY);
			if (s != null) {
				problem = s;
				return problem;
			}
		}

		for (int k = 0; k < am; k++) {
			CharSequence s = p.placability.itemProblem(px[k]+offX, py[k]+offY, pit[k].group, pit[k], p.instance);
			if (s != null) {
				problem = s;
				return problem;
			}
		}
		return problem;
	}

	private void drop() {

		if (!copy && unchanged()) {
			restore();
			pending = selector;
			return;
		}

		MapDataF d = SETT.ROOMS().fData;

		if (!copy)
			for (int k = 0; k < am; k++)
				if (built[k])
					setBuilt(k, 0);

		// walls first, so the items are checked next to them
		CharSequence fail = null;
		int cut = 0;
		for (; cut < wAm; cut++) {
			int x = pwx[cut]+offX;
			int y = pwy[cut]+offY;
			fail = wallProblem(x, y);
			if (fail != null)
				break;
			wData[cut] = SETT.ROOMS().data.get(x, y);
			p.instance.clear(x, y);
		}

		int placed = 0;
		if (fail == null) {
			for (; placed < am; placed++) {
				FurnisherItem it = pit[placed];
				int x = px[placed]+offX;
				int y = py[placed]+offY;
				fail = fitsNow(x, y, it);
				if (fail != null)
					break;
				d.itemSet(x, y, it, p.instance);
				p.resources.updatee();
			}
		}

		if (fail != null) {
			for (int k = 0; k < placed; k++)
				d.itemClear(px[k]+offX+pit[k].firstX(), py[k]+offY+pit[k].firstY(), p.instance);
			for (int k = 0; k < cut; k++) {
				int x = pwx[k]+offX;
				int y = pwy[k]+offY;
				p.instance.set(x, y);
				SETT.ROOMS().data.set(p.instance, x, y, wData[k]);
			}
			if (!copy)
				for (int k = 0; k < am; k++)
					if (built[k])
						setBuilt(k, 1);
			p.resources.updatee();
			dirty = false;
			problem = fail;
			return;
		}

		// what the area placer does when it shrinks or expands the room
		for (int k = 0; k < wAm; k++)
			if (ConstructionData.dFloored.is(wData[k], 1))
				p.resources.removeTile(pwx[k]+offX, pwy[k]+offY);
		if (wLifted)
			for (int k = 0; k < wAm; k++)
				if (p.instance.is(wx[k], wy[k]))
					SETT.JOBS().clearer.set(wx[k], wy[k]);
		nAm = 0;
		for (int k = 0; k < wAm; k++)
			validateItems(pwx[k]+offX, pwy[k]+offY);

		// undo goes newest first: the dropped items, the new walls, the old walls, then the removed items come back
		if (!copy) {
			for (int k = 0; k < am; k++)
				p.history.placeItem(items[k], ox[k], oy[k], -1);
			for (int k = 0; k < am; k++)
				if (built[k])
					p.resources.removeItem(ox[k], oy[k], items[k]);
		}
		for (int k = 0; k < nAm; k++)
			p.history.placeItem(nIt[k], nx[k], ny[k], -1);
		if (wLifted)
			for (int k = 0; k < wAm; k++)
				p.history.placeEmbryo(wx[k], wy[k], 1);
		for (int k = 0; k < wAm; k++)
			p.history.placeEmbryo(pwx[k]+offX, pwy[k]+offY, -1);
		for (int k = 0; k < am; k++)
			p.history.placeItem(pit[k], px[k]+offX, py[k]+offY, 1);
		p.resources.updatee();
		scanDirty = true;

		if (copy) {
			dirty = true;
		}else {
			lifted = false;
			wLifted = false;
			am = 0;
			wAm = 0;
			pending = selector;
		}
	}

	// items around a new wall that don't fit any more are removed, like the area placer does
	private void validateItems(int tx, int ty) {
		MapDataF d = SETT.ROOMS().fData;
		for (int i = 0; i < DIR.ALL.size(); i++) {
			DIR dir = DIR.ALL.get(i);
			int dx = tx+dir.x();
			int dy = ty+dir.y();
			if (!p.instance.is(dx, dy))
				continue;
			FurnisherItem it = d.item.get(dx, dy);
			if (it == null)
				continue;
			COORDINATE c = d.itemX1Y1(dx, dy, Coo.TMP);
			int x1 = c.x();
			int y1 = c.y();
			if (dropped(it, x1, y1))
				continue;
			boolean constructed = ConstructionData.dConstructed.is(dx, dy, 1);
			d.itemClear(dx, dy, p.instance);
			if (fitsNow(x1, y1, it) == null) {
				d.itemSet(x1, y1, it, p.instance);
				if (constructed)
					for (int y = 0; y < it.height(); y++)
						for (int x = 0; x < it.width(); x++)
							if (it.get(x, y) != null)
								ConstructionData.dConstructed.set(p.instance, x1+x, y1+y, 1);
				continue;
			}
			if (nAm < MAX) {
				nIt[nAm] = it;
				nx[nAm] = x1;
				ny[nAm] = y1;
				nAm++;
			}else {
				p.history.placeItem(it, x1, y1, -1);
			}
			if (constructed)
				p.resources.removeItem(x1, y1, it);
		}
	}

	// the dropped items were checked against the new walls already
	private boolean dropped(FurnisherItem it, int x1, int y1) {
		for (int k = 0; k < am; k++)
			if (pit[k] == it && px[k]+offX == x1 && py[k]+offY == y1)
				return true;
		return false;
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

		private final Str hint = new Str(256);

		Selector(){
			super(¤¤select, ¤¤selectDesc, null);
		}

		@Override
		public CharSequence isPlacable(int tx, int ty, AREA area, PLACER_TYPE type) {
			scan();
			if (wall(tx, ty) >= 0)
				return null;
			if (!p.instance.is(tx, ty) || SETT.ROOMS().fData.item.get(tx, ty) == null)
				return PlacableMessages.¤¤ITEM_MUST;
			return null;
		}

		@Override
		public boolean expandsTo(int fromX, int fromY, int toX, int toY) {
			int w = wall(fromX, fromY);
			if (w >= 0)
				return wall(toX, toY) == w;
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
			wAm = 0;
			for (int i = 0; i < wallSel.length; i++)
				wallSel[i] = false;
		}

		@Override
		public void place(int tx, int ty, AREA area, PLACER_TYPE type) {
			int w = wall(tx, ty);
			if (w >= 0) {
				addWall(w);
				return;
			}
			if (am >= MAX || !SETT.ROOMS().fData.isMaster.is(tx, ty))
				return;
			add(tx, ty);
		}

		@Override
		public void finishPlacing(AREA placedArea) {
			if (am + wAm > 0 && KEYS.MAIN().MOD.isPressed())
				addSameTypes();
			if (am + wAm > 0)
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
		public void placeInfo(GBox b, int oktiles, AREA a) {
			super.placeInfo(b, oktiles, a);
			b.NL();
			b.text(hint.clear().add(¤¤allOfType).insert(0, KEYS.MAIN().MOD.repr()));
			if (!withWalls) {
				b.NL();
				b.text(¤¤wallsHint);
			}
		}

		@Override
		public void hoverDesc(GBox box) {
			box.title(copy ? ¤¤copy : ¤¤move);
			box.text(hint.clear().add(¤¤selectDesc).insert(0, KEYS.MAIN().MOD.repr()).insert(0, KEYS.MAIN().MOD.repr()));
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

		// 4 so the tool shows its rotate button and listens to the rotate key, 1 when an item can't be turned.
		// Each step turns the selection
		@Override
		public int rotations() {
			return blocked[ROTATE] == null ? 4 : 1;
		}

		@Override
		public void rotSet(int r) {
			int turns = ((r - rot()) % 4 + 4) % 4;
			super.rotSet(r);
			for (int i = 0; i < turns; i++)
				transform(ROTATE);
		}

		// The tool's -/+ buttons and grow / shrink keys (Q / E, Ctrl + wheel) work on a size index. The selection
		// says it has a size below the current one if all items can shrink, and one above if all can grow, so the
		// buttons are greyed (or hidden) like for a single item. A step off sizeBase() resizes the selection
		// (sizeSet is final, so it's read back in updateRegardless). Walls alone have no sizes
		@Override
		public int sizes() {
			return sizeBase() + 1 + (am > 0 && blocked[GROW] == null ? 1 : 0);
		}

		int sizeBase() {
			return am > 0 && blocked[SHRINK] == null ? 1 : 0;
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
			if (gridAt(rx, ry) >= 0 && gridOverlap[rx+ry*(halfW*2+1)])
				return ¤¤overlap;
			if (wallAt(rx, ry) >= 0)
				return wallProblem(tx, ty);
			int k = itemAt(rx, ry);
			if (k < 0)
				return null;
			return p.placability.itemPlacable(tx, ty, rx-rx(k), ry-ry(k), pit[k], p.instance);
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
			if (size() != sizeBase()) {
				boolean bigger = size() > sizeBase();
				resize(bigger);
				sizeSet(sizeBase());
			}
			// also when the undo button put the lifted items back
			if (pending == selector || am + wAm == 0) {
				pending = null;
				am = 0;
				wAm = 0;
				VIEW.s().tools.place(selector, config);
			}
		}

		@Override
		public void renderPlaceHolder(SPRITE_RENDERER r, int mask, int x, int y, int tx, int ty, int rx, int ry, boolean isPlacable, boolean areaIsPlacable) {
			// a full tile per wall tile, the striped sprite shrinks to a dot on a 1x1 wall
			if (wallAt(rx, ry) >= 0) {
				SPRITES.cons().BIG.filled.render(r, 0, x, y);
				return;
			}
			int k = itemAt(rx, ry);
			if (k < 0)
				return;
			FurnisherItem it = pit[k];
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
			if (wAm > 0) {
				b.tab(4);
				b.add(b.text().add(wAm).s().add(¤¤walls));
			}
			b.NL();
			b.add(b.text().lablify().add(¤¤offset));
			b.tab(4);
			b.add(b.text().add(offX).add(',').s().add(offY));
			b.NL();
			b.add(b.text().add(¤¤rotate).s().add(KEYS.MAIN().ROTATE.repr()));
			b.NL();
			b.add(b.text().add(¤¤size).s().add(KEYS.MAIN().SHRINK.repr()).add(' ').add('/').add(' ').add(KEYS.MAIN().GROW.repr()));
			b.NL();
			b.text(¤¤cancel);
			if (noteUntil > VIEW.renderSecond()) {
				b.NL();
				b.error(note);
			}
		}

	};

}
