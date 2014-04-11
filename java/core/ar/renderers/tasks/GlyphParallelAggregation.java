package ar.renderers.tasks;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RecursiveTask;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Selector;
import ar.aggregates.AggregateUtils;
import ar.aggregates.implementations.ConstantAggregates;
import ar.aggregates.wrappers.TouchedBoundsWrapper;
import ar.renderers.AggregationStrategies;
import ar.renderers.ProgressReporter;
import ar.util.Util;

public class GlyphParallelAggregation<G,I,A> extends RecursiveTask<Aggregates<A>> {
	private static final long serialVersionUID = 705015978061576950L;
	protected final long taskSize;
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
		Rectangle viewport,  //TODO: Remove, lift empty check out (or don't bother with it...)
		long taskSize,
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
		if (viewport.isEmpty()) {return new ConstantAggregates<>(op.identity());}
		Aggregates<A> rslt;
		if ((high-low) > taskSize) {rslt=split();}
		else {rslt=local();}
		recorder.update((high-low)/3);
		
		if (rslt instanceof TouchedBoundsWrapper) {
			TouchedBoundsWrapper<A> tbr = (TouchedBoundsWrapper<A>) rslt;
			if (AggregateUtils.bounds(tbr).equals(AggregateUtils.bounds(tbr.base()))) {return tbr.base();}
		} 

		return rslt;
	}
	
	protected final Aggregates<A> local() {
		long step = recorder.reportStep() <= 0 ? high-low : recorder.reportStep();  //How often should reports be made?
		Aggregates<A> target = allocateAggregates(glyphs.bounds());
		
		for (long bottom=low; bottom < high; bottom+= step) {
			long top = Math.min(bottom+step, high);
			Glyphset<? extends G, ? extends I> subset = glyphs.segment(bottom, top);
			selector.processSubset(subset, view, target, op);
			recorder.update(2*(step/3));
		}
		
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
		A update = op.combine(existing, v);
		target.set(x, y, update);
	}
	

	protected Aggregates<A> allocateAggregates(Rectangle2D bounds) {
		Rectangle fullBounds = view.createTransformedShape(bounds).getBounds();
		Aggregates<A> aggs = AggregateUtils.make(fullBounds.x, fullBounds.y,
				fullBounds.x+fullBounds.width, fullBounds.y+fullBounds.height, 
				op.identity());
		return new TouchedBoundsWrapper<>(aggs, false);
	}	
}
