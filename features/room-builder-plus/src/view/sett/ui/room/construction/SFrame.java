package view.sett.ui.room.construction;

import init.constant.C;
import init.sprite.SPRITES;
import init.sprite.UI.UI;
import settlement.main.SETT;
import snake2d.KEYCODES;
import snake2d.SPRITE_RENDERER;
import snake2d.util.gui.GUI_BOX;
import snake2d.util.gui.GuiSection;
import snake2d.util.gui.clickable.CLICKABLE;
import snake2d.util.misc.ACTION;
import snake2d.util.sprite.text.Str;
import util.gui.misc.GBox;
import util.gui.misc.GButt;
import util.gui.panel.GPanel;
import util.text.D;
import view.keyboard.KEYS;
import view.keyboard.KeyButt;
import view.main.VIEW;

final class SFrame {

	private int width = C.SG*470;
	private final State state;
	private final GPanel panel = new GPanel();
	private GuiSection section = new GuiSection();
	private final Str title = new Str(50);
	private final GuiSection bottomButtons = new GuiSection();
	{
		D.gInit(this);
	}
	private CharSequence sconstruction = D.g("{0} construction");
	private CharSequence sexpensive = D.g("expensive", "The layout of the room will make it more expensive to construct and maintain. The yellow squares denote where support for the room is weak and will need extra materials. To increase support, remove some of the room in this area, so that it can be used to build supportive walls. Proceed anyway?");
	private boolean message = false;
	// GOUL: not in the dictionary yet, so plain text
	private CharSequence sundoDesc = "Undo the last change to the room.";
	private CharSequence sredo = "Redo";
	private CharSequence sredoDesc = "Redo what was undone.";

	SFrame(State state){
		this.state = state;
	}
	
	{

		
//		bottomButtons.add(UI.PANEL().panelL.get(DIR.E), 0, 0);
//		bottomButtons.addRight(0, UI.PANEL().panelL.get(DIR.E, DIR.W));
//		bottomButtons.addRight(0, UI.PANEL().panelL.get(DIR.E, DIR.W));
//		bottomButtons.addRight(0, UI.PANEL().panelL.get(DIR.E, DIR.W));
//		bottomButtons.addRight(0, UI.PANEL().panelL.get(DIR.W));
		
		GuiSection s = new GuiSection() {
			@Override
			public void render(SPRITE_RENDERER r, float ds) {
				UI.PANEL().butt.render(r, body(), 0);
				super.render(r, ds);
			};
		};
		
		s.addRightC(0, new GButt.Panel(SPRITES.icons().m.trash, D.g("removeRoom", "remove room")) {
			
			@Override
			protected void clickA() {
				state.config.build = false;
				VIEW.s().tools.placer.deactivate();
			};				
		});
		
		// GOUL: undo with Ctrl+Z by default, rebindable. While items are moved, it puts them back first
		ACTION undo = new ACTION() {

			@Override
			public void exe() {
				if (VIEW.s().tools.configCurrent() != state.config || !state.b.constructor().usesArea())
					return;
				if (!SETT.ROOMS().placement.placer.mover.cancel())
					SETT.ROOMS().placement.placer.popHistory();
			}
		};
		CLICKABLE undoButt = new GButt.Panel(SPRITES.icons().m.arrow_left) { // GOUL: was s.addRightC(32, new GButt.Panel(SPRITES.icons().m.arrow_left, D.g("undo")) {
			
			@Override
			protected void clickA() {
				SETT.ROOMS().placement.placer.popHistory();
			};
			
			@Override
			protected void renAction() {
				activeSet(SETT.ROOMS().placement.placer.hasHistory());
			}
			
		}; // GOUL
		GuiSection arrows = new GuiSection(); // GOUL: undo and redo centered between remove room and construct
		arrows.add(KeyButt.wrap(undo, undoButt, KEYS.SETT(), "ROOM_UNDO", D.g("undo"), sundoDesc, KEYCODES.KEY_LEFT_CONTROL, KEYCODES.KEY_Z)); // GOUL
		
		// GOUL: redo with Ctrl+Y by default
		ACTION redo = new ACTION() {

			@Override
			public void exe() {
				if (VIEW.s().tools.configCurrent() != state.config || !state.b.constructor().usesArea())
					return;
				SETT.ROOMS().placement.placer.redo();
			}
		};
		CLICKABLE redoButt = new GButt.Panel(SPRITES.icons().m.arrow_right) {
			
			@Override
			protected void clickA() {
				redo.exe();
			};
			
			@Override
			protected void renAction() {
				activeSet(SETT.ROOMS().placement.placer.hasRedo());
			}
			
		};
		arrows.addRightC(0, KeyButt.wrap(redo, redoButt, KEYS.SETT(), "ROOM_REDO", sredo, sredoDesc, KEYCODES.KEY_LEFT_CONTROL, KEYCODES.KEY_Y));
		arrows.pad(16, 0);
		s.addRightC(0, arrows);

		ACTION create = new ACTION() {
			
			@Override
			public void exe() {
				SETT.ROOMS().placement.placer.create();
//				if (c != null && SETT.ROOMS().map.get(c) != null)
//					SETT.ROOMS().map.get(c).upgradeSet(state.upgrade[state.b.index()]);
				VIEW.s().tools.placer.deactivate();
			}
		};
		
		CharSequence cc = D.g("construct!");
		
		s.addRightC(0, new GButt.Panel(SPRITES.icons().m.ok) {
			
			@Override
			protected void clickA() {
				CharSequence s = SETT.ROOMS().placement.placer.createProblem();
				if (s != null) {
					if (SETT.ROOMS().placement.placer.createProblemItem() != null) {
						state.problemGroup = SETT.ROOMS().placement.placer.createProblemItem();
						state.problemTimer = VIEW.renderSecond() + 4;
					}if (SETT.ROOMS().placement.placer.createProblemWalls()) {
						state.problemTimer = VIEW.renderSecond() + 4;
						state.problemneedDoor = true;
					}
				}else {
					CharSequence warn = SETT.ROOMS().placement.placer.createWarning();
					if (warn != null) {
						VIEW.inters().yesNo.activate(warn, create, ACTION.NOP, true);
					}else {
						if (!message && state.b.constructor().mustBeIndoors() && state.placement.placer.cost().support() > 0 ) {
							message = true;
							VIEW.inters().yesNo.activate(sexpensive, create, ACTION.NOP, true);
							
						}else
							create.exe();
					}
				}
				
			};
			
			@Override
			public void hoverInfoGet(GUI_BOX text) {
				text.title(cc);
				CharSequence s = SETT.ROOMS().placement.placer.createProblem();
				if (s != null) {
					GBox b = (GBox) text;
					b.error(s);
				}
				super.hoverInfoGet(text);
			}
			
		});

		bottomButtons.add(s);
	}
	
	GuiSection get(GuiSection s) {
		section.clear();
		if (state.collection != null)
			title.clear().add(state.collection.name());
		else
			title.clear().add(sconstruction).insert(0, state.b.info.name);
		title.toUpper();
		

		if (s.body().width() < width)
			s.pad((width-s.body().width())/2, 0);
		panel.inner().set(s);
		panel.setTitle(title);
		section.add(panel);
		s.body().centerIn(panel.inner());
		section.add(s);
		
		if (state.b.constructor().usesArea()) {
			bottomButtons.body().moveX2(section.body().x2()-60);
			bottomButtons.body().moveCY(section.body().y2());
			section.add(bottomButtons);
		}
		
		
		return section;
	}
	
}
