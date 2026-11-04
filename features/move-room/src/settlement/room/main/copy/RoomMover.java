package settlement.room.main.copy;

import init.sprite.SPRITES;
import settlement.job.Job;
import settlement.job.JobBuildStructure;
import settlement.main.SETT;
import settlement.room.main.ROOMA;
import settlement.room.main.furnisher.FurnisherItem;
import settlement.room.main.placement.UtilWallPlacability;
import settlement.tilemap.terrain.TBuilding;
import snake2d.util.datatypes.DIR;
import snake2d.util.datatypes.RECTANGLE;
import snake2d.util.datatypes.Rec;
import snake2d.util.sprite.SPRITE;
import util.text.D;

/**
 * GOUL: Move Room. The Room Copier in move mode: before the copy is placed, the room is read into this and
 * dismantled, so the copy may overlap where it was. Afterwards its old walls and roof that no room or roofed
 * tile needs any more are queued for dismantling.
 */
final class RoomMover {

	private static CharSequence ¤¤name = "Move Room";
	private static CharSequence ¤¤desc = "Moves a room. It's dismantled, recovering some of the resources used to build it, and planned again where you place it, with the same settings. Its old walls and roof are dismantled too, unless something else still needs them.";

	static {
		D.ts(RoomMover.class);
	}

	private final Rec body = new Rec();
	private boolean[] tiles = new boolean[0];
	private FurnisherItem[] items = new FurnisherItem[0];
	private int mx, my, area;
	private final SPRITE icon = SPRITES.icons().l.copyRoom.twin(SPRITES.icons().m.arrow_right, DIR.NE, 1);

	CharSequence name() {
		return ¤¤name;
	}

	CharSequence desc() {
		return ¤¤desc;
	}

	SPRITE icon() {
		return icon;
	}

	/**
	 * Reads the room's tiles and items, then dismantles it.
	 * @return the room as it was, to copy from
	 */
	ROOMA lift(ROOMA room) {
		body.moveX1Y1(room.body().x1(), room.body().y1());
		body.setDim(room.body().width(), room.body().height());
		mx = room.mX();
		my = room.mY();
		area = room.area();
		int size = body.width()*body.height();
		if (tiles.length < size) {
			tiles = new boolean[size];
			items = new FurnisherItem[size];
		}
		for (int y = 0; y < body.height(); y++) {
			for (int x = 0; x < body.width(); x++) {
				int tx = body.x1()+x;
				int ty = body.y1()+y;
				int i = x+y*body.width();
				tiles[i] = room.is(tx, ty);
				items[i] = tiles[i] && SETT.ROOMS().fData.isMaster.is(tx, ty) ? SETT.ROOMS().fData.item.get(tx, ty) : null;
			}
		}
		SETT.ROOMS().DELETE.place(mx, my, null, null);
		return lifted;
	}

	/**
	 * @return the item whose master tile this was, or null
	 */
	FurnisherItem item(int tx, int ty) {
		if (!lifted.is(tx, ty))
			return null;
		return items[tx-body.x1()+(ty-body.y1())*body.width()];
	}

	/**
	 * Queues Dismantle Structure on the building walls and roof on and next to the old room, unless they're a room,
	 * a job (the new room's walls are jobs by now), or still needed: next to a room, or next to a roofed tile
	 * further out (a corridor) that they enclose. Walls and doorways that were only planned (a construction site
	 * nobody has walled yet) are cancelled instead, by the same rule.
	 */
	void dismantle() {
		Job job = SETT.JOBS().clearss.structure;
		for (int ty = body.y1()-1; ty <= body.y2(); ty++) {
			for (int tx = body.x1()-1; tx <= body.x2(); tx++) {
				if (!SETT.IN_BOUNDS(tx, ty) || !onOrNextToOld(tx, ty) || SETT.ROOMS().map.is(tx, ty))
					continue;
				if (planned(tx, ty, false)) {
					if (!needed(tx, ty))
						SETT.JOBS().clearer.set(tx, ty);
					continue;
				}
				if (SETT.JOBS().getter.get(tx, ty) != null || !(SETT.TERRAIN().get(tx, ty) instanceof TBuilding.BuildingComponent))
					continue;
				if (needed(tx, ty))
					continue;
				if (job.placer().isPlacable(tx, ty, null, null) == null)
					job.placer().place(tx, ty, null, null);
			}
		}
	}
	
	private boolean onOrNextToOld(int tx, int ty) {
		for (int i = 0; i < DIR.ALLC.size(); i++) {
			DIR d = DIR.ALLC.get(i);
			if (lifted.is(tx+d.x(), ty+d.y()))
				return true;
		}
		return false;
	}
	
	private boolean needed(int tx, int ty) {
		for (int i = 0; i < DIR.ALL.size(); i++) {
			DIR d = DIR.ALL.get(i);
			int x = tx+d.x();
			int y = ty+d.y();
			if (!SETT.IN_BOUNDS(x, y))
				continue;
			if (SETT.ROOMS().map.is(x, y))
				return true;
			if ((UtilWallPlacability.openingIsReal.is(x, y) || planned(x, y, true)) && !onOrNextToOld(x, y))
				return true;
		}
		return false;
	}

	/**
	 * @return whether a wall (unless ceilingOnly) or a ceiling (a doorway, or a planned corridor) is planned here,
	 * as a building job not done yet
	 */
	private static boolean planned(int tx, int ty, boolean ceilingOnly) {
		Job j = SETT.JOBS().getter.get(tx, ty);
		if (j == null)
			return false;
		for (int i = 0; i < SETT.JOBS().build_structure.size(); i++) {
			JobBuildStructure s = SETT.JOBS().build_structure.get(i);
			if (j == s.ceiling || (!ceilingOnly && j == s.wall))
				return true;
		}
		return false;
	}
	
	private final ROOMA lifted = new ROOMA() {

		@Override
		public RECTANGLE body() {
			return body;
		}

		@Override
		public boolean is(int tile) {
			return is(tile%SETT.TWIDTH, tile/SETT.TWIDTH);
		}

		@Override
		public boolean is(int tx, int ty) {
			return body.holdsPoint(tx, ty) && tiles[tx-body.x1()+(ty-body.y1())*body.width()];
		}

		@Override
		public int area() {
			return area;
		}

		@Override
		public int index() {
			return 0;
		}

		@Override
		public int mX() {
			return mx;
		}

		@Override
		public int mY() {
			return my;
		}
	};

}
