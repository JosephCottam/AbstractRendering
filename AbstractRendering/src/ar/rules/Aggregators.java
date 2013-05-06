package ar.rules;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import ar.GlyphSet;
import ar.GlyphSet.Glyph;
import ar.Aggregator;
import ar.Util;

/**Aggregators produce aggregates (which go into aggregate sets).**/
public class Aggregators {

	public static final class Gradient implements Aggregator<Color> {
		private final float width,height;
		public Gradient(int width, int height) {this.width=width; this.height=height;}
		public Color at(int x, int y, GlyphSet glyphs, AffineTransform inverseView) {
			return new Color(x/width, y/height,.5f ,1.0f);
		}
	
	}

	public static final class IDColor implements Aggregator<Color> {
		private final Color c;
		public IDColor(Color c) {this.c=c;}
		public Color at(int x, int y, GlyphSet glyphs, AffineTransform inverseView) {return c;}
	}

	public static final class Count implements Aggregator<Integer> {
		public Integer at(int x, int y, GlyphSet glyphs, AffineTransform v) {
			Point2D p = new Point2D.Double(x,y);
			v.transform(p, p);
			Collection<Glyph> items = glyphs.containing(p);
			return items.size();
		}
	}

	public static final class First implements Aggregator<Color> {
		public Color at(int x, int y, GlyphSet glyphs, AffineTransform v) {
			Point2D p = new Point2D.Double(x,y);
			v.transform(p, p);
			Collection<Glyph> hits = glyphs.containing(p);
			if (hits.size()>0) {return hits.iterator().next().color;}
			else {return Util.CLEAR;}
		}		
	}

	public static final class Last implements Aggregator<Color> {
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
	}	

	
	public static final class RLEColor implements Aggregator<RLE> {
		private static final Comparator<Glyph> glyphColorSorter  = new Comparator<Glyph>() {
			public int compare(Glyph o1, Glyph o2) {
				return Integer.compare(o1.color.getRGB(), o2.color.getRGB());
			}
		};
		
		private final boolean sort;
    private final boolean topLeft;
    public RLEColor(boolean sort) {this(sort, false);}
		public RLEColor(boolean sort, boolean topLeft) {
      this.sort = sort;
      this.topLeft = topLeft;
    }
		
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
      
      if (topLeft) {
        Collection<Glyph> superHits = new ArrayList<Glyph>(hits.size());
        for (Glyph g: hits) {
          Rectangle2D bounds = g.shape.getBounds2D();
          Rectangle2D r = new Rectangle2D.Double(x,y,1,1);
          r = v.createTransformedShape(r).getBounds2D();

          if (r.contains(bounds.getX(), bounds.getY())) {
            superHits.add(g);
          }
        }
        hits = superHits;
        if (hits.size() >0) {System.out.println("SIZE:" + hits.size());}
      }


			List<Glyph> ordered;
			if (sort) {ordered = sortColors(hits);}
			else {ordered = new ArrayList<Glyph>(hits);}
			return encode(ordered);
		}		
	}
	
	public static final class DeltaNeighbors implements Aggregator<Integer> {
		private final int reach;
		public DeltaNeighbors(int reach) {this.reach = reach;}
		public Integer at(int x, int y, GlyphSet glyphs, AffineTransform v) {
			Point2D p = new Point2D.Double(x,y);
			v.transform(p, p);
			Collection<Glyph> gs = glyphs.containing(p);
			if (gs.size()==0) {return 0;}

			int count=0;
			HashSet<Color> colors = new HashSet<Color>();
			for (Glyph g:gs) {colors.add(g.color);}
			
			
			for (int xs=x-reach; xs<x+reach; xs++) {
				if (xs<0) {continue;}
				for (int ys=y-reach; ys<y+reach; ys++) {
					if (ys<0) {continue;}
					p.setLocation(xs, ys);
					v.transform(p, p);
					gs = glyphs.containing(p);
					for (Glyph g:gs) {if (!colors.contains(g.color)) {count++;}}
				}
			}
			return count;
		}
	}

}
