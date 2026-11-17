package settlement.environment;

import static settlement.main.SETT.IN_BOUNDS;

import java.io.IOException;
import java.util.Arrays;

import game.GAME;
import game.boosting.BoostSpecs;
import init.paths.PATH;
import init.paths.PATHS;
import init.race.bio.Opinion;
import init.sprite.SPRITES;
import init.sprite.UI.UI;
import settlement.main.SETT;
import settlement.path.AVAILABILITY;
import settlement.path.AvailabilityListener;
import settlement.room.main.Room;
import settlement.stats.STATS;
import settlement.stats.standing.StatStanding.StandingDef;
import settlement.stats.stat.STAT;
import settlement.tilemap.terrain.Terrain.TerrainTile;
import snake2d.Errors;
import snake2d.pathing.PathUtilOnline.Flooder;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.Coo;
import snake2d.util.datatypes.RECTANGLE;
import snake2d.util.datatypes.Rec;
import snake2d.util.file.Alloc;
import snake2d.util.file.Json;
import snake2d.util.map.MAP_DOUBLE;
import snake2d.util.misc.CLAMP;
import snake2d.util.sets.ArrayList;
import snake2d.util.sets.ArrayListGrower;
import snake2d.util.sets.Bitmap1D;
import snake2d.util.sets.Bitsmap1D;
import snake2d.util.sets.KeyMap;
import snake2d.util.sets.LIST;
import snake2d.util.sets.LISTE;
import snake2d.util.sets.LinkedList;
import snake2d.util.sets.QueueInteger;
import snake2d.util.sprite.SPRITE;
import util.GUTIL;
import util.info.INFO;
import util.keymap.MAPPED;
import util.keymap.RMAP;

public final class SettEnvMap {

	public final SettEnv NOISE;
	public final SettEnv LIGHT;
	public final SettEnv SPACE;
	public final SettEnv WATER_SWEET;
	public final SettEnv WATER_SALT;
	public final SettEnv URBAN;
	public final SettEnv GUARD;
	public final SettEnv PUNISHMENT;
	
	public final SettEnvShape SHAPE;
	public final SettEnvMonument MONUMENT;
//	public final SettEnvRoundness ROUNDNESS;
	
	public final RMAP<SettEnv> rmap;
	private final ArrayList<SettEnv> all;
	private final ArrayListGrower<Updatable> ups = new ArrayListGrower<>();

	private final Chunks chunks = new Chunks();
	private final Updater updater = new Updater();

	public static final int RADIUS = 16;
	static final double maxRadiusI = 1.0 / RADIUS;

	public SettEnvMap() throws IOException {
		new AvailabilityListener() {

			@Override
			protected void changed(int tx, int ty, AVAILABILITY a, AVAILABILITY old, boolean playerChange) {
				if (a.player * old.player < 0)
					chunks.change(tx, ty);
			}
		};
		
		PATH jp = PATHS.INIT().getFolder("settlement").getFolder("environment");
		PATH tp = PATHS.TEXT().getFolder("settlement").getFolder("environment");

		String[] keys = jp.getFiles();

		LinkedList<SettEnv> all = new LinkedList<>();
		final KeyMap<SettEnv> kmap = new KeyMap<>();
		
		NOISE = new SettEnv(ups, all, "_NOISE", jp, tp, 2, EUpdater.flooder) {

			@Override
			public double getCost(int toX, int toY) {
				if (SETT.LIGHTS().los().get(toX, toY).blocksEnv(toX, toY))
					return 8;
				if (SETT.TERRAIN().get(toX, toY).roofIs())
					return 3;
				return 1;
			}

		};
		kmap.put(NOISE.key, NOISE);
		LIGHT = new SettEnv(ups, all, "_LIGHT", jp, tp, 2, EUpdater.tracer) {

			@Override
			public double getCost(int toX, int toY) {
				return SETT.LIGHTS().los().get(toX, toY).blocksEnv(toX, toY) ? RADIUS : 1;
			}
		};
		kmap.put(LIGHT.key, LIGHT);
		SPACE = new SettEnv(ups, all, "_SPACE", jp, tp, 4, EUpdater.flooder) {

			@Override
			public double getCost(int toX, int toY) {
				return 1;
			}

			@Override
			public double get(int tile) {
				return 1.0 - super.get(tile);
			}

			@Override
			public double getBaseValue(int tx, int ty) {
				return SETT.PATH().availability.get(tx, ty).tileCollide ? 1 : 0;
			}

			@Override
			public double radius() {
				return 0.5;
			}
			
			@Override
			double getRadius(int tx, int ty) {
				return 0.5;
			}
		};
		kmap.put(SPACE.key, SPACE);
		
		URBAN = new SettEnv(ups, all, "_URBANISATION", jp, tp, 1, EUpdater.flooder) {
			@Override
			public double radius() {
				return 0.5;
			}
			
			@Override
			public double getBaseValue(int tx, int ty) {
				if (SETT.ROOMS().map.is(tx, ty))
					return 1.0;
				
				TerrainTile t = SETT.TERRAIN().get(tx, ty);
				
				if (t.clearing().isStructure()) {
					return 1.0;
				}
				
				if (SETT.FLOOR().getter.get(tx, ty) != null)
					return 1.0;
				return 0;
			}
			
		};
		
		GUARD = new SettEnv(ups, all, "_GUARD", jp, tp, 4, EUpdater.tracer) {

			@Override
			public double getCost(int toX, int toY) {
				return 1;
			}
			
		};
		kmap.put(GUARD.key, GUARD);
		
		PUNISHMENT = new SettEnv(ups, all, "_PUNISHMENT", jp, tp, 2, EUpdater.tracer) {

			@Override
			public double getCost(int toX, int toY) {
				return 1;
			}
			
		};
		kmap.put(PUNISHMENT.key, PUNISHMENT);
		
		WATER_SWEET = new SettEnv(ups, all, "_WATER_SWEET", jp, tp, 4, EUpdater.flooder) {

			@Override
			public double getCost(int toX, int toY) {
				return 1;
			}

			@Override
			public double getBaseValue(int tx, int ty) {
				if (SETT.TERRAIN().WATER.groundWater.is(tx, ty))
					return 1;
				return super.getBaseValue(tx, ty);
			}
			
			@Override
			public double radius() {
				return 1.0;
			}
			
			@Override
			double getRadius(int tx, int ty) {
				return 1.0;
			}
			

		};
		kmap.put(WATER_SWEET.key, WATER_SWEET);
		
		WATER_SALT = new SettEnv(ups, all, "_WATER_SALT", jp, tp, 1, EUpdater.flooder) {

			@Override
			public double getCost(int toX, int toY) {
				return 1;
			}

			@Override
			public double getBaseValue(int tx, int ty) {
				if (SETT.TERRAIN().WATER.groundWaterSalt.is(tx, ty))
					return 1.0;
				return super.getBaseValue(tx, ty);
			}

			@Override
			public double radius() {
				return 1.0;
			}
		};
		kmap.put(WATER_SALT.key, WATER_SALT);

		if (keys.length > 32 - all.size())
			throw new Errors.DataError("Too many environments declared, max is " + (32 - all.size()), jp.get());
		
		this.all = new ArrayList<SettEnvMap.SettEnv>(all);

		kmap.expand();

		rmap = new RMAP<SettEnvMap.SettEnv>("ENVIRONMENT", all);

		SHAPE = new SettEnvShape(ups);
		
		MONUMENT = new SettEnvMonument(ups);
		
	}

	protected void init() {

		
		SHAPE.goulCache(true); // GOUL: walls don't change during init(), the shape pass reads each tile's wall direction once
		for (Updatable s : ups) {
			s.clear();
		}

		chunks.clear();

		for (int ty = 0; ty < SETT.THEIGHT; ty++) {
			for (int tx = 0; tx < SETT.TWIDTH; tx++) {
				for (Updatable t : ups) {
					if (t.getBaseValue(tx, ty) > 0) {
						chunks.change(tx, ty, t.bit);
					}
				}
			}
		}
		while (chunks.has()) {
			int m = chunks.nextMask();
			COORDINATE c = chunks.next();
			updater.update(c.x(), c.y(), m);
			
		}
		SHAPE.goulCache(false); // GOUL

	}
	
	public void initWater() {
		WATER_SWEET.map.clear();
		chunks.clear();

		for (int ty = 0; ty < SETT.THEIGHT; ty++) {
			for (int tx = 0; tx < SETT.TWIDTH; tx++) {
				if (WATER_SWEET.getBaseValue(tx, ty) > 0) {
					chunks.change(tx, ty, WATER_SWEET.bit);
				}
			}
		}
		while (chunks.has()) {
			int m = chunks.nextMask();
			COORDINATE c = chunks.next();
			updater.update(c.x(), c.y(), m);
		}
	}

	public void setChanged(int tx, int ty) {
		chunks.change(tx, ty);
	}
	
	void setChanged(int tx, int ty, Updatable e) {
		chunks.change(tx, ty, e.bit);
	}

	public void setChanged(int tx, int ty, SettEnv e) {
		chunks.change(tx, ty, e.bit);
	}
	
	
	
	protected void update(double ds) {
		updater.update(ds);
	}

	public LIST<SettEnv> all() {
		return all;
	}


	public static class SettEnvValue {

		private final static SettEnvValue self = new SettEnvValue();

		private SettEnvValue() {

		};

		static SettEnvValue init() {
			self.approvedDirs = -1;
			return self;
		}
		
		public double radius;
		public double value;
		public int approvedDirs;
	}

	static abstract class Updatable {
		
		final int bit;
		
		Updatable(LISTE<Updatable> all){
			bit = 1 << all.size();
			all.add(this);
		}
		
		public abstract double getBaseValue(int tx, int ty);
		protected abstract void update(RECTANGLE bounds, RECTANGLE area);
		protected abstract boolean has(int tx, int ty);
		protected abstract void clear();
		
	}
	
	public static class SettEnv extends Updatable implements MAP_DOUBLE, MAPPED {

		private final int index;
		private int extraI = -1;
		public final String key;
		public final double declineSpeed;
		public final BoostSpecs bonuses;
		public final StandingDef standing;
		final Bitsmap1D map;
		final int max;
		public final double maxI;
		private final EUpdater uper;
		public final SPRITE icon;
		public final Opinion op;
		public final INFO info;
		
		SettEnv(LISTE<Updatable> uall, LISTE<SettEnv> all, String key, PATH pj, PATH tj, int bits, EUpdater uper) throws IOException {
			super(uall);
			info = new INFO(new Json(tj.gets(key)));
			this.key = key;
			index = all.add(this);
			Json j = new Json(pj.gets(key));
			icon = SPRITES.icons().get(j);
			declineSpeed = j.d("DECLINE_VALUE", 0, 1);
			bonuses = new BoostSpecs(info.name, UI.icons().s.eye, false);
			bonuses.read(j, null);
			standing = new StandingDef(j);
			map = new Bitsmap1D(0, bits, SETT.TAREA);
			max = (1 << bits) - 1;
			maxI = 1.0 / max;
			this.uper = uper;
			op = new Opinion();
			op.read(new Json(tj.gets(key)));
		}

		@Override
		public double get(int tile) {
			double v = map.get(tile) * maxI;
			// Add floor
			if (SETT.FLOOR().getter.get(tile) != null && !SETT.ROOMS().map.is(tile))
				v += SETT.FLOOR().getter.get(tile).envValue(this, tile);
			return CLAMP.d(v, 0, 1);
		}

		public double getCost(int toX, int toY) {
			return SETT.LIGHTS().los().get(toX, toY).blocksEnv(toX, toY) ? RADIUS : 1;
		}

		double getRadius(int tx, int ty) {
			if (SETT.ROOMS().construction.isser.is(tx, ty) || SETT.ROOMS().placement.embryo.is(tx, ty))
				return 0;
			Room r = SETT.ROOMS().map.get(tx, ty);
			if (r != null && r.constructor() != null && r.constructor().envValue(this, SettEnvValue.init(), tx, ty)) {
				return SettEnvValue.self.radius;
			}
			return 0;
		}

		@Override
		public double getBaseValue(int tx, int ty) {
			if (SETT.ROOMS().construction.isser.is(tx, ty) || SETT.ROOMS().placement.embryo.is(tx, ty))
				return 0;
			
			Room r = SETT.ROOMS().map.get(tx, ty);

			if (r != null && !SETT.ROOMS().construction.isser.is(tx, ty) && r.constructor() != null
					&& r.constructor().envValue(this, SettEnvValue.init(), tx, ty)) {
				return SettEnvValue.self.value;
			}
			return 0;
		}

		public int dirs(int tx, int ty) {
			Room r = SETT.ROOMS().map.get(tx, ty);
			if (r != null && r.constructor() != null && r.constructor().envValue(this, SettEnvValue.init(), tx, ty)) {
				return SettEnvValue.self.approvedDirs;
			}
			return 0;
		}
		
		@Override
		public double get(int tx, int ty) {
			if (!SETT.IN_BOUNDS(tx, ty))
				return 0;
			return get(tx + ty * SETT.TWIDTH);
		}

		public void addExtraView(double value, double radius, int tx, int ty, int approvedDirs) {
			GUTIL.flooder().init(this);
			SETT.ENV().map.updater.addExtraView(GUTIL.flooder(), this, value, radius, tx, ty, approvedDirs);
			GUTIL.flooder().done();
			
			extraI = GAME.updateI();
		}

		public double getView(int tx, int ty) {
			double g = get(tx, ty);
			if (extraI == GAME.updateI()) {
				g = uper.getExtraValue(this, g, tx, ty);
				g = CLAMP.d(g, 0, 1);
			}

			return g;
		}

		@Override
		public int index() {
			return index;
		}

		public STAT stat() {
			return STATS.ACCESS().ACCESS.all().get(index);
		}
		
		public int max() {
			return max;
		}
		
		public double radius() {
			return 1.0;
		}
		
		@Override
		public String key() {
			return key;
		}

		@Override
		public void update(RECTANGLE bounds, RECTANGLE area) {
			uper.update(this, bounds, area);
			
		}

		@Override
		public boolean has(int tx, int ty) {
			return map.get(tx+ty*SETT.TWIDTH) > 0;
		}

		@Override
		protected void clear() {
			map.clear();
		}

	}

	private final class Chunks {

		private final int mask = ~(RADIUS - 1);
		private final int width = SETT.TWIDTH / RADIUS;
		private final int size = width * SETT.THEIGHT / RADIUS;

		private final Bitmap1D changed = new Bitmap1D(size, false);
		private final int[] upMasks = Alloc.ii(size);
		private final QueueInteger updatables = new QueueInteger(size + 1);
		private Coo coo = new Coo();

		Chunks() {

		}

		public void change(int tx, int ty, int mask) {

			if (!SETT.IN_BOUNDS(tx, ty))
				return;
			int x1 = tx - RADIUS;
			int x2 = tx + RADIUS;
			int y1 = ty - RADIUS;
			int y2 = ty + RADIUS;

			for (int y = y1; y <= y2; y+=RADIUS) {
				for (int x = x1; x <= x2; x+=RADIUS) {
					if (SETT.IN_BOUNDS(x, y)) {
						changeP(x, y, mask);
					}
				}
			}

		}

		public void changeP(int tx, int ty, int mask) {
			int c = getChunk(tx, ty);
			upMasks[c] |= mask;
			if (!changed.get(c)) {
				changed.set(c, true);
				updatables.push(c);

			}
		}

		private int getChunk(int tx, int ty) {
			tx &= mask;
			ty &= mask;
			int c = tx / RADIUS + width * (ty / RADIUS);
			return c;
		}

		private final Rec rr = new Rec(5, 5);
		
		public void change(int tx, int ty) {

			if (!SETT.IN_BOUNDS(tx, ty))
				return;
			
			int m = 0;

			rr.moveC(tx, ty);
			
			for (Updatable e : ups) {
				if ((m & e.bit) != 0) {
					continue;
				}
				for (COORDINATE c : rr) {
					if (IN_BOUNDS(c) && (e.has(c.x(), c.y()) || e.getBaseValue(c.x(), c.y()) > 0)) {
						m |= e.bit;
						break;
					}
				}
					
//				
//				for (int di = 0; di < DIR.ALLC.size(); di++) {
//					DIR d = DIR.ALLC.get(di);
//					int dx = tx + d.x();
//					int dy = ty + d.y();
//					if (IN_BOUNDS(dx, dy) && (e.has(dx, dy) || e.getBaseValue(dx, dy) > 0)) {
//						
//						m |= e.bit;
//						continue;
//					}
//				}
				
			}
			if (m != 0)
				change(tx, ty, m);
		}

		void clear() {
			changed.clear();
			updatables.clear();
			Arrays.fill(upMasks, 0);
		}

		public boolean has() {
			return updatables.hasNext();
		}

		private int pt;
		
		public COORDINATE next() {
			int x = pt % width;
			int y = pt / width;
			coo.set(RADIUS * x, RADIUS * y);
			changed.set(pt, false);
			upMasks[pt] = 0;
			return coo;
		}

		public int nextMask() {
			pt = updatables.poll();
			return upMasks[pt];
		}

	}

	private class Updater {

		private double timer = 0;
		private double chunksPerTick = 1;

		protected void update(double ds) {
			timer += chunksPerTick;
			if (timer < 1)
				return;
			int am = (int) timer;
			timer -= am;
			while (chunks.has() && am > 0) {
				int m = chunks.nextMask();
				COORDINATE c = chunks.next();
				update(c.x(), c.y(), m);
				am--;
			}
		}

		private final Rec bounds = new Rec();
		private final Rec area = new Rec(RADIUS);
		private final int MAXR = RADIUS-1;

		private void update(int tx, int ty, int mask) {
			int x1 = CLAMP.i(tx - RADIUS, 0, SETT.TWIDTH);
			int x2 = CLAMP.i(tx + RADIUS * 2, 0, SETT.TWIDTH);
			int y1 = CLAMP.i(ty - RADIUS, 0, SETT.THEIGHT);
			int y2 = CLAMP.i(ty + RADIUS * 2, 0, SETT.THEIGHT);
			bounds.set(x1, x2, y1, y2);
			area.set(tx, CLAMP.i(tx + RADIUS, 0, SETT.TWIDTH), ty, CLAMP.i(ty + RADIUS, 0, SETT.THEIGHT));
			
			for (Updatable s : ups) {
				if ((s.bit & mask) == 0)
					continue;
				
				s.update(bounds, area);
				
			}
		}

	

		public void addExtraView(Flooder f, SettEnv thing, double value, double radius, int tx, int ty, int approvedDirs) {
			if (value <= 0)
				return;
			
			int rr = (int) Math.ceil(radius*MAXR);
			area.set(CLAMP.i(tx -rr, 0, SETT.TWIDTH), CLAMP.i(tx +rr+1, 0, SETT.TWIDTH), CLAMP.i(ty - rr, 0, SETT.THEIGHT), CLAMP.i(ty +1 + rr, 0, SETT.THEIGHT));
			
			thing.uper.addExtraView(area, f, thing, value, radius, tx, ty, approvedDirs);
			

			GUTIL.flooder().done();

		}

	}

}
