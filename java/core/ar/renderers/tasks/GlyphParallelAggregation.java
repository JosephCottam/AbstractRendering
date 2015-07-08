package ar.renderers.tasks;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RecursiveTask;
import java.util.function.BiFunction;
import java.util.function.Function;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Selector;
import ar.aggregates.AggregateUtils;
import ar.aggregates.wrappers.TouchedBoundsWrapper;
import ar.renderers.ProgressRecorder;
import ar.util.Util;

public class GlyphParallelAggregation<G,I,A> extends RecursiveTask<Aggregates<A>> {
	private static final long serialVersionUID = 705015978061576950L;
	
	protected final List<Glyphset<G, I>> glyphs;
	
	/**To save (potentially costly) multiple calculations of the overall bounds, the full bounds are passed around as a parameter.**/
	protected final Rectangle2D glyphBounds;
	protected final AffineTransform view;
	protected final Aggregator<? super I,A> op;
	protected final ProgressRecorder recorder;
	protected final Selector<? super G> selector;
	protected final int low, high;
	protected final Function<A, Aggregates<A>> allocator;
	protected final BiFunction<Aggregates<A>, Aggregates<A>, Aggregates<A>> merge;

	public GlyphParallelAggregation(List<Glyphset<G, I>> glyphs, 
			Rectangle2D glyphBounds,
			Selector<? super G> selector,
			Aggregator<? super I, A> op,
			AffineTransform view,
			Function<A, Aggregates<A>> allocator,
			BiFunction<Aggregates<A>, Aggregates<A>, Aggregates<A>> merge,
			ProgressRecorder recorder) {
		this(glyphs, glyphBounds, selector, op, view, allocator, merge, recorder, 0, glyphs.size());
	}
	
	private GlyphParallelAggregation(
		List<Glyphset<G, I>> glyphs, 
		Rectangle2D glyphBounds,
		Selector<? super G> selector,
		Aggregator<? super I,A> op,
		AffineTransform view,
		Function<A, Aggregates<A>> allocator,
		BiFunction<Aggregates<A>, Aggregates<A>, Aggregates<A>> merge,
		ProgressRecorder recorder,
		int low,
		int high) {

		this.glyphs = glyphs;
		this.glyphBounds = glyphBounds;
		this.selector = selector;
		this.op = op;
		this.view = view;
		this.allocator = allocator;
		this.merge = merge;
		this.recorder = recorder;
		this.low = low;
		this.high = high;
	}
	
	protected Aggregates<A> compute() {
		try {
			Aggregates<A> rslt;
			if (high-low > 1) {rslt=split();}
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
			throw new AggregationException(t, String.format("Error processign segment %d-%d", low,high));
		}
	}
	
	protected final Aggregates<A> local() {
		Aggregates<A> target = allocator.apply(op.identity());
		recorder.update(DOWN_MULT);
		selector.processSubset(glyphs.get(low), view, target, op);
		
		if (target.empty()) {return null;}
		else {return target;}
	}
	
	protected final Aggregates<A> split() {
		int midTask = Util.mean(low, high);
		
		GlyphParallelAggregation<G,I,A> top = new GlyphParallelAggregation<>(glyphs, glyphBounds, selector, op, view, allocator, merge, recorder, low, midTask);
		GlyphParallelAggregation<G,I,A> bottom = new GlyphParallelAggregation<>(glyphs, glyphBounds, selector, op, view, allocator, merge, recorder, midTask, high);
		invokeAll(top, bottom);
		Aggregates<A> aggs;
		
		try {aggs = merge.apply(top.get(), bottom.get());}
		catch (InterruptedException | ExecutionException e) {throw new RuntimeException(e);}
		catch (OutOfMemoryError e) {throw new RuntimeException(e);}
		

		return aggs;
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
