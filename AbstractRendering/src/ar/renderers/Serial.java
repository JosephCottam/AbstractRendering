package ar.renderers;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import ar.Aggregates;
import ar.Aggregator;
import ar.GlyphSet;
import ar.Renderer;
import ar.Transfer;

public final class Serial implements Renderer {	
	public <A> Aggregates<A> reduce(final GlyphSet glyphs, final AffineTransform inverseView, 
			final Aggregator<A> r, final int width, final int height) {		
		Aggregates<A> aggregates = new Aggregates<A>(width, height, r.defaultValue()); 
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				A value = r.at(x,y,glyphs,inverseView);
				aggregates.set(x,y,value);
			}
		}
		return aggregates;
	}
	
	public <A> BufferedImage transfer(Aggregates<A> aggregates, Transfer<A> t) {
		final int width = aggregates.highX();
		final int height = aggregates.highY();
		BufferedImage i = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				try {i.setRGB(x, y, t.at(x, y, aggregates).getRGB());}
				catch (Exception e) {throw new RuntimeException("Error transfering " + x + ", " + y, e);}
			}
		}
		return i;
	}
}
