package settlement.environment;

import init.sprite.UI.UI;
import settlement.environment.SettEnvMap.Updatable;
import settlement.main.SETT;
import settlement.tilemap.terrain.Terrain.TerrainTile;
import snake2d.pathing.PathTile;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.DIR;
import snake2d.util.datatypes.RECTANGLE;
import snake2d.util.map.MAP_BOOLEAN;
import snake2d.util.sets.ArrayList;
import snake2d.util.sets.Bitmap2D;
import snake2d.util.sets.LIST;
import snake2d.util.sets.LISTE;
import snake2d.util.sprite.SPRITE;
import util.GUTIL;
import util.text.D;

public final class SettEnvShape extends SettEnvMap.Updatable{

//	public final Type pround;
//	public final Type psquare;
	public final LIST<Type> all;
	public final Type round;
	public final Type square;
	public final double radius = 10;
	
	
	public static CharSequence ¤¤name = "Shape";
	private static CharSequence ¤¤square = "Squareness";
	private static CharSequence ¤¤round = "Roundness";
	static {
		D.ts(SettEnvShape.class);
	}
	
	SettEnvShape(LISTE<Updatable> all) {
		super(all);
		round = new Type("ROUND", ¤¤round, UI.icons().l.dia) {

			@Override
			protected boolean isBase(int tx, int ty, DIR dd) {
				
				if (dd.isOrtho())
					return false;
				DIR d = dd.next(2);
				DIR t = getWallDIR(tx+d.x(), ty+d.y());
				if (t != null && !t.isOrtho())
					return true;
				d = dd.next(-2);
				t = getWallDIR(tx+d.x(), ty+d.y());
				if (t != null && !t.isOrtho())
					return true;
				return false;
				
				
			}
			
			
		};
		
		square = new Type("SQUARE", ¤¤square, UI.icons().l.square) {
			
			private final DIR[] dir = new DIR[] {DIR.N, DIR.E};
			
			@Override
			protected boolean isBase(int tx, int ty, DIR dd) {
				
				for (DIR d : dir) {
					if (test(tx, ty, d, dd) && test(tx, ty, d.perpendicular(), dd))
						return true;
				}
				
				return false;
			}
			
			private boolean test(int tx, int ty, DIR d, DIR dd) {
				for (int i = 1; i < 3; i++) {
					if (getWallDIR(tx+d.x()*i, ty+d.y()*i) != dd)
						return false;
				}
				return true;
			}
			
		};
		
		this.all = new ArrayList<Type>(round, square);
	}

	@Override
	protected void update(RECTANGLE bounds, RECTANGLE area) {
		
		for (COORDINATE c : area) {
			
			for (Type t : all)
				t.set(c, false);
		}
		
		for (COORDINATE c : bounds) {
			GUTIL.flooder().setValue2(c, -1);
		}
		
		for (int ti = 0; ti < all.size(); ti++) {
			GUTIL.flooder().init(this);
			
			for (COORDINATE c : bounds) {
				
//				if (SETT.ROOMS().map.is(c))
//					continue;
				
				DIR d = getWallDIR(c.x(), c.y());
				if (d != null) {
					if (all.get(ti).isBase(c.x(), c.y(), d) && GUTIL.flooder().getValue2(c.x(), c.y()) == -1) {
						GUTIL.flooder().pushSloppy(c, 0);
					}
				}
			}
			
			while(GUTIL.flooder().hasMore()) {
				PathTile t = GUTIL.flooder().pollSmallest();
				if (!bounds.holdsPoint(t))
					continue;
				if (t.getValue() > 10)
					continue;
				if (wall.is(t))
					continue;
				if (t.getValue2() != -1)
					continue;
				t.setValue2(ti);
				if (area.holdsPoint(t)) {
					all.get(ti).set(t, true);
					
				}
				
				for (DIR d : DIR.ALL) {
					int dx = t.x()+d.x();
					int dy = t.y()+d.y();
					if (SETT.IN_BOUNDS(dx, dy)) {
						GUTIL.flooder().pushSmaller(dx, dy, t.getValue()+d.tileDistance());
					}
					
				}
				
			}
			
			GUTIL.flooder().done();
		}
		
		

		
	}
	
	public boolean isBase(int tx, int ty) {
//		if (SETT.ROOMS().map.is(tx, ty))
//			return false;
		DIR d = getWallDIR(tx, ty);
		if (d != null)
			for (Type t : all) {
				if (t.isBase(tx, ty, d))
					return true;
			}
		return false;
	}
	
	// GOUL: getWallDIR() per tile while SettEnvMap.init() runs (0 = not read yet, 1 = null, 2+ = index in DIR.ALL).
	// The chunks it updates overlap, so it was worked out for each tile many times over.
	private byte[] goulDir;
	
	void goulCache(boolean on) {
		goulDir = on ? new byte[SETT.TAREA] : null;
	}
	
	public DIR getWallDIR(int tx, int ty) {
		if (goulDir == null || !SETT.IN_BOUNDS(tx, ty))
			return wallDIR(tx, ty);
		int i = tx + ty*SETT.TWIDTH;
		if (goulDir[i] == 0) {
			DIR d = wallDIR(tx, ty);
			int k = 0;
			while (d != null && DIR.ALL.get(k) != d)
				k++;
			goulDir[i] = (byte) (d == null ? 1 : 2 + k);
		}
		return goulDir[i] == 1 ? null : DIR.ALL.get(goulDir[i]-2);
	}
	
	private DIR wallDIR(int tx, int ty) { // GOUL: was getWallDIR
		
		if (wall.is(tx, ty))
			return null;
		
		if (!SETT.IN_BOUNDS(tx, ty))
			return null;

		DIR res = null;
		for (int i = 0; i < DIR.ORTHO.size(); i++) {
			DIR d = DIR.ORTHO.get(i);
			if (isWallMask(tx, ty, d)) {
				
				if (!isWallMask(tx, ty, d.next(2)) && !isWallMask(tx, ty, d.next(-2)))
					return d;
				
				if (isWallMask(tx, ty, d.next(-2)) && isWallMask(tx, ty, d.next(-1))) {
					return d.next(-1);
				}
				
				if (isWallMask(tx, ty, d.next(2)) && isWallMask(tx, ty, d.next(1))) {
					return d.next(1);
				}
				
				return null;
			}
				
			
		}
		
		
		
		return res;
		
	}
	
	private boolean isWallMask(int tx, int ty, DIR d) {
		
		int dx = tx+d.x();
		int dy = ty+d.y();
		if (!SETT.IN_BOUNDS(dx, dy))
			return false;
		TerrainTile t = SETT.TERRAIN().get(dx, dy);
		
		if (t.clearing().isStructure() && t.getAvailability(dx, dy) != null && t.getAvailability(dx, dy).player < 0) {
			return true;
		}
		return false;
		
	}
	
	MAP_BOOLEAN wall = new MAP_BOOLEAN() {
		
		@Override
		public boolean is(int tx, int ty) {
			TerrainTile t = SETT.TERRAIN().get(tx, ty);
			
			if (t.clearing().isStructure() && t.getAvailability(tx, ty) != null && t.getAvailability(tx, ty).player < 0) {
				return true;
			}
			
			return false;
		}

		@Override
		public boolean is(int tile) {
			int tx = tile%SETT.TWIDTH;
			int ty = tile/SETT.TWIDTH;
			return is(tx, ty);
		}
	};
	
	public static abstract class Type extends Bitmap2D {
		
		public final CharSequence name;
		public final SPRITE icon;
		public final String key;
		
		Type(String key, CharSequence name, SPRITE icon){
			super(SETT.TILE_BOUNDS, false);
			this.key = key;
			this.name = name;
			this.icon = icon;
		}
		
		protected abstract boolean isBase(int tx, int ty, DIR dd);
		
	}

	@Override
	public double getBaseValue(int tx, int ty) {
		return isBase(tx, ty) ? 1 : 0;
	}

	@Override
	protected boolean has(int tx, int ty) {
		return round.is(tx, ty) || square.is(tx, ty);
	}

	@Override
	protected void clear() {
		for (Type t : all)
			t.clear();
	}
	
}
