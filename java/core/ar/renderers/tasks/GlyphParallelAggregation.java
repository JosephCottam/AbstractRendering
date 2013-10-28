package ar.renderers.tasks;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RecursiveTask;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Selector;
import ar.aggregates.AggregateUtils;
import ar.aggregates.ConstantAggregates;
import ar.renderers.AggregationStrategies;
import ar.renderers.ProgressReporter;
import ar.util.Util;

public class GlyphParallelAggregation<G,I,A> extends RecursiveTask<Aggregates<A>> {
	private static final long serialVersionUID = 705015978061576950L;
	protected final int taskSize;
	protected final long low;
	protected final long high;
	protected final Glyphset<? extends G, ? extends I> glyphs;
	protected final AffineTransform view;
	protected final Rectangle viewport;
	protected final Aggregator<I,A> op;
	protected final ProgressReporter recorder;
	protected final Selector<G> selector;

	public GlyphParallelAggregation(
		Glyphset<? extends G, ? extends I> glyphs, 
		Selector<G> selector,
		Aggregator<I,A> op,
		AffineTransform view,
		Rectangle viewport,
		int taskSize,
		ProgressReporter recorder,
		long low, long high) {

		this.glyphs = glyphs;
		this.selector = selector;
		this.op = op;
		this.view = view;
		this.viewport =viewport;
		this.taskSize = taskSize;
		this.recorder = recorder;
		this.low = low;
		this.high = high;
	}
	
	protected Aggregates<A> compute() {
		if ((high-low) > taskSize) {return split();}
		else {return local();}
	}
	
	protected final Aggregates<A> local() {
		Glyphset<? extends G, ? extends I> subset = glyphs.segment(low,  high);
		Aggregates<A> target = allocateAggregates(subset);
		selector.processSubset(subset, view, target, op);
		recorder.update(high-low);
		return target;
	}
	
	protected final Aggregates<A> split() {
		long mid = Util.mean(low, high);

		GlyphParallelAggregation<G,I,A> top = new GlyphParallelAggregation<>(glyphs, selector, op, view, viewport, taskSize, recorder, low, mid);
		GlyphParallelAggregation<G,I,A> bottom = new GlyphParallelAggregation<>(glyphs, selector, op, view, viewport, taskSize, recorder, mid, high);
		invokeAll(top, bottom);
		Aggregates<A> aggs;
		try {aggs = AggregationStrategies.horizontalRollup(top.get(), bottom.get(), op);}
		catch (InterruptedException | ExecutionException e) {throw new RuntimeException(e);}
		return aggs;
	}
	
	
	/**DESTRUCTIVELY updates the target at x/y with the value passed and the target operation.**/
	protected final void update(Aggregates<A> target, I v, int x, int y) {
		A existing = target.get(x,y);
		A update = op.combine(x,y,existing, v);
		target.set(x, y, update);
	}
	
	protected Aggregates<A> allocateAggregates(Glyphset<? extends G, ? extends I> subset) {
		//Intersect the subset data with the region to be rendered; skip rendering if there is nothing to render
		Rectangle bounds = view.createTransformedShape(subset.bounds()).getBounds();
		bounds = bounds.intersection(viewport);
		if (bounds.isEmpty()) {
			int x2 = bounds.x+bounds.width;
			int y2 = bounds.y+bounds.height;
			recorder.update(high-low);
			return new ConstantAggregates<A>(Math.min(x2, bounds.x), Math.min(y2, bounds.y),
					Math.max(x2, bounds.x), Math.min(y2, bounds.y),
					op.identity());
		}				
		return AggregateUtils.make(bounds.x, bounds.y,
				bounds.x+bounds.width, bounds.y+bounds.height, 
				op.identity());
	}



}
