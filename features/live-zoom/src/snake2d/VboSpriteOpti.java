package snake2d;

import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.GL_SHORT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_SHORT;
import static org.lwjgl.opengl.GL20.glUseProgram;

import java.nio.IntBuffer;

import snake2d.util.color.COLOR;
import snake2d.util.color.OPACITY;
import snake2d.util.file.Alloc;
import snake2d.util.sprite.TextureCoords;

final class VboSpriteOpti extends VboAbsExt{


	private final Shader shader;
	private final int[] opti;
	private int optiI;
	private final IntBuffer sBuff;

	static VboSpriteOpti getDebug(SETTINGS sett) {
		Shader shader = new Shader(sett.getCanvasWidth(), sett.getCanvasHeight(), "SpritePoint", "SpritePoint", "SpritePoint_debug");
		shader.setUniform1i("u_texture", 0);
		return new VboSpriteOpti(shader);
	}

	static VboSpriteOpti getDeffered(SETTINGS sett) {
		Shader shader = new Shader(sett.getCanvasWidth(), sett.getCanvasHeight(), "SpritePoint", "SpritePoint", "SpritePoint");
		shader.setUniform1i("sampler1", 0);
		shader.setUniform1i("sampler2", 1);
		return new VboSpriteOpti(shader);
	}

	public VboSpriteOpti(Shader shader) {
		super( GL_POINTS,
				1<<18, // GOUL: was 1<<17, a full screen city at the furthest live zoom needs ~170k sprites
				new VboAttribute(2, GL_SHORT, false, 2), // position upper left
				new VboAttribute(2, GL_SHORT, false, 2), // position lower right
				new VboAttribute(2, GL_UNSIGNED_SHORT, 2), // texture coords1
				new VboAttribute(2, GL_UNSIGNED_SHORT, 2), // texture coords2
				new VboAttribute(2, GL_UNSIGNED_SHORT, 2), // texture coords width
				new VboAttribute(4, GL_UNSIGNED_BYTE, true, 1) // color
		);
		this.shader = shader;
		opti = Alloc.ii(MAX_ELEMENTS*6);
		sBuff = buffer.asIntBuffer();
	}

	int setNew() {
		vTo[current] = count;
		current++;
		vFrom[current] = count;
		return current;
	}

	void flush() {
		
		if (count == 0) {
			clear();
			return;
		}
		
		int off = 0;
		bind();
		shader.bind();
		vTo[current] = count;
		while(count > 0) {
			int am = count;
			if (am > 0x01000)
				am = 0x01000;
			
			sBuff.put(opti, off*6, am*6);
			buffer.position(sBuff.position()*4);
			sBuff.clear();
			optiI = 0;
			upload();
			shader.bind();
			
			
			for (int i = 0; i <= current; i++) {
				int f = vFrom[i]-off;
				int t = vTo[i]-off;
				if (t < 0)
					continue;
				if (f < 0)
					f = 0;
				if (t > am) // GOUL: only this chunk's sprites are uploaded, and the point index buffer has 1<<16
					t = am;
				if (t <= f)
					continue;
				
					GlHelper.Stencil.setLEQUALreplaceOnPass(i);
				flush(f, t);
				
			}
			off+= am;
			count -= am;
		}
		
		clear();
		glUseProgram(0);
	}
	
	@Override
	public void clear() {
		optiI = 0;
		super.clear();
	}

	final void render(TextureCoords t, int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4, COLOR color,
			OPACITY opacity) {
		
		render(t, t, x1, x2, y1, y2, color, opacity);
	}
	
	final void render(TextureCoords t, TextureCoords to, int x1, int x2, int y1, int y2, COLOR color, OPACITY opacity) {
		if (count >= MAX_ELEMENTS) {
			return;
		}

		
		opti[optiI] = ((y1) << 16) | ((x1 & 0x0FFFF));
		opti[optiI+1] = ((y2) << 16) | ((x2 & 0x0FFFF));
		
		opti[optiI+2] = ((t.y1) << 16) | ((t.x1));
		opti[optiI+3] = ((to.y1) << 16) | ((to.x1));
		
		opti[optiI+4] = ((t.y2-t.y1) << 16) | ((t.x2-t.x1));
		
		opti[optiI+5] = (((opacity.get()) << 24) | ((color.blue()&0x0FF) << 16) | ((color.green()&0x0FF) <<8) | ((color.red()&0x0FF)));
		
		
		optiI += 6;
		count++;
	}

	@Override
	public void dis() {
		shader.dis();
		super.dis();
	}

}
