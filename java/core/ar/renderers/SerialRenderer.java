package ar.renderers;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyph;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.Transfer;
import ar.aggregates.AggregateUtils;

/**Simple renderer that implements the basic abstract rendering algorithm.
 * This class is largely for reference.  In almost all cases, a parallel renderer is better.
 * **/
public final class SerialRenderer implements Renderer {
	private static final long serialVersionUID = -377145195943991994L;
	private final ProgressRecorder recorder = new ProgressRecorder.Counter();
	
	

	@Override
	public <I, G, A> Aggregates<A> aggregate(
			Glyphset<? extends G, ? extends I> glyphs, 
			Selector<G> selector,
			Aggregator<I, A> aggregator, 
			AffineTransform viewTransform) {
		
		Rectangle viewport = viewTransform.createTransformedShape(glyphs.bounds()).getBounds();
		
		return aggregate(glyphs, selector, aggregator, viewTransform,
				(defVal) -> AggregateUtils.make(viewport.height, viewport.width, defVal),
				(l,r) -> null);
	}
	
	
	/**
	 * @param merge Ignored in this implementation
	 * @throws IllegalArgumentException If the view transform can't be inverted.**/
	@Override 
	public <I,G,A> Aggregates<A> aggregate(
			final Glyphset<? extends G, ? extends I> glyphs, 
			Selector<G> selector,
			final Aggregator<I,A> op,
			final AffineTransform view,
			Function<A, Aggregates<A>> allocator,
			BiFunction<Aggregates<A>, Aggregates<A>, Aggregates<A>> merge) {
		
		Rectangle viewport = view.createTransformedShape(glyphs.bounds()).getBounds();
		recorder.reset(viewport.height*viewport.width);
		Aggregates<A> aggregates = allocator.apply(op.identity());
		
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
	
	@Override public ProgressRecorder recorder() {return recorder;}	
}
