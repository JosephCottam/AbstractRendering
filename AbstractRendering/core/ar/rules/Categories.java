package ar.rules;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import ar.Aggregator;
import ar.Glyph;
import ar.Glyphset;
import ar.rules.Aggregators.First;
import ar.rules.Aggregators.IDColor;
import ar.rules.Aggregators.Last;
import ar.rules.Aggregators.RLE;
import ar.util.Util;

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
	
	public static class RunLengthEncode<T> implements Aggregator<T, RLE> {
		private final Class<T> type;
		public RunLengthEncode(Class<T> type) {this.type = type;}

		public Class<T> input() {return type;}
		public Class<RLE> output() {return RLE.class;}

		public RLE combine(long x, long y, RLE left, T update) {
			left.add(update, 1);
			return left;
		}

		/**Combines run-length encodings.  Assumes that the presentation order
		 * of the various RLEs matches contiguous blocks.  The result is essentially
		 * concatenating each encoding in iteration order.
		 */
		public RLE rollup(List<RLE> sources) {
			RLE union = new RLE();
			for (RLE r: sources) {
				for (int i=0; i< r.size(); i++) {
					union.add(r.key(i), r.count(i));
				}
			}
			return null;
		}

		public RLE identity() {return new RLE();}
	}
	
	
	/**Encapsulation of run-length encoding information.
	 * A run-length encoding describes the counts of the items found
	 * in the order they were found.  The same category may appear 
	 * multiple times if items of the category are interspersed with
	 * items from other categories.
	 */
	public static final class RLE {
		public final List<Object> keys = new ArrayList<Object>();
		public final List<Integer> counts = new ArrayList<Integer>();
		public int fullSize =0;
		public void add(Object key, int count) {
			keys.add(key);
			counts.add(count);
			fullSize+=count;
		}
		public int count(int i) {return counts.get(i);}
		public Object key(int i) {return keys.get(i);}
		public int size() {return keys.size();}
		public int fullSize() {return fullSize;}
		public String toString() {return "RLE: " + Arrays.deepToString(counts.toArray());}
		public int val(Object category) {
			for (int i=0; i<keys.size();i++) {
				if (keys.get(i).equals(category)) {return counts.get(i);}
			}
			return 0;
		}
		
		public boolean equals(Object other) {
			if (!(other instanceof RLE)) {return false;}
			RLE alter = (RLE) other;
			return counts.equals(alter.counts) && keys.equals(alter.keys);
		}
	}	
	
	/**Merge per-category counts.
	 * 
	 * Because function cannot know the original source interleaving,
	 * it cannot strictly merge run-length encodings.  Instead, it merges
	 * items by category and produces a new summation by category.
	 * This by-category summation is still stored in a run-length encoding
	 * for interaction with other methods that ignore RLE order.
	 * 
	 * TODO: Make a CoC class and an interface that captures what is shared between CoC and RLE.
	 * 
	 * **/
	public static class MergeCOC implements AggregateReducer<RLE,RLE,RLE> {
		public RLE combine(RLE left, RLE right) {
			if (left == null || left.size()==0) {return right;}
			if (right == null || left.size()==0) {return left;}
			
			HashSet<Object> categories = new HashSet<Object>();
			categories.addAll(left.keys);
			categories.addAll(right.keys);
			
			RLE total = new RLE();
			
			for (Object category: categories) {
				int v1 = left.val(category);
				int v2 = right.val(category);
				total.add(category, v1+v2);
			}
			return total;
		}
		
		@Override
		public RLE rollup(List<RLE> sources) {
			RLE acc = new RLE();
			for (RLE entry: sources) {acc = combine(acc, entry);}
			return acc;
		}
		
		
		public RLE zero() {return new RLE();}
		public String toString() {return "CoC (RLE x RLE -> RLE)";}
		public Class<RLE> left() {return RLE.class;}
		public Class<RLE> right() {return RLE.class;}
		public Class<RLE> output() {return RLE.class;}
	}

}
