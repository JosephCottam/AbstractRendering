package ar.rules;

import java.awt.Color;
import java.awt.Point;
import java.util.Comparator;

import ar.Aggregates;
import ar.Renderer;
import ar.Transfer;
import ar.rules.combinators.Seq;
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
		
		@Override public Boolean emptyValue() {return Boolean.FALSE;}
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
			 
			@Override
			public Aggregates<Boolean> process(Aggregates<? extends A> aggregates, Renderer rend) {
				Aggregates<Color> refImg = rend.transfer(aggregates, ref);
				return rend.transfer(aggregates, new Inner<>(refImg, ref.emptyValue(), comp, tolerance));
			}
		}
		
		private static final class Inner<A> implements Transfer.ItemWise<A, Boolean> {
			private final Color emptyRef;
			private final double tolerance;
			private final Comparator<A> comp;
			private final Aggregates<Color> refImg;

			public Inner(Aggregates<Color> refImg, Color emptyRef, Comparator<A> comp, double tolerance) {
				this.emptyRef = emptyRef;
				this.tolerance = tolerance;
				this.comp = comp;
				this.refImg = refImg;
			}
			
			@Override
			public Aggregates<Boolean> process(Aggregates<? extends A> aggregates, Renderer rend) {
				return rend.transfer(aggregates, this);
			}

			@Override public Boolean emptyValue() {return Boolean.FALSE;}
			@Override public Specialized<A, Boolean> specialize(Aggregates<? extends A> aggregates) {return this;}

			@Override
			public Boolean at(int x, int y, Aggregates<? extends A> aggregates) {
				A def = aggregates.defaultValue();
				A val = aggregates.get(x, y);
				Color out = refImg.get(x, y);
				double distance = euclidean(emptyRef, out);
				return comp.compare(val, def) != 0 && distance < tolerance;
			}		
		}
		
			
		private static final double euclidean(Color c1, Color c2) {
			double r = Math.pow(c1.getRed()-c2.getRed(),2);
			double g = Math.pow(c1.getGreen()-c2.getGreen(),2);
			double b = Math.pow(c1.getBlue()-c2.getBlue(),2);
			return Math.sqrt(r+g+b);
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
			return new Specialized<>(refSpecialized, comp);
		}
		
		protected static final class Specialized<A> extends OverSaturate<A> implements Transfer.Specialized<A,Boolean> {
			private static final long serialVersionUID = 3155281566160217841L;
			private final Transfer.Specialized<A, Color> ref;

			public Specialized(Transfer.Specialized<A, Color> ref, Comparator<A> comp) {
				super(ref, comp);
				this.ref = ref;
			}

			@Override
			public Aggregates<Boolean> process(Aggregates<? extends A> aggregates, Renderer rend) {
				Aggregates<Color> img = rend.transfer(aggregates, ref);

				Point p = max(aggregates, comp);
				A max = aggregates.get(p.x, p.y);
				Color top = img.get(p.x,p.y);
				return rend.transfer(aggregates, new Inner<>(img, max, top, comp));
			}
		}
		private static final class Inner<A> implements Transfer.ItemWise<A, Boolean> {
			final Aggregates<Color> refImg;
			final Color top;
			final A max;
			final Comparator<A> comp;
			
			public Inner(Aggregates<Color> img, A max, Color top, Comparator<A> comp) {
				this.refImg=img;
				this.top = top;
				this.max = max;
				this.comp = comp;
			}

			@Override
			public Aggregates<Boolean> process(Aggregates<? extends A> aggregates, Renderer rend) {
				return rend.transfer(aggregates, this);
			}

			@Override public Boolean emptyValue() {return false;}

			@Override
			public ar.Transfer.Specialized<A, Boolean> specialize(Aggregates<? extends A> aggregates) {
				return this;
			}

			@Override
			public Boolean at(int x, int y, Aggregates<? extends A> aggregates) {
				Color out = refImg.get(x, y);
				boolean same = Util.isEqual(top, out);
				A val = aggregates.get(x,y);
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

			@Override
			public Aggregates<Color> process(Aggregates<? extends A> aggregates, Renderer rend) {
				Aggregates<Boolean> overs = rend.transfer(aggregates, over);
				Aggregates<Boolean> unders = rend.transfer(aggregates, under);
				Aggregates<Color> bases = rend.transfer(aggregates, base);
				return rend.transfer(aggregates, new Inner(overs, unders, bases, base.emptyValue()));
			}		
			
			private class Inner implements Transfer.ItemWise<A,Color> {
				final Aggregates<Boolean> overs;
				final Aggregates<Boolean> unders;
				final Aggregates<Color> bases;
				final Color empty;
				
				public Inner(Aggregates<Boolean> overs,
						Aggregates<Boolean> unders,
						Aggregates<Color> bases,
						Color empty) {
					this.overs = overs;
					this.unders = unders;
					this.bases = bases;
					this.empty = empty;
				}

				@Override
				public Aggregates<Color> process(Aggregates<? extends A> aggregates, Renderer rend) {
					return rend.transfer(aggregates, this);
				}

				@Override
				public Color emptyValue() {return empty;}

				@Override
				public ar.Transfer.Specialized<A, Color> specialize(Aggregates<? extends A> aggregates) {
					return this;
				}
			
				@Override
				public Color at(int x, int y, Aggregates<? extends A> aggregates) {
					boolean below = unders.get(x, y);
					boolean above = overs.get(x, y);
					if (above) {
						return overColor;
					} else if (below) {
						return underColor;
					} else {
						return bases.get(x, y);
					}			
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
			inner = new Numbers.Interpolate<>(low,high,high);
		}
		
		/**Draw dark using the given transfer for interpolation of the values.**/ 
		public DrawDark(int distance, Transfer<Number,Color> inner) {
			this.distance = distance;
			this.inner = inner;
		}
	

		//TODO: Move this work to the constructor...
		@Override
		public Specialized specialize(Aggregates<? extends Number> aggs) {
			return new Specialized(distance, inner, aggs);
		}
		
		
		private static final class RatioNeighbors implements Transfer.ItemWise<Number, Number> {
			private final int distance;
			
			public RatioNeighbors(int distance) {this.distance = distance;}

			@Override
			public Aggregates<Number> process(Aggregates<? extends Number> aggregates, Renderer rend) {
				return rend.transfer(aggregates, this);
			}

			@Override public Double emptyValue() {return 0d;}

			@Override
			public ItemWise<Number, Number> specialize(Aggregates<? extends Number> aggregates) {return this;}
			public Double at(int x, int y, Aggregates<? extends Number> aggregates) {
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
		}

		public Color emptyValue() {return inner.emptyValue();}

		protected static final class Specialized extends DrawDark implements Transfer.Specialized<Number,Color> {
			private static final long serialVersionUID = 2548271516304517444L;
			private final Transfer.Specialized<Number, Color> seq;
			
			public Specialized(int distance, Transfer<Number, Color> inner, Aggregates<? extends Number> aggs) {
				super(distance, inner);
				this.seq= new Seq<>(new RatioNeighbors(distance), inner).specialize(aggs);
			}

			@Override
			public Aggregates<Color> process(Aggregates<? extends Number> aggregates, Renderer rend) {
				return seq.process(aggregates, rend);
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
