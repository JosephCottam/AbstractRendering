package ar.renderers;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicInteger;

import ar.AggregateReducer;
import ar.Aggregates;
import ar.Aggregator;
import ar.GlyphSet;
import ar.Util;
import ar.GlyphSet.Glyph;
import ar.Renderer;
import ar.Transfer;


/**Task-stealing renderer designed for use with a linear stored glyph-set.
 * Iterates the glyphs and produces many aggregate sets that are then combined
 * (i.e., glyph-driven iteration).
 */
public class ParallelGlyphs implements Renderer {
	private final int taskSize;
	private final ForkJoinPool pool = new ForkJoinPool();
	private final AggregateReducer<?,?,?> reducer;

	private static final AtomicInteger counter = new AtomicInteger();
	
	public ParallelGlyphs(int taskSize, AggregateReducer<?,?,?> red) {
		this.taskSize = taskSize;
		this.reducer = red;
		counter.set(0);
	}

	@Override
	public <A> Aggregates<A> reduce(GlyphSet glyphs,
			AffineTransform inverseView, Aggregator<A> op, int width, int height) {
		
		AffineTransform view;
		try {view = inverseView.createInverse();}
		catch (Exception e) {throw new RuntimeException("Error inverting the inverse-view transform....");}
		
		ReduceTask<A> t = new ReduceTask<A>(
				(GlyphSet.RandomAccess) glyphs, 
				view, inverseView, 
				op, (AggregateReducer<A, A, A>) reducer, 
				width, height, taskSize,
				0, glyphs.size());
		return pool.invoke(t);
	}
	
	
	
	public <A> BufferedImage transfer(Aggregates<A> aggregates, Transfer<A> t, int width, int height, Color background) {
		BufferedImage i = Util.initImage(width, height, background);
		for (int x=Math.max(aggregates.lowX(), 0); x<Math.min(aggregates.highX(), width); x++) {
			for (int y=Math.max(aggregates.lowY(), 0); y<Math.min(aggregates.highY(), height); y++) {
				i.setRGB(x, y, t.at(x, y, aggregates).getRGB());
			}
		}
		return i;
	}

	private static final class ReduceTask<A> extends RecursiveTask<Aggregates<A>> {
		private static final long serialVersionUID = 705015978061576950L;

		private final int taskSize;
		private final int low;
		private final int high;
		private final GlyphSet.RandomAccess glyphs;		//TODO: Can some hackery be done with iterators instead so generalized GlyphSet can be used?  At what cost??
		private final AffineTransform view, inverseView;
		private final int width;
		private final int height;
		private final AggregateReducer<A,A,A> reducer;
		private final Aggregator<A> op;
		
		public ReduceTask(GlyphSet.RandomAccess glyphs, 
				AffineTransform view, AffineTransform inverseView,
				Aggregator<A> op, AggregateReducer<A,A,A> reducer, 
				int width, int height, int taskSize,
				int low, int high) {
			this.glyphs = glyphs;
			this.view = view;
			this.inverseView = inverseView;
			this.op = op;
			this.reducer = reducer;
			this.width = width;
			this.height = height;
			this.taskSize = taskSize;
			this.low = low;
			this.high = high;
		}

		@Override
		protected Aggregates<A> compute() {
			if ((high-low) > taskSize) {
				int mid = low+((high-low)/2);
				ReduceTask<A> top = new ReduceTask<A>(glyphs, view, inverseView, op, reducer, width,height, taskSize, low, mid);
				ReduceTask<A> bottom = new ReduceTask<A>(glyphs, view, inverseView, op, reducer, width,height, taskSize, mid, high);
				invokeAll(top, bottom);
				Aggregates<A> aggs = AggregateReducer.Util.reduce(top.getRawResult(), bottom.getRawResult(), reducer);
				System.out.println("Completed: " + counter.get());
				return aggs;
			} else {
				GlyphSubset subset = new GlyphSubset(glyphs, low, high);
				Rectangle bounds = view.createTransformedShape(Util.bounds(subset)).getBounds();
				Aggregates<A> aggregates = new Aggregates<A>(bounds.x, bounds.y,
															 bounds.x+bounds.width+1, bounds.y+bounds.height+1, 
															 op.identity());
				Serial.renderInto(aggregates, subset, inverseView, op);
				counter.addAndGet(high-low);
				return aggregates;
			}
		}
	}
	
	
	public static final class GlyphSubset implements GlyphSet, GlyphSet.RandomAccess, Iterable<Glyph> {
		private final GlyphSet.RandomAccess glyphs;
		private final int low,high;
		private final Glyph[] cache;
		
		public GlyphSubset (GlyphSet.RandomAccess glyphs, int low, int high) {
			this.glyphs = glyphs;
			this.low = low; 
			this.high=high;
			this.cache = new Glyph[high-low];
		}
		
		public GlyphSubsetIterator iterator() {return new GlyphSubsetIterator(this,low,high);}

		public Collection<Glyph> intersects(Rectangle2D r) {
			ArrayList<Glyph> contained = new ArrayList<Glyph>();
			for (Glyph g: this) {if (g.shape.intersects(r)) {contained.add(g);}}
			return contained;
		}

		public boolean isEmpty() {return low >= high;}
		public int size() {return high-low;}
		public Rectangle2D bounds() {return Util.bounds(this);}
		public void add(Glyph g) {throw new UnsupportedOperationException("Cannot add items to subset view.");}

		@Override
		public Glyph get(int i) {
			if (cache[i-low] == null) {cache[i-low] = glyphs.get(i);}
			return cache[i-low];
		}
	}

	public static class GlyphSubsetIterator implements Iterator<Glyph> {
		private final GlyphSet.RandomAccess glyphs;
		private final int high;
		private int at;

		public GlyphSubsetIterator(GlyphSet.RandomAccess glyphs, int low, int high){
			this.glyphs = glyphs;
			this.high=high;
			at = low;
		}
		
		public boolean hasNext() {return at<high;}
		public Glyph next() {return glyphs.get(at++);}
		public void remove() {throw new UnsupportedOperationException();}
		
	}
	
}
