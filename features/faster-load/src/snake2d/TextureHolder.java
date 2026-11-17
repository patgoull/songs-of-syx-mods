package snake2d;

import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import snake2d.util.color.COLOR;
import snake2d.util.color.ColorImp;
import snake2d.util.file.SnakeImage;

/**
 * A spritesheet
 * @author mail__000
 *
 */
public class TextureHolder extends CORE_RESOURCE{

	public final int pixelWidth;
	public final int pixelHeight;
	
	private final Texture texture;
	private final Texture normalTexture;
	private final VboParticles pixels;
	private final int FBO;
	private TextureHolderChunk chunk;
	
	/**
	 * 
	 * @param diffusePath the path to the png image
	 */
	public TextureHolder(SnakeImage diffuse, SnakeImage normal, int x1, int y1, int w, int h){
		
		if (!CORE.isGLThread())
			throw new RuntimeException();
		GlHelper.checkErrors();
		texture = new Texture(diffuse, true);
		
		if (normal != null)
			normalTexture = Texture.normal(normal, true);
		else
			normalTexture = null;
		
		pixelWidth = texture.width; 
		pixelHeight = texture.height;
		
		texture.bind();
		if (normalTexture != null)
			normalTexture.bind();
		
		CORE.addDisposable(this);
		ColorImp.setSPRITE(x1, y1, w, h);
		
		pixels = VboParticles.getForTexture(pixelWidth, pixelHeight);
		
		FBO = glGenFramebuffers();
		
		glBindFramebuffer(GL_FRAMEBUFFER, FBO);
		glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_RECTANGLE, texture.id, 0);
		
		glDrawBuffers(GL_COLOR_ATTACHMENT0);
		
		if (GL_FRAMEBUFFER_COMPLETE != glCheckFramebufferStatus(GL_FRAMEBUFFER))
			throw new RuntimeException("Could not create fbo");
		
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
        
        GlHelper.checkErrors();
		
	}
	
	void flush() {
		
		if (pixels.count() > 0) {
			GlHelper.ViewPort.set(pixelWidth, pixelHeight);
			glBindFramebuffer(GL_DRAW_FRAMEBUFFER, FBO);
			glDrawBuffers(GL_COLOR_ATTACHMENT0);
			GlHelper.Stencil.enable(true);
			pixels.flush(1);
			GlHelper.ViewPort.setDefault();
		}
		
		if (chunk != null) {
			GlHelper.checkErrors();
			ByteBuffer drawBuff = chunk.buff;
			drawBuff.flip();
			texture.uploadPixels(chunk.x1, chunk.w, chunk.y1, chunk.am/(chunk.w), drawBuff);
			chunk = null;
			GlHelper.checkErrors();
		}
		
	}

	@Override
	void dis() {
		GlHelper.checkErrors();
		texture.dis();
		GlHelper.checkErrors();
		if (normalTexture != null)
			normalTexture.dis();
		GlHelper.checkErrors();
		pixels.dis();
		glDeleteFramebuffers(FBO);
		GlHelper.checkErrors();
	}

	public void putPixel(int x, int y, byte r, byte g, byte b) {
		pixels.render((short) (x), (short) (pixelHeight - y), r,g,b);
	}
	
	public void putPixelBatch(int x1, int y1, int width, byte[] pixels){
		ByteBuffer drawBuff = BufferUtils.createByteBuffer(pixels.length);
		
		for (byte b : pixels){
			drawBuff.put(b);
		}
		drawBuff.flip();
		new CORE.GlJob() {
			@Override
			public void doJob() {
				texture.uploadPixels(x1, width, y1, pixels.length/(width*4), drawBuff);
			}
		}.perform();
		
	}
	
	// GOUL: putPixelBatch for many batches of the same size in one GL job, so the caller waits for the GL thread once
	// instead of once per batch (a save load uploads the 576 minimap tiles this way)
	public void putPixelBatches(int[] x1, int[] y1, int width, byte[] pixels){
		int size = pixels.length/x1.length;
		ByteBuffer drawBuff = BufferUtils.createByteBuffer(pixels.length);
		drawBuff.put(pixels);
		drawBuff.flip();
		new CORE.GlJob() {
			@Override
			public void doJob() {
				for (int i = 0; i < x1.length; i++)
					texture.uploadPixels(x1[i], width, y1[i], size/(width*4), drawBuff.slice(i*size, size));
			}
		}.perform();
	}
	
	public void addChunk(int x1, int y1, int width, int am, TextureHolderChunk chunk) {
		chunk.x1 = x1;
		chunk.y1 = y1;
		chunk.w = width;
		chunk.am = am;
		this.chunk = chunk;
	}
	
	public static class TextureHolderChunk {
		
		public final int width,height;
		private final ByteBuffer buff;
		private int x1, y1, w, am;
		
		public TextureHolderChunk(int width, int height) {
			this.width = width;
			this.height = height;
			buff = BufferUtils.createByteBuffer(width*height*4);
		}
		
		public void put(int i, byte r, byte g, byte b, byte a) {
			buff.position(i*4);
			buff.put(r).put(g).put(b).put(a);
		}
		
		public void put(int i, COLOR c) {
			buff.position(i*4);
			buff.put((byte) (c.red()*2)).put((byte) (c.green()*2)).put((byte) (c.blue()*2)).put((byte) 0xFF);
		}
		
		
	}
	
	
}
