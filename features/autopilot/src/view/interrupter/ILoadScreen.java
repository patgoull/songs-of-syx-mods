package view.interrupter;

import init.sprite.SPRITES;
import snake2d.MButt;
import snake2d.Renderer;
import snake2d.util.datatypes.COORDINATE;
import util.gui.misc.GBox;
import util.text.D;
import view.keyboard.KEYS;

public class ILoadScreen extends Interrupter{

	private static CharSequence ¤¤clickToContinue = "¤CLICK TO CONTINUE!";
	static {
		D.ts(ILoadScreen.class);
	}
	private final InterManager m;
	
	public ILoadScreen(InterManager manager) {
		super("GAME_LOADED_FINISH");
		pin();
		this.m = manager;
	}
	
	public void activate(){
		super.show(m);
		
	}

	public void deactivate() {
		hide();
	}
	
	@Override
	protected void hoverTimer(GBox text) {
		
	}

	@Override
	protected boolean render(Renderer r, float ds) {
		
		SPRITES.loader().render(¤¤clickToContinue, true);
		return false;
	}


	@Override
	protected void mouseClick(MButt button) {
		deactivate();
	}

	@Override
	protected boolean hover(COORDINATE mCoo, boolean mouseHasMoved) {
		return true;
	}

	@Override
	protected boolean update(float ds) {
		
		if (System.getProperty("goul.autopilot") != null) // GOUL: test only, the autopilot doesn't click
			deactivate();
		if (MButt.LEFT.consumeClick())
			deactivate();
		
		if (MButt.RIGHT.consumeClick())
			deactivate();
		
		if (KEYS.anyDown())
			deactivate();
		return false;
	}
	
}
