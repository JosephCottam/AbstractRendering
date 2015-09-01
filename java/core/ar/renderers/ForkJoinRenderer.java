package ar.renderers;

import java.awt.geom.AffineTransform;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiFunction;
import java.util.function.Function;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.renderers.tasks.GlyphParallelAggregation;
import ar.renderers.tasks.PixelParallelTransfer;


/**Task-stealing renderer that works on a per-glyph basis, designed for use with a linear stored glyph-set.
 * Iterates the glyphs and produces many aggregate sets that are then combined
 * (i.e., glyph-driven iteration).
 */
public class ForkJoinRenderer implements Renderer {
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

	
	/**How small can a transfer task get before it won't be subdivided anymore.**/
	public static final long DEFAULT_TRANSFER_TASK_SIZE = 100000;
	//-------------------------------------------------------------------------------------
	
	private final ForkJoinPool pool;
	private final ProgressRecorder recorder;
	private final long transferTaskSize;
	
	private final int threadLoad;


	public ForkJoinRenderer() {this(null, RENDER_THREAD_LOAD, DEFAULT_TRANSFER_TASK_SIZE, null);}

	public ForkJoinRenderer(ProgressRecorder recorder) {this(null, RENDER_THREAD_LOAD, DEFAULT_TRANSFER_TASK_SIZE, recorder);}
	
	/**Render that uses the given thread pool for parallel operations.
	 * 
	 * @param pool -- Thread pool to use.  Null to create a pool
	 * **/
	public ForkJoinRenderer(ForkJoinPool pool, int threadLoad, long transferTaskSize, ProgressRecorder recorder) {
		this.pool = pool != null ? pool : new ForkJoinPool(RENDER_POOL_SIZE);
		this.threadLoad = threadLoad > 0 ? threadLoad : RENDER_THREAD_LOAD;
		this.transferTaskSize = transferTaskSize > 0 ? transferTaskSize : DEFAULT_TRANSFER_TASK_SIZE;
		this.recorder = recorder == null ? new ProgressRecorder.Counter() : recorder;
	}


	@Override
	public <I, G, A> Aggregates<A> aggregate(
			Glyphset<? extends G, ? extends I> glyphs, 
			Selector<G> selector,
			Aggregator<I, A> aggregator, 
			AffineTransform view,
			Function<A, Aggregates<A>> allocator,
			BiFunction<Aggregates<A>, Aggregates<A>, Aggregates<A>> merge) {
		
		return innerAggregate(glyphs, selector, aggregator, view, allocator, merge);
	}
	
	@Override
	public <I,G,A> Aggregates<A> aggregate(
			Glyphset<? extends G, ? extends I> glyphs, 
			Selector<G> selector,
			Aggregator<I,A> op,
			AffineTransform view) {
		return aggregate(glyphs, selector, op, view, Renderer.simpleAllocator(glyphs, view), Renderer.simpleMerge(op.identity(), op::rollup));
	}
	
	private <I,G,A, GG extends G, II extends I> Aggregates<A> innerAggregate(
			Glyphset<GG,II> glyphs, 
			Selector<? super GG> selector,
			Aggregator<? super II,A> op,
			AffineTransform view,
			Function<A, Aggregates<A>> allocator,
			BiFunction<Aggregates<A>, Aggregates<A>, Aggregates<A>> merge) {
		
		int taskCount = threadLoad* pool.getParallelism();
		long ticks = GlyphParallelAggregation.ticks(taskCount);
		recorder.reset(ticks);

		GlyphParallelAggregation<GG,II,A> t = new GlyphParallelAggregation<GG,II,A>(
				glyphs.segment(taskCount), 
				glyphs.bounds(), 
				selector,
				op, 
				view, 
				allocator,
				merge,
				recorder);
		
		try {return pool.invoke(t);}
		finally {if (pool.isShutdown()) {throw new Renderer.StopSignaledException();}}
	}
	
	
	@Override 
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer.ItemWise<IN,OUT> t) {
		Aggregates<OUT> result = AggregateUtils.make(aggregates, t.emptyValue());		
		long taskSize = Math.max(transferTaskSize, AggregateUtils.size(aggregates)/pool.getParallelism());
		
		recorder.reset(0);
		PixelParallelTransfer<IN, OUT> task = new PixelParallelTransfer<>(aggregates, result, t, taskSize, aggregates.lowX(),aggregates.lowY(), aggregates.highX(), aggregates.highY());
		try {
			pool.invoke(task);
			recorder.reset(1);
			recorder.update(1);
		}
		finally {if (pool.isShutdown()) {throw new Renderer.StopSignaledException();}}

		return result;
	}
	
	@Override 
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer.Specialized<IN,OUT> t) {
		if (t instanceof Transfer.ItemWise) {
			return transfer(aggregates, (Transfer.ItemWise<IN, OUT>) t);
		} else  {
			return t.process(aggregates, this);
		}
	}	
	
	@Override public ProgressRecorder recorder() {return recorder;}
	
	@Override public void stop() {pool.shutdownNow();}
}
