package ar.rules;

import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import ar.Aggregates;
import ar.Glyph;
import ar.Renderer;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.glyphsets.SimpleGlyph;
import ar.renderers.ParallelRenderer;

//Base algorithm: http://en.wikipedia.org/wiki/Marching_squares 
//Another implementation that does sub-cell interpolation for smoother contours: http://udel.edu/~mm/code/marchingSquares/IsoCell.java

public class ISOContours<N extends Number> implements Transfer<N, N> {
	private final N threshold;
	private final N empty;
	
	public ISOContours(N threshold, N empty) {
		this.threshold = threshold;
		this.empty = empty;
	}
	
	public N emptyValue() {return empty;}
	public Transfer.Specialized<N, N> specialize(Aggregates<? extends N> aggregates) {
		return new Specialized<>(threshold, empty, aggregates);
	}
	
	
	public static final class Specialized<N extends Number> extends ISOContours<N> implements Transfer.Specialized<N,N> { 
		private final Renderer renderer = new ParallelRenderer();
		private final Glyph<GeneralPath, N> contour;

		public Specialized(N threshold, N empty, Aggregates<? extends N> aggregates) {
			super(threshold, empty);

			Aggregates<Boolean> isoDivided = renderer.transfer(aggregates, new ISOBelow<>(threshold));
			Aggregates<MC_TYPE> classified = renderer.transfer(isoDivided, new MCClassifier());
			GeneralPath s = assembleContours(classified, isoDivided);
			contour = new SimpleGlyph<>(s, threshold);
			
			System.out.println(AggregateUtils.toString(aggregates));
			System.out.println(AggregateUtils.toString(isoDivided));
			System.out.println(AggregateUtils.toString(classified));

		}
		
		public N at(int x, int y, Aggregates<? extends N> aggregates) {return aggregates.get(x,y);}
		public Glyph<GeneralPath, N> contours() {return contour;}
		
		/** Build a single path from all of the contour parts.  
		 * 
		 * May be disjoint and have holes (thus GeneralPath).
		 * 
		 * @param classified The line-segment classifiers
		 * @param isoDivided Original classification, used to disambiguate saddle conditions
		 * @return
		 */
		private static final GeneralPath assembleContours(Aggregates<MC_TYPE> classified, Aggregates<Boolean> isoDivided) {
	        GeneralPath isoPath = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
	        
	        //Find an unambiguous case of an actual line, follow it around and build the line.  
	        //Stitching sets the line segments that have been "consumed" to MC_TYPE.empty, so segments are only processed once.
	        for (int x = classified.lowX(); x < classified.highX(); x++) {
	            for (int y = classified.lowY(); y < classified.highY(); y++) {
	            	MC_TYPE type = classified.get(x, y);
	                if (type != MC_TYPE.empty 
	                		&& type != MC_TYPE.surround
	                		&& type != MC_TYPE.diag_one
	                		&& type != MC_TYPE.diag_two) {
	                	stichContour(classified, isoDivided, isoPath, x, y);
	                }
	            }
	        }
	        return isoPath;
	    }

	    /**An ios level can be made of multiple regions with holes in them.
	     * This builds one path (into the passed GeneralPath) that represents one
	     * connected contour.
		 *
	     * @param isoData Marching-cubes classification at each cell
	     * @param isoDivided The boolean above/below classification for each cell (to disambiguate saddles)
	     * @param iso The path to build into
	     * @param r Start row of the contour
	     * @param c Start column of the contour
	     */
	    private static void stichContour(Aggregates<MC_TYPE> isoData, Aggregates<Boolean> isoDivided, GeneralPath iso, int r, int c) {
	    	int startRow = r, startCol = c;
	    	
	        SIDE prevSide = SIDE.NONE;

	        // Found an unambiguous iso line at [r][c], so start there.
	        MC_TYPE curCell = isoData.get(r,c);
	        Point2D pt = curCell.firstSide(prevSide).nextPoint(r,c);
	        iso.moveTo(pt.getX(), pt.getY());
	        
	        prevSide = curCell.secondSide(prevSide, isoDivided.get(r,c));

	        do {
	        	pt = curCell.secondSide(prevSide, isoDivided.get(r,c)).nextPoint(r,c);
	            iso.lineTo(pt.getX(), pt.getY());
	            SIDE nextSide = curCell.secondSide(prevSide, isoDivided.get(r,c));
	            switch (nextSide) {
		            case BOTTOM: r -= 1; break;
		            case LEFT: c -= 1; break;
		            case RIGHT: c += 1; break;
		            case TOP: r += 1; break;
		            case NONE: throw new IllegalArgumentException("Encountered side NONE after starting contour line.");
	            }
		        isoData.set(r, c, MC_TYPE.empty); // Erase this iso line entry

		        curCell = isoData.get(r,c);
		        prevSide = nextSide;
	        } while (r != startRow && c != startCol);
	        iso.closePath();
	    }
	}
	
	/**Classifies each cell as above or below the given ISO value
	 *TODO: Are doubles enough?  Should there be number-type-specific implementations?
     **/
	public static final class ISOBelow<N extends Number> implements Transfer.Specialized<N, Boolean> {
		private final Number threshold;

		public ISOBelow(Number threshold) {
			this.threshold = threshold;
		}
		
		public Boolean emptyValue() {return Boolean.FALSE;}
		public Specialized<N, Boolean> specialize(Aggregates<? extends N> aggregates) {return this;}
		public Boolean at(int x, int y,
				Aggregates<? extends N> aggregates) {
			Number v = aggregates.get(x,y);
			double delta = threshold.doubleValue() - v.doubleValue();
			return delta < 0;
		}
	}
	
	public static enum SIDE {
		NONE, LEFT, RIGHT, BOTTOM, TOP;
		
	    public Point2D nextPoint( int r, int c) {
	        switch (this) {
	        	case LEFT: return new Point2D.Double(r, c-1);
	        	case RIGHT: return new Point2D.Double(r, c+1);
	            case BOTTOM: return new Point2D.Double(r+1, c);
	            case TOP: return new Point2D.Double(r-1, c);
	            default: throw new IllegalArgumentException("No 'nextPoint' defiend for NONE.");
	        }
	    }
	}

	//Named according to scan-line convention
	//ui is "up index" which is lower on the screen
	//di is "down index" which is higher on the screen 
	public static enum MC_TYPE {
		empty(0b0000),
		surround(0b1111),
		ui_l_out(0b1110),
		ui_r_out(0b1101),
		di_r_out(0b1011),
		di_l_out(0b0111),
		ui_l_in(0b0001),
		ui_r_in(0b0010),
		di_r_in(0b0100),
		di_l_in(0b1000),
		di_in(0b1100),
		l_in(0b1001),
		ui_in(0b0011),
		r_in(0b0110),
		diag_one(0b1010),  //Ambiguous case
		diag_two(0b0101);   //Ambiguous case
		
		public final int idx;
		MC_TYPE(int idx) {this.idx = idx;}
		
		public SIDE firstSide(SIDE prev) {
			 switch (idx) {
	            case 1: case 3: case 7:
	                return SIDE.LEFT;
	            case 2: case 6: case 14:
	                return SIDE.BOTTOM;
	            case 4: case 12: case 13:
	                return SIDE.RIGHT;
	            case 8: case 9: case 11:
	                return SIDE.TOP;
	            case 5:
	                switch (prev) {
	                    case LEFT:
	                        return SIDE.RIGHT;
	                    case RIGHT:
	                        return SIDE.LEFT;
	                    default:
	                    	throw new RuntimeException("Illegal previous case for current case of " + this.name());
	                }
	            case 10:
	                switch (prev) {
	                    case BOTTOM:
	                        return SIDE.TOP;
	                    case TOP:
	                        return SIDE.BOTTOM;
	                    default:
	                    	throw new RuntimeException("Illegal previous case for current case of " + this.name());
	                }
	            default:
                	throw new RuntimeException("Cannot determine side for case of " + this.name());
	        }
		}
		
		public SIDE secondSide(SIDE prev, boolean flipped) {
			switch (idx) {
            	case 8: case 12: case 14:
            		return SIDE.LEFT;
            	case 1: case 9: case 13:
            		return SIDE.BOTTOM;
            	case 2: case 3: case 11:
            		return SIDE.RIGHT;
            	case 4: case 6: case 7:
            		return SIDE.TOP;
            	case 5:
            		switch (prev) {
                    	case LEFT: 
                    		return flipped ? SIDE.BOTTOM : SIDE.TOP;
                    	case RIGHT: 
                    		return flipped ? SIDE.TOP : SIDE.BOTTOM;
                    	default:
	                    	throw new RuntimeException("Illegal previous case for current case of " + this.name());
            		}
            	case 10:
            		switch (prev) {
                    	case BOTTOM: 
                    		return flipped ? SIDE.RIGHT : SIDE.LEFT;
                    	case TOP: 
                    		return flipped ? SIDE.LEFT : SIDE.RIGHT;
                    	default:
                    		throw new RuntimeException("Illegal previous case for current case of " + this.name());
            		}
            	default:
            		throw new RuntimeException("Cannot determine side for case of " + this.name());
			}
		}
		
		private static final Map<Integer,MC_TYPE> lookup = new HashMap<>();
		static {for(MC_TYPE mct : EnumSet.allOf(MC_TYPE.class))lookup.put(mct.idx, mct);}
		public static MC_TYPE get(int code) {return lookup.get(code);}
	}
	
	public static final class MCClassifier implements Transfer.Specialized<Boolean, MC_TYPE> {
		private final int DOWN_INDEX_LEFT  = 0b1000;
		private final int DOWN_INDEX_RIGHT = 0b0100;
		private final int UP_INDEX_RIGHT   = 0b0010;
		private final int UP_INDEX_LEFT    = 0b0001;
		
		public MC_TYPE emptyValue() {return MC_TYPE.empty;}

		@Override
		public Specialized<Boolean, MC_TYPE> specialize(Aggregates<? extends Boolean> aggregates) {return this;}

		@Override
		public MC_TYPE at(int x, int y, Aggregates<? extends Boolean> aggregates) {			
			int code = 0;
			if (aggregates.get(x-1,y-1)) {code = code | DOWN_INDEX_LEFT;}
			if (aggregates.get(x,y-1)) {code = code | DOWN_INDEX_RIGHT;}
			if (aggregates.get(x-1,y)) {code = code | UP_INDEX_LEFT;}
			if (aggregates.get(x,y)) {code = code | UP_INDEX_RIGHT;}
			return MC_TYPE.get(code);
		}
	}
}
