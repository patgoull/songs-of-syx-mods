package init.sprite;

import game.time.TIME;
import init.constant.C;
import init.paths.PATHS;
import init.sprite.UI.UI;
import snake2d.CORE;
import snake2d.LOG;
import snake2d.SPRITE_RENDERER;
import snake2d.util.color.COLOR;
import snake2d.util.color.OPACITY;
import snake2d.util.file.Json;
import snake2d.util.gui.GuiSection;
import snake2d.util.gui.renderable.RENDEROBJ;
import snake2d.util.light.AmbientLight;
import snake2d.util.light.Fire;
import snake2d.util.rnd.RND;
import snake2d.util.sprite.text.Str;
import snake2d.util.sprite.text.Text;
import util.colors.GCOLOR;
import util.gui.misc.GText;
import util.gui.misc.GTextR;
import util.rendering.ShadowBatch;
import util.text.Dic;
import view.main.VIEW;

public class RLoadPrinter {

	private final GuiSection section = new GuiSection();
	private final RENDEROBJ.Sprite bg = new RENDEROBJ.Sprite();
	private final GTextR info = new GTextR(UI.FONT().S, 200);
	
	private final Quote quote = new Quote();
	
	private final Fire torch1 = new Fire(5);
	private final Fire torch2 = new Fire(4);
	
	private final AmbientLight light = new AmbientLight();
	private long lastInit = 0;
	
	private boolean minified = false;
	
	private final GText miniThing = new GText(UI.FONT().H1, 24);
	private final Str regularThing = new Str(24);
	private CharSequence miniText;
	private int miniI;
	private long miniLast; // GOUL: time of the last minified print
	private int regularI;
	private final int miniD;
	private final RENDEROBJ loadingBig = UI.decor().getDecored(Dic.¤¤loading);
	RLoadPrinter(){
		
		section.add(UI.decor().frame(quote.body()));
		
		{
			RENDEROBJ r = UI.decor().getDecored("SONGS OF SYX");
			r.body().centerX(section);
			r.body().moveY2(section.body().y1());
			section.add(r);
		}
		
		
		section.add(quote);
		section.body().centerIn(C.DIM());
		
		bg.setSprite(SPRITES.loadScreen());
		bg.body().centerIn(C.DIM());
		
		

		
		torch1.setRadius(1000);
		torch1.set(bg.body().x1()-100, C.HEIGHT()/2);
		torch1.setFalloff(3f);
		torch1.setFlickerFactor(25f);
		torch1.setZ(25);
		torch2.setFalloff(3f);
		torch2.setRadius(500);
		torch2.set(bg.body().x2() + 100, C.HEIGHT()/2);;
		torch2.setFlickerFactor(10f);
		torch2.setZ(25);
		
		torch1.flicker(1f);
		torch2.flicker(1f);
		
		light.setDir(220);
		light.setTilt(25);
		light.g(1.3f);
		light.b(1.3f);
		light.r(1.3f);
		
		init();

		miniThing.lablify();
		miniThing.add('.').add('.').add('.');
		miniD = miniThing.adjustWidth().width();
	}
	
	public void minify(boolean minify, CharSequence title) {
		minified = minify;
		miniText = title;
		miniI = 0;
		miniLast = System.nanoTime() - 1000000000L; // GOUL: the first print draws
		
	}
	
	public void print(CharSequence string){
		if (minified) {
			// GOUL: each print draws the whole game and waits for the screen, and a save prints 31 times: that was a third
			// of the save. Only the dots move, so drawing at most every 100 ms looks the same.
			long now = System.nanoTime();
			if (now - miniLast < 100000000L)
				return;
			miniLast = now;
			
			CORE.renderer().clear();
			TIME.light().applyGuiLight(0, C.DIM());
			
			miniThing.set(miniText);
			int wi = miniThing.width();
			for (int i = 0; i < (miniI/4)%4; i++) {
				miniThing.add('.');
			}
			miniI++;
			int x1 = C.DIM().cX()-wi/2;
			
			UI.PANEL().titleBoxes[UI.PANEL().titleBoxes.length-1].renderCY(CORE.renderer(), x1, C.DIM().cY(), wi+miniD);
			miniThing.renderCY(CORE.renderer(), x1,  C.DIM().cY());

			CORE.renderer().newLayer(false, 0);
			VIEW.render();
			CORE.swapAndPoll();
			return;
		}
		
		regularThing.clear().add(string);
		for (int i = 0; i < (regularI/4)%4; i++) {
			regularThing.add('.');
		}
		regularI++;
		
		render(regularThing, false);
		
		CORE.swapAndPoll();
	}
	
	public void printempty() {

		CORE.renderer().clear();
		CORE.renderer().shadeLight(true);
		CORE.renderer().shadowDepthSet((byte) 0);
		shadow.init(1, 0.5, 0.5);
		shadow.setDistance2Ground(2).setHeight(24);
		light.register(C.DIM());
		
		loadingBig.body().centerIn(C.DIM());
		loadingBig.render(shadow, lastInit);
		loadingBig.render(CORE.renderer(), lastInit);
		
		CORE.renderer().newLayer(false, 0);
		byte none = 0;
		byte full = -1;
		CORE.renderer().registerLight(torch1, bg.body().x1(), bg.body().x1()+100, bg.body().y1(), bg.body().y2(), full, full, none, none);
		CORE.renderer().registerLight(torch1, bg.body().x1()+100, bg.body().x2(), bg.body().y1(), bg.body().y2(), full, full, full, full);
		
		CORE.renderer().registerLight(torch2, bg.body().x2()-100, bg.body().x2(), bg.body().y1(), bg.body().y2(), none, none, full, full);
		CORE.renderer().registerLight(torch2, bg.body().x1(), bg.body().x2()-100, bg.body().y1(), bg.body().y2(), full, full, full, full);
		
		bg.render(CORE.renderer(), 0);
		
		OPACITY.O50.bind();
		COLOR.BLACK.render(CORE.renderer(), quote.body(),  16);
		OPACITY.unbind();
		CORE.swapAndPoll();
	}
	
	private final ShadowBatch.Real shadow = new ShadowBatch.Real();
	{
		shadow.setDistance2GroundUI(12);
	}
	
	public void render(CharSequence string, boolean flash){
		if (string == null)
			return;
		
		
		CORE.renderer().clear();
		CORE.renderer().shadeLight(true);
		CORE.renderer().shadowDepthSet((byte) 0);
		shadow.init(1, 0.5, 0.5);
		shadow.setDistance2Ground(2).setHeight(24);
		light.register(C.DIM());
		
		info.text().clear().set(string);
		info.body().centerX(C.DIM());
		info.body().moveY1(section.body().y2()+50);
		
		if (flash)
			info.text().color(COLOR.WHITE2WHITE);
		else
			info.text().normalify();
		info.render(CORE.renderer(), 0);
		info.render(shadow, 0);
		
		shadow.setDistance2Ground(2).setHeight(24);
		section.render(CORE.renderer(), 0);
		section.render(shadow, 0);
		
		CORE.renderer().newLayer(false, 0);
		byte none = 0;
		byte full = -1;
		CORE.renderer().registerLight(torch1, bg.body().x1(), bg.body().x1()+100, bg.body().y1(), bg.body().y2(), full, full, none, none);
		CORE.renderer().registerLight(torch1, bg.body().x1()+100, bg.body().x2(), bg.body().y1(), bg.body().y2(), full, full, full, full);
		
		CORE.renderer().registerLight(torch2, bg.body().x2()-100, bg.body().x2(), bg.body().y1(), bg.body().y2(), none, none, full, full);
		CORE.renderer().registerLight(torch2, bg.body().x1(), bg.body().x2()-100, bg.body().y1(), bg.body().y2(), full, full, full, full);
		
		bg.render(CORE.renderer(), 0);
		
		OPACITY.O50.bind();
		COLOR.BLACK.render(CORE.renderer(), quote.body(),  16);
		OPACITY.unbind();
		
	}
	
	public void init() {
		if (System.currentTimeMillis()-lastInit < 10000)
			return;
		lastInit = System.currentTimeMillis();
		quote.set();
		bg.body().centerIn(C.DIM());
	}
	
	private final class Quote extends RENDEROBJ.RenderImp{
		
		private final Text quote = new Text(UI.FONT().M, 400);
		private final Text author = new Text(UI.FONT().H2, 400);
		
		private final String[] quotes;
		private final String[] authors;
		
		Quote(){
			body.setWidth(2*C.MIN_WIDTH/3);
			body.setHeight(180);
			quote.setMaxWidth(body.width());
			author.setMaxWidth(body.width());
			Json json = new Json(PATHS.TEXT_MISC().gets("Quotes"));
			String[] qs = json.texts("QUOTES");
			if (qs.length == 0)
				json.error("Insufficient quotes. Need at least one", "QUOTES");
			int l = qs.length;
			quotes = new String[l];
			authors = new String[l];
			for (int i = 0; i < l; i++) {
				String[] q = qs[i].split(":::");
				if (q.length != 2) {
					q = qs[i].split("::");
				}
				if (q.length != 2) {
					q = qs[i].split("::");
					LOG.ln("unable to parse " + qs[i]);
					//json.error("Unable to parse quote. Seperate quote and author with ':::'", "QUOTES");
					quotes[i] = "";
					authors[i] = "";
				}else {
					
					quotes[i] = q[0];
					
					authors[i] = q[1];
				}
				
				
			}
		}
		
		void set() {

			int i = RND.rInt(quotes.length);
			quote.set(quotes[i]);
			author.set(authors[i]); 
			
		}
		
		@Override
		public void render(SPRITE_RENDERER r, float ds) {
			
			int x1 = body.x1() + (body().width()-quote.width())/2;
			int y1 = body.y1() + (body.height()-(quote.height() + author.height() + 10))/2;
			
			
			quote.render(r, x1, y1);
			y1 += quote.height();
			
			int dx = (body().width()-author.width());
			if (dx > 30)
				dx -= 30;
			
			x1 =  body.x1() + dx;
			
			GCOLOR.T().H2.bind();
			author.render(r, x1, y1+10);
			COLOR.unbind();
		}
		
		
	}

	public boolean isMini() {
		return minified;
	}
	
}
