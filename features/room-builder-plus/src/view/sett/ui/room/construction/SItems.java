package view.sett.ui.room.construction;

import init.sprite.SPRITES;
import init.sprite.UI.Icon;
import init.sprite.UI.UI;
import settlement.room.main.furnisher.FurnisherStat;
import snake2d.KEYCODES;
import snake2d.SPRITE_RENDERER;
import snake2d.util.color.COLOR;
import snake2d.util.color.OPACITY;
import snake2d.util.gui.GUI_BOX;
import snake2d.util.gui.GuiSection;
import snake2d.util.gui.clickable.CLICKABLE;
import snake2d.util.gui.renderable.RENDEROBJ;
import snake2d.util.misc.ACTION;
import snake2d.util.sprite.SPRITE;
import util.data.GETTER;
import util.gui.misc.GBox;
import util.gui.misc.GButt;
import util.gui.misc.GHeader;
import util.gui.misc.GStat;
import util.gui.misc.GText;
import util.gui.table.GTableBuilder;
import util.gui.table.GTableBuilder.GRowBuilder;
import util.text.D;
import view.keyboard.KEYS;
import view.keyboard.KeyButt;
import view.main.VIEW;
import view.tool.PLACABLE;

final class SItems {

	private static CharSequence ¤¤Items = "¤items";
	// GOUL: plain text, not in the dictionary yet (D.ts would replace a ¤¤ field with its missing key)
	private static CharSequence sMove = "Move items";
	private static CharSequence sMoveDesc = "Drag a rectangle over items, then move them all with the mouse. Click to drop, right click to cancel. Hold Ctrl when releasing to select all items of the same types. Hold Alt (Ctrl+Alt+X, or Alt+click) to also pick up inner walls. While moving, resize, rotate and mirror the selection.";
	private static CharSequence sCopy = "Copy items";
	private static CharSequence sCopyDesc = "Drag a rectangle over items, then click to place copies of them. Right click to stop. Hold Ctrl when releasing to select all items of the same types. Hold Alt (Ctrl+Alt+C, or Alt+click) to also copy inner walls. While copying, resize, rotate and mirror the selection.";
	private static CharSequence sNudge = "Nudge room";
	private static CharSequence sNudgeDesc = "Shifts the room you're planning a few tiles, with its items and doorways: click the room, move the mouse, click to drop. Rotate and mirror it while nudging, right click to cancel. Made for small fixes, a far move doesn't fit in one undo step.";
	private static CharSequence sSelection = "Selection";
	private static CharSequence sFlipH = "Mirror left-right";
	private static CharSequence sFlipHDesc = "Mirrors the moved or copied items horizontally.";
	private static CharSequence sFlipV = "Mirror up-down";
	private static CharSequence sFlipVDesc = "Mirrors the moved or copied items vertically.";
//	private final RENDEROBJ upgrade;
	private final RENDEROBJ title = new GHeader(¤¤Items).subify();
	private final GuiSection section = new GuiSection();
	private final RENDEROBJ table;
	private final GuiSection stolen = new GuiSection();
	private final State state;
	private final IButt[] butts = new IButt[8];
	private final CLICKABLE bMove; // GOUL
	private final CLICKABLE bCopy; // GOUL
	private final CLICKABLE bNudge; // GOUL
	private final CLICKABLE bFlipH; // GOUL
	private final CLICKABLE bFlipV; // GOUL
	private final GuiSection selection = new GuiSection(); // GOUL: the selection tools, always in the same place
	private final GText selectionLabel = new GText(UI.FONT().S, sSelection).lablifySub(); // GOUL
	
	
	static {
		D.ts(SItems.class);
	}
	
	SItems(State state){
		this.state = state;
		
		GTableBuilder b = new GTableBuilder() {
			
			@Override
			public int nrOFEntries() {
				return state.b == null ? 0 : state.b.constructor().groups().size();
			}
		};
		
		b.column(null, 190, new GRowBuilder() {
			
			@Override
			public RENDEROBJ build(GETTER<Integer> ier) {
				return new IButt(state, ier);
			}
		});
		
		table = b.create(4, false);
		
		for (int i = 0; i < butts.length; i++) {
			final int k = i;
			GETTER<Integer> g = new GETTER<Integer>() {

				@Override
				public Integer get() {
					return k;
				}
				
			};
			butts[i] = new IButt(state, g);
		}
		
		bMove = moveButt(UI.icons().s.handOpen.sized(Icon.M), false, sMove, sMoveDesc); // GOUL
		bCopy = moveButt(SPRITES.icons().m.copy, true, sCopy, sCopyDesc); // GOUL
		bNudge = nudgeButt(sNudge, sNudgeDesc); // GOUL
		bFlipH = flipButt(arrows(UI.icons().s.arrow_left, UI.icons().s.arrow_right), true, sFlipH, sFlipHDesc); // GOUL
		bFlipV = flipButt(arrows(UI.icons().s.arrowUp, UI.icons().s.arrowDown), false, sFlipV, sFlipVDesc); // GOUL

//		upgrade = new GTarget(32, new GText(UI.FONT().S, DicMisc.¤¤Upgrade).lablifySub(), false, true, new INTE() {
//			
//			@Override
//			public int min() {
//				return 0;
//			}
//			
//			@Override
//			public int max() {
//				if (state.b != null)
//					return state.b.upgrades().max();
//				return 0;
//			}
//			
//			@Override
//			public int get() {
//				return CLAMP.i(state.upgrade[state.b.index()], 0, max());
//			}
//			
//			@Override
//			public void set(int t) {
//				state.upgrade[state.b.index()] = t;
//				SETT.ROOMS().placement.placer.setUpgrade(t);
//			}
//		});
//		
		
		
	}
	
	// GOUL
	// Ctrl+X / Ctrl+C by default, rebindable with the assign hotkey key while hovering the button
	private CLICKABLE moveButt(SPRITE icon, boolean copy, CharSequence title, CharSequence desc) {
		ACTION a = new ACTION() {

			@Override
			public void exe() {
				// settlement hotkeys fire anywhere in the settlement view, so only act while this panel is up
				if (VIEW.s().tools.configCurrent() != state.config || state.b == null || state.collection != null)
					return;
				if (!state.b.constructor().usesArea() || state.b.constructor().groups().size() == 0)
					return;
				VIEW.s().tools.place(state.placement.placer.mover.select(copy, state.config), state.config);
			}
		};

		CLICKABLE c = new GButt.ButtPanel(icon) {

			@Override
			protected void clickA() {
				a.exe();
			}

			@Override
			protected void renAction() {
				selectedSet(state.placement.placer.mover.isCurrent(copy));
			}
		};
		return KeyButt.wrap(a, c, KEYS.SETT(), copy ? "ROOM_ITEMS_COPY" : "ROOM_ITEMS_MOVE", title, desc, KEYCODES.KEY_LEFT_CONTROL, copy ? KEYCODES.KEY_C : KEYCODES.KEY_X);
	}

	// Ctrl+M by default. Not while refurbishing (a built room or a construction site): its walls and floor are
	// terrain, they'd stay behind
	private CLICKABLE nudgeButt(CharSequence title, CharSequence desc) {
		ACTION a = new ACTION() {

			@Override
			public void exe() {
				if (VIEW.s().tools.configCurrent() != state.config || state.b == null || state.collection != null)
					return;
				if (!state.b.constructor().usesArea() || state.refurnishing)
					return;
				VIEW.s().tools.place(state.placement.placer.mover.selectRoom(state.config), state.config);
			}
		};

		CLICKABLE c = new GButt.ButtPanel(cross()) {

			@Override
			protected void clickA() {
				a.exe();
			}

			@Override
			protected void renAction() {
				selectedSet(state.placement.placer.mover.isCurrentRoom());
			}
		};
		return KeyButt.wrap(a, c, KEYS.SETT(), "ROOM_NUDGE", title, desc, KEYCODES.KEY_LEFT_CONTROL, KEYCODES.KEY_M);
	}

	// four small arrows pointing out, the game has no move icon
	private static SPRITE cross() {
		SPRITE u = UI.icons().s.arrowUp;
		SPRITE d = UI.icons().s.arrowDown;
		SPRITE l = UI.icons().s.arrow_left;
		SPRITE ri = UI.icons().s.arrow_right;
		return new SPRITE.Imp(Icon.M, Icon.M) {

			@Override
			public void render(SPRITE_RENDERER r, int X1, int X2, int Y1, int Y2) {
				int s = (X2-X1)*10/24;
				int c = (X2-X1-s)/2;
				u.render(r, X1+c, X1+c+s, Y1, Y1+s);
				d.render(r, X1+c, X1+c+s, Y2-s, Y2);
				l.render(r, X1, X1+s, Y1+c, Y1+c+s);
				ri.render(r, X2-s, X2, Y1+c, Y1+c+s);
			}
		};
	}

	// H / V by default, only does something while items are carried
	private CLICKABLE flipButt(SPRITE icon, boolean horizontal, CharSequence title, CharSequence desc) {
		ACTION a = new ACTION() {

			@Override
			public void exe() {
				if (VIEW.s().tools.configCurrent() == state.config)
					state.placement.placer.mover.flip(horizontal);
			}
		};

		CLICKABLE c = new GButt.ButtPanel(icon) {

			@Override
			protected void clickA() {
				a.exe();
			}

			@Override
			protected void renAction() {
				activeSet(state.placement.placer.mover.isCarrying() && state.placement.placer.mover.cantFlip(horizontal) == null);
			}

			@Override
			public void hoverInfoGet(GUI_BOX text) {
				CharSequence cant = state.placement.placer.mover.cantFlip(horizontal);
				if (cant != null)
					((GBox) text).error(cant);
			}
		};
		return KeyButt.wrap(a, c, KEYS.SETT(), horizontal ? "ROOM_ITEMS_FLIP_H" : "ROOM_ITEMS_FLIP_V", title, desc, -1, horizontal ? KEYCODES.KEY_H : KEYCODES.KEY_V);
	}

	// GOUL: rebuilt with the panel, Nudge room is left out while refurbishing
	private void buildSelection() {
		selection.clear();
		selection.addRightC(0, selectionLabel);
		selection.addRightC(8, bMove);
		selection.addRightC(2, bCopy);
		if (!state.refurnishing)
			selection.addRightC(2, bNudge);
		selection.addRightC(12, bFlipH);
		selection.addRightC(0, bFlipV);
	}

	// two small arrows side by side, the game has no mirror icon
	private static SPRITE arrows(SPRITE a, SPRITE b) {
		return new SPRITE.Imp(a.width()+b.width(), Math.max(a.height(), b.height())) {

			@Override
			public void render(SPRITE_RENDERER r, int X1, int X2, int Y1, int Y2) {
				a.render(r, X1, Y1);
				b.render(r, X1+a.width(), Y1);
			}
		};
	}

	// GOUL: the item tool buttons (and the remove toggle) stay while removing items, like SShape does for the area
	private boolean itemToolActive() {
		PLACABLE p = state.placement.placer.itemPlacerCurrent();
		if (p == null)
			return false;
		return VIEW.s().tools.is(p) || (p.getUndo() != null && VIEW.s().tools.is(p.getUndo()));
	}

	GuiSection get() {
		section.clear();
		
		section.addRightC(0, title);
		stolen.clear();
		stolen.body().setDim(1, 32);
		if (itemToolActive() || state.placement.placer.mover.isCarrying()) // GOUL: was VIEW.s().tools.is(state.placement.placer.itemPlacerCurrent()). While carrying, they size and turn the selection
			VIEW.s().tools.placer.stealButtons(stolen, true);
		section.addRightC(8, stolen);
		buildSelection(); // GOUL
		section.add(selection, 0, section.getLastY2()+4); // GOUL
		section.add(table, 0, section.getLastY2()+4);
		
//		if (state.upgradeMax[state.b.index()] > 0) {
//			section.addRelBody(2, DIR.N, upgrade);
//		}
		
		return section;
	}
	
	GuiSection getFlat() {
		section.clear();
		section.addRightC(0, title);
		stolen.clear();
		stolen.body().setDim(1, 32);
		if (itemToolActive()) // GOUL: was VIEW.s().tools.is(state.placement.placer.itemPlacerCurrent())
			VIEW.s().tools.placer.stealButtons(stolen, true);
		section.addRightC(8, stolen);
		int y1 = section.getLastY2()+4;
		for (int i = 0; i < state.b.constructor().groups().size(); i++) {
			section.add(butts[i], (i%2)*butts[0].body.width(), y1 + (i/2)*butts[0].body.height());
		}
//		if (state.upgradeMax[state.b.index()] > 0) {
//			section.addRelBody(2, DIR.N, upgrade);
//		}
		return section;
	}
	
	GuiSection getSingle() {
		section.clear();
		section.addRightC(0, title);
		stolen.clear();
		stolen.body().setDim(1, 32);
		if (itemToolActive()) // GOUL: was VIEW.s().tools.is(state.placement.placer.itemPlacerCurrent())
			VIEW.s().tools.placer.stealButtons(stolen, true);
		section.addRightC(8, stolen);
//		if (state.upgradeMax[state.b.index()] > 0) {
//			section.addRelBody(2, DIR.N, upgrade);
//		}
		return section;
	}

	
	static class IButt extends GButt.ButtPanel {
		
		private final GETTER<Integer> ier;
		private final State state;
		
		IButt(State state, GETTER<Integer> ier){
			super(new GStat() {
				
				@Override
				public void update(GText text) {
					text.lablify().add(state.b.constructor().groups().get(ier.get()).name());
				}
			});
			this.state = state;
			this.ier = ier;
			setDim(190, 24);
		}
		
		@Override
		protected void renAction() {
			selectedSet(VIEW.s().tools.placer.getCurrent() == state.placement.placer.itemPlacerCurrent() && state.item() == ier.get());
			if (state.placement.placer.mover.carriedGroup() == state.b.constructor().groups().get(ier.get())) // GOUL: the carried item type
				selectedSet(true);
		}
		
		@Override
		protected void render(SPRITE_RENDERER r, float ds, boolean isActive, boolean isSelected, boolean isHovered) {
			super.render(r, ds, isActive, isSelected, isHovered);
			if (state.problemGroup == state.b.constructor().groups().get(ier.get()) && state.problemTimer > VIEW.renderSecond()) {
				COLOR.RED100.renderFrame(r, body, 2, 3);
				OPACITY.O25To50.bind();
				COLOR.RED100.render(r, body);
				OPACITY.unbind();
			}
		}
		
		@Override
		protected void clickA() {
			if (state.placement.placer.item(ier.get()) == null)
				return;
				
			state.setItem(ier.get());
			VIEW.s().tools.place(state.placement.placer.item(ier.get()), state.config);
		}
		
		@Override
		public void hoverInfoGet(GUI_BOX text) {
			text.title(state.b.constructor().groups().get(ier.get()).name());
			text.text(state.b.constructor().groups().get(ier.get()).desc());
			
			GBox b = (GBox) text;
			b.NL(8);
			for (FurnisherStat s : state.b.constructor().stats()) {
				double d = state.b.constructor().groups().get(ier.get()).stat(s.index());
				if (d < 0) {
					b.error(s.name());
					b.tab(6);
					b.add(SPRITES.icons().m.minus);
				}else if (d > 0) {
					b.text(s.name());
					b.tab(6);
					b.add(SPRITES.icons().m.plus);
				}
				b.NL();
					
			}
			
		}
		
	}
	
	
}
