package settlement.environment;

import static settlement.environment.SettEnvMap.RADIUS;
import static settlement.environment.SettEnvMap.maxRadiusI;
import static settlement.main.SETT.TWIDTH;

import settlement.environment.SettEnvMap.SettEnv;
import settlement.main.SETT;
import settlement.misc.util.TileRayTracer;
import settlement.misc.util.TileRayTracer.Ray;
import snake2d.pathing.PathTile;
import snake2d.pathing.PathUtilOnline.Flooder;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.DIR;
import snake2d.util.datatypes.RECTANGLE;
import snake2d.util.datatypes.Rec;
import snake2d.util.misc.CLAMP;
import snake2d.util.misc.IntChecker;
import snake2d.util.sets.ArrayList;
import snake2d.util.sets.LIST;
import util.GUTIL;
import view.main.VIEW;

abstract class EUpdater {

	static final TileRayTracer traces = new TileRayTracer(RADIUS);
	private static final IntChecker rCheck = new IntChecker(traces.rays().length);
	private static final ArrayList<Ray> rays = new ArrayList<Ray>(traces.rays().length);
	private static final int MAXR = SettEnvMap.RADIUS-1;

	private static final Rec eArea = new Rec();
	private static final Rec eArea2 = new Rec();
	private static int aI = -1;
	
	
	public abstract void update(SettEnv s, RECTANGLE bounds, RECTANGLE area);
	public abstract void addExtraView(RECTANGLE area, Flooder f, SettEnv thing, double value, double radius, int tx, int ty, int approvedDirs);
	public abstract double getExtraValue(SettEnv s, double g, int tx, int ty);
	
	public static EUpdater tracer = new EUpdater() {
		
		
		@Override
		public void update(SettEnv s, RECTANGLE bounds, RECTANGLE area) {
			for (COORDINATE c : area) {
				if (SETT.IN_BOUNDS(c)) {
					s.map.set(c.x() + c.y() * SETT.TWIDTH, 0);
					GUTIL.flooder().setValue2(c, 0);
				}
				
			}
			
			for (COORDINATE c : bounds) {
				if (tracetest(area, s, c)) {
					trace(c.x(), c.y(), s, area);
				}
			}
			
			for (COORDINATE c : area) {
				int v = (int)Math.ceil(GUTIL.flooder().getValue2(c.x(), c.y()));
				v = CLAMP.i(v, 0, s.max);
				
				s.map.set(c.x()+c.y()*TWIDTH, v);
			}
			
		}
		

		@Override
		public void addExtraView(RECTANGLE area, Flooder f, SettEnv thing, double value, double radius, int tx, int ty, int approvedDirs) {
			

			if (aI != VIEW.RI()) {
				aI = VIEW.RI();
				for (COORDINATE c : area) {
					if (SETT.IN_BOUNDS(c))
						GUTIL.flooder().setValue2(c, 0);
				}
				eArea.set(area);
			}else {
				eArea2.set(eArea);
				eArea.unify(area);
				for (COORDINATE c : eArea) {
					if (SETT.IN_BOUNDS(c) && !eArea2.holdsPoint(c))
						GUTIL.flooder().setValue2(c, 0);
				}
			}
			if (SETT.IN_BOUNDS(tx, ty)) {
				trace(tx, ty, thing, value, radius, area, approvedDirs);
			}
			
//			for (COORDINATE c : area) {
//				if (SETT.IN_BOUNDS(c))
//					GUTIL.flooder().setValue2(c, 0);
//			}		
		}
		
		
//		@Override
//		public void addExtraView(RECTANGLE area, Flooder f, SettEnv thing, double value, double radius, int tx, int ty, int w, int h) {
//			
//			if (aI != VIEW.RI()) {
//				aI = VIEW.RI();
//				for (COORDINATE c : area) {
//					if (SETT.IN_BOUNDS(c))
//						GUTIL.flooder().setValue2(c, 0);
//				}
//				eArea.set(area);
//			}else {
//				eArea2.set(eArea);
//				eArea.unify(area);
//				for (COORDINATE c : area) {
//					if (SETT.IN_BOUNDS(c) && !eArea2.holdsPoint(c))
//						GUTIL.flooder().setValue2(c, 0);
//				}
//			}
//			if (SETT.IN_BOUNDS(tx, ty)) {
//				trace(tx, ty, thing, value, radius, area);
//			}
//			
//			for (COORDINATE c : area) {
//				if (SETT.IN_BOUNDS(c))
//					GUTIL.flooder().setValue2(c, 0);
//			}
//			for (int y = 0; y < h; y++) {
//				for (int x = 0; x < w; x++) {
//					
//				}
//			}
//			
//			for (COORDINATE c : area) {
//				int iv = CLAMP.i((int)Math.ceil(GUTIL.flooder().getValue2(c.x(), c.y())), 0, thing.max);
//				double v = iv*thing.maxI;
//				GUTIL.flooder().setValue2(c, (int)(v*thing.max));
//			}
//			
//			eArea.set(area);
//			
////			for (COORDINATE c : area) {
////				RES.flooder().close(c.x(), c.y(), RES.flooder().getValue2(c.x(), c.y())/thing.max);
////			}
////			return;
//			
//		}

		@Override
		public double getExtraValue(SettEnv s, double g, int tx, int ty) {
			if (aI != VIEW.RI())
				return g;
			if (!eArea.holdsPoint(tx, ty))
				return g;
			

			double d = (Math.ceil((g*s.max) + Math.ceil(GUTIL.flooder().getValue2(tx, ty))))*s.maxI;
			return d;
		}
	};
	

	
	private static boolean tracetest(RECTANGLE area, SettEnv s, COORDINATE c) {
		int ra = (int) (s.getRadius(c.x(), c.y())*MAXR);
		if (ra == 0)
			return false;
		if (s.getBaseValue(c.x(), c.y()) == 0)
			return false;
		if (area.holdsPoint(c))
			return true;
		if (Math.abs(area.cX()-c.x())-ra > RADIUS/2)
			return false;
		if (Math.abs(area.cY()-c.y())-ra > RADIUS/2)
			return false;
		return true;
	}
	
	private static void trace(int sourceX, int sourceY, SettEnv s, RECTANGLE area) {
		
		trace(sourceX, sourceY, s, s.getBaseValue(sourceX, sourceY), s.getRadius(sourceX, sourceY), area, s.dirs(sourceX, sourceY));

	}
	
	private static void trace(int sourceX, int sourceY, SettEnv s, double value, double radius, RECTANGLE area, int approvedDir) {
		
		radius = (int)(radius*MAXR);
		
		traces.checkInit();
		
		for (Ray r : rays(sourceX, sourceY, area)){
			
			if ((approvedDir & DIR.get(r.get(r.size()-1)).bit) == 0) {
				
				continue;
			}
			
			double cost = 0;
			for (int i = 0; i < r.size(); i++) {
				COORDINATE d = r.get(i);
				int dx = d.x()+sourceX;
				int dy = d.y()+sourceY;
				if (!SETT.IN_BOUNDS(dx, dy))
					break;
				if (r.radius(i) + cost >= radius)
					break;
				
				if (area.holdsPoint(dx, dy)) {
					if (traces.check(d)) {
						double rv = (radius-r.radius(i)-cost)/radius;
						double vv = value*rv;
						double v = (GUTIL.flooder().getValue2(dx, dy) + vv*s.max);
						GUTIL.flooder().setValue2(dx, dy, v);
					}
					//

				}
				cost+= s.getCost(dx, dy)-1;
				
			}
			
		}

	}
	
	static LIST<Ray> rays(int sourceX, int sourceY, RECTANGLE area) {
		rays.clear();
		
		if (area.touches(sourceX, sourceY)) {
			rays.add(traces.rays());
			return rays;
		}
		
		rCheck.init();
		
		int y1 = area.y1()-sourceY;
		int y2 = area.y2()-sourceY;
		for (int x = area.x1(); x < area.x2(); x++) {
			int dx = x-sourceX;
			for (Ray r : traces.rays(dx, y1))
				if (!rCheck.isSetAndSet(r.index))
					rays.add(r);
			for (Ray r : traces.rays(dx, y2))
				if (!rCheck.isSetAndSet(r.index))
					rays.add(r);
		}
		
		int x1 = area.x1()-sourceX;
		int x2 = area.x2()-sourceX;
		
		for (int y = area.y1(); y <= area.y2(); y++) {
			int dy = y-sourceY;
			for (Ray r : traces.rays(x1, dy))
				if (!rCheck.isSetAndSet(r.index))
					rays.add(r);
			for (Ray r : traces.rays(x2, dy))
				if (!rCheck.isSetAndSet(r.index))
					rays.add(r);
		}
		return rays;
	}
	
	
	public static EUpdater flooder = new EUpdater() {
		
		@Override
		public void update(SettEnv s, RECTANGLE bounds, RECTANGLE area) {
			for (COORDINATE c : area) {
				s.map.set(c.x() + c.y() * SETT.TWIDTH, 0);
			}
			GUTIL.flooder().init(this);
			flood2(GUTIL.pathTools().getFlooder(), bounds, area, s);
			GUTIL.flooder().done();
			
		}
		
		// GOUL: the open tiles of flood2(), in a binary max-heap over the bounds, instead of the shared Flooder's tree
		// (a red-black tree of tile objects). All costs are positive, so a tile's value is final when it comes out, and
		// the values are the same whichever order equal tiles come out in. Same float values and checks as Flooder.
		// Neighbours that are already closed are skipped (pushGreater did nothing for them), and each tile's getCost(),
		// which only reads the map, is read once per flood instead of once per neighbour.
		private float[] hVal = new float[0];
		private byte[] hState = new byte[0]; // 0 not pushed, 1 open, 2 closed
		private int[] hHeap = new int[0];
		private int[] hPos = new int[0];
		private double[] hCost = new double[0]; // getCost() of the tile, read once per flood
		private boolean[] hCostRead = new boolean[0];
		private int hSize;
		private final int[] hDx = new int[8], hDy = new int[8]; // DIR.ALL, in the same order
		private final double[] hDist = new double[8];
		{
			for (int k = 0; k < 8; k++) {
				hDx[k] = DIR.ALL.get(k).x();
				hDy[k] = DIR.ALL.get(k).y();
				hDist[k] = DIR.ALL.get(k).tileDistance();
			}
		}
		
		private void hInit(int n) {
			if (hVal.length < n) {
				hVal = new float[n];
				hState = new byte[n];
				hHeap = new int[n];
				hPos = new int[n];
				hCost = new double[n];
				hCostRead = new boolean[n];
			}
			for (int i = 0; i < n; i++) {
				hState[i] = 0;
				hCostRead[i] = false;
			}
			hSize = 0;
		}
		
		// Flooder.pushGreater
		private void hPush(int i, double value) {
			if (hState[i] != 0) {
				if (hVal[i] >= value)
					return;
				if (hState[i] == 2)
					return;
				hVal[i] = (float) value;
				hUp(hPos[i]);
				return;
			}
			hState[i] = 1;
			hVal[i] = (float) value;
			hHeap[hSize] = i;
			hPos[i] = hSize;
			hUp(hSize++);
		}
		
		// Flooder.pollGreatest
		private int hPoll() {
			int top = hHeap[0];
			hSize--;
			if (hSize > 0) {
				hHeap[0] = hHeap[hSize];
				hPos[hHeap[0]] = 0;
				hDown(0);
			}
			hState[top] = 2;
			return top;
		}
		
		private void hUp(int k) {
			int i = hHeap[k];
			while (k > 0) {
				int p = (k-1) >> 1;
				if (hVal[hHeap[p]] >= hVal[i])
					break;
				hHeap[k] = hHeap[p];
				hPos[hHeap[k]] = k;
				k = p;
			}
			hHeap[k] = i;
			hPos[i] = k;
		}
		
		private void hDown(int k) {
			int i = hHeap[k];
			while (true) {
				int c = 2*k+1;
				if (c >= hSize)
					break;
				if (c+1 < hSize && hVal[hHeap[c+1]] > hVal[hHeap[c]])
					c++;
				if (hVal[hHeap[c]] <= hVal[i])
					break;
				hHeap[k] = hHeap[c];
				hPos[hHeap[k]] = k;
				k = c;
			}
			hHeap[k] = i;
			hPos[i] = k;
		}
		
		private void flood2(Flooder f, RECTANGLE bounds, RECTANGLE area, SettEnv thing) {
			
			double r = 1.0/thing.radius();
			int bx = bounds.x1();
			int by = bounds.y1();
			int bx2 = bounds.x2();
			int by2 = bounds.y2();
			int w = bx2-bx;
			hInit(w*(by2-by));
			
			for (COORDINATE c : bounds) {
				double b = thing.getBaseValue(c.x(), c.y())*RADIUS;
				if (b > 0) {
					hPush(c.x()-bx + (c.y()-by)*w, b);
				}
			}

			while (hSize > 0) {
				int i = hPoll();
				double value = hVal[i];
				int x = bx + i%w;
				int y = by + i/w;

				for (int k = 0; k < 8; k++) {
					int tx = x + hDx[k];
					int ty = y + hDy[k];
					if (tx >= bx && tx < bx2 && ty >= by && ty < by2) { // bounds.holdsPoint
						int ni = tx-bx + (ty-by)*w;
						if (hState[ni] == 2) // a closed tile is never changed, so its cost isn't needed
							continue;
						if (!hCostRead[ni]) {
							hCost[ni] = thing.getCost(tx, ty);
							hCostRead[ni] = true;
						}
						double v = value - hDist[k] * hCost[ni]*r;
						if (v > 0) {
							hPush(ni, v);
						}
					}
				}
			}

			for (COORDINATE c : area) {
				int i = c.x()-bx + (c.y()-by)*w;
				if (hState[i] != 0) {
					int v = (int) Math.ceil(thing.max * hVal[i]*maxRadiusI);
					v = CLAMP.i(v, 0, thing.max);
					thing.map.set(c.x() + c.y() * SETT.TWIDTH, v);
				}
			}

		}
		
		@Override
		public void addExtraView(RECTANGLE area, Flooder f, SettEnv thing, double value, double radius, int tx, int ty, int approvedDirs) {
			double b = value;
			double dr = maxRadiusI / (radius) * b;

			if (b > 0) {
				GUTIL.flooder().pushSloppy(tx, ty, b);
				GUTIL.flooder().setValue2(tx, ty, dr);
				
				

			}
			
			while (f.hasMore()) {
				PathTile t = f.pollGreatest();
				dr = t.getValue2();
				value = t.getValue();

				for (DIR d : DIR.ALL) {
					tx = t.x() + d.x();
					ty = t.y() + d.y();
					double v = value - dr * d.tileDistance() * thing.getCost(tx, ty);
					if (v > 0) {
						if (f.pushGreater(tx, ty, v) != null) {
							f.setValue2(tx, ty, dr);
						}
					}
				}
			}

			
		}

		@Override
		public double getExtraValue(SettEnv s, double g, int tx, int ty) {
			g = Math.max(g, GUTIL.flooder().getValue(tx, ty));
			return g;
		}
	};
	
//	public static EUpdater water = new EUpdater() {
//		
//		
//		@Override
//		public void update(SettEnv s, RECTANGLE bounds, RECTANGLE area) {
//			for (COORDINATE c : area) {
//				if (SETT.IN_BOUNDS(c)) {
//					s.map.set(c.x() + c.y() * SETT.TWIDTH, 0);
//					GUTIL.flooder().setValue2(c, 0);
//				}
//				
//			}
//			
//			GUTIL.flooder().init(this);
//			flood2(GUTIL.flooder(), bounds, area, s);
//			GUTIL.flooder().done();
//			
//			for (COORDINATE c : bounds) {
//				if (tracetest(area, s, c) && !isNatural(c, s)) {
//					trace(c.x(), c.y(), s, area);
//				}
//			}
//			
//			for (COORDINATE c : area) {
//				if (SETT.IN_BOUNDS(c))
//					s.map.set(c.x()+c.y()*TWIDTH, CLAMP.i((int)Math.ceil(GUTIL.flooder().getValue2(c.x(), c.y())), 0, s.max));
//			}
//			
//		}
//		
//		private boolean isNatural(COORDINATE c, SettEnv thing) {
//			if (thing == SETT.ENV().map.WATER_SWEET && SETT.TERRAIN().WATER.groundWater.is(c))
//				return true;
//			if (thing == SETT.ENV().map.WATER_SALT && SETT.TERRAIN().WATER.groundWaterSalt.is(c))
//				return true;
//			return false;
//		}
//		
//		private void flood2(Flooder f, RECTANGLE bounds, RECTANGLE area, SettEnv thing) {
//
//			
//			
//			for (COORDINATE c : bounds) {
//				double b = thing.getBaseValue(c.x(), c.y());
//				if (b > 0 && isNatural(c, thing)) {
//					f.pushGreater(c, RADIUS);
//				}
//			}
//			double r = 1.0/thing.radius();
//			
//			while (f.hasMore()) {
//				PathTile t = f.pollGreatest();
//				double value = t.getValue();
//
//				for (DIR d : DIR.ALL) {
//					int tx = t.x() + d.x();
//					int ty = t.y() + d.y();
//					if (bounds.holdsPoint(tx, ty)) {
//						double v = value - d.tileDistance() * thing.getCost(tx, ty)*r;
//						if (v > 0) {
//							f.pushGreater(tx, ty, v);
//						}
//					}
//				}
//			
//			}
//
//			for (COORDINATE c : area) {
//				if (f.hasBeenPushed(c.x(), c.y())) {
//					GUTIL.flooder().setValue2(c, thing.max*f.getValue(c)*maxRadiusI);
//				}
//			}
//
//		}
//
//		
//		@Override
//		public void addExtraView(RECTANGLE area, Flooder f, SettEnv thing, double value, double radius, int tx, int ty, int w, int h) {
//			for (COORDINATE c : area) {
//				if (SETT.IN_BOUNDS(c))
//					GUTIL.flooder().setValue2(c, 0);
//			}
//			for (int y = 0; y < h; y++) {
//				for (int x = 0; x < w; x++) {
//					if (SETT.IN_BOUNDS(tx + x, ty + y)) {
//						trace(tx + x, ty + y, thing, value, radius, area, -1);
//					}
//				}
//			}
//			
//			for (COORDINATE c : area) {
//				int iv = CLAMP.i((int)Math.ceil(GUTIL.flooder().getValue2(c.x(), c.y())), 0, thing.max);
//				double v = iv*thing.maxI;
//				GUTIL.flooder().setValue2(c, (int)(v*thing.max));
//			}
//			
//			eArea.set(area);
//			
////			for (COORDINATE c : area) {
////				RES.flooder().close(c.x(), c.y(), RES.flooder().getValue2(c.x(), c.y())/thing.max);
////			}
////			return;
//			
//		}
//
//		@Override
//		public double getExtraValue(SettEnv s, double g, int tx, int ty) {
//			if (!eArea.holdsPoint(tx, ty))
//				return g;
//			double d = Math.ceil(GUTIL.flooder().getValue2(tx, ty));
//			d /= s.max;
//			g += d;
//			return g;
//		}
//	};
	
}
