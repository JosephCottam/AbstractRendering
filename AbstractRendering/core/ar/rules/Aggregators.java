package ar.rules;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import ar.Glyph;
import ar.Glyphset;
import ar.util.Util;
import ar.Aggregator;

/**Aggregators produce aggregates (which go into aggregate sets).**/
public class Aggregators {

	/**Compute a gradient across the 2D space.  
	 * This class was used largely for debugging; it ignores its inputs. 
	 */
	public static final class Gradient implements Aggregator<Object, Color> {
		private final float width,height;
		public Gradient(int width, int height) {this.width=width; this.height=height;}
		public Color at(Rectangle r, Glyphset<? extends Object> glyphs, AffineTransform inverseView) {
			return new Color(r.x/width, r.y/height,.5f ,1.0f);
		}
		public Color identity() {return Util.CLEAR;}
		public Class<Object> input() {return Object.class;}
		public Class<Color> output() {return Color.class;}
		public boolean equals(Object other) {return other instanceof Gradient;}

	}

	/**Create a solid fill.  
	 * This class was used largely for debugging; it ignores its inputs. 
	 */
	public static final class IDColor implements Aggregator<Object, Color> {
		private final Color c;
		public IDColor(Color c) {this.c=c;}
		public Color at(Rectangle r, Glyphset<? extends Object> glyphs, AffineTransform inverseView) {return c;}
		public Color identity() {return Util.CLEAR;}
		public Class<Object> input() {return Object.class;}
		public Class<Color> output() {return Color.class;}
		public boolean equals(Object other) {return other instanceof IDColor;}
	}

	
	/**How many items are in the given pixel**/
	public static final class Count implements Aggregator<Object, Integer> {
		public Integer at(Rectangle pixel, Glyphset<? extends Object> glyphs, AffineTransform v) {
			Rectangle2D b = v.createTransformedShape(pixel).getBounds2D();
			Collection<? extends Glyph<? extends Object>> items = glyphs.intersects(b);
			return items.size();
		}
		public Integer identity() {return 0;}
		public Class<Object> input() {return Object.class;}
		public Class<Integer> output() {return Integer.class;}
		public boolean equals(Object other) {return other instanceof Count;}
	}

	/**What is the first item in the given pixel (an over-plotting strategy)**/
	public static final class First implements Aggregator<Color, Color> {
		public Color at(Rectangle pixel, Glyphset<? extends Color> glyphs, AffineTransform v) {
			Rectangle2D b = v.createTransformedShape(pixel).getBounds2D();
			Collection<? extends Glyph<? extends Color>> hits = glyphs.intersects(b);
			if (hits.size()>0) {return hits.iterator().next().value();}
			else {return Util.CLEAR;}
		}
		public Color identity() {return Util.CLEAR;}
		public Class<Color> input() {return Color.class;}
		public Class<Color> output() {return Color.class;}
		public boolean equals(Object other) {return other instanceof First;}
	}

	/**What is the last item in the given pixel (an over-plotting strategy)**/
	public static final class Last implements Aggregator<Color, Color> {
		public Color at(Rectangle pixel, Glyphset<? extends Color> glyphs, AffineTransform v) {
			Rectangle2D b = v.createTransformedShape(pixel).getBounds2D();
			Collection<? extends Glyph<? extends Color>> hits = glyphs.intersects(b);
			Color color = Util.CLEAR;
			for (Glyph<? extends Color> g:hits) {color = g.value();}
			return color;
		}
		public Color identity() {return Util.CLEAR;}
		public Class<Color> input() {return Color.class;}
		public Class<Color> output() {return Color.class;}
		public boolean equals(Object other) {return other instanceof Last;}
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


	/**Run-length encode based on colors.  Optionally sort the items by color before encoding.**/ 
	public static final class RLEColor implements Aggregator<Color, RLE> {
		private static final Comparator<Glyph<? extends Color>> glyphColorSorter  = new Comparator<Glyph<? extends Color>>() {
			public int compare(Glyph<? extends Color> o1, Glyph<? extends Color> o2) {
				return o1.value().getRGB() - o2.value().getRGB();
			}
		};

		private final boolean sort;
		public RLEColor(boolean sort) {
			this.sort = sort;
		}

		private List<Glyph<? extends Color>> sortColors(Collection<? extends Glyph<? extends Color>> glyphs) {
			ArrayList<Glyph<? extends Color>> l = new ArrayList<Glyph<? extends Color>>(glyphs);
			Collections.sort(l, glyphColorSorter);
			return l;
		}

		private RLE encode(List<? extends Glyph<? extends Color>> glyphs) {
			RLE rle = new RLE();
			Color key = null;
			int count=0;

			if (glyphs.size() ==0) {return rle;}
			for (Glyph<? extends Color> g: glyphs) {
				Color val = g.value();
				if ((key == null && val == null) || (key != null && key.equals(val))) {count++;}
				else if (count == 0) {
					key = val;
					count = 1;
				} else if (count > 0) {
					rle.add(key, count);
					key = val;
					count = 1;
				}
			}
			if (count >0) {rle.add(key, count);}
			return rle;
		}

		public RLE at(Rectangle pixel, Glyphset<? extends Color> glyphs, AffineTransform v) {
			Rectangle2D b = v.createTransformedShape(pixel).getBounds2D();
			Collection<? extends Glyph<? extends Color>> hits = glyphs.intersects(b);

			List<Glyph<? extends Color>> ordered;
			if (sort) {ordered = sortColors(hits);}
			else {ordered = new ArrayList<Glyph<? extends Color>>(hits);}
			return encode(ordered);
		}
		public RLE identity() {return new RLE();}
		public Class<Color> input() {return Color.class;}
		public Class<RLE> output() {return RLE.class;}
		public boolean equals(Object other) {
			if (!(other instanceof RLEColor)) {return false;}
			RLEColor alter = (RLEColor) other;
			return alter.sort == sort;
		}
	}

	
	/**Compare the items found in a given pixel to the items found 
	 * in the rectangle n-steps to the left/right/top/bottom (current pixel is the center).
	 * Report how many times a neighboring pixel had an item that belonged to a category
	 * when the current pixel does not have an item belonging to that category.
	 * Item category is determined by color in this case.
	 *  
	 * @author jcottam
	 *
	 */
	public static final class DeltaNeighbors implements Aggregator<Object, Integer> {
		private final int reach;
		public DeltaNeighbors(int reach) {this.reach = reach;}
		public Integer at(Rectangle pixel, Glyphset<? extends Object> glyphs, AffineTransform v) {
			Rectangle2D b = v.createTransformedShape(pixel).getBounds2D();
			Collection<? extends Glyph<? extends Object>> gs = glyphs.intersects(b);
			if (gs.size()==0) {return 0;}

			int count=0;
			HashSet<Object> categories = new HashSet<Object>();
			for (Glyph<? extends Object> g:gs) {categories.add(g.value());}
		
			int x = pixel.x;
			int y = pixel.y;

			for (int xs=x-reach; xs<x+reach; xs++) {
				if (xs<0) {continue;}
				for (int ys=y-reach; ys<y+reach; ys++) {
					if (ys<0) {continue;}
					Rectangle r2 = new Rectangle(x,y, pixel.width, pixel.height);
					b = v.createTransformedShape(r2).getBounds2D();
					gs = glyphs.intersects(b);
					for (Glyph<? extends Object> g:gs) {if (!categories.contains(g.value())) {count++;}}
				}
			}
			return count;
		}
		public Integer identity() {return 0;}
		public Class<Object> input() {return Object.class;}
		public Class<Integer> output() {return Integer.class;}
		public boolean equals(Object other) {
			if (!(other instanceof DeltaNeighbors)) {return false;}
			DeltaNeighbors alter = (DeltaNeighbors) other;
			return alter.reach == reach;
		}
	}

}
