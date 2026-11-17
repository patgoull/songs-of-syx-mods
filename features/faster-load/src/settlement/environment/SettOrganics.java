package settlement.environment;

import java.util.Arrays;

import settlement.main.SETT;
import settlement.path.AVAILABILITY;
import settlement.path.AvailabilityListener;
import settlement.tilemap.terrain.TBuilding;
import settlement.tilemap.terrain.Terrain.TerrainTile;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.DIR;
import snake2d.util.datatypes.RECTANGLE;
import snake2d.util.datatypes.Rec;
import snake2d.util.map.MAP_BOOLEAN;
import snake2d.util.map.MAP_DOUBLE;
import snake2d.util.misc.CLAMP;
import snake2d.util.sets.Bitmap1D;
import snake2d.util.sets.Bitmap2D;
import snake2d.util.sets.Bitsmap2D;
import snake2d.util.sets.QueueInteger;

public final class SettOrganics {

    private final static int CHUNK_SIZE = 32;
    private final static int searchSize = 32;
    private final static int blurSize = 4;
    
    private final Chunks chunks = new Chunks();
    private QuickSum sum = new QuickSum();
    private QuickSumValue value = new QuickSumValue();

    private Bitmap2D neg = new Bitmap2D(SETT.TILE_BOUNDS, false);
    private Bitsmap2D map = new Bitsmap2D(0, 5, SETT.TILE_BOUNDS);

    private Rec tmprec = new Rec();
    
    SettOrganics() {
        new AvailabilityListener() {
            @Override
            protected void changed(int tx, int ty, AVAILABILITY a, AVAILABILITY old, boolean playerChange) {
                if (a.player*old.player < 0) {
                    chunks.change(tx, ty);
                }
            }
        };
    }

	public void init() {
		chunks.clear();
		// GOUL: the walls are read once, QuickSum and getDist() test each tile ~10 and up to ~190 times over the overlapping chunks
		wallCache = new Bitmap1D(SETT.TAREA, false);
		for (int ty = 0; ty < SETT.THEIGHT; ty++)
			for (int tx = 0; tx < SETT.TWIDTH; tx++)
				if (wall.is(tx, ty))
					wallCache.set(tx + ty*SETT.TWIDTH, true);
		Rec rec = new Rec(CHUNK_SIZE);
		for (int cy = 0; cy < SETT.THEIGHT; cy+=CHUNK_SIZE) {
			for (int cx = 0; cx < SETT.TWIDTH; cx+=CHUNK_SIZE) {
				rec.moveX1Y1(cx, cy);
				update(rec);
			}
		}
		wallCache = null; // GOUL
		
	}
    
    void update() {
        RECTANGLE chunkBounds = chunks.next();
        if (chunkBounds == null)
        	return;
        
        update(chunkBounds);
    }
    
    private void update(RECTANGLE chunkBounds) {
        
        sum.init(chunkBounds.x1(), chunkBounds.y1());
        value.init(chunkBounds.x1(), chunkBounds.y1());
        tmprec.setDim(CHUNK_SIZE+blurSize*2);
        tmprec.centerIn(chunkBounds);
        
        
        for (COORDINATE c : tmprec) {
        	
        	if (!SETT.IN_BOUNDS(c))
        		continue;
        	
        	if (wallIs(c.x(), c.y())) { // GOUL: was wall.is(c)
        		map.set(c, 0);
        		continue;
        	}
        	
        	int n = getDist(DIR.N, c.x(), c.y());
        	int s = getDist(DIR.S, c.x(), c.y());
        	int w = getDist(DIR.W, c.x(), c.y());
        	int e = getDist(DIR.E, c.x(), c.y());
        	
        	if (n+s >= searchSize && w+e >= searchSize) {
        		map.set(c, 0);
        		continue;
        	}
        	
        	
        	int x1 = c.x()-w+1;
        	int x2 = c.x()+e;
        	int y1 = c.y()-n+1;
        	int y2 = c.y()+s;
        	
        	
        	
        	double walls = sum.count(x1, x2-1, y1, y2-1);
        		
        	int width = x2-x1;
        	int height = y2-y1;
        	
        	value.setValue(c.x(), c.y(), -1 + walls*16/(width*height));
        	
        	//map[c.y()][c.x()] = -1 + walls*16/(width*height);
        	
        }
        
        value.process();
        
        
        for (COORDINATE c : chunkBounds) {
        	double v = value.sample(c.x(), c.y());
        	boolean neg = false;
        	if (v < 0) {
        		neg = true;
        		v = -v;
        	}
        	int vv = (int) Math.round(v*0b1111);
        	vv = CLAMP.i(vv, 0, 0b1111);
        	map.set(c, vv);
        	this.neg.set(c, neg);
        	
        	
        }
    }
    
    
    private int getDist(DIR n, int wx, int wy) {
		for (int i = 1; i < searchSize; i++) {
			int x = wx+n.x()*i;
			int y = wy+n.y()*i;
			if (wallIs(x, y)) // GOUL: was wall.is(x, y)
				return i;
		}
		return searchSize;
	}

	// GOUL: wall.is(tx, ty). QuickSum has already read the walls of the square around the chunk being updated, which
	// holds every tile getDist() reaches, so in the game too each tile is read once per update instead of up to ~125 times.
	// While init() runs, from the bitmap (tiles outside the map are asked directly, as before)
	private Bitmap1D wallCache;
	
	private boolean wallIs(int tx, int ty) {
		if (sum.holds(tx, ty))
			return sum.count(tx, tx, ty, ty) != 0;
		if (wallCache == null || !SETT.IN_BOUNDS(tx, ty))
			return wall.is(tx, ty);
		return wallCache.get(tx + ty*SETT.TWIDTH);
	}
	
    private final MAP_BOOLEAN wall = new MAP_BOOLEAN() {
        @Override
        public boolean is(int tx, int ty) {
        	
        	if (SETT.ROOMS().map.is(tx, ty)) {
        		 AVAILABILITY a = SETT.ROOMS().getAvailability(tx, ty);
        		 if (a!= null && a.tileCollide)
        			 return true;
        	}
        	
            TerrainTile t = SETT.TERRAIN().get(tx, ty);
            if (t == null)
            	return false;
            if (!t.clearing().isStructure())
            	return false;
            if ( t instanceof TBuilding.Ceiling.Opening)
            	return true;
            AVAILABILITY a = t.getAvailability(tx, ty);
            if (a == null)
            	return false;
            
            
            return a.player < 0 || a.enemy < 0;
        }

        @Override
        public boolean is(int tile) {
            int tx = tile % SETT.TWIDTH;
            int ty = tile / SETT.TWIDTH;
            return is(tx, ty);
        }
    };
    
    public final MAP_DOUBLE MAP = new MAP_DOUBLE() {
        @Override
        public double get(int tx, int ty) {
        	double d = map.get(tx, ty);
        	d /= 0b1111;
        	if (neg.is(tx, ty))
        		return -d;
        	return d;
        }
        
        @Override
        public double get(int tile) {
            int x = tile%SETT.TWIDTH;
            int y = tile/SETT.TWIDTH;
        	return get(x, y);
        }
    };


    private final class Chunks {
    	private final int chunkMask = ~(CHUNK_SIZE - 1);
        private final int chunksWidth = SETT.TWIDTH / CHUNK_SIZE;
        private final int chunksSize = chunksWidth * (SETT.THEIGHT / CHUNK_SIZE);
        private final Bitmap1D changed = new Bitmap1D(chunksSize, false);
        private final QueueInteger updatables = new QueueInteger(chunksSize + 1);
        private Rec chunk = new Rec(CHUNK_SIZE, CHUNK_SIZE);
        
        Chunks() {}

        public void change(int tx, int ty) {
            if (!SETT.IN_BOUNDS(tx, ty)) return;
            
            int x1 = tx - searchSize;
            int x2 = tx + searchSize;
            int y1 = ty - searchSize;
            int y2 = ty + searchSize;

            for (int y = y1; y <= y2; y += CHUNK_SIZE) {
                for (int x = x1; x <= x2; x += CHUNK_SIZE) {
                    if (SETT.IN_BOUNDS(x, y)) {
                        changeP(x, y);
                    }
                }
            }
        }

        private int getChunk(int tx, int ty) {
            tx &= chunkMask;
            ty &= chunkMask;
            int c = (tx / CHUNK_SIZE) + chunksWidth * (ty / CHUNK_SIZE);
            return c;
        }
        
        
        public void changeP(int tx, int ty) {
            int c = getChunk(tx, ty);
            if (!changed.get(c)) {
                changed.set(c, true);
                updatables.push(c);
            }
        }

        void clear() {
            changed.clear();
            updatables.clear();
        }

        public RECTANGLE next() {
            if (!updatables.hasNext()) return null;
            int pt = updatables.poll();
            int x = pt % chunksWidth;
            int y = pt / chunksWidth;
            
            chunk.moveX1Y1(CHUNK_SIZE * x, CHUNK_SIZE * y);
            changed.set(pt, false);
            return chunk;
        }
    }

    
    private class QuickSum {
    	
    	int dim = (searchSize+blurSize)*2+CHUNK_SIZE+1;
    	
    	private int[][] wallcounts = new int[dim][dim]; 
    	int offX;
    	int offY;
    	private boolean ready; // GOUL: wallcounts holds the square at offX, offY
    	
    	void init(int sx, int sy) {
    		
    		ready = false; // GOUL
    		//we get the x and y for the current chunk. We're going to count outside of that
    		sx -= searchSize+blurSize;
    		sy -= searchSize+blurSize;
    		offX = sx;
    		offY = sy;
    		
    		for (int[] ii : wallcounts)
    			Arrays.fill(ii, 0);

    		for (int dy = 0; dy < wallcounts.length-1; dy++) {
    			
    			for (int dx = 0; dx < wallcounts[dy].length-1; dx++) {
    				int wallVal = wallIs(sx + dx, sy + dy) ? 1 : 0; // GOUL: was wall.is
                    
    				wallcounts[dx + 1][dy + 1] = wallVal 
                        + wallcounts[dx][dy + 1]     // Left
                        + wallcounts[dx + 1][dy]     // Top
                        - wallcounts[dx][dy];        // Top-Left (subtract duplicate)
        		}
    		}
    		ready = true; // GOUL
    		
    	}
    	
    	// GOUL: whether count() can answer for this tile
    	boolean holds(int tx, int ty) {
    		int x = tx - offX;
    		int y = ty - offY;
    		return ready && x >= 0 && y >= 0 && x < dim-1 && y < dim-1;
    	}
    	
    	private int count(int x1, int x2, int y1, int y2) {
    		x1 -= offX;
    		y1 -= offY;
    		x2 -= offX;
    		y2 -= offY;
            return wallcounts[x2 + 1][y2 + 1] 
                 - wallcounts[x1][y2 + 1] 
                 - wallcounts[x2 + 1][y1] 
                 + wallcounts[x1][y1];
        }
    }
    
    private class QuickSumValue {
    	
    	int dim = blurSize*2+CHUNK_SIZE+1;
    	private double[][] values = new double[dim][dim]; 
    	private double[][] counts = new double[dim][dim]; 
    	int offX;
    	int offY;
    	
    	void init(int sx, int sy) {
    		
    		//we get the x and y for the current chunk. We're going to count outside of that
    		sx -= blurSize;
    		sy -= blurSize;
    		offX = sx;
    		offY = sy;
    		
    		for (double[] ii : values)
    			Arrays.fill(ii, 0);

    		
    	}
    	
    	private void setValue(int tx, int ty, double value) {
    		tx -= offX;
    		ty -= offY;
    		values[ty][tx] = value;
    	}
    	
    	void process() {
    		
    		for (double[] ii : counts)
    			Arrays.fill(ii, 0);

    		for (int dy = 0; dy < values.length-1; dy++) {
    			
    			for (int dx = 0; dx < values[dy].length-1; dx++) {
    				double val = values[dy][dx];
    				counts[dx + 1][dy + 1] = val 
                        + counts[dx][dy + 1]     // Left
                        + counts[dx + 1][dy]     // Top
                        - counts[dx][dy];        // Top-Left (subtract duplicate)
        		}
    		}
    		
    	}
    	
    	private double sample(int tx, int ty) {
    		int x1 = tx-blurSize;
    		int x2 = tx +blurSize-1;
    		int y1 = ty -blurSize;
    		int y2 = ty+blurSize-1;
    		double width = (x2 - x1 + 1);
    		double height = (y2 - y1 + 1);
    		return count(x1, x2, y1, y2)/(width * height);
    	}
    	
    	private double count(int x1, int x2, int y1, int y2) {
    		x1 -= offX;
    		y1 -= offY;
    		x2 -= offX;
    		y2 -= offY;
            return counts[x2 + 1][y2 + 1] 
                 - counts[x1][y2 + 1] 
                 - counts[x2 + 1][y1] 
                 + counts[x1][y1];
        }
    }



    
}
