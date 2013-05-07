package ar.renderers;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

import ar.AggregateReducer;
import ar.Aggregates;
import ar.Aggregator;
import ar.GlyphSet;
import ar.GlyphSet.Glyph;
import ar.Renderer;
import ar.Transfer;
import ar.glyphsets.GlyphList;
import ar.glyphsets.GlyphSingleton;


/**Task-stealing renderer designed for use with a linear stored glyph-set.
 * Iterates the glyphs and produces many aggregate sets that are then combined
 * (i.e., glyph-driven iteration).
 */
public class ParallelGlyphs implements Renderer {
	private final int taskSize;
	private final ForkJoinPool pool = new ForkJoinPool();
	private final AggregateReducer reducer;

	public ParallelGlyphs(int taskSize, AggregateReducer red) {
		this.taskSize = taskSize;
		this.reducer = red;
	}

	@Override
	public <A> Aggregates<A> reduce(GlyphSet glyphs,
			AffineTransform inverseView, Aggregator<A> r, int width, int height) {
		
		AffineTransform view;
		try {view = inverseView.createInverse();}
		catch (Exception e) {throw new RuntimeException("Error inverting the inverse-view transform....");}
		
		ReduceTask<A> t = new ReduceTask<A>((GlyphList) glyphs, view, 
				r, reducer, 
				width, height, taskSize,
				0, glyphs.size());
		return pool.invoke(t);
	}
	
	
	
	public <A> BufferedImage transfer(Aggregates<A> aggregates, Transfer<A> t) {
		final int width = aggregates.width();
		final int height = aggregates.height();
		BufferedImage i = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				i.setRGB(x, y, t.at(x, y, aggregates).getRGB());
			}
		}
		return i;
	}

	private static final class ReduceTask<A> extends RecursiveTask<Aggregates<A>> {
		private static final long serialVersionUID = 705015978061576950L;
		private static final AffineTransform ID = new AffineTransform();

		private final int taskSize;
		private final int low;
		private final int high;
		private final GlyphList glyphs;		//TODO: Can some hackery be done with iterators instead so GlyphSet can be used?  At what cost??
		private final AffineTransform view;
		private final int width;
		private final int height;
		private final AggregateReducer<A,A,A> reducer;
		private final Aggregator<A> op;
		
		public ReduceTask(GlyphList glyphs, AffineTransform view, 
				Aggregator<A> op, AggregateReducer<A,A,A> reducer, 
				int width, int height, int taskSize,
				int low, int high) {
			this.taskSize = taskSize;
			this.low = low;
			this.high = high;
			this.glyphs = glyphs;
			this.view = view;
			this.width = width;
			this.height = height;
			this.op = op;
			this.reducer = reducer;
		}

		@Override
		protected Aggregates<A> compute() {
			if (high-low > taskSize) {
				ReduceTask<A> top = new ReduceTask<A>(glyphs, view, op, reducer, width,height, taskSize, low, high/2);
				ReduceTask<A> bottom = new ReduceTask<A>(glyphs, view, op, reducer, width,height, taskSize, high/2, low);
				invokeAll(top, bottom);
				return AggregateReducer.Util.reduce(top.getRawResult(), bottom.getRawResult(), reducer);
			} else {
				Aggregates<A> aggs = new Aggregates<A>(1, 1, op.defaultValue());
				for (int i=low; i<high; i++) {
					//Discretize the glyph into the aggregates array
					//HACK: assumes the glyph really is a rectangle...
					Glyph g = glyphs.get(i);
					Rectangle2D r = view.createTransformedShape(g.shape).getBounds2D();
					int lowx = (int) Math.floor(r.getMinX());
					int lowy = (int) Math.floor(r.getMinY());
					int highx = (int) Math.ceil(r.getMaxX());
					int highy = (int) Math.ceil(r.getMaxY());
					GlyphSingleton s = new GlyphSingleton(g);
					Aggregates<A> subAggs = new Aggregates<A>(lowx,lowy, highx, highy, op.defaultValue());
					for (int x=lowx; x<highx && x<width; x++){
						for (int y=lowy; y<highy && y<height; y++) {
							A v = op.at(x, y, s, ID);
							subAggs.set(x, y, v);
						}
					}
					
					aggs = AggregateReducer.Util.reduce(aggs, subAggs, reducer);
				}
				return aggs;
			}
		}
		
	}
	
}
