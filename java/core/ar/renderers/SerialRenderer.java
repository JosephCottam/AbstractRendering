package ar.renderers;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyph;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.Transfer;
import ar.aggregates.AggregateUtils;

/**Simple renderer that implements the basic abstract rendering algorithm.
 * This class is largely for reference.  In most cases, a parallel renderer is better.
 * **/
public final class SerialRenderer implements Renderer {
	private static final long serialVersionUID = -377145195943991994L;
	private final ProgressReporter recorder = RenderUtils.recorder();
	
	/**@throws IllegalArgumentException If the view transform can't be inverted.**/
	@Override 
	public <I,G,A> Aggregates<A> aggregate(
			final Glyphset<? extends G, ? extends I> glyphs, 
			Selector<G> selector,
			final Aggregator<I,A> op,
			final AffineTransform view, final int width, final int height) {
		
		recorder.reset(width*height);
		Aggregates<A> aggregates = AggregateUtils.make(width, height, op.identity());
		
		for (int x=aggregates.lowX(); x<aggregates.highX(); x++) {
			for (int y=aggregates.lowY(); y<aggregates.highY(); y++) {
				A acc = aggregates.get(x, y);
				Collection<Glyph<? extends G, ? extends I>>  subset = new ArrayList<>();
				for (Glyph<? extends G, ? extends I> g: glyphs) {
					if (selector.hitsBin(g, view, x, y)) {subset.add(g);}
				}

				for (Glyph<? extends G, ? extends I> g: subset) {
					I val = g.info();
					acc = op.combine(acc, val);
				}
				
				aggregates.set(x, y, acc);
				recorder.update(1);
			}
		}
		return aggregates;
	}

	@Override 
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer.ItemWise<IN,OUT> t) {
		recorder.reset(AggregateUtils.size(aggregates));
		
		Aggregates<OUT> out = AggregateUtils.make(aggregates, t.emptyValue());
		for (int x=aggregates.lowX(); x<aggregates.highX(); x++) {
			for (int y=aggregates.lowY(); y<aggregates.highY(); y++) {
				OUT val = t.at(x, y, aggregates);
				out.set(x,y,val);
				recorder.update(1);
			}
		}
		return out;
	}

	@Override 
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer.Specialized<IN,OUT> t) {
		if (t instanceof Transfer.ItemWise) {
			return transfer(aggregates, (Transfer.ItemWise<IN, OUT>) t);
		} else {
			return t.process(aggregates, this);
		}
	}
	
	@Override public ProgressReporter progress() {return recorder;}
}
