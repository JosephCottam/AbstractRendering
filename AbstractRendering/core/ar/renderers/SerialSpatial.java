package ar.renderers;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;
import ar.aggregates.FlatAggregates;

/**Simple renderer that implements the basic abstract rendering algorithm.
 * This class is largely for reference.  In most caes, a parallel renderer is better.
 * **/
public final class SerialSpatial<G,A> implements Renderer<G,A> {
	private final RenderUtils.Progress recorder;
	
	public SerialSpatial() {recorder = RenderUtils.recorder();}

	
	public Aggregates<A> reduce(final Glyphset<G> glyphs, final Aggregator<G,A> op,   
			final AffineTransform inverseView, final int width, final int height) {
		recorder.reset(width*height);
		Aggregates<A> aggregates = new FlatAggregates<A>(width, height, op.identity());
		Rectangle pixel = new Rectangle(0,0,1,1);
		for (int x=aggregates.lowX(); x<aggregates.highX(); x++) {
			for (int y=aggregates.lowY(); y<aggregates.highY(); y++) {
				pixel.setLocation(x,y);
				A value = op.at(pixel,glyphs,inverseView);
				aggregates.set(x,y,value);
				recorder.update(1);
			}
		}
		return aggregates;
	}
	
	public Aggregates<Color> transfer(Aggregates<A> aggregates, Transfer<A,Color> t) {
		Aggregates<Color> out = new FlatAggregates<>(aggregates, t.identity());
		for (int x=aggregates.lowX(); x<aggregates.highX(); x++) {
			for (int y=aggregates.lowY(); y<aggregates.highY(); y++) {
				Color val = t.at(x, y, aggregates);
				out.set(x,y,val);
			}
		}
		return out;
	}
	public double progress() {return recorder.percent();}
}
