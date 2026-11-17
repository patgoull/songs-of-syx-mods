package snake2d;

import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_READ;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;

import java.nio.IntBuffer;

import org.lwjgl.system.MemoryUtil;

import snake2d.util.file.Alloc;

abstract class ElementArrays {

	abstract void bind();
	abstract void dis();
	
	private static ElementArrays quad;
	private static ElementArrays point;

	static void quadBind() {
		if (quad != null) {
			quad.bind();
			return;
		}
		
		int[] indices = Alloc.ii((1<<16)*6);
		int tmp = 0;
		for (int i = 0; i < indices.length; i+=6){
        	indices[i] = tmp++;    //0
        	indices[i+1] = tmp++;  //1	
        	indices[i+2] = tmp--;  //1
        	indices[i+3] = tmp++;  //2
        	indices[i+4] = tmp++;  //3
        	indices[i+5] = tmp++;  //4
        }
		
		IntBuffer indicesBuffer = MemoryUtil.memAllocInt(indices.length);
		indicesBuffer.put(indices).flip();
		GlHelper.checkErrors();
        final int vertexFixID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vertexFixID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_READ);
        MemoryUtil.memFree(indicesBuffer);
        
        quad =  new ElementArrays() {
			
			@Override
			void dis() {
				glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
	            glDeleteBuffers(vertexFixID);
				
			}
			
			@Override
			void bind() {
				glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vertexFixID);
				
			}
		};
		quad.bind();
		GlHelper.checkErrors();
	}
	
	static void pointBind() {
		
		if (point != null) {
			point.bind();
			return;
		}
		
		int[] indices = Alloc.ii((1<<17)); // GOUL: was 1<<16, the light buffer draws up to 1<<17 points in one call
		for (int i = 0; i < indices.length; i++) {
			indices[i] = i;
		}

		IntBuffer indicesBuffer = MemoryUtil.memAllocInt(indices.length);
		indicesBuffer.put(indices).flip();
		GlHelper.checkErrors();
		final int vertexFixID = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vertexFixID);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_READ);
		MemoryUtil.memFree(indicesBuffer);
		
		point =  new ElementArrays() {
			
			@Override
			void dis() {
				glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
	            glDeleteBuffers(vertexFixID);
				
			}
			
			@Override
			void bind() {
				glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vertexFixID);
				
				
			}
		};
		point.bind();
		GlHelper.checkErrors();
	}
	
	static void dispose() {
		GlHelper.checkErrors();
		if (quad != null) {
			quad.dis();
			GlHelper.checkErrors();
		}
		if (point != null) {
			point.dis();
			GlHelper.checkErrors();
		}
		quad = null;
		point = null;
	}
	
}
