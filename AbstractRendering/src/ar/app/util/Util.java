package ar.app.util;

import java.awt.geom.Rectangle2D;

import ar.GlyphSet.Glyph;

public class Util {
	public static Rectangle2D bounds(Iterable<Glyph> glyphs) {
		Rectangle2D bounds = new Rectangle2D.Double(0,0,-1,-1);
		for (Glyph g: glyphs) {
			Rectangle2D bound = g.shape.getBounds2D();
			if (bound != null) {add(bounds, bound);}
		}
		return bounds;
	}

	public static Rectangle2D fullBounds(Rectangle2D... rs) {
		Rectangle2D bounds = new Rectangle2D.Double(0,0,-1,-1);
		for (Rectangle2D r: rs) {
			if (r != null) {add(bounds, r);}
		}
		return bounds;
	}

	public static void add(Rectangle2D target, Rectangle2D more) {
		double x = more.getX();
		double y = more.getY();
		double w = more.getWidth();
		double h = more.getHeight();
		
		x = Double.isNaN(x) ? 0 : x;
		y = Double.isNaN(y) ? 0 : y;
		w = Double.isNaN(w) ? 0 : w;
		h = Double.isNaN(h) ? 0 : h;
	
		if (target.isEmpty()) {
			target.setFrame(x,y,w,h);
		} else if (!more.isEmpty()) {
			target.add(new Rectangle2D.Double(x,y,w,h));
		}
	}
}