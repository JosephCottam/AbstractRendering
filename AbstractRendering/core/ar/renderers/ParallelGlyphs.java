package ar.renderers;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import ar.AggregateReducer;
import ar.Aggregates;
import ar.Aggregator;
import ar.GlyphSet;
import ar.GlyphSet.Glyph;
import ar.util.Util;
import ar.Renderer;
import ar.Transfer;


/**Task-stealing renderer that works on a per-glyph basis, designed for use with a linear stored glyph-set.
 * Iterates the glyphs and produces many aggregate sets that are then combined
 * (i.e., glyph-driven iteration).
 */
public class ParallelGlyphs implements Renderer {	
	private final ForkJoinPool pool = new ForkJoinPool();

	private final int taskSize;
	private final AggregateReducer<?,?,?> reducer;
	private final RenderUtils.Progress recorder;

	public ParallelGlyphs(int taskSize, AggregateReducer<?,?,?> red) {
		this.taskSize = taskSize;
		this.reducer = red;
		recorder = RenderUtils.recorder();
	}
	
	protected void finalize() {pool.shutdownNow();}

	@Override
	public <A> Aggregates<A> reduce(GlyphSet glyphs, Aggregator<A> op, 
			AffineTransform inverseView, int width, int height) {
		
		AffineTransform view;
		try {view = inverseView.createInverse();}
		catch (Exception e) {throw new RuntimeException("Error inverting the inverse-view transform....");}
		recorder.reset(glyphs.size());
		
		ReduceTask<A> t = new ReduceTask<A>(
				(GlyphSet.RandomAccess) glyphs, 
				view, inverseView, 
				op, (AggregateReducer<A, A, A>) reducer, 
				width, height, taskSize,
				recorder,
				0, glyphs.size());
		
		Aggregates<A> a= pool.invoke(t);
		
		return a;
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
	
	public double progress() {return recorder.percent();}

	private static final class ReduceTask<A> extends RecursiveTask<Aggregates<A>> {
		private static final long serialVersionUID = 705015978061576950L;

		private final int taskSize;
		private final long low;
		private final long high;
		private final GlyphSet.RandomAccess glyphs;		//TODO: Can some hackery be done with iterators instead so generalized GlyphSet can be used?  At what cost??
		private final AffineTransform view, inverseView;
		private final int width;
		private final int height;
		private final AggregateReducer<A,A,A> reducer;
		private final Aggregator<A> op;
		private final RenderUtils.Progress recorder;

		
		public ReduceTask(GlyphSet.RandomAccess glyphs, 
				AffineTransform view, AffineTransform inverseView,
				Aggregator<A> op, AggregateReducer<A,A,A> reducer, 
				int width, int height, int taskSize,
				RenderUtils.Progress recorder,
				long low, long high) {
			this.glyphs = glyphs;
			this.view = view;
			this.inverseView = inverseView;
			this.op = op;
			this.reducer = reducer;
			this.width = width;
			this.height = height;
			this.taskSize = taskSize;
			this.recorder = recorder;
			this.low = low;
			this.high = high;
		}

		protected Aggregates<A> compute() {
			if ((high-low) > taskSize) {return split();}
			else {return local();}
		}
		
		private final Aggregates<A> split() {
			long mid = low+((high-low)/2);
			ReduceTask<A> top = new ReduceTask<A>(glyphs, view, inverseView, op, reducer, width,height, taskSize, recorder, low, mid);
			ReduceTask<A> bottom = new ReduceTask<A>(glyphs, view, inverseView, op, reducer, width,height, taskSize, recorder, mid, high);
			invokeAll(top, bottom);
			Aggregates<A> aggs = Util.reduceAggregates(top.getRawResult(), bottom.getRawResult(), reducer);
			return aggs;
		}
		
		//TODO: Respect the actual shape.  Currently assumes that the bounds box matches the actual item bounds..
		private final Aggregates<A> local() {
			GlyphSet.RandomAccess subset = new GlyphSubset(glyphs, low, high);
			Rectangle bounds = view.createTransformedShape(Util.bounds(subset)).getBounds();
			Aggregates<A> aggregates = new Aggregates<A>(bounds.x, bounds.y,
														 bounds.x+bounds.width+1, bounds.y+bounds.height+1, 
														 op.identity());
			
			
			Point2D lowP = new Point2D.Double();
			Point2D highP = new Point2D.Double();
			
			for (Glyph g: subset) {
				//Discretize the glyph into the aggregates array
				Rectangle2D b = g.shape.getBounds2D();
				lowP.setLocation(b.getMinX(), b.getMinY());
				highP.setLocation(b.getMaxX(), b.getMaxY());
				
				view.transform(lowP, lowP);
				view.transform(highP, highP);
				
				int lowx = (int) Math.floor(lowP.getX());
				int lowy = (int) Math.floor(lowP.getY());
				int highx = (int) Math.ceil(highP.getX());
				int highy = (int) Math.ceil(highP.getY());

				Rectangle pixel = new Rectangle(lowx, lowy, 1,1);
				A v = op.at(pixel, new GlyphSingleton(g), inverseView);
				
				
				for (int x=Math.max(0,lowx); x<highx && x<width; x++){
					for (int y=Math.max(0, lowy); y<highy && y<height; y++) {
						aggregates.set(x, y, reducer.combine(aggregates.at(x,y), v));
					}
				}
			}
			
			recorder.update(high-low);
			return aggregates;
		}
	}
	
	
	public static final class GlyphSingleton implements GlyphSet.RandomAccess  {
		private final List<Glyph> glyphs;
		private final Glyph glyph;
		private final Rectangle2D bounds;
		
		public GlyphSingleton(Glyph g) {
			glyphs = Collections.singletonList(g);
			glyph = g;
			bounds = g.shape.getBounds2D();
		}
		
		public Iterator<Glyph> iterator() {return glyphs.iterator();}
		public Glyph get(long i) {return glyphs.get(0);}
		public boolean isEmpty() {return glyphs.isEmpty();}
		public void add(Glyph g) {throw new UnsupportedOperationException();}
		public long size() {return glyphs.size();}
		public Rectangle2D bounds() {return bounds;}

		public Collection<Glyph> intersects(Rectangle2D r) {
			if (glyph.shape.intersects(r)) {return glyphs;}
			else {return Collections.emptyList();}
		}
	}
	
	public static final class GlyphSubset implements GlyphSet.RandomAccess {
		private final GlyphSet.RandomAccess glyphs;
		private final long low,high;
		private final Glyph[] cache;
		
		public GlyphSubset (GlyphSet.RandomAccess glyphs, long low, long high) {
			this.glyphs = glyphs;
			this.low = low; 
			this.high=high;
			if (high-low > Integer.MAX_VALUE) {throw new IllegalArgumentException("Must subset smaller number of items (this class uses int-based caching)");}
			this.cache = new Glyph[(int) (high-low)];
		}
		
		public GlyphSubsetIterator iterator() {return new GlyphSubsetIterator(this,low,high);}

		public Collection<Glyph> intersects(Rectangle2D r) {
			ArrayList<Glyph> contained = new ArrayList<Glyph>();
			for (int i=0; i<cache.length; i++) {
				Glyph g = get(i+low);
				if (g.shape.intersects(r)) {contained.add(g);}
			}
			for (Glyph g: this) {if (g.shape.intersects(r)) {contained.add(g);}}
			return contained;
		}

		public boolean isEmpty() {return low >= high;}
		public long size() {return high-low;}
		public Rectangle2D bounds() {return Util.bounds(this);}
		public void add(Glyph g) {throw new UnsupportedOperationException("Cannot add items to subset view.");}

		@Override
		public Glyph get(long l) {
			long at = l-low;
			if (at > cache.length) {throw new IllegalArgumentException();}
			int i= (int) at;
			
			if (cache[i] == null) {cache[i] = glyphs.get(l);}
			return cache[i];
		}
	}

	public static class GlyphSubsetIterator implements Iterator<Glyph> {
		private final GlyphSet.RandomAccess glyphs;
		private final long high;
		private long at;

		public GlyphSubsetIterator(GlyphSet.RandomAccess glyphs, long low, long high){
			this.glyphs = glyphs;
			this.high=high;
			at = low;
		}
		
		public boolean hasNext() {return at<high;}
		public Glyph next() {return glyphs.get(at++);}
		public void remove() {throw new UnsupportedOperationException();}
		
	}

}
