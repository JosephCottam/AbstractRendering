package ar.rules;

import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import ar.Aggregates;
import ar.Aggregator;
import ar.Transfer;
import ar.rules.CategoricalCounts.CoC;
import ar.util.Util;
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

		public Color rollup(Color left, Color right) {
			if (right != null) {return right;}
			if (left != null) {return left;}
			return identity();
		}
		
		public Color identity() {return Util.CLEAR;}
		public boolean equals(Object other) {return other instanceof First;}
		public int hashCode() {return First.class.hashCode();}
	}

	/**What is the last item in the given pixel (an over-plotting strategy)**/
	public static final class Last implements Aggregator<Color, Color> {
		private static final long serialVersionUID = -3640093539839073637L;
		public Color combine(long x, long y, Color left, Color update) {return update;}
		public Color rollup(Color left, Color right) {
			if (right != null) {return right;}
			if (left != null) {return left;}
			return identity();
		}
		
		public Color identity() {return Util.CLEAR;}
		public boolean equals(Object other) {return other instanceof Last;}
		public int hashCode() {return Last.class.hashCode();}
	}
	

	
	/**Convert a set of categorical counts to its total.**/ 
	public static final class NumCategories<IN> implements Transfer.Specialized<CategoricalCounts<IN>, Integer> {
		private static final long serialVersionUID = -8842454931082209229L;

		@Override
		public Integer at(int x, int y,Aggregates<? extends CategoricalCounts<IN>> aggregates) {
			return aggregates.get(x,y).size();
		}

		@Override
		public Integer emptyValue() {return 0;}

		@Override
		public NumCategories<IN> specialize(Aggregates<? extends CategoricalCounts<IN>> aggregates) {return this;}
	}
	
	
	/**Convert a set of categorical counts to its total.**/ 
	public static final class ToCount<IN> implements Transfer.Specialized<CategoricalCounts<IN>, Integer> {
		private static final long serialVersionUID = -8842454931082209229L;

		@Override
		public Integer at(int x, int y,Aggregates<? extends CategoricalCounts<IN>> aggregates) {
			return aggregates.get(x,y).fullSize();
		}

		@Override
		public Integer emptyValue() {return 0;}

		@Override
		public ToCount<IN> specialize(Aggregates<? extends CategoricalCounts<IN>> aggregates) {return this;}
	}
	
	/**Replace categories with other categories.
	 * 
	 * Useful for (for example) assigning categories to colors.
	 * **/ 
	public static final class ReKey<IN,OUT> implements Transfer.Specialized<CategoricalCounts<IN>, CategoricalCounts<OUT>> {
		private static final long serialVersionUID = -1547309163997797688L;
		
		final CategoricalCounts<OUT> like;
		final Map<IN,OUT> rekey;
		final OUT missing;

		/**
		 * @param like Used as the default value
		 * @param rekey Mapping from key in the input to new key in the output
		 * @param missing Key to use if the input key is not found in the rekey
		 */
		public ReKey(CategoricalCounts<OUT> like, Map<IN,OUT> rekey, OUT missing) {
			this.like = like;
			this.rekey = rekey;
			this.missing= missing;
		}

		@Override
		public CategoricalCounts<OUT> at(int x, int y,
				Aggregates<? extends CategoricalCounts<IN>> aggregates) {
			CategoricalCounts<IN> v = aggregates.get(x, y);
			CategoricalCounts<OUT> acc = emptyValue();
			
			for (int i=0; i<v.size();i++) {
				IN cat = v.key(i);
				int count = v.count(i);
				OUT cat2;
				if (rekey.containsKey(cat)) {cat2 = rekey.get(cat);}
				else {cat2 = missing;}
				acc = acc.extend(cat2, count);
			}
			return acc;
		}

		@Override
		public CategoricalCounts<OUT> emptyValue() {return like.empty();}

		@Override
		public ReKey<IN,OUT> specialize(Aggregates<? extends CategoricalCounts<IN>> aggregates) {return this;}		
	}
	

	
	/**Return one value if a key value is found,
	 * return another value if the key value is not found.
	 * 
	 * The class is biased towards the key value, so if
	 * multiple values are presented and ANY of them are
	 * not the expected value, then it is treated as the unexpected value.
	 */
	public static final class Binary<IN,OUT> implements Transfer.Specialized<IN,OUT> {
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
		public Binary<IN,OUT> specialize(Aggregates<? extends IN> aggregates) {return this;}
		
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
		public RLE<T> rollup(RLE<T> left, RLE<T> right) {
			RLE<T> union = new RLE<T>();
			for (RLE<T> r: Arrays.asList(left, right)) {
				for (int i=0; i< r.size(); i++) {
					union = union.extend(r.key(i), r.count(i));
				}
			}
			return union;
		}

		public RLE<T> identity() {return new RLE<T>();}
	}
	
	/**Given a CoC as value on a glyph, create CoC aggregates.**/
	public static final class MergeCategories<T> implements Aggregator<CoC<T>, CoC<T>> {
		private static final long serialVersionUID = 1L;

		public CoC<T> combine(long x, long y, CoC<T> current, CoC<T> update) {
			return CategoricalCounts.CoC.rollupTwo(current, update);
		}

		public CoC<T> rollup(CoC<T> left, CoC<T> right) {
			return CategoricalCounts.CoC.rollupTwo(left, right);
		}

		public CoC<T> identity() {return new CoC<T>();}
		
		public boolean equals(Object other) {return other instanceof MergeCategories;}
		
		public int hashCode() {return MergeCategories.class.hashCode() + 901812091;}
	}
	
	/**Create categorical counts for each aggregate.
	 * Source data should be individuals of the given category,
	 * 
	 * @param <T> The type of the categories
	 */
	public static final class CountCategories<T> implements Aggregator<T, CoC<T>> {
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
		public CoC<T> rollup(CoC<T> left, CoC<T> right) {
			return CategoricalCounts.CoC.rollupTwo(left, right);
		}

		@Override
		public CoC<T> identity() {return new CoC<T>(comp);}
		
		@SuppressWarnings("rawtypes")
		public boolean equals(Object other) {
			if (!(other instanceof CountCategories)) {return false;}
			CountCategories alter = (CountCategories) other;
			return comp == alter.comp ||
					(comp != null && comp.equals(alter.comp));
		}
		
		public int hashCode() {
			int base = comp == null ? Categories.class.hashCode() : comp.hashCode();
			return base + 891734501; //Plus noise....
		}
	}
	
	/**Pull the nth-item from a set of categories.**/
	public static final class NthItem<T> implements Transfer.Specialized<CategoricalCounts<T>, Integer> {
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
		
		public NthItem<T> specialize(Aggregates<? extends CategoricalCounts<T>> aggregates) {return this;}
	}

	/**Switch between two colors depending on the percent contribution of
	 * a specified category.
	 * 
	 * A particular category is distinguished as the key-category.  If the
	 * key-category constitutes more than X percent of the total then 
	 * return one value.  Otherwise return another.  If category X is not present, return a third.
	 * 
	 ***/
	public static final class KeyPercent<T> implements Transfer.Specialized<CategoricalCounts<T>, Color> {
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

		public KeyPercent<T> specialize(Aggregates<? extends CategoricalCounts<T>> aggregates) {return this;}
		
		public Color emptyValue() {return background;}
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
	public static class HighAlpha implements Transfer<CategoricalCounts<Color>, Color> {
		private static final long serialVersionUID = 2468586294425442332L;
		protected final Color background;
		protected final boolean log;
		protected final double omin;

		/**
		 * @param background Background color
		 * @param omin Opacity minimum (range 0-1)
		 * @param log Use a log scale?
		 */
		public HighAlpha(Color background, double omin, boolean log) {
			this.background = background;
			this.log = log;
			this.omin = omin;
		}
		

		public Color emptyValue() {return background;}
		public HighAlpha.Specialized specialize(Aggregates<? extends CategoricalCounts<Color>> aggregates) {
			int max=Integer.MIN_VALUE;
			for (CategoricalCounts<Color> cats:aggregates) {max = Math.max(max,cats.fullSize());}
			return new Specialized(max, background, omin, log);
		}
		
		protected static final class Specialized extends HighAlpha implements Transfer.Specialized<CategoricalCounts<Color>, Color> {
			private static final long serialVersionUID = 4453971577170705122L;
			private final int max;
			
			public Specialized(int max, Color background, double omin, boolean log) {
				super(background, omin, log);
				this.max = max;
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
					alpha = (int) Math.min(255, (alpha*255));
					c = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) alpha);
				}
				return c;			
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
		}
	}
	
	/**Implements color-weaving with a random distribution of points.**/
	public static class RandomWeave implements Transfer.Specialized<CoC<Color>, Color> {
		private static final long serialVersionUID = -6006747974949256518L;
		
		public Color at(int x, int y,
				Aggregates<? extends CoC<Color>> aggregates) {
			CoC<Color> counts = aggregates.get(x, y);
			int top = counts.fullSize();
			int r = (int) (Math.random()*top);
			for (int i = 0; i<counts.size();i++) {
				int w = counts.count(i);
				r -= w;
				if (r <= 0) {return counts.key(i);}
			}
			if (counts.size() >0) {return counts.key(counts.size()-1);}
			else {return emptyValue();}
		}

		@Override
		public Color emptyValue() {return Util.CLEAR;}

		@Override
		public RandomWeave specialize(Aggregates<? extends CoC<Color>> aggregates) {return this;}		
	}
	
	

}
