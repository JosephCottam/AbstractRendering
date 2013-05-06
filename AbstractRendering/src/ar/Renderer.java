package ar;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public interface Renderer {
	public <A> Aggregates<A> reduce(final GlyphSet glyphs, final AffineTransform inverseView, 
			final Aggregator<A> r, final int width, final int height);
	
	public <A> BufferedImage transfer(Aggregates<A> aggregates, Transfer<A> t);
}
