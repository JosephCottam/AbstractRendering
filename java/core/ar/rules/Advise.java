package ar.rules;

import java.awt.Color;
import java.awt.Point;
import java.util.Comparator;

import ar.Aggregates;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.util.Util;

/**Advise methods provide information about where to look in a visualization.
 * 
 * These are experimental methods of unproven value, use at your own risk.
 *  
 * @author jcottam
 */
public class Advise {
	//TODO: Extend to reporting the magnitude of the under-saturation
	//TODO: Should this look at mins instead-of/in-addition-to empty?
	//TODO: What about "perceptual differences" vs euclidean RGB space difference.
	/** Mark regions where multiple values are represented in the same way as the minimum value.*/
	public static class UnderSaturate<A> implements Transfer<A, Boolean> {
		private static final long serialVersionUID = -5898665841659861105L;
		protected final Transfer<A,Color> ref;
		protected final double tolerance; 
		protected final Comparator<A> comp;
		
		/**@param reference Transfer function that determines representation**/
		public UnderSaturate(Transfer<A,Color> reference, Comparator<A> comp, double tolerance) {
			this.ref = reference;
			this.comp = comp;
			this.tolerance = tolerance;
		}
		
		/**What is the maximum distance two items can be apart and still considered the same.**/
		public double tolerance() {return tolerance;}
		
		public Boolean emptyValue() {return Boolean.FALSE;}
		public UnderSaturate.Specialized<A> specialize(Aggregates<? extends A> aggregates) {
			return new Specialized<>(ref.specialize(aggregates), comp, tolerance);
		}
		
		protected static final class Specialized<A> extends UnderSaturate<A> implements Transfer.Specialized<A, Boolean> {
			private static final long serialVersionUID = 470073013225719009L;
			private final Transfer.Specialized<A, Color> ref;
			
			public Specialized(Transfer.Specialized<A,Color> ref, Comparator<A> comp, double tolerance) {
				super(ref, comp, tolerance);
				this.ref = ref;
			}
			
			public Boolean at(int x, int y, Aggregates<? extends A> aggregates) {
				A def = aggregates.defaultValue();
				A val = aggregates.get(x, y);
				Color empty = ref.emptyValue();
				Color out = ref.at(x, y, aggregates);
				double distance = euclidean(empty, out);
				return comp.compare(val, def) != 0 && distance < tolerance;
			}
			
			private static final double euclidean(Color c1, Color c2) {
				double r = Math.pow(c1.getRed()-c2.getRed(),2);
				double g = Math.pow(c1.getGreen()-c2.getGreen(),2);
				double b = Math.pow(c1.getBlue()-c2.getBlue(),2);
				return Math.sqrt(r+g+b);
			}

		}
	}
	
	//TODO: Extend to reporting the magnitude of the over-saturation
	//TODO: What about "perceptual differences" vs just absolute differences
	/** Mark regions where multiple values are represented in the same way as the maximum value.*/
	public static class OverSaturate<A> implements Transfer<A, Boolean> {
		private static final long serialVersionUID = -134839100328128893L;
		final Transfer<A,Color> ref;
		protected Comparator<A> comp;

		/**@param reference Transfer function that determines representation
		 * @param comp Comparator used to determine "sameness"**/
		public OverSaturate(Transfer<A,Color> reference, Comparator<A> comp) {
			this.ref = reference;
			this.comp = comp;
		}

		public Boolean emptyValue() {return Boolean.FALSE;}

		
		
		@Override
		public Transfer.Specialized<A, Boolean> specialize(Aggregates<? extends A> aggregates) {
			Transfer.Specialized<A,Color> refSpecialized = ref.specialize(aggregates);
			Point p = max(aggregates, comp);
			A max = aggregates.get(p.x, p.y);
			Color top = refSpecialized.at(p.x,p.y, aggregates);
			return new Specialized<>(refSpecialized, comp, max, top);
		}
		
		protected static final class Specialized<A> extends OverSaturate<A> implements Transfer.Specialized<A,Boolean> {
			private static final long serialVersionUID = 3155281566160217841L;
			private final Transfer.Specialized<A, Color> ref;
			private final A max;
			private final Color top;

			public Specialized(Transfer.Specialized<A, Color> ref, Comparator<A> comp, A max, Color top) {
				super(ref, comp);
				this.ref = ref;
				this.max = max;
				this.top = top;
			}
			
			public Boolean at(int x, int y, Aggregates<? extends A> aggregates) {
				A val = aggregates.get(x, y);
				Color out = ref.at(x, y, aggregates);
				boolean same = Util.isEqual(top, out);
				int diff = comp.compare(val, max);
				return diff !=0 && same; 
			}
		}
		
	}
	
	/** Mark regions where multiple values are represented in the same way as the minimum or maximum values.*/
	public static class OverUnder<A> implements Transfer<A, Color> {
		private static final long serialVersionUID = 7662347822550778810L;
		protected final Transfer<A, Color> base;
		protected final Transfer<A, Boolean> under;
		protected final Transfer<A, Boolean> over;
		protected final Comparator<A> comp;
		protected final Color overColor, underColor;
		protected final double lowTolerance; //TODO: use under.tolerance instead....

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public OverUnder(Color overColor, Color underColor, Transfer<A, Color> base, double lowTolerance) {
			this(overColor, underColor, base, lowTolerance, new Util.ComparableComparator());
		}
		
		/**
		 * @param overColor Color to mark over saturation
		 * @param underColor Color to mark under saturation
		 * @param base Transformation that determines all colors and to find over/under saturation
		 * @param lowTolerance How close should be considered too-close in undersaturation
		 * @param comp Comparator used to determine similarity between items
		 */
		public OverUnder(Color overColor, Color underColor, Transfer<A, Color> base, double lowTolerance, Comparator<A> comp) {
			this.overColor = overColor;
			this.underColor = underColor;
			this.base = base;
			this.lowTolerance = lowTolerance;
			this.comp = comp;
			this.under = new Advise.UnderSaturate<A>(base, comp, lowTolerance);
			this.over = new Advise.OverSaturate<A>(base, comp);
		}
		
		public Transfer<A,Color> baseTransfer() {return base;}
		
		public Color emptyValue() {return base.emptyValue();}
 		
		@Override
		public Specialized<A> specialize(Aggregates<? extends A> aggregates) {
			Transfer.Specialized<A,Color> b2 = base.specialize(aggregates);
			Transfer.Specialized<A, Boolean> o2 = over.specialize(aggregates);
			Transfer.Specialized<A, Boolean> u2 = under.specialize(aggregates);
			return new Specialized<A>(overColor, underColor, b2,o2,u2, comp, lowTolerance);
		}
		
		protected static final class Specialized<A> extends OverUnder<A> implements Transfer.Specialized<A, Color> {
			private static final long serialVersionUID = 7535365761511428962L;
			private final Transfer.Specialized<A, Color> base;
			private final Transfer.Specialized<A, Boolean> under;
			private final Transfer.Specialized<A, Boolean> over;

			public Specialized(
					Color overColor, Color underColor,
					Transfer.Specialized<A, Color> base,
					Transfer.Specialized<A, Boolean> over,
					Transfer.Specialized<A, Boolean> under,
					Comparator<A> comp,
					double lowTolerance) {
				super(overColor, underColor, base, lowTolerance, comp);
				this.base = base;
				this.under = under;
				this.over = over;
			}

			public Color at(int x, int y, Aggregates<? extends A> aggregates) {
				boolean below = under.at(x, y, aggregates);
				boolean above = over.at(x, y, aggregates);
				if (above) {
					return overColor;
				} else if (below) {
					return underColor;
				} else {
					return base.at(x, y, aggregates);
				}
			}			
		}
	}

	
	
	/**Highlight aggregates that carry an unusually high amount of value in their neighborhood.
	 * 
	 * Only pixels with a value will be colored.
	 * 
	 * Neighborhood is generally square of size 2*distance+1 and the aggregate set under consideration as its center.
	 * However, no items beyond the bounding box of the data are considered.  
	 * This edge effect is factored out by weighting all measures based on the number aggregates
	 * examined. 
	 * 
	 * **/
	public static class DrawDark implements Transfer<Number, Color> {
		private static final long serialVersionUID = 4417984252053517048L;
		
		/**How large is the neighborhood?**/
		public final int distance;
		
		/**Transfer function used to determine the colors after the ratios have been determined.**/
		public final Transfer<Number, Color> inner;
		
		/**Construct a draw dark using a linear HD interpolation as the inner function.
		 * 
		 * @param low Color to represent average or low value in the neighborhood
		 * @param high Color to represent high value for the neighborhood
		 * @param distance Distance that defines the neighborhood.
		 */
		public DrawDark(Color low, Color high, int distance) {
			this.distance=distance;
			inner = new Numbers.Interpolate(low,high,high,-1);
		}
		
		/**Draw dark using the given transfer for interpolation of the values.**/ 
		public DrawDark(int distance, Transfer<Number,Color> inner) {
			this.distance = distance;
			this.inner = inner;
		}
	

		@Override
		public Specialized specialize(Aggregates<? extends Number> aggs) {
			Aggregates<Double> cache = AggregateUtils.make(aggs.lowX(), aggs.lowY(), aggs.highX(), aggs.highY(), Double.NaN);
			for (int x=aggs.lowX(); x <aggs.highX(); x++) {
				for (int y=aggs.lowY(); y<aggs.highY(); y++) {
					if (aggs.get(x, y).doubleValue() > 0) {
						cache.set(x, y, preprocOne(x,y,aggs));
					} else {
						cache.set(x,y, Double.NaN);
					}
				}
			}
			Transfer.Specialized<Number, Color> innerS = inner.specialize(cache);
			return new Specialized(distance, innerS, cache);
		}
		
		private double preprocOne(int x, int y, Aggregates<? extends Number> aggregates) {
			double surroundingSum =0;
			int cellCount = 0;
			for (int dx=-distance; dx<=distance; dx++) {
				for (int dy=-distance; dy<=distance; dy++) {
					int cx=x+dx;
					int cy=y+dy;
					if (cx < aggregates.lowX() || cy < aggregates.lowY() 
							|| cx>aggregates.highX() || cy> aggregates.highY()) {continue;}
					cellCount++;
					double dv = aggregates.get(cx,cy).doubleValue();
					if (dv != 0) {surroundingSum++;}
				}
			}
			return surroundingSum/cellCount;
		}

		public Color emptyValue() {return Util.CLEAR;}

		protected static final class Specialized extends DrawDark implements Transfer.Specialized<Number,Color> {
			private static final long serialVersionUID = 2548271516304517444L;
			private final Transfer.Specialized<Number, Color> inner;
			Aggregates<Double> cache;
			
			public Specialized(int distance, Transfer.Specialized<Number, Color> inner, Aggregates<Double> cache) {
				super(distance, inner);
				this.inner=inner;
				this.cache = cache;
			}
			public Color at(int x, int y, Aggregates<? extends Number> aggregates) {
				return inner.at(x,y,cache);
			}

		}
	}
	
	/**Implementation of number comparator.  
	 * Mimics behavior of Java 1.7 Number.compare.
	 */
	public static class NumberComp implements Comparator<Number> {
		public int compare(Number o1, Number o2) {return (int) (o1.doubleValue()-o2.doubleValue());}
	}

	/**Find the smallest value.  
	 * 
	 * @param aggs Set of aggregates to search
	 * @param comp Comparator used in the search
	 * @return The location of the first "smallest" value 
	 */
	public static <A> Point min(Aggregates<? extends A> aggs, Comparator<A> comp) {
		A min = aggs.get(aggs.lowX(), aggs.lowY());
		Point p = new Point(aggs.lowX(), aggs.lowY());
		for (int x=aggs.lowX(); x<aggs.highX(); x++) {
			for (int y=aggs.lowY(); y<aggs.highY(); y++) {
				A val = aggs.get(x,y);
				int v = comp.compare(val, min);
				if (v < 0) {
					min = val;
					p.setLocation(x, y);
				}
				
			}
		}
		return p;
	}
	
	/**Find the largest value.  
	 * 
	 * @param aggs Set of aggregates to search
	 * @param comp Comparator used in the search
	 * @return The location of the first "largest" value 
	 */
	public static <A> Point max(Aggregates<? extends A> aggs, Comparator<A> comp) {
		A max = aggs.get(aggs.lowX(), aggs.lowY());
		Point p = new Point(aggs.lowX(), aggs.lowY());
		for (int x=aggs.lowX(); x<aggs.highX(); x++) {
			for (int y=aggs.lowY(); y<aggs.highY(); y++) {
				A val = aggs.get(x,y);
				int v = comp.compare(val, max);
				if (v > 0) {
					max = val;
					p.setLocation(x, y);
				} 
			}
		}
		return p;
	}
}
