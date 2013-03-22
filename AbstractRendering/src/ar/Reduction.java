package ar;

import java.awt.geom.AffineTransform;

public interface Reduction<A> {
	public A at(int x, int y, GlyphSet glyphs, AffineTransform inverseView);
}
