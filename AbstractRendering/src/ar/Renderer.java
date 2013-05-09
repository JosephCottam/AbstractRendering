package ar;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public interface Renderer {
	public <A> Aggregates<A> reduce(final GlyphSet glyphs, final AffineTransform inverseView, 
			final Aggregator<A> op, final int width, final int height);
	
	public <A> BufferedImage transfer(Aggregates<A> aggregates, Transfer<A> t, int width, int height, Color background);
}
