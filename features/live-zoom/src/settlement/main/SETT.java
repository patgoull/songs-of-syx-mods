package settlement.main;


import java.io.IOException;

import game.GAME;
import game.GAME.GameResource;
import game.GameDisposable;
import game.audio.AUDIO;
import game.battle.div.Div;
import game.faction.FACTIONS;
import game.faction.Faction;
import game.profile.Profiler;
import game.time.TIME;
import init.constant.C;
import init.constant.Config;
import init.settings.S;
import init.sprite.SPRITES;
import settlement.battle.SBattle;
import settlement.battle.invasion.Invador;
import settlement.entity.ENTETIES;
import settlement.entity.animal.Animals;
import settlement.entity.humanoid.Humanoids;
import settlement.entry.SENTRY;
import settlement.environment.ENVIRONMENT;
import settlement.job.JOBS;
import settlement.maintenance.MAINTENANCE;
import settlement.misc.ParticleRenderer;
import settlement.misc.SettPlacability;
import settlement.misc.placers.ComplexPlacers;
import settlement.overlay.SettOverlay;
import settlement.path.AvailabilityListener;
import settlement.path.PATHING;
import settlement.recipe.Recipes;
import settlement.room.main.ROOMS;
import settlement.room.main.throne.THRONE;
import settlement.stats.STATS;
import settlement.stats.standing.STANDINGS;
import settlement.thing.THINGS;
import settlement.thing.halfEntity.HalfEnts;
import settlement.thing.pointlight.POINTLIGHTS;
import settlement.thing.projectiles.SProjectiles;
import settlement.tilemap.TileMap;
import settlement.tilemap.floor.Floors;
import settlement.tilemap.floor.Grass;
import settlement.tilemap.ground.Ground;
import settlement.tilemap.ground.Minables;
import settlement.tilemap.terrain.Terrain;
import settlement.trade.SettTrade;
import settlement.weather.SWEATHER;
import snake2d.CORE;
import snake2d.Renderer;
import snake2d.util.color.COLOR;
import snake2d.util.color.RGB;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.DIR;
import snake2d.util.datatypes.RECTANGLE;
import snake2d.util.datatypes.Rec;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import snake2d.util.file.SuperSaver;
import snake2d.util.light.AmbientLight;
import snake2d.util.misc.ACTION;
import snake2d.util.sets.ArrayList;
import snake2d.util.sets.LinkedList;
import util.rendering.Minimap;
import util.rendering.RenderData;
import util.rendering.ShadowBatch;
import util.text.D;
import util.text.Dic;
import view.main.VIEW;
import view.sett.IDebugPanelSett;
import view.sett.ui.minimap.UIMinimapSettConfig;
import view.subview.GameWindow;

public final class SETT extends GameResource{

	public static final int TWIDTH = Config.sett().DIMENSION;
	public static final int THEIGHT = TWIDTH;
	public static final int PWIDTH = TWIDTH*C.TILE_SIZE;
	public static final int PHEIGHT = THEIGHT*C.TILE_SIZE;
	public static final int TAREA = TWIDTH*THEIGHT;
	public static final RECTANGLE TILE_BOUNDS = new Rec(0, TWIDTH, 0, THEIGHT);
	public static final RECTANGLE TILE_BOUNDS_I = new Rec(1, TWIDTH-1, 1, THEIGHT-1);
	public static final RECTANGLE PIXEL_BOUNDS = new Rec(0, TWIDTH*C.TILE_SIZE, 0, THEIGHT*C.TILE_SIZE);
	public static final SettlementGrid GRID = new SettlementGrid();
	private static SETT i;
	
	public static boolean IN_BOUNDS(int tx, int ty){
		return TILE_BOUNDS.holdsPoint(tx, ty);
	}
	
	public static boolean IN_BOUNDS(COORDINATE c){
		return TILE_BOUNDS.holdsPoint(c.x(), c.y());
	}
	
	public static boolean IN_BOUNDS(COORDINATE c, DIR d){
		return TILE_BOUNDS.holdsPoint(c.x()+d.x(), c.y()+d.y());
	}
	
	public static boolean IN_BOUNDS(int tx, int ty, DIR d){
		return IN_BOUNDS(tx + d.x(), ty + d.y());
	}
	
	public static boolean PIXEL_IN_BOUNDS(int x, int y){
		return PIXEL_BOUNDS.holdsPoint(x, y);
	}
	
	public static ENTETIES ENTITIES() {
		return i.eHandler;
	}
	
	public static Terrain TERRAIN() {
		return i.terrain.topology;
	}
	
	public static TileMap TILE_MAP() {
		return i.terrain;
	}
	
	public static Ground GROUND() {
		return i.terrain.ground;
	}
	
	public static Floors FLOOR() {
		return i.terrain.floors;
	}
	
	public static Grass GRASS() {
		return i.terrain.grass;
	}
	
	public static PATHING PATH() {
		return i.path;
	}
	
	public static JOBS JOBS() {
		return i.jobs;
	}
	
	public static THINGS THINGS(){
		return i.things;
	}
	
	public static ROOMS ROOMS(){
		return i.rooms;
	}
	
	public static CapitolArea WORLD_AREA(){
		return i.worldArea;
	}
	
	public static Animals ANIMALS(){
		return i.animals;
	}
	
	public static Humanoids HUMANOIDS(){
		return i.creatures;
	}
	
	public static Minables MINERALS(){
		return GROUND().minerals;
	}
	
	public static MAINTENANCE MAINTENANCE(){
		return i.maintenance;
	}
	
	public static SettPlacability PLACA() {
		return i.placability;
	}
	
//	public static FormationManager BATTLE(){
//		return i.battle;
//	}
	
	public static SBattle BATTLE(){
		return i.battle;
	}
	
	public static ParticleRenderer PARTICLES() {
		return i.particles;
	}
	
	public static POINTLIGHTS LIGHTS() {
		return i.lights;
	}
	
	public static SETT CITY() {
		return i;
	}
	
	public static Faction FACTION() {
		return i.faction();
	}
	
	public static HalfEnts HALFENTS() {
		return i.halfEnts;
	}
	
	public static SProjectiles PROJS() {
		return i.projectiles;
	}
	
	public static ENVIRONMENT ENV() {
		return i.env;
	}
	
	public static SettMaps MAPS() {
		return i.maps;
	}
	
	public static SettOverlay OVERLAY() {
		return i.details;
	}
	
	public static ComplexPlacers PLACERS() {
		return i.complexPlacers;
	}
	
	public static Invador INVADOR() {
		return i.invador;
	}
	
	public static Minimap MINIMAP() {
		return i.minimap;
	}
	
	public static SWEATHER WEATHER() {
		return i.weather;
	}
	
	public static SENTRY ENTRY(){
		return i.entry;
	}
	
	public static SettTrade TRADE(){
		return i.trade;
	}
	
	public static SettAudio audio() {
		return i.audio;
	}
	
	public static Recipes RECIPES(){
		return i.rescipes;
	}
	
	
	
	{i = this;}


	
	
	private final SettMaps maps = new SettMaps();
	private final SettPlacability placability = new SettPlacability();
	private final ParticleRenderer particles = new ParticleRenderer();
	
	private final ENVIRONMENT env = new ENVIRONMENT();
	
	private final TileMap terrain = new TileMap();
	private final Animals animals = new Animals();
	private final ROOMS rooms = new ROOMS();
	private final JOBS jobs = new JOBS();

	private final SBattle battle = new SBattle(null);
	private final SENTRY entry = new SENTRY();

	private boolean exists = false;
	private final ShadowBatch.Real shadowBatch = new ShadowBatch.Real();
	private final ShadowBatch shadowDummy = new ShadowBatch.Dummy();
	
	private final RenderData renData = new RenderData(TWIDTH, THEIGHT);
	
	private final CapitolArea worldArea = new CapitolArea();
	
	{
		STATS.create();
		STANDINGS.create();
	}
	
	private final ENTETIES eHandler = new ENTETIES();
	private final THINGS things = new THINGS();
	private final SProjectiles projectiles = new SProjectiles();

	private final Humanoids creatures = new Humanoids();
	private final MAINTENANCE maintenance = new MAINTENANCE();
	private final POINTLIGHTS lights = new POINTLIGHTS();
	private final ComplexPlacers complexPlacers = new ComplexPlacers();
	private final HalfEnts halfEnts = new HalfEnts();
	private final PATHING path = new PATHING();

	private final Invador invador = new Invador();
	private final Minimap minimap = new Minimap(TWIDTH);
	private final SWEATHER weather = new SWEATHER();
	private final SettTrade trade = new SettTrade();
	private final Recipes rescipes = new Recipes();
	private final SettOverlay details = new SettOverlay();
	private final SuperSaver<SettResource> saver;
	
	private final SettAudio audio;
	
	public SETT() throws IOException{
		super("SETT", true);
		new TUpdater();
		
		IDebugPanelSett.add("Regenerate settlement", new ACTION() {
			
			@Override
			public void exe() {
				reGenerate();
			}
		});
		saver = new SuperSaver<SettResource>(getClass(), SettResource.resources) {
			
			{D.gInit(SETT.class);}
			private CharSequence sSave = D.g("Saving City Resource");
			private CharSequence sLoad = D.g("Loading City Resource");
			
			@Override
			protected void save(SettResource t, FilePutter f) {
				CharSequence debug = S.get().developer ? " " + t.getClass().getSimpleName() : "";
				SPRITES.loader().print(sSave + ": " + t.i + "/" + SettResource.resources.size() + debug);
				t.save(f);
			}
			
			@Override
			protected void load(SettResource t, FileGetter f) throws IOException {
				CharSequence debug = S.get().developer ? " " + t.getClass().getSimpleName() : "";
				SPRITES.loader().print(sLoad + ": " + t.i + "/" + SettResource.resources.size() + debug);
				t.load(f);
			}
			
			@Override
			protected String key(SettResource t) {
				return t.key;
			}
			
			@Override
			protected void clear(SettResource t) {
				t.clear();
			}
			
			
		};
		audio = new SettAudio();
	}
	
	public void CreateFromWorldMap(int wx1, int wy1, boolean isBattle){
		D.gInit(getClass());
		this.worldArea.init(wx1, wy1, isBattle);
		
		SPRITES.loader().init();
		SPRITES.loader().print(D.g("Clearing"));
		for (int i = 0; i < SettResource.resources.size(); i++) {
			SettResource r = SettResource.resources.get(i);
			r.clear();
		}
		GAME.ARMIES().clear();
		
		
		for (int i = 0; i < SettResource.resources.size(); i++) {
			SettResource r = SettResource.resources.get(i);
			SPRITES.loader().print(""+D.g("Generating") + ": " + (1+i) + "/" + SettResource.resources.size());
			r.generate(worldArea);
		}
		
		
		SettlementGrid.Tile t = GRID.tile(worldArea.arrivalTile());
		rooms.THRONE.init.markArround(t.coo(DIR.C).x(), t.coo(DIR.C).y());
		VIEW.s().getWindow().centerAt(
				THRONE.coo().x()*C.TILE_SIZE, 
				THRONE.coo().y()*C.TILE_SIZE);
		VIEW.s().clear();
		//events.landingPartys[0].placeSingle(terrain.rooms.THRONE.getThrone().x(), terrain.rooms.THRONE.getThrone().y());
		setExists();
		
		for (int i = 0; i < SettResource.resources.size(); i++) {
			SettResource r = SettResource.resources.get(i);
			SPRITES.loader().print(""+D.g("Initializing") + ": " + (1+i) + "/" + SettResource.resources.size());
			r.init(false);
		}

		AvailabilityListener.listenAll(true);
		update(0, Profiler.DUMMY);
		//ArroundPlacer.placeArround(PLACERS().landingParty, terrain.rooms.THRONE.getThrone().x(), terrain.rooms.THRONE.getThrone().y());
		//update(0);
		System.gc();
		for (ACTION a : gHooks)
			a.exe();
		for (Div d : GAME.ARMIES().player().divisions())
			d.info.raceSet(GAME.player().race());
		
		
		
		
	}
	
	public static int tileRan(int tx, int ty) {
		return i.renData.random(tx, ty);
	}
	
	public static void reGenerate() {
		i.CreateFromWorldMap(i.worldArea.tiles().x1(), i.worldArea.tiles().y1(), i.worldArea.isBattle);
	}
	
	private void setExists(){
		if (GAME.SPEED.speedTarget() > 1)
			GAME.SPEED.speedSet(1);
		exists = true;
	}
	
	
	
	@Override
	protected void save(FilePutter saveFile){
		
		saveFile.bool(exists);
		if (!exists)
			return;
		D.gInit(getClass());
		worldArea.saver.save(saveFile);
		
		saver.save(saveFile);
		
		
	}
	
	@Override
	protected void load(FileGetter saveFile) throws IOException{

		D.gInit(getClass());
		
		exists = saveFile.bool();
		if (!exists)
			return;
	
		worldArea.saver.load(saveFile);
		
		
		int k = 0;
		int m = SettResource.resources.size();
		
		saver.load(saveFile);
		
		k = 0;
		for (int i = 0; i < SettResource.resources.size(); i++) {
			SettResource r = SettResource.resources.get(i);
			CharSequence s = S.get().debug ? ""+r : Dic.empty;
			SPRITES.loader().print(Dic.¤¤Generating + ": " + k++ + "/" + m  + s);
			r.init(true);
		}
		
		setExists();

		
//		for (HStat s : POPSTATS().all()) {
//			fixStat(s);
//		}
		
		
	}
	
//	private void fixStat(HStat s) {
//		
//		double g = s.global().getPlayer();
//		double b = s.global().get(FACTION().race(), HTYPE.CITIZEN);
//		POPSTATS().clear(s);
//		for(ENTITY e : ENTITIES().getAllEnts())
//			if (e instanceof Humanoid && !e.isRemoved())
//				POPSTATS().reAdd(s, (Humanoid)e);
//		
//		if (g != s.global().getPlayer()) {
//			OUT.err(s.name() + " " + g + " " + s.global().getPlayer() + " " + s.global().get(FACTION().race(), HTYPE.CITIZEN));
//		}
//		
//		if (b != s.global().get(FACTION().race(), HTYPE.CITIZEN)) {
//			OUT.err("b " + g + " " + s.name() + " " + b + " " + s.global().get(FACTION().race(), HTYPE.CITIZEN));
//		}
//		
////		if (s.global().getPlayer() != s.global().get(FACTION().race(), SUBJECT_CAST.CITIZEN))
////			OUT.err("q " + s.name() + " " + s.global().getPlayer() + " " + s.global().get(FACTION().race(), SUBJECT_CAST.CITIZEN));
//		
//	}
	
	public static void init() {
		int k = 0;
		int m = SettResource.resources.size();
		for (int i = 0; i < SettResource.resources.size(); i++) {
			SettResource r = SettResource.resources.get(i);
			SPRITES.loader().print("Processing Settlement " + k++ + "/" + m);
			r.init(false);
		}
	}

	
	public void render(Renderer r, float ds, GameWindow window, UIMinimapSettConfig con) {
		render(r, ds, window.zoomout(), window.pixels(), window.view().x1()<<window.zoomout(), window.view().y1()<<window.zoomout(), con);
	}

	private final Rec tmpWin = new Rec();
	
	public void render(Renderer r, float ds, int zoomout, int cx, int cy, RECTANGLE bounds, UIMinimapSettConfig con) {
		
		int offX = bounds.x1() << zoomout;
		int offY = bounds.y1() << zoomout;
		
		int w = bounds.width() << zoomout;
		int h = bounds.height() << zoomout;
		
		
		int x1 = cx - w/2;
		int y1 = cy -h/2;
		tmpWin.moveX1Y1(x1, y1);
		tmpWin.setWidth(w);
		tmpWin.setHeight(h);
		render(r, ds, zoomout, tmpWin, offX, offY, con);
		
	}
	
	private void renderFrame(int zoomout, RECTANGLE renWindow, int offX, int offY) {
		CORE.renderer().newLayer(false, 0);
		
		
		COLOR c = COLOR.WHITE10;
		
		int x1 = offX;
		int y1 = offY;
		int x2 = offX + (renWindow.width());
		int y2 = offY + (renWindow.height());
		AmbientLight.full.register(C.DIM());
		if (renWindow.x1() < 0) {
			x1 = (-renWindow.x1());
			x1 += offX;
		}
		if (renWindow.x2() > SETT.PWIDTH) {
			x2 = renWindow.width();
			x2 -= (renWindow.x2()-SETT.PWIDTH);
			x2 += offX;
		}
		
		if (renWindow.y1() < 0) {
			y1 = (-renWindow.y1());
			y1 += offY;
		}
		if (renWindow.y2() > SETT.PHEIGHT) {
			y2 = renWindow.height();
			y2 -= (renWindow.y2()-SETT.PHEIGHT);
			y2 += offY;
		}
		
		x1 = x1 >> zoomout;
		y1 = y1 >> zoomout;
		x2 = x2 >> zoomout;
		y2 = y2 >> zoomout;

		
		c.render(CORE.renderer(), x1-32, x1, y1-32, y2+32);
		c.render(CORE.renderer(), x2, x2+32, y1-32, y2+32);
		c.render(CORE.renderer(), x1, x2, y1-32, y1);
		c.render(CORE.renderer(), x1, x2, y2, y2+32);
	}
	
	private long goulLogNext = 0; // GOUL: test
	private final long[] goulNs = new long[7]; // GOUL: test, render time per stage
	private int goulFrames = 0; // GOUL: test
	private static final String[] goulStage = {"top", "above", "people", "things", "jobs", "ground", "post"}; // GOUL: test
	
	private long goulTime(int stage, long from) { // GOUL: test
		long now = System.nanoTime();
		goulNs[stage] += now-from;
		return now;
	}
	
	private static String goulMs(snake2d.CoreStats.Value v) { // GOUL: test, CoreStats are % of a 60 fps frame
		return String.format("%.1f", v.ave*1000.0/60/100);
	}
	
	/**
	 * 
	 * @param r
	 * @param ds
	 * @param spec
	 * @param renWindow - window of game world
	 * @param offX - absolute start X
	 * @param offY - absolute start Y;
	 */
	public void render(Renderer r, float ds, int zoomout, RECTANGLE renWindow, int offX, int offY, UIMinimapSettConfig con){
		
		audio.init();
		
		if (zoomout > 3) {
			return;
		}
		
		renderFrame(zoomout, renWindow, offX, offY);
		
		ds *= GAME.SPEED.speedTarget();
		
		// GOUL: test, logs last frame's sprite count every 3 s (the sprite buffer holds 262144 with this mod, more are dropped)
		if (System.currentTimeMillis() > goulLogNext) {
			goulLogNext = System.currentTimeMillis() + 3000;
			snake2d.LOG.ln("GOUL live-zoom: zoom " + zoomout + ", sprites last frame " + r.getSpritesSprocessed() + " / 262144");
			String st = "";
			for (int i = 0; i < goulNs.length; i++) {
				st += " " + goulStage[i] + " " + String.format("%.2f", goulFrames == 0 ? 0 : goulNs[i]/1e6/goulFrames);
				goulNs[i] = 0;
			}
			goulFrames = 0;
			snake2d.LOG.ln("GOUL perf: fps " + (int)snake2d.CoreStats.FPS.ave
				+ ", ms update " + goulMs(snake2d.CoreStats.updatePercentage) + " render " + goulMs(snake2d.CoreStats.renderPercentage)
				+ " flush " + goulMs(snake2d.CoreStats.coreFlush) + " sleep " + goulMs(snake2d.CoreStats.coreSleep) + " swap " + goulMs(snake2d.CoreStats.swapPercentage)
				+ ", city render ms" + st);
		}
		
		// GOUL: test, zoom 3 is drawn live like 0-2 instead of as the semi-map
		if (zoomout == 3 && false) {
			renderSemiMap(r, ds, zoomout, renWindow, offX, offY, con);
			return;
		}
		
		AUDIO.setSettGain(1.0/(1 + zoomout*1.5));
		CORE.getSoundCore().set(renWindow.cX()+offX, renWindow.cY()+offY);
		
		
		long goulT = System.nanoTime(); // GOUL: test
		goulFrames++;
		ShadowBatch s = shadowDummy;
		if (S.get().shadows.get() > 0){
			shadowBatch.init(zoomout, TIME.light().shadow.sx(), TIME.light().shadow.sy());
			s = shadowBatch;
		}

		renData.init(renWindow, offX, offY);
		weather.renderDownfall(r, ds, renData, zoomout);
		
		projectiles.renderAbove(r, s, ds, zoomout, renData);
		
		details.renderAbove(r, renData, zoomout);
		
		for (int i = ON_TOP_RENDERABLE.renderables.size()-1; i >= 0; i --) {
			ON_TOP_RENDERABLE ren = ON_TOP_RENDERABLE.renderables.get(i);
			ren.render(s, renData, zoomout, ds);
		}
		
		halfEnts.renderInit(renWindow);
		goulT = goulTime(0, goulT); // GOUL: test
		
//		r.newLayer(false, zoomout);
//		AmbientLight.full.register(0, C.WIDTH<<zoomout, 0, C.HEIGHT<<zoomout);
//		for (ON_TOP_RENDERABLE ren : ON_TOP_RENDERABLE.renderables) {
//			
//			ren.render(r, s, renData);
//		}
		
		halfEnts.renderAbove(r, s, ds, renWindow, offX, offY);
		r.newLayer(false, zoomout);
		
		
		terrain.renderAboveEnts(r, s, ds, zoomout, renData);
		battle.bannerR.render(r, s, ds, renWindow, offX, offY);
		goulT = goulTime(1, goulT); // GOUL: test
		//RENDER ENTITIES
		

		r.newLayer(false, zoomout);
		eHandler.renderA(r, s, ds, renWindow, offX, offY);
		goulT = goulTime(2, goulT); // GOUL: test
		
		r.newLayer(false, zoomout);
		halfEnts.render(r, s, ds, renWindow, offX, offY);
		
		r.newLayer(false, zoomout);
		things.render(r, s, ds, renWindow, offX, offY);
		r.newLayer(false, zoomout);
		lights.render(r, s, ds, renWindow, offX, offY);
		goulT = goulTime(3, goulT); // GOUL: test
		
		
		
		r.newLayer(false, zoomout);
		VIEW.current().renderBelowTerrain(r, s, renData);
		goulT = goulTime(4, goulT); // GOUL: test
		
		
		r.newLayer(false, zoomout);
		
		terrain.renderTheRest(r, s, ds, zoomout, renData,renWindow, offX, offY);
		goulT = goulTime(5, goulT); // GOUL: test
		
		for (SettResource rs : SettResource.resources) {
			rs.postRender(ds);
		}
		
		audio.makeSounds(zoomout, renData.area(), ds);
		goulTime(6, goulT); // GOUL: test
		
//		double dz = 1.0/(zoomout+1);
//		
//		for (Ambiance a : AUDIO.AMBI().all()) {
//			a.prioritySet(a.priority()*dz);
//			a.gainSet(dz*a.priority()/renData.area());
//		}
//		
//		{
//			double d = renData.waters();
//			AUDIO.AMBI().water.prioritySet(d*dz).gainSet(d*dz/renData.area());
//		}
//		
//		{
//			double d = renData.caves();
//			AUDIO.AMBI().windhowl.prioritySet(d*dz).gainSet(d*dz/renData.area());
//		}
//		
//		double nature = renData.vegitations();
//		if (SETT.WEATHER().rain.getD() > 0 && !SETT.WEATHER().snow.rainIsSnow() && !GAME.SPEED.isPaused()) {
//			AUDIO.AMBI().rain.prioritySet(renData.area()).gainSet(SETT.WEATHER().rain.getD());
//		}else {
//			if (TIME.light().nightIs()) {
//				AUDIO.AMBI().night.prioritySet(nature*dz).gainSet(dz);
//			}else {
//				AUDIO.AMBI().nature.prioritySet(nature*dz).gainSet(dz);
//			}
//			AUDIO.AMBI().windTrees.prioritySet(nature*dz).gainSet(CLAMP.d(WEATHER().wind.getD(), 0, 1));
//		}
//
//		weather.thunder.makeSounds(1.0, ds);
//		
//
//		{
//			double wind = (1.0-dz);
//			AUDIO.AMBI().wind.prioritySet(wind*renData.area()).gainSet(wind*WEATHER().wind.getD());
//		}
		
		
	}
	
	public void renderSemiMap(Renderer r, float ds, int zoomout, RECTANGLE renWindow, int offX, int offY, UIMinimapSettConfig con) {
		
		
		renData.init(renWindow, offX, offY);
		
		{
			r.newLayer(false, 3);
			AmbientLight.full.register(0, C.WIDTH()<<zoomout, 0, C.HEIGHT()<<zoomout);
			details.renderAbove(r, renData, zoomout);
		}
		for (int i = ON_TOP_RENDERABLE.renderables.size()-1; i >= 0; i --) {
			ON_TOP_RENDERABLE ren = ON_TOP_RENDERABLE.renderables.get(i);
			ren.render(shadowBatch, renData, zoomout, ds);
		}
		r.newLayer(false, 3);
		TIME.light().apply(C.DIM().x1()<<3, C.DIM().x2()<<3, C.DIM().y1()<<3, C.DIM().y2()<<3, RGB.WHITE);
		
		boolean bat = GAME.ARMIES().enemy().men() > 0 || VIEW.b().isActive();
		
		if (!bat)
			THINGS().renderZoomed(r, renWindow, offX, offY);
		
		ENTITIES().renderZoomed(r, shadowBatch, ds, renWindow, offX, offY, con);
		HALFENTS().renderZoomed(r, shadowBatch, ds, renWindow, offX, offY);
		r.newLayer(true, 3);
		//details.renderOnGround(r, shadowBatch, renData, zoomout);
		
		VIEW.current().renderBelowTerrain(r, shadowBatch, renData);
		
		TILE_MAP().renderSemiMap(r, ds, renData);
		
	}
	
	
	@Override
	protected void update(double ds, Profiler prof){

		prof.logStart(SETT.class);
		if (!exists)
			return;
		for (SettResource r : SettResource.resources) {
			if (r.isBattle|| !VIEW.b().isActive()) {
				prof.logStart(r);
				r.update(ds, prof);
				prof.logEnd(r);
			}
		}
		prof.logEnd(SETT.class);
	}
	
	@Override
	protected void afterTick() {
		for (SettResource r : SettResource.resources)
			r.afterTick();
	}
	
	public static abstract class SettResource {
		
		private final static LinkedList<SettResource> resources = new LinkedList<SettResource>();

		private final String key;
		private final boolean isBattle;
		private final int i;
		static {
			new GameDisposable() {
				@Override
				protected void dispose() {
					resources.clear();
				}
			};
		}
		
		
		protected SettResource(String key, boolean battle) {
			i = resources.add(this);
			this.key = key;
			this.isBattle = battle;
		}
		
		protected void save(FilePutter file) {
			
		}
		
		protected void load(FileGetter file) throws IOException{
			
		}
		
		protected void clear(){
			
		}
		
		protected void generate(CapitolArea area){
			
		}
		
		protected void update(double ds, Profiler profiler){
			
		}
		
		/**
		 * Will be called once after the settlement has renderered
		 * @param ds
		 */
		protected void postRender(float ds) {
			
		}
		
		protected void afterTick() {
			
		}
		
		protected void init(boolean loaded) {
			
		}

	}
	
	
	public static boolean exists(){
		return i.exists;
	}

	public Faction faction() {
		return FACTIONS.player();
	}
	
	private final ArrayList<ACTION> gHooks = new ArrayList<>(16);
	
	public static void addGeneratorHook(ACTION action) {
		i.gHooks.add(action);
	}
	
	
}
