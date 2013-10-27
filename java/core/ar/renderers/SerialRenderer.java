package ar.renderers;

import java.awt.geom.AffineTransform;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;
import ar.aggregates.AggregateUtils;

/**Simple renderer that implements the basic abstract rendering algorithm.
 * This class is largely for reference.  In most cases, a parallel renderer is better.
 * **/
public final class SerialRenderer implements Renderer {
	private static final long serialVersionUID = -377145195943991994L;
	private final RenderUtils.Progress recorder = RenderUtils.recorder();
	
	/**@throws IllegalArgumentException If the view transform can't be inverted.**/
	public <I,G,A> Aggregates<A> aggregate(final Glyphset<? extends G, ? extends I> glyphs, final Aggregator<I,A> op,   
			final AffineTransform view, final int width, final int height) {
		
		recorder.reset(width*height);
		Aggregates<A> aggregates = AggregateUtils.make(width, height, op.identity());

		AffineTransform inverseView;
		try {inverseView = view.createInverse();}
		catch (Exception e) {throw new IllegalArgumentException(e);}
		
		
		for (int x=aggregates.lowX(); x<aggregates.highX(); x++) {
			for (int y=aggregates.lowY(); y<aggregates.highY(); y++) {
				A val = AggregationStrategies.pixel(aggregates, op, glyphs, inverseView, x, y);
				aggregates.set(x, y, val);
				recorder.update(1);
			}
		}
		return aggregates;
	}
	
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer.Specialized<IN,OUT> t) {
		Aggregates<OUT> out = AggregateUtils.make(aggregates, t.emptyValue());
		
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
