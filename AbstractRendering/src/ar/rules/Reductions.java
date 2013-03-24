package ar.rules;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ar.GlyphSet;
import ar.GlyphSet.Glyph;
import ar.Reduction;
import ar.Util;

public class Reductions {

	public static final class Gradient implements Reduction<Color> {
		private final float width,height;
		public Gradient(int width, int height) {this.width=width; this.height=height;}
		public Color at(int x, int y, GlyphSet glyphs, AffineTransform inverseView) {
			return new Color(x/width, y/height,.5f ,1.0f);
		}
	
	}

	public static final class IDColor implements Reduction<Color> {
		private final Color c;
		public IDColor(Color c) {this.c=c;}
		public Color at(int x, int y, GlyphSet glyphs, AffineTransform inverseView) {return c;}
	}

	public static final class Count implements Reduction<Integer> {
		public Integer at(int x, int y, GlyphSet glyphs, AffineTransform v) {
			Point2D p = new Point2D.Double(x,y);
			v.transform(p, p);
			return glyphs.containing(p).size();
		}
	}

	public static final class First implements Reduction<Color> {
		public Color at(int x, int y, GlyphSet glyphs, AffineTransform v) {
			Point2D p = new Point2D.Double(x,y);
			v.transform(p, p);
			Collection<Glyph> hits = glyphs.containing(p);
			if (hits.size()>0) {return hits.iterator().next().color;}
			else {return Util.CLEAR;}
		}		
	}

	public static final class Last implements Reduction<Color> {
		public Color at(int x, int y, GlyphSet glyphs, AffineTransform v) {
			Point2D p = new Point2D.Double(x,y);
			v.transform(p, p);
			Collection<Glyph> hits = glyphs.containing(p);
			Color color = Util.CLEAR;
			for (Glyph g:hits) {color = g.color;}
			return color;
		}		
	}


	public static final class RLE {
		public final List<Object> keys = new ArrayList<Object>();
		public final List<Integer> counts = new ArrayList<Integer>();
		public void add(Object key, int count) {
			keys.add(key);
			counts.add(count);
		}
		public int count(int i) {return counts.get(i);}
		public Object key(int i) {return keys.get(i);}
		public int size() {return keys.size();}
		public int fullSize() {
			int count=0;
			for (Integer i:counts) {count+=i;}
			return count;
		}
		public String toString() {return "RLE: " + Arrays.deepToString(counts.toArray());}
	}	

	
	public static final class RLEColor implements Reduction<RLE> {
		private static final Comparator<Glyph> glyphColorSorter  = new Comparator<Glyph>() {
			public int compare(Glyph o1, Glyph o2) {return Integer.compare(o1.color.getRGB(), o2.color.getRGB());}
		};
		
		private final boolean sort;
		public RLEColor(boolean sort) {this.sort = sort;}
		
		private List<Glyph> sortColors(Collection<Glyph> glyphs) {
			ArrayList<Glyph> l = new ArrayList<Glyph>(glyphs);
			Collections.sort(l, glyphColorSorter);
			return l;
		}
				
		private RLE encode(List<Glyph> glyphs) {
			RLE rle = new RLE();
			Color key = null;
			int count=0;

			if (glyphs.size() ==0) {return rle;}
			for (Glyph g: glyphs) {
				Color val = g.color;
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
		
		public RLE at(int x, int y, GlyphSet glyphs, AffineTransform v) {
			Point2D p = new Point2D.Double(x,y);
			v.transform(p, p);
			Collection<Glyph> hits = glyphs.containing(p);
			List<Glyph> ordered;
			if (sort) {ordered = sortColors(hits);}
			else {ordered = new ArrayList<Glyph>(hits);}
			return encode(ordered);
		}		
	}

}
