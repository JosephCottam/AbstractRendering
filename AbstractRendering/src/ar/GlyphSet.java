package ar;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.Shape;
import java.util.Collection;

import ar.GlyphSet.Glyph;

public interface GlyphSet extends Iterable<Glyph> {
	
	public Collection<Glyph> containing(Point2D p);
	public boolean isEmpty();
	public int size();
	public Rectangle2D bounds();
	public boolean add(Glyph g);
	
	public static final class Glyph {
		public final Shape shape;
		public final Color color;
		public Glyph(Shape shape, Color color) {this.shape=shape; this.color=color;}
	}
}
