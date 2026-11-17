package util.rendering;

import java.io.IOException;

import game.GAME;
import snake2d.SPRITE_RENDERER;
import snake2d.util.color.COLOR;
import snake2d.util.datatypes.RECTANGLE;
import snake2d.util.file.Alloc;
import snake2d.util.rnd.RND;
import snake2d.util.sprite.TILE_SHEET;
import snake2d.util.sprite.TextureCoords;
import util.spritecomposer.ComposerDests;
import util.spritecomposer.ComposerSources;
import util.spritecomposer.ComposerThings;
import util.spritecomposer.ComposerUtil;

public final class Minimap{

	private final static double grit = 0.075;
	private boolean open = true;
	
	private final int TILESIZE = 32;
	private final int DIM_PIXEL;
	private final int DIM_TILES;
	private final TILE_SHEET sheet;
	
	public Minimap(int dim) throws IOException{
		DIM_PIXEL = dim;
		DIM_TILES = DIM_PIXEL/TILESIZE;
		
		sheet = new ComposerThings.ITileSheet() {
			
			@Override
			protected TILE_SHEET init(ComposerUtil c, ComposerSources s, ComposerDests d) {
				d.s32.skipNPaint(DIM_TILES*DIM_TILES);
				return d.s32.save(1);
			}
		}.get();
	}
	
	public void setOpen(boolean o) {
		this.open = o;
	}
	
	public void putPixel(int x, int y, COLOR col){
		if (open) {
			int tile = (x/TILESIZE) + (y/TILESIZE)*DIM_TILES;
			int dx = x % TILESIZE;
			int dy = y % TILESIZE;
			TextureCoords c = sheet.getTexture(tile);
			GAME.texture().putPixel(c.x1+dx, c.y1+dy, getC(col.red()), getC(col.green()), getC(col.blue()));
		}
			
	}
	
	public void putPixels(byte[] pixels){
		
		int s = TILESIZE*TILESIZE;
		byte[] tmp = Alloc.bb(s*4);
		// GOUL: the tiles are collected and uploaded in one GL job at the end
		int tiles = (DIM_PIXEL+TILESIZE-1)/TILESIZE;
		int[] bx = new int[tiles*tiles];
		int[] by = new int[tiles*tiles];
		byte[] all = new byte[tiles*tiles*s*4];
		int bi = 0;
		
		for (int py = 0; py < DIM_PIXEL; py+=TILESIZE) {
			for (int px = 0; px < DIM_PIXEL; px+=TILESIZE) {
				int tile = px/TILESIZE + (py/TILESIZE)*DIM_TILES;
				TextureCoords c = sheet.getTexture(tile);
				for (int y = 0; y < TILESIZE; y++) {
					for (int x = 0; x <TILESIZE; x++) {
						int to = (x+y*TILESIZE)*4;
						int from = ((py+y)*DIM_PIXEL + px+x)*4;
						for (int j = 0; j < 3; j++) {
							tmp[to+j] = getC(pixels[from+j]);
						}
						tmp[to+3] = pixels[from+3];
					}
				}
				// GOUL: was GAME.texture().putPixelBatch(c.x1, c.y1, TILESIZE, tmp), which waited for the GL thread once per tile
				bx[bi] = c.x1;
				by[bi] = c.y1;
				System.arraycopy(tmp, 0, all, bi*s*4, s*4);
				bi++;
			}
		}
		GAME.texture().putPixelBatches(bx, by, TILESIZE, all); // GOUL
		
	}
	
	public static byte getC(byte c) {
		int res = Byte.toUnsignedInt((byte) (c*2));
		int gmax = (int) (res * grit);
		int g = (int) (-gmax + RND.rFloat()*2*gmax);
		res += g;
		if (res > 255)
			res = 255;
		return (byte) res;
	}
	
	public void flush(){
		
	}

	private final TextureCoords texture = new TextureCoords();
	
	

	
	public void render(SPRITE_RENDERER r, double px1d, double py1d, int sx1, int sy1, int swidth, int sheight, double scale) {
		
		if (px1d < 0) {
			double d = px1d*scale;
			px1d = 0;
			sx1 -= d;
			swidth += d;
		}
		
		if (py1d < 0) {
			double d = py1d*scale;
			py1d = 0;
			sy1 -= d;
			sheight += d;
		}
		
		
		int px1 = (int) px1d;
		int py1 = (int) py1d;
		{
			int dx = (int) Math.ceil((px1d-px1)*scale);
			int dy = (int) Math.ceil((py1d-py1)*scale);
			sx1 -= dx;
			sy1 -= dy;
			swidth+=dx;
			sheight+=dy;
		}
		
		while(sheight > 0) {
			
			int ph = TILESIZE;
			ph -= (py1%TILESIZE);
			if (py1 + ph > DIM_PIXEL) {
				ph = DIM_PIXEL-py1;
				if (ph <= 0)
					return;
			}
				
			
			int th = (int) (ph*scale);
			
			
			if (th > sheight) {
				ph = (int) Math.ceil(sheight/scale);
				th = (int) (ph*scale);
			}	
			
			int px = px1;
			int x = sx1;
			int w = swidth;
			while(w > 0) {
				int pw = TILESIZE;
				pw -= (px%TILESIZE);
				
				if (px + pw > DIM_PIXEL) {
					pw = DIM_PIXEL-px;
					if (pw <= 0)
						break;
				}
				
				int tw = (int) (pw*scale);
				
				
				if (tw > w) {
					pw = (int) Math.ceil(w/scale);
					tw = (int) (pw*scale);
				}	
				
				TextureCoords c = get(px, py1, pw, ph);
				r.renderSprite(x, x+tw, sy1, sy1+th, c);
				w-= tw;
				x += tw;
				px += pw;
			}
			
			sheight-= th;
			sy1 += th;
			py1 += ph;
		}
		
		
	}
	
	
	
	
	
//	private boolean has(int px1, int py1) {
//		if (px1 < 0)
//			return false;
//		if (py1 < 0)
//			return false;
//		int tx = (px1/TILESIZE);
//		int ty = (py1/TILESIZE);
//		if (tx < 0 || ty < 0 || tx >= DIM_TILES || ty >= DIM_TILES)
//			return false;
//		return true;
//	}
	
	private TextureCoords get(int px1, int py1, int w, int h) {
		int tile = (px1/TILESIZE) + (py1/TILESIZE)*DIM_TILES;
		int dx = px1 % TILESIZE;
		int dy = py1 % TILESIZE;
		TextureCoords c = sheet.getTexture(tile);
		
		
		texture.get(c.x1+dx, c.y1+dy, w, h);
		
		return texture;
	}
	
	public TextureCoords texture(int tx, int ty, int w, int h) {
		int tile = (tx/TILESIZE) + (ty/TILESIZE)*DIM_TILES;
		int dx = tx % TILESIZE;
		int dy = ty % TILESIZE;
		TextureCoords c = sheet.getTexture(tile);
		texture.get(c.x1+dx, c.y1+dy, w, h);
		
		return texture;
	}
	
	public void render(SPRITE_RENDERER r, int x1, int y1, RECTANGLE quad){
		COLOR.WHITE10.render(r, x1, x1+quad.width(),  y1,  y1+ quad.height());
		render(r, quad.x1(), quad.y1(), x1, y1, quad.width(), quad.height(), 1);
		
	}
	
	public void render(SPRITE_RENDERER r, int x1, int y1){
		COLOR.WHITE10.render(r, x1, x1+width(),  y1,  y1+height());
		render(r, 0, 0, x1, y1, width(), height(), 1);
		
	}
	
	public int width() {
		return DIM_PIXEL;
	}

	public int height() {
		return DIM_PIXEL;
	}
	
}
