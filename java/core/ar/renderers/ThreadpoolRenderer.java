package ar.renderers;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.aggregates.wrappers.TouchedBoundsWrapper;
import ar.renderers.tasks.GlyphParallelAggregation;


/**Task-stealing renderer that works on a per-glyph basis, designed for use with a linear stored glyph-set.
 * Iterates the glyphs and produces many aggregate sets that are then combined
 * (i.e., glyph-driven iteration).
 */
public class ThreadpoolRenderer implements Renderer {
	private static final long serialVersionUID = 1103433143653202677L;
	

	////--------------------  Performance Control Parameters ---------------------------
	/**Target "parallelism level" in the thread pool.  Roughly corresponds to the number of threads 
	 * but actual interpretation is left up to the ForJoinPool implementation.*
	 * 
	 * May be set as a system parameter (-DRENDER_POOL_SIZE=x) but will default to the number of cores if
	 * any values less than 1 is given.
	 * */ 
	public static final int RENDER_POOL_SIZE;
	static{
		int size = -1;
		if (System.getProperties().containsKey("RENDER_POOL_SIZE")) {
			size = Integer.parseInt(System.getProperty("RENDER_POOL_SIZE"));
		}
		if (size < 1) {size = Runtime.getRuntime().availableProcessors();}
		RENDER_POOL_SIZE = size;	
	}
	
	
	/**Default number of tasks used in aggregation.
	 * May be set as a system parameter (-DRENDER_THREAD_LOAD=x) but will default to 2 if any value less than 1 is given.**/ 
	public static final int RENDER_THREAD_LOAD;
	static{
		int size = -1;
		if (System.getProperties().containsKey("RENDER_THREAD_LOAD")) {
			size = Integer.parseInt(System.getProperty("RENDER_THREAD_LOAD"));
		}
		if (size < 1) {size = 2;}
		RENDER_THREAD_LOAD = size;	
	}

	//-------------------------------------------------------------------------------------
	
	private final ExecutorService pool;
	private final ProgressRecorder recorder;
	
	private final int threadLoad;


	public ThreadpoolRenderer() {this(null, RENDER_THREAD_LOAD, null);}

	public ThreadpoolRenderer(ProgressRecorder recorder) {this(null, RENDER_THREAD_LOAD, recorder);}
	
	/**Render that uses the given thread pool for parallel operations.
	 * 
	 * @param pool -- Thread pool to use.  Null to create a pool
	 * **/
	public ThreadpoolRenderer(ExecutorService pool, int threadLoad, ProgressRecorder recorder) {
		this.pool = pool != null ? pool : Executors.newFixedThreadPool(RENDER_POOL_SIZE);
		this.threadLoad = threadLoad > 0 ? threadLoad : RENDER_THREAD_LOAD;
		this.recorder = recorder == null ? new ProgressRecorder.Counter() : recorder;
	}

	@Override
	public <I,G,A> Aggregates<A> aggregate(
			Glyphset<? extends G, ? extends I> glyphs, 
			Selector<G> selector,
			Aggregator<I,A> op,
			AffineTransform view, int width, int height) {
		//TODO: height/width may be extraneous now...

		//return stepOne(glyphs, selector, op, view);
		return oneStep(glyphs, selector, op, view);

	}
	
	public <I,G,A> Aggregates<A> oneStep(
			Glyphset<? extends G, ? extends I> glyphs, 
			Selector<G> selector,
			Aggregator<I,A> op,
			AffineTransform view) {

		
		int taskCount = threadLoad * RENDER_POOL_SIZE;
		long ticks = GlyphParallelAggregation.ticks(taskCount);
		recorder.reset(ticks);
		ExecutorCompletionService<Aggregates<A>> service = new ExecutorCompletionService<>(pool);
		
		Rectangle2D allocateBounds = glyphs.bounds();
		for (int i=0; i<taskCount; i++) {
			Glyphset<? extends G, ? extends I> subset =glyphs.segmentAt(taskCount, i);
			AggregateTask<G,I,A> task = new AggregateTask<>(
					recorder, allocateBounds, view,
					subset, selector, op);
			service.submit(task);
		}
		
	
		Aggregates<A> result = allocateAggregates(allocateBounds, view, op.identity()).base();
		try {
			for (int i=0; i<taskCount; i++) {
				Aggregates<A> from = service.take().get();
				AggregateUtils.__unsafeMerge(result, from, result.defaultValue(), op::rollup);
			}
		}  catch (Exception e) {
			throw new RuntimeException("Error completing transfer", e);
		} 
		
		return result;
	}
	
	public <A,G,I> Aggregates<A> stepOne(Glyphset<? extends G, ? extends I> glyphs, 
			Selector<G> selector,
			Aggregator<I,A> op,
			AffineTransform view) {
		
		int taskCount = threadLoad * RENDER_POOL_SIZE;
		long ticks = GlyphParallelAggregation.ticks(taskCount);
		recorder.reset(ticks);
		

		List<AggregateTask<G,I,A>> tasks = new ArrayList<>();
		for (int i=0; i<taskCount; i++) {
			AggregateTask<G,I,A> task = new AggregateTask<>(
					recorder, 
					glyphs.bounds(),
					view,
					glyphs.segmentAt(taskCount, i),
					selector,
					op);
			tasks.add(task);
		}
	
		try {
			List<Future<Aggregates<A>>> parts = pool.invokeAll(tasks);
			return stepTwo(parts, glyphs.bounds(), view, op);
		} catch (Exception e) {throw new RuntimeException("Error completing transfer", e);} 
	}
	
	public <A> Aggregates<A> stepTwo(List<Future<Aggregates<A>>> parts, Rectangle2D allocateBounds, AffineTransform view, Aggregator<?,A> op) throws Exception {
		Aggregates<A> result = allocateAggregates(allocateBounds, view, op.identity()).base();

			for (Future<Aggregates<A>> part: parts) {
				Aggregates<A> from = part.get();
				AggregateUtils.__unsafeMerge(result, from, result.defaultValue(), op::rollup);
			}
	
		return result;
	}
	
	
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer.ItemWise<IN,OUT> t) {
		Aggregates<OUT> result = AggregateUtils.make(aggregates, t.emptyValue());		
		
		int taskCount = threadLoad * RENDER_POOL_SIZE;
		recorder.reset(taskCount);

		int span = (aggregates.highX() - aggregates.lowX())/taskCount;
		List<TransferTask<IN,OUT>> tasks = new ArrayList<>();
		for (int i=0; i<taskCount; i++) {
			int lowX = aggregates.lowX() + (span*i);
			int lowY = aggregates.lowY();
			int highX = Math.max(aggregates.highX(), aggregates.lowX() + (span*(i+1)));
			int highY = aggregates.highY();
					
			TransferTask<IN,OUT> task = new TransferTask<>(recorder, t, lowX, lowY, highX, highY, aggregates, result);
			tasks.add(task);
		}
		
		try {pool.invokeAll(tasks);}
		catch (InterruptedException e) {throw new RuntimeException("Error completing transfer", e);}
		return result;
	}
	
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer.Specialized<IN,OUT> t) {
		if (t instanceof Transfer.ItemWise) {
			return transfer(aggregates, (Transfer.ItemWise<IN, OUT>) t);
		} else  {
			return t.process(aggregates, this);
		}
	}	
	
	public ProgressRecorder recorder() {return recorder;}
	
	
	protected static <A> TouchedBoundsWrapper<A> allocateAggregates(Rectangle2D bounds, AffineTransform viewTransform, A identity) {
		Rectangle fullBounds = viewTransform.createTransformedShape(bounds).getBounds();
		Aggregates<A> aggs = AggregateUtils.make(
				fullBounds.x, fullBounds.y,
				fullBounds.x+fullBounds.width, fullBounds.y+fullBounds.height,
				identity);
		return new TouchedBoundsWrapper<>(aggs, false);
	}	
	
	
	
	
	private static final class TransferTask<IN,OUT> implements Callable<Aggregates<OUT>> {
		private final int lowX, lowY, highX, highY;
		private final Aggregates<? extends IN> in;
		private final Aggregates<OUT> out;
		private final Transfer.ItemWise<IN,OUT> t;
		private final ProgressRecorder recorder;

		
		public TransferTask(ProgressRecorder recorder, Transfer.ItemWise<IN,OUT> t, int lowX, int lowY, int highX, int highY, Aggregates<? extends IN> in, Aggregates<OUT> out) {
			this.recorder = recorder;
			this.lowX=lowX;
			this.lowY = lowY;
			this.highX = highX;
			this.highY = highY;
			this.in = in;
			this.out = out;
			this.t = t;
		}
		
		public Aggregates<OUT> call() throws Exception {
			recorder.update(1);
			for (int x=lowX; x<highX; x++) {
				for (int y=lowY; y<highY; y++) {
					OUT val = t.at(x, y, in);
					out.set(x, y, val);
				}
			}
			return out;
		}
	}
	
	private static final class AggregateTask<G,I,A> implements Callable<Aggregates<A>> {
		private final ProgressRecorder recorder;
		private final Glyphset<? extends G, ? extends I> glyphset;
		private final Selector<G> selector;
		private final AffineTransform viewTransform;
		private final Aggregator<I,A> op;
		private final Rectangle2D allocateBounds;
		
		public AggregateTask(
				ProgressRecorder recorder, 
				Rectangle2D allocateBounds,
				AffineTransform viewTransform,
				Glyphset<? extends G, ? extends I> glyphs,
				Selector<G> selector,
				Aggregator<I,A> op
				) {
			this.recorder = recorder;
			this.glyphset = glyphs;
			this.selector = selector;
			this.viewTransform = viewTransform;
			this.op = op;
			this.allocateBounds = allocateBounds;
		}
		
		
		@Override
		public Aggregates<A> call() throws Exception {
			TouchedBoundsWrapper<A> target = allocateAggregates(allocateBounds, viewTransform, op.identity());
			recorder.update(1);
			selector.processSubset(glyphset, viewTransform, target, op);
						
			if (target.untouched()) {return null;}
			else {return target;}
		}
	}
	
}
