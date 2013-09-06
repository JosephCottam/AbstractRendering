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

/**Tools for working with categorical entries.**/
public class Categories {
	/**What is the first item in the given pixel (an over-plotting strategy)**/
	public static final class First implements Aggregator<Color, Color> {
		private static final long serialVersionUID = 5899328174090941310L;
		public Color combine(long x, long y, Color left, Color update) {
			if (left == Util.CLEAR) {return update;}
			else {return left;}
		}

		public Color rollup(List<Color> sources) {
			if (sources.size() >0) {return sources.get(0);}
			else {return identity();}
		}
		
		public Color identity() {return Util.CLEAR;}
		public boolean equals(Object other) {return other instanceof First;}
		public int hashCode() {return First.class.hashCode();}
	}

	/**What is the last item in the given pixel (an over-plotting strategy)**/
	public static final class Last implements Aggregator<Color, Color> {
		private static final long serialVersionUID = -3640093539839073637L;
		public Color combine(long x, long y, Color left, Color update) {return update;}
		public Color rollup(List<Color> sources) {
			if (sources.size() >0) {return sources.get(sources.size()-1);}
			else {return identity();}
		}
		
		public Color identity() {return Util.CLEAR;}
		public boolean equals(Object other) {return other instanceof Last;}
		public int hashCode() {return Last.class.hashCode();}
	}

	
	/**Return one value if a key value is found,
	 * return another value if the key value is not found.
	 * 
	 * The class is biased towards the key value, so if
	 * multiple values are presented and ANY of them are
	 * not the expected value, then it is treated as the unexpected value.
	 */
	public static final class Binary<IN,OUT> implements Transfer<IN,OUT> {
		private static final long serialVersionUID = 7268579911789809640L;
		private final IN key;
		private final OUT match, noMatch;
		private final Comparator<IN> comp;
		
		/**
		 * @param key Value to check for 
		 * @param match Value to return if the key is found
		 * @param noMatch Value to return if the key is not found
		 * @param comp Comparator to use to determine match; if null then object identity (==) is used.
		 */
		public Binary(IN key, OUT match, OUT noMatch, Comparator<IN> comp) {
			this.key = key;
			this.match = match;
			this.noMatch = noMatch;
			this.comp = comp;
		}

		public OUT at(int x, int y, Aggregates<? extends IN> aggregates) {
			IN v = aggregates.get(x, y);
			if ((comp != null && comp.compare(v, key) == 0) || v == key) {return match;}
			return noMatch;
		}

		public OUT emptyValue() {return noMatch;}

		@Override
		public void specialize(Aggregates<? extends IN> aggregates) {}
		
	}
	
	/**Create run-length-encodings (RLE objects) for each aggregate value.
	 * 
	 * See the class description for {@link ar.rules.CategoricalCounts.RLE} 
	 * @param <T> Type of the categories
	 */
	public static class RunLengthEncode<T> implements Aggregator<T, RLE<T>> {
		private static final long serialVersionUID = 1379800289471184022L;

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
	
	/**Create categorical counts for each aggregate.
	 * 
	 * @param <T> The type of the categories
	 */
	public static final class CountCategories<T> implements Aggregator<T, CoC<T>> {
		private static final long serialVersionUID = 6049570347397483699L;
		private final Comparator<T> comp;
		
		/**Create categories based on the passed comparator.
		 *   
		 * Relative order is ignored, but equality according to the Comparator will yield the same category.
		 * The first instance of a category will be label used. 
		 * 
		 * @param comp
		 */
		public CountCategories(Comparator<T> comp) {this.comp = comp;}
		
		/**Create categories based on the default definition of equality.**/
		public CountCategories() {this(null);}

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
		private static final long serialVersionUID = -7261917422124936899L;
		private final Integer background;
		private final int n;
		
		/**
		 * @param background Value to use if the nth category does not exist
		 * @param n Category to select
		 */
		public NthItem(Integer background, int n) {
			this.background = background;
			this.n = n;
		}
		
		public Integer at(int x, int y, Aggregates<? extends CategoricalCounts<T>> aggregates) {
			CategoricalCounts<T> cats = aggregates.get(x,y);
			if (cats.size() <= n) {return background;}
			else {return cats.count(n);}
		}
		
		public Integer emptyValue() {return background;}
		
		public void specialize(Aggregates<? extends CategoricalCounts<T>> aggregates) {/*No work to perform*/}
	}

	/**Switch between two colors depending on the percent contribution of
	 * a specified category.
	 * 
	 * A particular category is distinguished as the key-category.  If the
	 * key-category constitutes more than X percent of the total then 
	 * return one value.  Otherwise return another.  If category X is not present, return a third.
	 * 
	 ***/
	public static final class KeyPercent<T> implements Transfer<CategoricalCounts<T>, Color> {
		private static final long serialVersionUID = -5019762670520542229L;
		private final double ratio;
		private final Color background, match, noMatch;
		private final Object firstKey;
		
		/**
		 * @param ratio Target ratio
		 * @param keyCategory Category to consider 
		 * @param background Color to return if the key-category is not present
		 * @param match Color to return if key-category constitutes at least ratio percent of the total
		 * @param noMatch Color to return if the key-category does not constitute at least ratio percent of the total
		 */
		public KeyPercent(double ratio, Object keyCategory,  Color background, Color match, Color noMatch) {
			this.ratio = ratio;
			this.background = background;
			this.match = match;
			this.noMatch = noMatch;
			this.firstKey = keyCategory;
		}
		
		public Color at(int x, int y, Aggregates<? extends CategoricalCounts<T>> aggregates) {
			CategoricalCounts<T> cats = aggregates.get(x,y);
			double size = cats.fullSize();
			
			if (size == 0) {return background;}
			else if (!cats.key(0).equals(firstKey)) {return noMatch;} 
			else if (cats.count(0)/size >= ratio) {return match;}
			else {return noMatch;}
		}

		public void specialize(Aggregates<? extends CategoricalCounts<T>> aggregates) {/*No work to perform*/}
		
		public Color emptyValue() {return Util.CLEAR;}
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
		private static final long serialVersionUID = 2468586294425442332L;
		private final Color background;
		private final boolean log;
		private final double omin;
		private int max;

		/**
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
		private static Color fullInterpolate(CategoricalCounts<Color> cats) {
			double total = cats.fullSize();
			double r = 0;
			double g = 0;
			double b = 0;
			
			for (int i=0; i< cats.size(); i++) {
				Color c = cats.key(i);
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
			CategoricalCounts<Color> cats = aggregates.get(x, y);
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
	}
}
