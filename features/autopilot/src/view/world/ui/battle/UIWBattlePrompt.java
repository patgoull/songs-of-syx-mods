package view.world.ui.battle;

import game.GAME;
import init.constant.C;
import init.race.RACES;
import init.trade.TR;
import snake2d.LOG;
import snake2d.MButt;
import snake2d.Renderer;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.gui.GuiSection;
import snake2d.util.misc.ACTION;
import util.gui.misc.GBox;
import util.gui.panel.GPanel;
import view.interrupter.Interrupter;
import view.main.VIEW;
import world.battle.spec.BATTLE_RESULT;
import world.battle.spec.WBattleResult;
import world.battle.spec.WBattleSiege;
import world.battle.spec.WBattleSpec;

public class UIWBattlePrompt {
	
	private final Inter inter = new Inter();
	private final Battle battleBattle = new BattleBattle(inter);
	private final Battle battleAssist = new BattleAssist(inter);
	private final Battle battleLastStand = new BattleLastStand(inter);
	private final Battle battleSally = new BattleSally(inter);
	private final BattleSiege siege = new BattleSiege(inter);
	
	public UIWBattlePrompt(){

	}
	
	public boolean isBusty() {
		return inter.isActivated() || goul.autopilot.AutoChoice.busy(); // GOUL: was inter.isActivated()
	}
	
	public void battle(WBattleSpec battle) {
		prompt(battle, battleBattle);
	}
	
	public void assist(WBattleSpec battle) {
		prompt(battle, battleAssist);
	}
	
	public void lastStand(WBattleSpec battle) {
		prompt(battle, battleLastStand);
	}
	
	public void battleSally(WBattleSpec battle) {
		prompt(battle, battleSally);
	}
	
	private void prompt(WBattleSpec battle, Battle b) {
		if (inter.isActivated()) {
			LOG.err("Oh no!");
			return;
		}
		
		if (goul.autopilot.AutoChoice.later("battle popup, auto-resolved", () -> battle.auto())) // GOUL
			return;
		
		b.get(battle);
		
		
		inter.set(b.get(battle), true, battle.player.coo().x(), battle.player.coo().y());
	}
	
	public void siege(WBattleSiege siege) {
		if (inter.isActivated()) {
			LOG.err("Oh no!");
			return;
		}
		if (goul.autopilot.AutoChoice.later("siege popup, retreated", () -> siege.retreat())) // GOUL
			return;
		inter.set(this.siege.getS(siege), true, siege.besiged.cx(), siege.besiged.cy());
		
	}
	
	public void result(WBattleSiege.Result siege) {
		if (goul.autopilot.AutoChoice.later("conquer popup, abandoned", () -> siege.abandon(0, 0, new int[RACES.all().size()], new int[TR.ALL().size()]))) // GOUL
			return;
		inter.set(new Conquer(inter, siege), false, siege.besiged.cx(), siege.besiged.cy());
	}
	
	public void result(WBattleResult battle, boolean retreat) {
		if (goul.autopilot.AutoChoice.later("battle result " + battle.result + ", accepted", () -> { // GOUL: no slaves or spoils taken
			if (battle.result == BATTLE_RESULT.VICTORY)
				battle.accept(new int[battle.capturedRaces.length], new int[battle.lostResources.length]);
			else
				battle.accept(null, null);
		}))
			return;
		inter.set(new Res(inter, battle, retreat), false,  battle.player.coo().x(), battle.player.coo().y());
	}
	
	private static class Inter extends Interrupter implements ACTION{
		
		private GuiSection s;
		private boolean canSave;
		private final GPanel panel = new GPanel();
		
		Inter(){
			super("WORLD_BATTLE_PROMPT");
			pin();
			persistantSet();
			panel.setBig();
		}
		
		@Override
		protected boolean hover(COORDINATE mCoo, boolean mouseHasMoved) {
			s.hover(mCoo);
			panel.hover(mCoo);
			return true;
		}
		
		void set(GuiSection s, boolean canSave, int cx, int cy) {
			if (isActivated())
				throw new RuntimeException();
			
			VIEW.world().uiManager.clear();
			VIEW.world().panels.clear();
			this.s = s;
			this.canSave = canSave;
			panel.inner().set(s);
			panel.inner().centerIn(C.DIM());
			panel.inner().moveY2(C.HEIGHT()-100);
			
			s.body().centerIn(panel.inner());
			VIEW.world().activate();
			VIEW.world().window.setZoomout(0);
			VIEW.world().window.centererTile.set(cx, cy+200/C.TILE_SIZE);
			show(VIEW.inters().manager);
		}

		@Override
		protected void mouseClick(MButt button) {
			if (button == MButt.LEFT)
				s.click();
			
		}

		@Override
		protected void hoverTimer(GBox text) {
			s.hoverInfoGet(text);
		}

		@Override
		protected boolean render(Renderer r, float ds) {
			panel.render(r, ds);
			s.render(r, ds);
			return true;
		}

		@Override
		protected boolean update(float ds) {
			GAME.SPEED.tmpPause();
			return false;
		}


		@Override
		public boolean canSave() {
			return canSave;
		}

		@Override
		public void exe() {
			hide();
		}

		
	}
}
