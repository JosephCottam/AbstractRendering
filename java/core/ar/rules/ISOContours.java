package ar.rules;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ar.Aggregates;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;
import ar.glyphsets.GlyphList;
import ar.glyphsets.SimpleGlyph;
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.Util;
import ar.aggregates.AggregateUtils;
import ar.aggregates.Iterator2D;

//Base algorithm: http://en.wikipedia.org/wiki/Marching_squares 
//Another implementation that does sub-cell interpolation for smoother contours: http://udel.edu/~mm/code/marchingSquares

//TODO: Better ISO contour picking (round numbers?)
public interface ISOContours<N> {
	/**List of contours from smallest value to largest value.**/
	public Glyphset.RandomAccess<Shape, N> contours();

	/**Produce a set of ISO contours that are spaced at the given interval.**/
	public static class SpacedContours<N extends Number> implements Transfer<N,N> {
		final double spacing;
		final N floor;
		final Renderer renderer;
		final boolean fill;
		
		/**@param spacing How far apart to place contours
		 * @param floor Lowest contour value (if omitted, will be the min value in the input)
		 */
		public SpacedContours(Renderer renderer, double spacing, N floor, boolean fill) {
			this.spacing = spacing;
			this.floor = floor;
			this.renderer = renderer;
			this.fill = fill;
		}
		
		public N emptyValue() {return null;}

		@Override
		public ar.Transfer.Specialized<N, N> specialize(Aggregates<? extends N> aggregates) {
			return new Specialized<>(renderer, spacing, floor, fill, aggregates);
		}
		
		public static final class Specialized<N extends Number> extends SpacedContours<N> implements ISOContours<N>, Transfer.Specialized<N, N> {
			final GlyphList<Shape, N> contours;
			final Aggregates<N> cached;
			
			public Specialized(Renderer renderer, double spacing, N floor, boolean fill, Aggregates<? extends N> aggregates) {
				super(renderer, spacing, floor, fill);
				Util.Stats<N> stats = Util.stats(aggregates, true, true);
				contours = new GlyphList<>();
				ArrayList<Aggregates<N>> cachedAggregates = new ArrayList<>();
				
				int i=0;
				N threshold;
				N bottom = floor == null ? stats.min : floor;
				do {
					threshold = LocalUtils.addTo(bottom, i*spacing);
					Single.Specialized<N> t = new Single.Specialized<>(renderer, threshold, fill, aggregates);
					this.contours.addAll(t.contours());
					cachedAggregates.add(t.cached);
					i++;
				} while (threshold.doubleValue() < stats.max.doubleValue());
				cached = LocalUtils.flatten(cachedAggregates);
			}
			
			public GlyphList<Shape, N> contours() {return contours;}

			public N at(int x, int y, Aggregates<? extends N> aggregates) {return cached.get(x,y);}
		}
		
	}

	/**Produce N contours, evenly spaced between max and min.**/
	public static class NContours<N extends Number> implements Transfer<N,N> {
		final int n;
		final Renderer renderer;
		final boolean fill;
		
		public NContours(Renderer r, int n, boolean fill) {
			this.n = n;
			this.renderer = r;
			this.fill = fill;
		}
		
		public N emptyValue() {return null;}

		@Override
		public ar.Transfer.Specialized<N, N> specialize(Aggregates<? extends N> aggregates) {
			return new NContours.Specialized<>(renderer, n, fill, aggregates);
		}
		
		public static final class Specialized<N extends Number> extends NContours<N> implements ISOContours<N>, Transfer.Specialized<N, N> {
			final GlyphList<Shape, N> contours;
			final Aggregates<N> cached;
			
			public Specialized(Renderer r, int n, boolean fill, Aggregates<? extends N> aggregates) {
				super(r, n, fill);
				Util.Stats<N> stats = Util.stats(aggregates, true, true);
				contours = new GlyphList<>();
				
				ArrayList<Aggregates<N>> cachedAggregates = new ArrayList<>();
				double step = (stats.max.doubleValue()-stats.min.doubleValue())/n;
				for (int i=0;i<n;i++) {
					N threshold = LocalUtils.addTo(stats.min, (step*i));
					Single.Specialized<N> t = new Single.Specialized<>(renderer, threshold, fill, aggregates);
					this.contours.addAll(t.contours());
					cachedAggregates.add(t.cached);
				}
				cached = LocalUtils.flatten(cachedAggregates);
			}
			public GlyphList<Shape, N> contours() {return contours;}
			public N at(int x, int y, Aggregates<? extends N> aggregates) {return cached.get(x,y);}
		}
	}
	
	/**Produce a single ISO contour at the given division point.**/
	public static class Single<N extends Number> implements Transfer<N, N> {
		protected final N threshold;
		protected final Renderer renderer;
		protected final boolean fill;

		public Single(Renderer r, N threshold, boolean fill) {
			this.threshold = threshold;
			this.renderer = r;
			this.fill = fill;
		}

		public N emptyValue() {return null;}
		public Transfer.Specialized<N, N> specialize(Aggregates<? extends N> aggregates) {
			return new Specialized<>(renderer, threshold, fill,  aggregates);
		}


		public static final class Specialized<N extends Number> extends Single<N> implements Transfer.Specialized<N,N>, ISOContours<N> { 
			private final GlyphList<Shape, N> contours;
			protected final Aggregates<N> cached;

			public Specialized(Renderer renderer, N threshold, boolean fill, Aggregates<? extends N> aggregates) {
				super(renderer, threshold, fill);
				Aggregates<? extends N> padAggs = new PadAggregates<>(aggregates, null);  

				Aggregates<Boolean> isoDivided = renderer.transfer(padAggs, new ISOBelow<>(threshold));
				Aggregates<MC_TYPE> classified = renderer.transfer(isoDivided, new MCClassifier());
				Shape s = Assembler.assembleContours(classified, isoDivided);
				contours = new GlyphList<>();

				contours.add(new SimpleGlyph<>(s, threshold));
				if (fill) {isoDivided = renderer.transfer(isoDivided, new General.Simplify<>(isoDivided.defaultValue()));}
				cached = renderer.transfer(isoDivided, new General.Retype<>(true, threshold, null));
			}
			public GlyphList<Shape, N> contours() {return contours;}
			public N at(int x, int y, Aggregates<? extends N> aggregates) {return cached.get(x,y);}
		}
	}
	
	public static class LocalUtils {

		@SuppressWarnings("unchecked")
		public static <N extends Number> N addTo(N val, double more) {
			if (val instanceof Double) {return (N) new Double(((Double) val).doubleValue()+more);}
			if (val instanceof Float) {return (N) new Float(((Float) val).floatValue()+more);}
			if (val instanceof Long) {return (N) new Long((long) (((Long) val).longValue()+more));}
			if (val instanceof Integer) {return (N) new Integer((int) (((Integer) val).intValue()+more));}
			if (val instanceof Short) {return (N) new Short((short) (((Short) val).shortValue()+more));}
			throw new IllegalArgumentException("Cannot add to " + val.getClass().getName());
		}
		
		@SuppressWarnings("unchecked")
		public static <N extends Number> Valuer<Double, N> wrapperFor(N val) {
			if (val == null) {throw new NullPointerException("Cannot infer type for null value.");}
			if (val instanceof Double) {return (Valuer<Double, N>) new MathValuers.DoubleWrapper();}
			if (val instanceof Float) {return (Valuer<Double, N>) new MathValuers.FloatWrapper();}
			if (val instanceof Long) {return (Valuer<Double, N>) new MathValuers.LongWrapper();}
			if (val instanceof Integer) {return (Valuer<Double, N>) new MathValuers.IntegerWrapper();}
			if (val instanceof Short) {return (Valuer<Double, N>) new MathValuers.ShortWrapper();}
			throw new IllegalArgumentException("Cannot infer numeric wrapper for " + val.getClass().getName());
		}
		
		public static <N> Aggregates<N> flatten(List<Aggregates<N>> cascade) {
			if (cascade.size() == 1) {return cascade.get(0);}
			Aggregates<N> exemplar = cascade.get(0);
			final Aggregates<N> target = AggregateUtils.make(exemplar, null);
			for (int x=target.lowX(); x<target.highX(); x++) {
				for (int y=target.lowY(); y< target.highY(); y++) {
					for (int i=cascade.size()-1; i>=0; i--) {
						exemplar = cascade.get(i);
						N def = exemplar.defaultValue();
						N val = exemplar.get(x, y);
						if (!Util.isEqual(def, val)) {
							target.set(x, y, val);
							break;
						} 
					}
				}
			}
			return target;
		}
	}

	
	public static final class Assembler {
		/** Build a single path from all of the contour parts.  
		 * 
		 * May be disjoint and have holes (thus GeneralPath).
		 * 
		 * @param classified The line-segment classifiers
		 * @param isoDivided Original classification, used to disambiguate saddle conditions
		 * @return
		 */
		public static final GeneralPath assembleContours(Aggregates<MC_TYPE> classified, Aggregates<Boolean> isoDivided) {
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
		 */
		public static void stichContour(Aggregates<MC_TYPE> isoData, Aggregates<Boolean> isoDivided, GeneralPath iso, int startX, int startY) {
			int x=startX, y=startY;
	
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
			if (v == null) {return false;}
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
