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
	// GOUL
	private static CharSequence ¤¤Move = "Move items";
	private static CharSequence ¤¤MoveDesc = "Drag a rectangle over items, then move them all with the mouse. Click to drop, right click to cancel.";
	private static CharSequence ¤¤Copy = "Copy items";
	private static CharSequence ¤¤CopyDesc = "Drag a rectangle over items, then click to place copies of them. Right click to stop.";
//	private final RENDEROBJ upgrade;
	private final RENDEROBJ title = new GHeader(¤¤Items).subify();
	private final GuiSection section = new GuiSection();
	private final RENDEROBJ table;
	private final GuiSection stolen = new GuiSection();
	private final State state;
	private final IButt[] butts = new IButt[8];
	private final CLICKABLE bMove; // GOUL
	private final CLICKABLE bCopy; // GOUL
	
	
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
		
		bMove = moveButt(UI.icons().s.handOpen.sized(Icon.M), false, ¤¤Move, ¤¤MoveDesc); // GOUL
		bCopy = moveButt(SPRITES.icons().m.copy, true, ¤¤Copy, ¤¤CopyDesc); // GOUL

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
		section.addRightC(8, bMove); // GOUL
		section.addRightC(2, bCopy); // GOUL
		stolen.clear();
		stolen.body().setDim(1, 32);
		if (itemToolActive()) // GOUL: was VIEW.s().tools.is(state.placement.placer.itemPlacerCurrent())
			VIEW.s().tools.placer.stealButtons(stolen, true);
		section.addRightC(8, stolen);
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
