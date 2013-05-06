package ar.renderers;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import ar.Aggregates;
import ar.Aggregator;
import ar.GlyphSet;
import ar.Renderer;
import ar.Transfer;


/**Task-stealing renderer designed for use with a linear stored glyph-set.
 * Iterates the glyphs and produces many aggregate sets that are then combined
 * (i.e., glyph-driven iteration).
 */
public class ParallelGlyphs implements Renderer {

	@Override
	public <A> Aggregates<A> reduce(GlyphSet glyphs,
			AffineTransform inverseView, Aggregator<A> r, int width, int height) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <A> BufferedImage transfer(Aggregates<A> aggregates, Transfer<A> t) {
		// TODO Auto-generated method stub
		return null;
	}

}
