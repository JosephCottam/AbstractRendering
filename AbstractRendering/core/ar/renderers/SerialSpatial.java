package ar.renderers;

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
public final class SerialSpatial implements Renderer {
	private final RenderUtils.Progress recorder;
	
	public SerialSpatial() {recorder = RenderUtils.recorder();}

	
	public <V,A> Aggregates<A> reduce(final Glyphset<? extends V> glyphset, final Aggregator<V,A> op,   
			final AffineTransform inverseView, final int width, final int height) {
		recorder.reset(width*height);
		Aggregates<A> aggregates = new FlatAggregates<A>(width, height, op.identity());
		for (int x=aggregates.lowX(); x<aggregates.highX(); x++) {
			for (int y=aggregates.lowY(); y<aggregates.highY(); y++) {
				A val = AggregationStrategies.pixel(aggregates, op, glyphset, inverseView, x, y);
				aggregates.set(x, y, val);
				recorder.update(1);
			}
		}
		return aggregates;
	}
	
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer<IN,OUT> t) {
		Aggregates<OUT> out = new FlatAggregates<OUT>(aggregates, t.emptyValue());
		for (int x=aggregates.lowX(); x<aggregates.highX(); x++) {
			for (int y=aggregates.lowY(); y<aggregates.highY(); y++) {
				OUT val = t.at(x, y, aggregates);
				out.set(x,y,val);
			}
		}
		return out;
	}
	public double progress() {return recorder.percent();}
}
