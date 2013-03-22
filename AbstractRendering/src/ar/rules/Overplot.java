package ar.rules;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Collection;

import ar.*;
import ar.GlyphSet.Glyph;

public class Overplot {
	
	private static class First implements Reduction<Color> {
		public Color at(int x, int y, GlyphSet glyphs, AffineTransform v) {
			Point2D p = new Point2D.Double(x,y);
			v.transform(p, p);
			Collection<Glyph> hits = glyphs.containing(p);
			if (hits.size()>0) {return hits.iterator().next().color;}
			else {return Util.CLEAR;}
		}		
	}
	
	private static class Last implements Reduction<Color> {
		public Color at(int x, int y, GlyphSet glyphs, AffineTransform v) {
			Point2D p = new Point2D.Double(x,y);
			v.transform(p, p);
			Collection<Glyph> hits = glyphs.containing(p);
			Color color = Util.CLEAR;
			for (Glyph g:hits) {color = g.color;}
			return color;
		}		
	}
	
	public static final class T implements Transfer<Color> {
		public Color at(int x, int y, Aggregates<Color> aggregates) {
			return aggregates.at(x, y);
		}
	}
	
	public static Reduction<Color> R(boolean first) {
		if (first) {return new First();} 
		else {return new Last();}
	}
	
	public static Transfer<Color> T() {return new T();}
}
