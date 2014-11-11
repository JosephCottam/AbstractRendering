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
	protected final int lowTask;
	protected final int highTask;
	protected final int totalTasks;
	
	protected final Glyphset<? extends G, ? extends I> glyphs;
	
	/**To save (potentially costly) multiple calculations of the overall bounds, the full bounds are passed around as a parameter.**/
	protected final Rectangle2D glyphBounds;
	protected final AffineTransform view;
	protected final Rectangle viewport;
	protected final Aggregator<I,A> op;
	protected final ProgressReporter recorder;
	protected final Selector<G> selector;

	public GlyphParallelAggregation(
		Glyphset<? extends G, ? extends I> glyphs, 
		Rectangle2D glyphBounds,
		Selector<G> selector,
		Aggregator<I,A> op,
		AffineTransform view,
		Rectangle viewport,
		ProgressReporter recorder,
		int lowTask, int highTask, int totalTasks) {

		this.glyphs = glyphs;
		this.glyphBounds = glyphBounds;
		this.selector = selector;
		this.op = op;
		this.view = view;
		this.viewport =viewport;
		this.recorder = recorder;
		this.lowTask = lowTask;
		this.highTask = highTask;
		this.totalTasks = totalTasks;		
	}
	
	protected Aggregates<A> compute() {
		try {
			if (viewport.isEmpty()) {return new ConstantAggregates<>(op.identity());}
			Aggregates<A> rslt;
			if (highTask-lowTask != 1) {rslt=split();}
			else {rslt=local();}
			recorder.update(UP_MULT);
			
			if (rslt instanceof TouchedBoundsWrapper) {
				TouchedBoundsWrapper<A> tbr = (TouchedBoundsWrapper<A>) rslt;
				if (AggregateUtils.bounds(tbr).equals(AggregateUtils.bounds(tbr.base()))) {return tbr.base();}
			} 
	
			return rslt;
		} catch (AggregationException e) {
			throw e;
		} catch (Throwable t) {
			recorder.message("Error");
			throw new AggregationException(t, "Error processign segments %d-%d of %d",  lowTask, highTask, totalTasks);
		}
	}
	
	protected final Aggregates<A> local() {
		TouchedBoundsWrapper<A> target = allocateAggregates(glyphBounds);
		recorder.update(DOWN_MULT);
		Glyphset<? extends G, ? extends I> subset = glyphs.segmentAt(totalTasks, lowTask);
		selector.processSubset(subset, view, target, op);
		Runtime rt = Runtime.getRuntime();
		
		if (target.untouched()) {return null;}
		else {return target;}
	}
	
	protected final Aggregates<A> split() {
		int midTask = Util.mean(lowTask, highTask);
		
		GlyphParallelAggregation<G,I,A> top = new GlyphParallelAggregation<>(glyphs, glyphBounds, selector, op, view, viewport, recorder, lowTask, midTask, totalTasks);
		GlyphParallelAggregation<G,I,A> bottom = new GlyphParallelAggregation<>(glyphs, glyphBounds, selector, op, view, viewport, recorder, midTask, highTask, totalTasks);
		invokeAll(top, bottom);
		Aggregates<A> aggs;
		
		try {
			aggs = AggregationStrategies.horizontalRollup(top.get(), bottom.get(), op);
			//System.out.printf("%s\n%s\n%s\n------------------\n", AggregateUtils.bounds(top.get()),AggregateUtils.bounds(bottom.get()),AggregateUtils.bounds(aggs));
		}
		catch (InterruptedException | ExecutionException e) {throw new RuntimeException(e);}
		catch (OutOfMemoryError e) {throw new RuntimeException(e);}
		

		return aggs;
	}
	
	
	/**DESTRUCTIVELY updates the target at x/y with the value passed and the target operation.**/
	protected final void update(Aggregates<A> target, I v, int x, int y) {
		A existing = target.get(x,y);
		A update = op.combine(existing, v);
		target.set(x, y, update);
	}
	

	protected TouchedBoundsWrapper<A> allocateAggregates(Rectangle2D bounds) {
		Rectangle fullBounds = view.createTransformedShape(bounds).getBounds();
		Aggregates<A> aggs = AggregateUtils.make(fullBounds.x, fullBounds.y,
				fullBounds.x+fullBounds.width+1, fullBounds.y+fullBounds.height+1, 
				op.identity());
		return new TouchedBoundsWrapper<>(aggs, false);
	}
	
	private static final int DOWN_MULT = 5;
	private static final int UP_MULT = 100;
	public static final long ticks(int taskCount) {
		int tasks = (taskCount*2)-1;
		int down = tasks*DOWN_MULT;
		int up = tasks*UP_MULT;
		return down+up;
	}
	
	public static final class AggregationException extends RuntimeException {
		public AggregationException(Throwable cause, String format, Object... args) {
			super(String.format(format, args), cause);
		}
	}
}
