package ar.renderers;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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
import ar.Glyphset;
import ar.Glyphset.Glyph;
import ar.aggregates.ConstantAggregates;
import ar.aggregates.FlatAggregates;
import ar.glyphsets.GlyphsetIterator;
import ar.util.Util;
import ar.Renderer;
import ar.Transfer;


/**Task-stealing renderer that works on a per-glyph basis, designed for use with a linear stored glyph-set.
 * Iterates the glyphs and produces many aggregate sets that are then combined
 * (i.e., glyph-driven iteration).
 * 
 * <p>
 * TODO: Extend beyond aggregate reducers with same LEFT/RIGHT/OUT
 */
public class ParallelGlyphs<G,A> implements Renderer<G,A> {
	public static int DEFAULT_TASK_SIZE = 100000;
	private final ForkJoinPool pool = new ForkJoinPool();

	private final int taskSize;
	private final AggregateReducer<A,A,A> reducer;
	private final RenderUtils.Progress recorder;

	public ParallelGlyphs(AggregateReducer<A,A,A> red) {
		this(DEFAULT_TASK_SIZE, red);
	}
	
	public ParallelGlyphs(int taskSize, AggregateReducer<A,A,A> red) {
		this.taskSize = taskSize;
		this.reducer = red;
		recorder = RenderUtils.recorder();
	}
	
	protected void finalize() {pool.shutdownNow();}

	@Override
	public Aggregates<A> reduce(Glyphset<G> glyphs, Aggregator<G,A> op, 
			AffineTransform inverseView, int width, int height) {
		
		AffineTransform view;
		try {view = inverseView.createInverse();}
		catch (Exception e) {throw new RuntimeException("Error inverting the inverse-view transform....");}
		recorder.reset(glyphs.size());
		
		ReduceTask<G,A> t = new ReduceTask<G,A>(
				(Glyphset.RandomAccess<G>) glyphs, 
				view, inverseView, 
				op, reducer, 
				width, height, taskSize,
				recorder,
				0, glyphs.size());
		
		Aggregates<A> a= pool.invoke(t);
		
		return a;
	}
	
	
	
	public Aggregates<Color> transfer(Aggregates<A> aggregates, Transfer<A,Color> t) {
		Aggregates<Color> rslt = new FlatAggregates<>(aggregates, t.identity());
		for (int x=aggregates.lowX(); x<aggregates.highX(); x++) {
			for (int y=aggregates.lowY(); y<aggregates.highY(); y++) {
				Color val = t.at(x,y, aggregates);
				rslt.set(x, y, val);
			}
		}
		return rslt;
	}
	
	public double progress() {return recorder.percent();}

	private static final class ReduceTask<G,A> extends RecursiveTask<Aggregates<A>> {
		private static final long serialVersionUID = 705015978061576950L;

		private final int taskSize;
		private final long low;
		private final long high;
		private final Glyphset.RandomAccess<G> glyphs;		//TODO: Can some hackery be done with iterators instead so generalized GlyphSet can be used?  At what cost??
		private final AffineTransform view, inverseView;
		private final int width;
		private final int height;
		private final AggregateReducer<A,A,A> reducer;
		private final Aggregator<G,A> op;
		private final RenderUtils.Progress recorder;

		
		public ReduceTask(Glyphset.RandomAccess<G> glyphs, 
				AffineTransform view, AffineTransform inverseView,
				Aggregator<G,A> op, AggregateReducer<A,A,A> reducer, 
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

			ReduceTask<G,A> top = new ReduceTask<G,A>(glyphs, view, inverseView, op, reducer, width,height, taskSize, recorder, low, mid);
			ReduceTask<G,A> bottom = new ReduceTask<G,A>(glyphs, view, inverseView, op, reducer, width,height, taskSize, recorder, mid, high);
			invokeAll(top, bottom);
			Aggregates<A> aggs = Util.reduceAggregates(top.getRawResult(), bottom.getRawResult(), reducer);
			return aggs;
		}
		
		//TODO: Respect the actual shape.  Currently assumes that the bounds box matches the actual item bounds..
		private final Aggregates<A> local() {
			Glyphset.RandomAccess<G> subset = new GlyphSubset<G>(glyphs, low, high);
			Rectangle bounds = view.createTransformedShape(Util.bounds(subset)).getBounds();
			bounds = bounds.intersection(new Rectangle(0,0,width,height));
			
			if (bounds.isEmpty()) {
				return new ConstantAggregates<>(bounds.x, bounds.y, 
												bounds.x+bounds.width, bounds.y+bounds.height, 
												op.identity());
			}				
			Aggregates<A> aggregates = new FlatAggregates<>(bounds.x, bounds.y,
														 bounds.x+bounds.width, bounds.y+bounds.height, 
														 op.identity());
			
			
			Point2D lowP = new Point2D.Double();
			Point2D highP = new Point2D.Double();
			
			for (Glyph<G> g: subset) {
				//Discretize the glyph into the aggregates array
				Rectangle2D b = g.shape().getBounds2D();
				lowP.setLocation(b.getMinX(), b.getMinY());
				highP.setLocation(b.getMaxX(), b.getMaxY());
				
				view.transform(lowP, lowP);
				view.transform(highP, highP);
				
				int lowx = (int) Math.floor(lowP.getX());
				int lowy = (int) Math.floor(lowP.getY());
				int highx = (int) Math.ceil(highP.getX());
				int highy = (int) Math.ceil(highP.getY());

				Rectangle pixel = new Rectangle(lowx, lowy, 1,1);
				A v = op.at(pixel, new GlyphSingleton<G>(g, subset.valueType()), inverseView);
				
				
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
	
	
	public static final class GlyphSingleton<G> implements Glyphset.RandomAccess<G> {
		private final List<Glyph<G>> glyphs;
		private final Glyph<G> glyph;
		private final Class<G> valueType;
		private final Rectangle2D bounds;
		
		public GlyphSingleton(Glyph<G> g, Class<G> valueType) {
			glyphs = Collections.singletonList(g);
			glyph = g;
			bounds = g.shape().getBounds2D();
			this.valueType = valueType;
		}
		
		public Iterator<Glyph<G>> iterator() {return glyphs.iterator();}
		public Glyph<G> get(long i) {return glyphs.get(0);}
		public boolean isEmpty() {return glyphs.isEmpty();}
		public void add(Glyph<G> g) {throw new UnsupportedOperationException();}
		public long size() {return glyphs.size();}
		public Rectangle2D bounds() {return bounds;}
		public Class<G> valueType() {return valueType;}

		public Collection<Glyph<G>> intersects(Rectangle2D r) {
			if (glyph.shape().intersects(r)) {return glyphs;}
			else {return Collections.emptyList();}
		}
	}
	
	public static final class GlyphSubset<G> implements Glyphset.RandomAccess<G> {
		private final Glyphset.RandomAccess<G> glyphs;
		private final long low,high;
		private final Glyph<G>[] cache;
		
		@SuppressWarnings("unchecked")
		public GlyphSubset (Glyphset.RandomAccess<G> glyphs, long low, long high) {
			this.glyphs = glyphs;
			this.low = low; 
			this.high=high;
			if (high-low > Integer.MAX_VALUE) {throw new IllegalArgumentException("Must subset smaller number of items (this class uses int-based caching)");}
			this.cache = new Glyph[(int) (high-low)];
		}
		
		public GlyphsetIterator<G> iterator() {return new GlyphsetIterator<G>(this,low,high);}

		public Collection<Glyph<G>> intersects(Rectangle2D r) {
			ArrayList<Glyph<G>> contained = new ArrayList<Glyph<G>>();
			for (int i=0; i<cache.length; i++) {
				Glyph<G> g = get(i+low);
				if (g.shape().intersects(r)) {contained.add(g);}
			}
			for (Glyph<G> g: this) {if (g.shape().intersects(r)) {contained.add(g);}}
			return contained;
		}

		public boolean isEmpty() {return low >= high;}
		public long size() {return high-low;}
		public Rectangle2D bounds() {return Util.bounds(this);}
		public void add(Glyph<G> g) {throw new UnsupportedOperationException("Cannot add items to subset view.");}
		public Class<G> valueType() {return glyphs.valueType();}

		@Override
		public Glyph<G> get(long l) {
			long at = l-low;
			if (at > cache.length) {throw new IllegalArgumentException();}
			int i= (int) at;
			
			if (cache[i] == null) {cache[i] = glyphs.get(l);}
			return cache[i];
		}
	}
}
