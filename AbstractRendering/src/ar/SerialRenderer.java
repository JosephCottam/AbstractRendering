package ar;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public final class SerialRenderer implements Renderer {	
	public <A> Aggregates<A> reduce(final GlyphSet glyphs, final AffineTransform inverseView, 
			final Reduction<A> r, final int width, final int height) {		
		Aggregates<A> aggregates = new Aggregates<A>(width, height); 
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				A value = r.at(x,y,glyphs,inverseView);
				aggregates.set(x,y,value);
			}
		}
		return aggregates;
	}
	
	public <A> BufferedImage transfer(Aggregates<A> aggregates, Transfer<A> t) {
		final int width = aggregates.width();
		final int height = aggregates.height();
		BufferedImage i = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				i.setRGB(x, y, t.at(x, y, aggregates).getRGB());
			}
		}
		return i;
	}
}
