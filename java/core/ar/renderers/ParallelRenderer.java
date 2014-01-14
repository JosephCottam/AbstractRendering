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
	 * but actual interpretation is left up to the ForJoinPool implementation.**/ 
	public static int THREAD_POOL_PARALLELISM = Runtime.getRuntime().availableProcessors();
	
	/**How many tasks should be created for each potential parallel worker during aggregation?*/
	public static int AGGREGATE_TASK_MULTIPLIER = 2;

	/**Largest task size for aggregation. 
	 * Aggregation intermediate resources can cause issues (especially in memory mapping) if task sizes are too large.
	 * */
	public static int AGGREGATE_TASK_MAX = 2000000;
	
	/**How small can a transfer task get before it won't be subdivided anymore.**/
	public static final long TRANSFER_TASK_MIN = 100000;
	//-------------------------------------------------------------------------------------
	
	private final ForkJoinPool pool;

	private final ProgressReporter recorder = RenderUtils.recorder();
	
	public ParallelRenderer() {this(null);}
	
	/**Render that uses the given thread pool for parallel operations.
	 * 
	 * @param ForkJoinPool -- Pool to use.  Null to create a pool
	 * **/
	public ParallelRenderer(ForkJoinPool pool) {
		if (pool == null) {pool = new ForkJoinPool(THREAD_POOL_PARALLELISM);}
		this.pool = pool;
	}

	public long taskSize(Glyphset<?,?> glyphs) {
		return glyphs.size()/(pool.getParallelism()*AGGREGATE_TASK_MULTIPLIER);
	}
	
	@Override
	public <I,G,A> Aggregates<A> aggregate(
			Glyphset<? extends G, ? extends I> glyphs, 
			Selector<G> selector,
			Aggregator<I,A> op,
			AffineTransform view, int width, int height) {
		
		//long taskSize = Math.min(AGGREGATE_TASK_MAX, glyphs.size()/(pool.getParallelism()*AGGREGATE_TASK_MULTIPLIER));
		long taskSize = taskSize(glyphs);
		recorder.reset(glyphs.size());

		GlyphParallelAggregation<G,I,A> t = new GlyphParallelAggregation<>(
				glyphs, 
				selector,
				op, 
				view, 
				new Rectangle(0,0,width,height),
				taskSize,
				recorder,
				0, glyphs.segments());
		
		Aggregates<A> a= pool.invoke(t);
		return a;
	}
	
	
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer.ItemWise<IN,OUT> t) {
		Aggregates<OUT> result = AggregateUtils.make(aggregates, t.emptyValue());		
		long taskSize = Math.max(TRANSFER_TASK_MIN, AggregateUtils.size(aggregates)/pool.getParallelism());
		
		PixelParallelTransfer<IN, OUT> task = new PixelParallelTransfer<>(aggregates, result, t, taskSize, aggregates.lowX(),aggregates.lowY(), aggregates.highX(), aggregates.highY());
		pool.invoke(task);
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
