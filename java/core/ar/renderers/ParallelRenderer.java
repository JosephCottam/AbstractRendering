package ar.renderers;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.concurrent.ForkJoinPool;

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
public class ParallelRenderer implements Renderer {
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
	private final ProgressReporter recorder = RenderUtils.recorder();
	private final long transferTaskSize;
	
	private final int threadLoad;
	
	public ParallelRenderer() {this(null, RENDER_THREAD_LOAD, DEFAULT_TRANSFER_TASK_SIZE);}
	
	/**Render that uses the given thread pool for parallel operations.
	 * 
	 * @param pool -- Thread pool to use.  Null to create a pool
	 * **/
	public ParallelRenderer(ForkJoinPool pool, int threadLoad, long transferTaskSize) {
		this.pool = pool != null ? pool : new ForkJoinPool(RENDER_POOL_SIZE);
		this.threadLoad = threadLoad > 0 ? threadLoad : RENDER_THREAD_LOAD;
		this.transferTaskSize = transferTaskSize > 0 ? transferTaskSize : DEFAULT_TRANSFER_TASK_SIZE;
	}

	@Override
	public <I,G,A> Aggregates<A> aggregate(
			Glyphset<? extends G, ? extends I> glyphs, 
			Selector<G> selector,
			Aggregator<I,A> op,
			AffineTransform view, int width, int height) {
		
		int taskCount = threadLoad* pool.getParallelism();
		long ticks = GlyphParallelAggregation.ticks(taskCount);
		recorder.reset(ticks);

		GlyphParallelAggregation<G,I,A> t = new GlyphParallelAggregation<>(
				glyphs, 
				glyphs.bounds(), 
				selector,
				op, 
				view, 
				new Rectangle(0,0,width,height),
				recorder,
				0, taskCount, taskCount);
		
		Aggregates<A> a= pool.invoke(t);
		
		return a;
	}
	
	
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer.ItemWise<IN,OUT> t) {
		Aggregates<OUT> result = AggregateUtils.make(aggregates, t.emptyValue());		
		long taskSize = Math.max(transferTaskSize, AggregateUtils.size(aggregates)/pool.getParallelism());
		
		recorder.reset(0);
		PixelParallelTransfer<IN, OUT> task = new PixelParallelTransfer<>(aggregates, result, t, taskSize, aggregates.lowX(),aggregates.lowY(), aggregates.highX(), aggregates.highY());
		pool.invoke(task);
		recorder.reset(1);
		recorder.update(1);
		return result;		
	}
	
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer.Specialized<IN,OUT> t) {
		if (t instanceof Transfer.ItemWise) {
			return transfer(aggregates, (Transfer.ItemWise<IN, OUT>) t);
		} else  {
			return t.process(aggregates, this);
		}
	}	
	
	public ProgressReporter progress() {return recorder;}
}
