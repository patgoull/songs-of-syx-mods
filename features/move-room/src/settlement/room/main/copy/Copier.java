package settlement.room.main.copy;

import static settlement.main.SETT.JOBS;
import static settlement.main.SETT.ROOMS;

import game.faction.FACTIONS;
import init.constant.C;
import init.sprite.SPRITES;
import init.sprite.UI.Icon;
import init.sprite.UI.UI;
import init.structure.STRUCTURES;
import init.structure.Structure;
import settlement.main.SETT;
import settlement.room.main.ROOMA;
import settlement.room.main.Room;
import settlement.room.main.RoomBlueprintImp;
import settlement.room.main.TmpArea;
import settlement.room.main.construction.ConstructionInit;
import settlement.room.main.furnisher.Furnisher;
import settlement.room.main.furnisher.FurnisherItem;
import settlement.room.main.furnisher.FurnisherItemTile;
import settlement.room.main.placement.PLACEMENT;
import settlement.room.main.placement.UtilWallPlacability;
import settlement.room.main.util.Deleter;
import settlement.room.main.util.RoomAreaWrapper;
import settlement.tilemap.terrain.TBuilding;
import snake2d.SPRITE_RENDERER;
import snake2d.util.color.COLOR;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.Coo;
import snake2d.util.datatypes.DIR;
import snake2d.util.gui.GUI_BOX;
import snake2d.util.gui.GuiSection;
import snake2d.util.gui.clickable.CLICKABLE;
import snake2d.util.gui.renderable.RENDEROBJ;
import snake2d.util.sets.ArrayList;
import snake2d.util.sets.LIST;
import snake2d.util.sets.LISTE;
import snake2d.util.sprite.SPRITE;
import util.colors.GCOLOR;
import util.gui.misc.GBox;
import util.gui.misc.GButt;
import util.text.D;
import util.text.Dic;
import view.main.VIEW;
import view.tool.PlacableFixed;
import view.tool.PlacableFixedImp;
import view.tool.PlacableSingle;
import view.tool.ToolConfig;

class Copier extends PlacableSingle{

	private static CharSequence ¤¤name = "¤Room Copier";
	private static CharSequence ¤¤IncludeWalls = "¤Include Walls";
	private static CharSequence ¤¤desc = "¤Copies already planned rooms.";
	private static CharSequence ¤¤indoor = "¤This room requires to be built indoors and you must pick a structure type.";

	static {
		D.ts(Copier.class);
	}
	private static RoomAreaWrapper wrap = new RoomAreaWrapper();
	private ROOMA room;
	private TBuilding structure;
	private boolean w = true;
	private final BSwap swap;
	private final RoomMover mover; // GOUL: null when copying
	
	
	private final GuiSection buttonsIndoor = new GuiSection();
	
	{
		D.gInit(this);
		
		for (Structure t : STRUCTURES.all()) {
			
			
			CLICKABLE c = new GButt.Panel(t.terrain().iconCombo, t.desc) {
				@Override
				public void hoverInfoGet(GUI_BOX text) {
					GBox b = (GBox) text;
					b.title(t.name);
					b.text(t.desc);
					b.NL();
					b.setResource(t.resource, t.resAmount);
				}
				
				@Override
				protected void clickA() {
					structure = t.terrain();
					VIEW.inters().popup.close();
				}
				
				@Override
				protected void renAction() {
					selectedSet(structure == t.terrain());
				}
			};
			buttonsIndoor.addDownC(0, c);
		}
		
	}
	
	private final LIST<CLICKABLE> walls = new ArrayList<CLICKABLE> (
		new GButt.Panel(SPRITES.icons().m.wall) {
			
			@Override
			protected void clickA() {
				w = !w;
				
			};
			@Override
			protected void renAction() {
				selectedSet(w);
			};
			@Override
			public void hoverInfoGet(GUI_BOX text) {
				text.text(¤¤IncludeWalls);
			};
			
		},
		new GButt.Panel(SPRITES.icons().m.cancel) {
			@Override
			protected void clickA() {
				VIEW.inters().popup.show(buttonsIndoor, this);
			}
			@Override
			protected void renAction() {
				replaceLabel(structure == null ? SETT.TERRAIN().BUILDINGS.all().get(0).iconCombo : structure.iconCombo, DIR.C);
			}
			
			@Override
			public void hoverInfoGet(GUI_BOX text) {
				text.text(¤¤indoor);
			};
			
		}
	);
	
	private final CLICKABLE bOverlay = new GButt.ButtPanel(UI.icons().s.eye.sized(Icon.M)) {
		
		@Override
		protected void clickA() {
			SETT.ROOMS().placement.placer.showOverlay.toggle();
		}
		
		@Override
		protected void renAction() {
			selectedSet(SETT.ROOMS().placement.placer.showOverlay.is());
		}
		
		@Override
		public void hoverInfoGet(GUI_BOX text) {
			text.title(Dic.¤¤Overlay);
			if (swap.current().constructor().overlay() != null && swap.current().constructor().overlay().desc != null) {
				text.text(swap.current().constructor().overlay().desc);
			}
		};
	};
	
	private final CLICKABLE bFoundation = new GButt.ButtPanel(UI.icons().m.foundation) {
		
		@Override
		protected void clickA() {
			SETT.ROOMS().placement.placer.showFoundation.toggle();
		}
		
		@Override
		protected void renAction() {
			selectedSet(SETT.ROOMS().placement.placer.showFoundation.is());
		}
		
		@Override
		public void hoverInfoGet(GUI_BOX text) {
			text.title(SETT.OVERLAY().FOUNDATION.name);
			text.text(SETT.OVERLAY().FOUNDATION.desc);
		};
	};
	
	private final ArrayList<CLICKABLE> butts = new ArrayList<CLICKABLE>(walls.size()+1);
	
	public Copier(BSwap s) {
		super(¤¤name, ¤¤desc);
		this.swap = s;
		this.mover = null; // GOUL
	}
	
	// GOUL: Move Room, the same tool, but the room is dismantled and only its copy is left
	public Copier(BSwap s, RoomMover mover) {
		super(mover.name(), mover.desc());
		this.swap = s;
		this.mover = mover;
	}

	private RoomBlueprintImp pppp;
	
	@Override
	public CharSequence isPlacable(int tx, int ty) {
		
		Room r = SETT.ROOMS().map.get(tx, ty);
		if (r != null && r.constructor() != null && r.constructor().canBeCopied()) {
			
			pppp = r.constructor().blue();
			if (!pppp.reqs.passes(FACTIONS.player()))
				return Dic.¤¤Locked;
			if (!Deleter.canRemove(tx, ty))
				return E;
			return null;
		}
		
			
		return E;
	}
	
	@Override
	public void placeInfo(GBox b, int tiles) {
		if (pppp != null)
			b.text(pppp.info.name);
		super.placeInfo(b, tiles);
		pppp = null;
	}

	@Override
	public void placeFirst(int tx, int ty) {
		Room room = ROOMS().map.get(tx, ty);
		wrap.done();
		this.room = wrap.init(room, tx, ty);
		
		structure = SETT.ROOMS().construction.structure(tx, ty); 
		

		nextStep.rotSet(0);
		swap.init(ROOMS().map.get(tx, ty).constructor().blue());
		VIEW.s().tools.place(nextStep, config);
		wrap.done();
	}
	
	@Override
	public boolean expandsTo(int fromX, int fromY, int toX, int toY) {
		return ROOMS().map.get(fromX, fromY) != null && ROOMS().map.get(fromX, fromY).isSame(fromX, fromY, toX, toY);
	}
	
	@Override
	public SPRITE getIcon() {
		if (mover != null) // GOUL
			return mover.icon();
		return SPRITES.icons().l.copyRoom;
	}
	
	private final ToolConfig config = new ToolConfig() {
		
		@Override
		public boolean back() {
			VIEW.s().tools.place(Copier.this);
			return false;
		};
		
		@Override
		public void addUI(LISTE<RENDEROBJ> uis) {
			VIEW.s().tools.placer.addStandardButtons(uis, true);
		};
		
	};
	
	private final PlacableFixed nextStep = new PlacableFixedImp(Copier.this.name(), 4, 1) {
		
		private final Coo cTmp = new Coo();
		
		private boolean update() {
			Room r = SETT.ROOMS().map.get(room.mX(), room.mY());
			
			if (r == null)
				return false;
			if (r.constructor() == null)
				return false;
			if (structure == null){
				if (r.constructor().mustBeIndoors()) {
					structure = ConstructionInit.findStructure(room.mX(), room.mY());
				}else {
					structure = null;
				}
			}
			wrap.done();
			room = wrap.init(r, room.mX(), room.mY());
			
			return true;
		}
		
		
		
		@Override
		public void place(final int tx, final int ty, int rx, int ry) {
			
			if (rx != 0 || ry != 0) {
				return;
			}
			
			update();
			
			Furnisher furnisher = swap.current().constructor();
			
			// GOUL: read before a move dismantles the room
			Room r = SETT.ROOMS().map.get(room.mX(), room.mY());
			ConstructionInit init = new ConstructionInit(r.upgrade(room.mX(), room.mY()), furnisher, structure, 0, r.makeState(room.mX(), room.mY(), false));
			if (mover != null) // GOUL: the room is dismantled first, so its copy may overlap where it was
				room = mover.lift(room);
			
			if (furnisher.mustBeIndoors() && w) {
				
				for (int dy = 0; dy < height(); dy++) {
					for (int dx = 0; dx < width(); dx++) {
						COORDINATE c = getSourceTile(dx, dy);
						if (room.is(c)) {
							for (int i = 0; i < DIR.ALL.size(); i++) {
								DIR d = DIR.ALL.get(i);
								c = getSourceTile(dx+d.x(), dy+d.y());
								if (!room.is(c)) {
								
									int x = tx+dx+d.x();
									int y = ty+dy+d.y();
									
									if (UtilWallPlacability.wallisReal.is(c)) {
										if (UtilWallPlacability.wallShouldBuild.is(x, y))
											UtilWallPlacability.wallBuild(x, y, structure);
									}
									else if(UtilWallPlacability.openingIsReal.is(c)) {
										if (UtilWallPlacability.openingShouldBuild.is(x, y))
											UtilWallPlacability.openingBuild(x, y, structure);
									}
								}
							}
						}
						
					}
					
				}
				
				
				
//				COORDINATE c = getSourceTile(rx, ry);
//				if (room.is(c)) {
//					for (int i = 0; i < DIR.NORTHO.size(); i++) {
//						DIR d = DIR.NORTHO.get(i);
//						c = getSourceTile(rx+d.x(), ry+d.y());
//						if (!room.is(c)) {
//						
//							
//							if (UtilWallPlacability.wallisReal.is(c)) {
//								if (UtilWallPlacability.wallShouldBuild.is(tx+d.x(), ty+d.y()))
//									UtilWallPlacability.wallBuild(tx+d.x(), ty+d.y(), structure);
//							}
//							else if(UtilWallPlacability.openingIsReal.is(c)) {
//								if (UtilWallPlacability.openingShouldBuild.is(tx+d.x(), ty+d.y()))
//									UtilWallPlacability.openingBuild(tx+d.x(), ty+d.y(), structure);
//							}
//						}
//					}
//				}
				
			}
			
//			if (rx != 0 || ry != 0) {
//				return;
//			}
			
			TmpArea tmp = SETT.ROOMS().tmpArea(this);
			
			
			int w = width();
			int h = height();
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					COORDINATE c = getSourceTile(x, y);
					if (!room.is(c))
						continue;
					tmp.set(tx+x, ty+y);
				}
			}
			
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {

					COORDINATE c = getSourceTile(x, y);
					int sx = c.x();
					int sy = c.y();
					if (!room.is(sx, sy))
						continue;
					FurnisherItem it = master(sx, sy); // GOUL
					if (it == null)
						continue;
					
					c = getOrigionalDelta(sx, sy);
					int x1 = c.x()+tx;
					int y1 = c.y()+ty;
					
					c = getSourceItemOff(it, it.firstX(), it.firstY());
					x1-= c.x();
					y1 -= c.y();
					
					int rot = (it.rotation+rot()%it.group.rotations());
					rot %= it.group.rotations();		
					it = it.group.item(it.variation(), rot);
					
					SETT.ROOMS().fData.itemSet(x1, y1, it, tmp.room());
				}
			}
			
			SETT.ROOMS().construction.createClean(tmp, init);
			
			if (mover != null && furnisher.mustBeIndoors() && Copier.this.w) // GOUL: and its old walls and roof
				mover.dismantle();
			
			return;
			
		}
		
		// GOUL: the item that has its master tile here. A moved room is gone, its items are in the mover
		private FurnisherItem master(int sx, int sy) {
			if (mover != null)
				return mover.item(sx, sy);
			if (!SETT.ROOMS().fData.isMaster.is(sx, sy))
				return null;
			return SETT.ROOMS().fData.item.get(sx, sy);
		}
		
		// GOUL: the moved room is gone, pick the next one
		@Override
		public void afterPlaced(int tx1, int ty1) {
			if (mover != null)
				VIEW.s().tools.place(Copier.this);
		}
		
		@Override
		public CharSequence placable(int tx, int ty, int rx, int ry) {
			if (!update()) {
				VIEW.s().tools.place(null);
				return E;
			}
			
			COORDINATE c = getSourceTile(rx, ry);
			if (!room.is(c)) {
				return null;
			
			}
			

			Furnisher furnisher = swap.current().constructor();
			CharSequence s = PLACEMENT.placable(tx, ty, furnisher.blue(), true);
			if (mover != null && room.is(tx, ty)) // GOUL: a moved room may overlap where it is now
				s = null;
			if (s != null)
				return s;
			
			
			return furnisher.placable(tx, ty, SETT.ROOMS().fData.item.get(c), SETT.ROOMS().fData.tile.get(c));
		}
		
		@Override
		public void updateRegardless(view.subview.GameWindow window) {
			if (swap.current().constructor().overlay() != null && SETT.ROOMS().placement.placer.showOverlay.is()) {
				swap.current().constructor().overlay().add();
			}
			if (swap.current().constructor().isHeavy() && SETT.ROOMS().placement.placer.showFoundation.is()) {
				SETT.OVERLAY().FOUNDATION.add();
			}
		};
		
		@Override
		public int width() {
			int wi = (rot() & 1) == 1 ? room.body().height() : room.body().width();
			return wi;
		}
		
		@Override
		public int height() {
			int h = (rot() & 1) == 0 ? room.body().height() : room.body().width();
			return h;
		}
		
		private COORDINATE getSourceTile(int rx, int ry) {
			switch (rot()) {
			case 0:
				cTmp.set(room.body().x1()+rx, room.body().y1()+ry);
				break;
			case 1:
				cTmp.ySet(room.body().y2()-rx-1);
				cTmp.xSet(room.body().x1()+ry);
				break;
			case 2:
				cTmp.ySet(room.body().y2()-ry-1);
				cTmp.xSet(room.body().x2()-rx-1);
				break;
			case 3:
				cTmp.ySet(room.body().y1()+rx);
				cTmp.xSet(room.body().x2()-ry-1);
				break;
			default:
				throw new RuntimeException();
			}
			return cTmp;
		}
		
		private COORDINATE getOrigionalDelta(int rx, int ry) {
			int dx = rx - room.body().x1();
			int dy = ry - room.body().y1();
			switch (rot()) {
			case 0:
				cTmp.set(dx, dy);
				break;
			case 1:
				cTmp.ySet(dx);
				cTmp.xSet(room.body().height()-dy-1);
				break;
			case 2:
				cTmp.ySet(room.body().height()-dy-1);
				cTmp.xSet(room.body().width()-dx-1);
				break;
			case 3:
				cTmp.ySet(room.body().width()-dx-1);
				cTmp.xSet(dy);
				break;
			default:
				throw new RuntimeException();
			}
			return cTmp;
		}
		
		private COORDINATE getSourceItemOff(FurnisherItem i, int rx, int ry) {
			
			
			switch (rot()) {
			case 0:
				cTmp.set(rx, ry);
				break;
			case 1:
				cTmp.ySet(rx);
				cTmp.xSet(i.height()-ry-1);
				break;
			case 2:
				cTmp.ySet(i.height()-ry-1);
				cTmp.xSet(i.width()-rx-1);
				break;
			case 3:
				cTmp.ySet(i.width()-rx-1);
				cTmp.xSet(ry);
				break;
			default:
				throw new RuntimeException();
			}
			return cTmp;
		}
		
		@Override
		public void renderPlaceHolder(SPRITE_RENDERER r, int mask, int x, int y, int tx, int ty, int rx, int ry, boolean isPlacable, boolean areaIsPlacable) {
			COORDINATE c = getSourceTile(rx, ry);
			if (!room.is(c)) {
				
				return;
			
			}
				
			if (isPlacable && areaIsPlacable) {
				if (!JOBS().planMode.is()) {
					GCOLOR.MAP().JOB_ACTIVE.bind();
				} else {
					GCOLOR.MAP().JOB_DORMANT.bind();
				}
			}
			
			
			
			
			FurnisherItemTile tile = ROOMS().fData.tile.get(c);

			if (tile == null || tile.sprite() == null || !tile.isBlocker()) {
				SPRITES.cons().BIG.dashed.render(r, mask, x, y);
			}else {
				SPRITES.cons().BIG.filled.render(r, mask, x, y);
			}
			
			Furnisher furnisher = swap.current().constructor();
			if (furnisher.mustBeIndoors() && w) {
				for (int i = 0; i < DIR.NORTHO.size(); i++) {
					DIR d = DIR.NORTHO.get(i);
					c = getSourceTile(rx+d.x(), ry+d.y());
					if (!room.is(c)) {
						if (UtilWallPlacability.wallisReal.is(c) && UtilWallPlacability.wallShouldBuild.is(tx+d.x(), ty+d.y()))
							SPRITES.cons().BIG.filled.render(r, 0, x+d.x()*C.TILE_SIZE, y+d.y()*C.TILE_SIZE);
						else if(UtilWallPlacability.openingIsReal.is(c) && UtilWallPlacability.openingShouldBuild.is(tx+d.x(), ty+d.y()))
							SPRITES.cons().BIG.dashed_hollow.render(r, 0, x+d.x()*C.TILE_SIZE, y+d.y()*C.TILE_SIZE);
					}
				}	
			}
			
			COLOR.unbind();
		};
		
		@Override
		public LIST<CLICKABLE> getAdditionalButt() {
			Furnisher furnisher = swap.current().constructor();
			butts.clearSloppy();
			if (furnisher.mustBeIndoors()) {
				butts.add(walls);
			}
			if (furnisher.overlay() != null)
				butts.add(bOverlay);
			if (furnisher.isHeavy())
				butts.add(bFoundation);
			return swap.wrap(butts);
		}
		
		@Override
		public CharSequence name() {
			Furnisher furnisher = swap.current().constructor();
			return furnisher.blue().info.name;
		}
		
	};
	

	
}
