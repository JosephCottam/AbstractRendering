package ar.rules;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;

import ar.Aggregates;
import ar.Aggregator;
import ar.Transfer;
import ar.util.Util;
import static ar.rules.CategoricalCounts.CoC;
import static ar.rules.CategoricalCounts.RLE;

public class Categories {
	/**What is the first item in the given pixel (an over-plotting strategy)**/
	public static final class First implements Aggregator<Color, Color> {
		public Color combine(long x, long y, Color left, Color update) {
			if (left == Util.CLEAR) {return update;}
			else {return left;}
		}

		public Color rollup(List<Color> sources) {
			if (sources.size() >0) {return sources.get(0);}
			else {return identity();}
		}
		
		public Color identity() {return Util.CLEAR;}
		public Class<Color> input() {return Color.class;}
		public Class<Color> output() {return Color.class;}
		public boolean equals(Object other) {return other instanceof First;}
	}

	/**What is the last item in the given pixel (an over-plotting strategy)**/
	public static final class Last implements Aggregator<Color, Color> {
		public Color combine(long x, long y, Color left, Color update) {return update;}
		public Color rollup(List<Color> sources) {
			if (sources.size() >0) {return sources.get(sources.size()-1);}
			else {return identity();}
		}
		
		public Color identity() {return Util.CLEAR;}
		public Class<Color> input() {return Color.class;}
		public Class<Color> output() {return Color.class;}
		public boolean equals(Object other) {return other instanceof Last;}
	}

	
	/**Return a specific value if an item is present, 
	 * otherwise return another specific value. 
	 *
	 * @param <T> Type of the value that may be returned 
	 */
	public static final class Binary<T> implements Aggregator<T,T> {
		private final T val, def;
		private final Class<T> type;
		public Binary(T val, T def, Class<T> type) {
			this.val = val;
			this.type = type;
			this.def = def;
		}

		public T combine(long x, long y, T left, T update) {return val;}
		public T rollup(List<T> sources) {
			for (T s: sources) {if (s != def) {return val;}}
			return def;
		}

		public T identity() {return def;}
		public Class<T> input() {return type;}
		public Class<T> output() {return type;}
		public boolean equals(Object other) {
			return other instanceof Binary && this.val == ((Binary<?>) other).val;
		}
	}
	
	public static class RunLengthEncode<T> implements Aggregator<T, RLE<T>> {
		private final Class<T> type;
		public RunLengthEncode(Class<T> type) {this.type = type;}

		@SuppressWarnings("rawtypes")
		public Class<RLE> output() {return RLE.class;}
		public Class<T> input() {return type;}
		

		public RLE<T> combine(long x, long y, RLE<T> left, T update) {
			return left.extend(update, 1);
		}

		/**Combines run-length encodings.  Assumes that the presentation order
		 * of the various RLEs matches contiguous blocks.  The result is essentially
		 * concatenating each encoding in iteration order.
		 */
		public RLE<T> rollup(List<RLE<T>> sources) {
			RLE<T> union = new RLE<T>();
			for (RLE<T> r: sources) {
				for (int i=0; i< r.size(); i++) {
					union = union.extend(r.key(i), r.count(i));
				}
			}
			return union;
		}

		public RLE<T> identity() {return new RLE<T>();}
	}
	
	
	public static final class CountCategories<T> implements Aggregator<T, CoC<T>> {
		private final Class<T> type;
		private final Comparator<T> comp;
		
		public CountCategories(Class<T> type) {this(null, type);}
		public CountCategories(Comparator<T> comp, Class<T> type) {
			this.comp = comp;
			this.type = type;
		}

		public Class<T> input() {return type;}
		public Class<CoC<T>> output() {return (Class<CoC<T>>) identity().getClass();}

		@Override
		public CoC<T> combine(long x, long y, CoC<T> left, T update) {
			return left.extend(update, 1);
		}

		@Override
		public CoC<T> rollup(List<CoC<T>> sources) {
			CoC<T> combined = new CoC<T>(comp);
			for (CoC<T> counts: sources) {
				for (T key: counts.counts.keySet()) {
					combined = combined.extend(key, counts.count(key));
				}
			}
			return combined;
		}

		@Override
		public CoC<T> identity() {return new CoC<T>(comp);}
	}
	
	/**Pull the nth-item from a set of categories.**/
	public static final class NthItem<T> implements Transfer<CategoricalCounts<T>, Integer> {
		private final Integer background;
		private final int n;
		
		public NthItem(Integer background, int n) {
			this.background = background;
			this.n = n;
		}
		
		public Integer at(int x, int y, Aggregates<? extends CategoricalCounts<T>> aggregates) {
			CategoricalCounts<T> cats = aggregates.at(x,y);
			if (cats.size() <= n) {return background;}
			else {return cats.count(n);}
		}
		
		public Integer emptyValue() {return background;}
		
		@SuppressWarnings("rawtypes")
		public Class<CategoricalCounts> input() {return CategoricalCounts.class;}
		public Class<Integer> output() {return Integer.class;}
		public void specialize(Aggregates<? extends CategoricalCounts<T>> aggregates) {/*No work to perform*/}
	}

	/**Switch between two colors depending on the percent contribution of
	 * a specified category.
	 ***/
	public static final class FirstPercent<T> implements Transfer<CategoricalCounts<T>, Color> {
		private final double ratio;
		private final Color background, match, noMatch;
		private final Object firstKey;
		
		public FirstPercent(double ratio, Object firstKey,  Color background, Color match, Color noMatch) {
			this.ratio = ratio;
			this.background = background;
			this.match = match;
			this.noMatch = noMatch;
			this.firstKey = firstKey;
		}
		
		public Color at(int x, int y, Aggregates<? extends CategoricalCounts<T>> aggregates) {
			CategoricalCounts<T> cats = aggregates.at(x,y);
			double size = cats.fullSize();
			
			if (size == 0) {return background;}
			else if (!cats.key(0).equals(firstKey)) {return noMatch;} 
			else if (cats.count(0)/size >= ratio) {return match;}
			else {return noMatch;}
		}

		public void specialize(Aggregates<? extends CategoricalCounts<T>> aggregates) {/*No work to perform*/}
		
		public Color emptyValue() {return Util.CLEAR;}
		
		@SuppressWarnings("rawtypes")
		public Class<CategoricalCounts> input() {return CategoricalCounts.class;}
		public Class<Color> output() {return Color.class;}

	}
	
	
	/**Performs high-definition alpha composition on a run-length encoding.
	 * High-definition alpha composition computes color compositions in double space
	 * with knowledge of the full range of compositions that will be required.
	 * (See "Visual Analysis of Inter-Process Communication for Large-Scale Parallel Computing"
	 *  by Chris Muelder, Francois Gygi, and Kwan-Liu Ma).
	 *  
	 * @author jcottam
	 *
	 */
	public static final class HighAlpha implements Transfer<CategoricalCounts<Color>, Color> {
		private final Color background;
		private final boolean log;
		private final double omin;
		private int max;

		/**
		 * @param colors Mapping from categories to colors
		 * @param reserve Color to use for a cateogry not found in the mapping
		 * @param background Background color
		 * @param omin Opacity minimum
		 * @param log Use a log scale?
		 */
		public HighAlpha(Color background, double omin, boolean log) {
			this.background = background;
			this.log = log;
			this.omin = omin;
		}
		
		//TODO: Update to use a color mapping outside of the category set
		private Color fullInterpolate(CategoricalCounts<Color> cats) {
			double total = cats.fullSize();
			double r = 0;
			double g = 0;
			double b = 0;
			
			for (int i=0; i< cats.size(); i++) {
				Color c = (Color) cats.key(i);
				double p = cats.count(i)/total;
				double r2 = (c.getRed()/255.0) * p;
				double g2 = (c.getGreen()/255.0) * p;
				double b2 = (c.getBlue()/255.0) * p;

				r += r2;
				g += g2;
				b += b2;
			}
			return new Color((int) (r*255), (int) (g * 255), (int) (b*255));
		}
		
		public Color at(int x, int y, Aggregates<? extends CategoricalCounts<Color>> aggregates) {
			CategoricalCounts<Color> cats = aggregates.at(x, y);
			Color c;
			if (cats.fullSize() == 0) {c = background;}
			else {
				c = fullInterpolate(cats);
				double alpha;
				if (log) {
					alpha = omin + ((1-omin) * (Math.log(cats.fullSize())/Math.log(max)));
				} else {
					alpha = omin + ((1-omin) * (cats.fullSize()/max));
				}
				c = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (alpha*255));
			}
			return c;			
		}
		
		public Color emptyValue() {return Util.CLEAR;}
		public void specialize(Aggregates<? extends CategoricalCounts<Color>> aggregates) {
			for (CategoricalCounts<Color> cats:aggregates) {max = Math.max(max,cats.fullSize());}
		}

		@SuppressWarnings("rawtypes")
		public Class<CategoricalCounts> input() {return CategoricalCounts.class;}
		public Class<Color> output() {return Color.class;}
	}

	

}
