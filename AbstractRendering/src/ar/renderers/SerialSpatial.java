package ar.renderers;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import ar.Aggregates;
import ar.Aggregator;
import ar.GlyphSet;
import ar.Renderer;
import ar.Transfer;
import ar.Util;

public final class SerialSpatial implements Renderer {
	private final RenderUtils.Progress recorder;
	
	public SerialSpatial() {recorder = RenderUtils.recorder();}

	
	public <A> Aggregates<A> reduce(final GlyphSet glyphs, final AffineTransform inverseView, 
			final Aggregator<A> op, final int width, final int height) {
		recorder.reset(width*height);
		Aggregates<A> aggregates = new Aggregates<A>(width, height, op.identity());
		renderInto(aggregates, glyphs, recorder, inverseView, op);
		return aggregates;
	}
	
	public static <A> void renderInto(final Aggregates<A> aggregates, final GlyphSet glyphs, 
			RenderUtils.Progress recorder,
			final AffineTransform inverseView, final Aggregator<A> op) {
		Rectangle pixel = new Rectangle(0,0,1,1);
		for (int x=aggregates.lowX(); x<aggregates.highX(); x++) {
			for (int y=aggregates.lowY(); y<aggregates.highY(); y++) {
				pixel.setLocation(x,y);
				A value = op.at(pixel,glyphs,inverseView);
				aggregates.set(x,y,value);
				recorder.update(1);
			}
		}
	}
	
	public <A> BufferedImage transfer(Aggregates<A> aggregates, Transfer<A> t, int width, int height, Color background) {
		BufferedImage i = Util.initImage(width, height, background);
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				try {i.setRGB(x, y, t.at(x, y, aggregates).getRGB());}
				catch (Exception e) {throw new RuntimeException("Error transfering " + x + ", " + y, e);}
			}
		}
		return i;
	}
	public double progress() {return recorder.percent();}
}
