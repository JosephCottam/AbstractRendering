package ar.rules;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ar.Aggregates;
import ar.Glyph;
import ar.Renderer;
import ar.Transfer;
import ar.glyphsets.SimpleGlyph;
import ar.renderers.ParallelRenderer;
import ar.aggregates.Iterator2D;

//Base algorithm: http://en.wikipedia.org/wiki/Marching_squares 
//Another implementation that does sub-cell interpolation for smoother contours: http://udel.edu/~mm/code/marchingSquares

public abstract class ISOContours<N> {
	
	public abstract Collection<Glyph<Shape, ? extends N>> contours();

	public static class SpacedContours<N extends Number> implements Transfer<N,N> {

		@Override
		public N emptyValue() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ar.Transfer.Specialized<N, N> specialize(
				Aggregates<? extends N> aggregates) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public static class NContours<N extends Number> implements Transfer<N,N> {

		@Override
		public N emptyValue() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ar.Transfer.Specialized<N, N> specialize(
				Aggregates<? extends N> aggregates) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	public static class Single<N extends Number> implements Transfer<N, N> {
		private final N threshold;
		private final N empty;
		private final N pad;

		public Single(N threshold, N empty, N pad) {
			this.threshold = threshold;
			this.empty = empty;
			this.pad=pad;
		}

		public N emptyValue() {return empty;}
		public Transfer.Specialized<N, N> specialize(Aggregates<? extends N> aggregates) {
			return new Specialized<>(threshold, empty, pad, aggregates);
		}


		public static final class Specialized<N extends Number> extends Single<N> implements Transfer.Specialized<N,N> { 
			private final Renderer renderer = new ParallelRenderer();
			private final Glyph<GeneralPath, N> contour;

			public Specialized(N threshold, N empty, N pad, Aggregates<? extends N> aggregates) {
				super(threshold, empty, pad);

				//Aggregates<? extends N> padAggs = new PadAggregates<>(aggregates, empty); 

				Aggregates<Boolean> isoDivided = renderer.transfer(aggregates, new ISOBelow<>(threshold));
				Aggregates<MC_TYPE> classified = renderer.transfer(isoDivided, new MCClassifier());
				GeneralPath s = assembleContours(classified, isoDivided);
				contour = new SimpleGlyph<>(s, threshold);
			}

			//TODO: Convert to not being a pass-through.  Instead returns an indicator of the iso contour containing the point...
			public N at(int x, int y, Aggregates<? extends N> aggregates) {return aggregates.get(x,y);}
			public Glyph<GeneralPath, N> contours() {return contour;}
		}
	}

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

	/**An iso level can be made of multiple regions with holes in them.
	 * This builds one path (into the passed GeneralPath) that represents one
	 * connected contour.
	 *
	 * @param isoData Marching-cubes classification at each cell
	 * @param isoDivided The boolean above/below classification for each cell (to disambiguate saddles)
	 * @param iso The path to build into
	 * @param r Start row of the contour
	 * @param c Start column of the contour
	 */
	private static void stichContour(Aggregates<MC_TYPE> isoData, Aggregates<Boolean> isoDivided, GeneralPath iso, int x, int y) {
		int startX = x, startY = y;

		SIDE prevSide = SIDE.NONE;

		// Found an unambiguous iso line at [r][c], so start there.
		MC_TYPE startCell = isoData.get(x,y);
		Point2D nextPoint = startCell.firstSide(prevSide).nextPoint(x,y);
		iso.moveTo(nextPoint.getX(), nextPoint.getY());	        
		prevSide = isoData.get(x,y).secondSide(prevSide, isoDivided.get(x,y));

		//System.out.printf("-------------------\n);

		do {
			//Process current cell
			MC_TYPE curCell = isoData.get(x,y);
			nextPoint = curCell.secondSide(prevSide, isoDivided.get(x,y)).nextPoint(x,y);
			//System.out.printf("%d,%d: %s\n",x,y,curCell.secondSide(prevSide, isoDivided.get(x,y)));
			iso.lineTo(nextPoint.getX(), nextPoint.getY());
			SIDE nextSide = curCell.secondSide(prevSide, isoDivided.get(x,y));
			isoData.set(x,y, curCell.clearWith()); // Erase this marching cube line entry

			//Advance for next cell
			prevSide = nextSide;
			switch (nextSide) {
				case LEFT: x -= 1; break;
				case RIGHT: x += 1; break;
				case BOTTOM: y += 1; break;
				case TOP: y -= 1; break;
				case NONE: throw new IllegalArgumentException("Encountered side NONE after starting contour line.");
			}

		} while (x != startX || y != startY);
		iso.closePath();
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

		public Point2D nextPoint(int x, int y) {
			switch (this) {
			case LEFT: return new Point2D.Double(x-1, y);
			case RIGHT: return new Point2D.Double(x+1, y);
			case BOTTOM: return new Point2D.Double(x, y+1);
			case TOP: return new Point2D.Double(x, y-1);
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
		diag_two(0b1010),  //Ambiguous case
		diag_one(0b0101);   //Ambiguous case

		public final int idx;
		MC_TYPE(int idx) {this.idx = idx;}

		public MC_TYPE clearWith() {
			switch (this) {
			case empty: case surround: case diag_two: case diag_one: return this;
			default: return MC_TYPE.empty;
			}
		}

		public SIDE firstSide(SIDE prev) {
			switch (this) {
			case ui_l_in: case ui_in: case di_l_out:
				//case 1: case 3: case 7:
				return SIDE.LEFT;
			case ui_r_in: case r_in: case ui_l_out:
				//case 2: case 6: case 14:
				return SIDE.BOTTOM;
			case di_r_in: case di_in: case ui_r_out:
				//case 4: case 12: case 13:
				return SIDE.RIGHT;
			case di_l_in: case l_in: case di_r_out:
				//case 8: case 9: case 11:
				return SIDE.TOP;
			case diag_one:
				//case 5:
				switch (prev) {
				case LEFT:
					return SIDE.RIGHT;
				case RIGHT:
					return SIDE.LEFT;
				default:
					throw new RuntimeException(String.format("Illegal previous (%s) case for current case (%s)", prev.name(), this.name()));
				}
			case diag_two:
				//case 10:
				switch (prev) {
				case BOTTOM:
					return SIDE.TOP;
				case TOP:
					return SIDE.BOTTOM;
				default:
					throw new RuntimeException(String.format("Illegal previous (%s) case for current case (%s)", prev.name(), this.name()));
				}
			default:
				throw new RuntimeException("Cannot determine side for case of " + this.name());
			}
		}

		public SIDE secondSide(SIDE prev, boolean flipped) {
			switch (this) {
			case di_l_in: case di_in: case ui_l_out:
				//case 8: case 12: case 14:
				return SIDE.LEFT;
			case ui_l_in: case l_in: case ui_r_out:
				//case 1: case 9: case 13:
				return SIDE.BOTTOM;
			case ui_r_in: case ui_in: case di_r_out:
				//case 2: case 3: case 11:
				return SIDE.RIGHT;
			case di_r_in: case r_in: case di_l_out:
				//case 4: case 6: case 7:
				return SIDE.TOP;
			case diag_one:
				//case 5:
				switch (prev) {
				case LEFT: 
					return flipped ? SIDE.BOTTOM : SIDE.TOP;
				case RIGHT: 
					return flipped ? SIDE.TOP : SIDE.BOTTOM;
				default:
					throw new RuntimeException(String.format("Illegal previous (%s) case for current case (%s)", prev.name(), this.name()));
				}
			case diag_two:
				//case 10:
				switch (prev) {
				case BOTTOM: 
					return flipped ? SIDE.RIGHT : SIDE.LEFT;
				case TOP: 
					return flipped ? SIDE.LEFT : SIDE.RIGHT;
				default:
					throw new RuntimeException(String.format("Illegal previous (%s) case for current case (%s)", prev.name(), this.name()));
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

	/**Adds a row and column to each side of an aggregate set filled with a specific value.  
	 * 
	 * Only the original range remains set-able.**/
	public static final class PadAggregates<A> implements Aggregates<A> {
		private final Aggregates<? extends A> base;
		private final A pad;

		public PadAggregates(Aggregates<? extends A> base, A pad) {
			this.base = base;
			this.pad =pad;
		}

		@Override
		public Iterator<A> iterator() {return new Iterator2D<A>(this);}

		@Override
		public A get(int x, int y) {
			if ((x >= base.lowX() && x < base.highX())
					&& (y >= base.lowY() && y < base.highY())) {
				return base.get(x,y); //Its inside
			} else if ((x == base.lowX()-1 || x== base.highX()+1) 
					&& (y >= base.lowY()-1 && y < base.highY()+1)) {
				return pad; //Its immediate above or below
			} else if ((y == base.lowY()-1 || y == base.highY()+1) 
					&& (x >= base.lowX()-1 && x < base.highX()+1)) {
				return pad; //Its immediately left or right
			} else {
				return base.defaultValue();
			}
		}

		public void set(int x, int y, A val) {throw new UnsupportedOperationException();}
		public A defaultValue() {return base.defaultValue();}
		public int lowX() {return base.lowX()-1;}
		public int lowY() {return base.lowY()-1;}
		public int highX() {return base.highX()+1;}
		public int highY() {return base.highY()+1;}		
	}
}
