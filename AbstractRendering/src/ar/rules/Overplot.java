package ar.rules;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import ar.*;
import ar.GlyphSet.Glyph;

public class Overplot {
	
	private static class First implements Reduction<Color> {
		public Color at(int x, int y, GlyphSet glyphs, AffineTransform v) {
			Point2D p = new Point2D.Double(x,y);
			v.transform(p, p);
			
			for (Glyph g:glyphs) {
				if (g.shape.contains(p)) {return g.color;}
			}
			return Util.CLEAR;
		}		
	}
	
	private static class Last implements Reduction<Color> {
		public Color at(int x, int y, GlyphSet glyphs, AffineTransform v) {
			Point2D p = new Point2D.Double(x,y);
			v.transform(p, p);
			
			Color last= Util.CLEAR;
			for (Glyph g:glyphs) {if (g.shape.contains(p)) {last = g.color;}}
			return last;
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
