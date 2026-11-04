package view.sett.ui.bottom;

import static settlement.main.SETT.JOBS;
import static settlement.main.SETT.ROOMS;
import static settlement.main.SETT.TERRAIN;

import init.sprite.SPRITES;
import init.sprite.UI.UI;
import settlement.main.SETT;
import snake2d.util.color.COLOR;
import snake2d.util.datatypes.AREA;
import snake2d.util.datatypes.DIR;
import snake2d.util.gui.GUI_BOX;
import snake2d.util.gui.GuiSection;
import snake2d.util.gui.clickable.CLICKABLE;
import snake2d.util.misc.ACTION;
import snake2d.util.sets.ArrayListGrower;
import snake2d.util.sets.LIST;
import snake2d.util.sprite.SPRITE;
import util.data.INT.IntImp;
import util.gui.misc.GBox;
import util.gui.misc.GButt;
import util.gui.misc.GGrid;
import util.text.D;
import view.keyboard.KEYS;
import view.keyboard.KeyButt;
import view.main.VIEW;
import view.sett.ui.room.prints.UISavedPrints;
import view.subview.GameWindow;
import view.tool.PLACABLE;
import view.tool.PLACER_TYPE;
import view.tool.PlacableMulti;
import view.tool.ToolConfig;

final class Options extends SPanel{

	private static CharSequence ¤¤CopyArea = "Copy Area";
	
	static {
		D.ts(Options.class);
	}
	
	Options(){

		D.gInit(this);
		
		body().setWidth(BButt.WIDTH*2);
		GGrid grid = new GGrid(this, 2);
		
		{
			ACTION a = new ACTION() {
				
				@Override
				public void exe() {
					VIEW.inters().popup.close();
					VIEW.s().ui.copier.activate();
				}
			};
			CLICKABLE c = new BButt(SPRITES.icons().l.copy, ¤¤CopyArea){
				
				@Override
				protected void clickA() {
					a.exe();
				};
				
				
			};
			c = KeyButt.wrap(a, c, KEYS.SETT(), "COPY_SUPER", ¤¤CopyArea, "");
			SearchToolPanel.add(c, ¤¤CopyArea, "");
			
			grid.add(c);
		}
		make("COPY_ROOM", ROOMS().copy.copy(), grid);
		make("MOVE_ROOM", ROOMS().copy.move(), grid); // GOUL: Move Room
		
		{
			CharSequence name = D.g("Planning");
			
			ACTION a = new ACTION() {
				
				@Override
				public void exe() {
					SETT.JOBS().planMode.toggle();
				}
			};
			
			CLICKABLE c = new BButt(UI.icons().l.suspend.twin(UI.icons().m.cog, DIR.NW, 1), name) {
				
				@Override
				protected void clickA() {
					a.exe();
				};
				
				@Override
				protected void renAction() {
					selectedSet(SETT.JOBS().planMode.is());
				};
				
				
			};
			c.hoverInfoSet(D.g("PlanningD", "When enabled, placed jobs will not be performed until manually activated by your grace."));
			c = KeyButt.wrap(a, c, KEYS.SETT(), "PLANNING_MODE", name, "");
			SearchToolPanel.add(c, name, "");
			grid.add(c);
		}
		
		
		
		
		
		
		make("REPAIR", JOBS().tool_repair, grid);
		
		make("ACTIVATE", JOBS().tool_activate, grid);
		make("DORMANT", JOBS().tool_dormant, grid);
		
		make("MAINTENANCE_ON", SETT.MAINTENANCE().enablePlacer, grid);
		make("MAINTENANCE_OFF", SETT.MAINTENANCE().enablePlacer.getUndo(), grid);
		
		make("DIAGONALIZE", TERRAIN().diagonal.placer, grid);
		make("SQUAREIFY", TERRAIN().diagonal.undo, grid);
		
		PlacableMulti ppundo = new PlacableMulti("") {
			
			@Override
			public void place(int tx, int ty, AREA area, PLACER_TYPE type) {
				SETT.FLOOR().floorundernot.set(tx, ty, true);
			}
			
			@Override
			public CharSequence isPlacable(int tx, int ty, AREA area, PLACER_TYPE type) {
				return !SETT.FLOOR().floorundernot.is(tx, ty) ? null : E;
			}
			
			@Override
			public void updateRegardless(GameWindow window, AREA selected) {
				SETT.OVERLAY().RODIFY.add();
				super.updateRegardless(window, selected);
			}
		};
		
		PlacableMulti pp = new PlacableMulti(D.g("Roadify"), D.g("RoadifyD", "Allow buildings such as fences to visually try to match their tile to the roads around them."), SETT.FLOOR().defaultRoad.getIcon()) {
			
			@Override
			public void place(int tx, int ty, AREA area, PLACER_TYPE type) {
				SETT.FLOOR().floorundernot.set(tx, ty, false);
			}
			
			@Override
			public CharSequence isPlacable(int tx, int ty, AREA area, PLACER_TYPE type) {
				return !SETT.FLOOR().floorundernot.is(tx, ty) ? E : null;
			}
			
			@Override
			public void updateRegardless(GameWindow window, AREA selected) {
				SETT.OVERLAY().RODIFY.add();
				super.updateRegardless(window, selected);
			}
			
			@Override
			public PLACABLE getUndo() {
				return ppundo;
			}
		};
		
		make("RODIFY", pp, grid);
		
//		Placer p = new Placer();
//		make("SAVE_PRINT", p, grid, p.config);
//		
		{

			
			ACTION a = new ACTION() {
				
				@Override
				public void exe() {
					VIEW.s().ui.prints.open();
				}
			};
			
			CLICKABLE c = new BButt(SPRITES.icons().l.prints, UISavedPrints.¤¤title) {
				
				@Override
				protected void clickA() {
					a.exe();
				};
				
			};
			CharSequence desc = D.g("printsD", "Add and manage saved blueprints.");
			c.hoverInfoSet(desc);
			c = KeyButt.wrap(a, c, KEYS.SETT(), "SAVE_PRINT", UISavedPrints.¤¤title, "");
			SearchToolPanel.add(c, UISavedPrints.¤¤title, desc);
			grid.add(c);
		}
		
		make("UPGRADE_PLACE", new RoomUpgrader(), grid);
		
		{
			
			final IntImp iii = new IntImp();
			ArrayListGrower<CLICKABLE> li = new ArrayListGrower<>();
			
			for (int i = 0; i < SETT.JOBS().paintmap.max()-1; i++) {
				final int k = i;
				
				SPRITE c = k == 0 ? COLOR.WHITE10 : COLOR.UNIQUE.get(i).makeSprite(16, 16);
				li.add(new GButt.ButtPanel(c) {
					
					@Override
					protected void clickA() {
						iii.set(k);
					}
					
					@Override
					protected void renAction() {
						selectedSet(iii.get() == k);
					}
				});
				
				
			}
			
			
			pp = new PlacableMulti(D.g("Paint-tool"), D.g("PlanToolD", "Paint the map in different colors. Has no impact on game-play"), SETT.FLOOR().defaultRoad.getIcon()) {
				
				
				@Override
				public void place(int tx, int ty, AREA area, PLACER_TYPE type) {
					SETT.JOBS().paintmap.set(tx,  ty, iii.get());
				}
				
				@Override
				public CharSequence isPlacable(int tx, int ty, AREA area, PLACER_TYPE type) {
					return null;
				}
				
				@Override
				public void updateRegardless(GameWindow window, AREA selected) {
					SETT.OVERLAY().PAINTER.add();
					super.updateRegardless(window, selected);
				}
				
				@Override
				public PLACABLE getUndo() {
					return null;
				}
				
				@Override
				public LIST<CLICKABLE> getAdditionalButt() {
					return li;
				}
			};
			
			make("PLAN_PAINT", pp, grid);
		}
		

		pad(8, 8);
	}
	
	private void make(String code, PLACABLE p, GGrid grid) {
		make(code, p, grid, null);
	}
	
	private void make(String code, PLACABLE p, GGrid grid, ToolConfig con) {
		ACTION a = new ACTION() {
			
			@Override
			public void exe() {
				VIEW.inters().popup.close();
				if (con != null)
					VIEW.s().tools.place(p, con);
				else
					VIEW.s().tools.place(p);
			}
		};
		CLICKABLE c = new BButt(p.getIcon(), p.name()){
			
			@Override
			protected void clickA() {
				a.exe();
			};
			
			@Override
			public void hoverInfoGet(GUI_BOX text) {
				p.hoverDesc((GBox) text);
			}
			
			
		};
		c = KeyButt.wrap(a, c, KEYS.SETT(), code, p.name(), "");
		SearchToolPanel.add(c, p.name(), "");
		grid.add(c);
	}
	
	public GuiSection ge2t() {
		return this;
	}

	
}
