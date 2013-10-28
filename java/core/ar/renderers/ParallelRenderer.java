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
	
	/**Default task size for parallel operations.**/ 
	public static int DEFAULT_TASK_SIZE = 100000;

	/**Thread pool size used for parallel operations.**/ 
	public static int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
	private final ForkJoinPool pool;

	private final int taskSize;
	private final ProgressReporter recorder = RenderUtils.recorder();
	
	public ParallelRenderer() {this(DEFAULT_TASK_SIZE);}
	public ParallelRenderer(int taskSize) {this(taskSize, null);}

	/**Render with task-size determined by the passed parameter and use the given thread pool for parallel operations.
	 * 
	 * @param geometryType -- The geometry type renderers should expect to us.  Null means "auto detect from passed glyphs"...which may fail at runtime.
	 * @param taskSize -- Granularity of tasks 
	 * @param ForkJoinPool -- Pool to use.  Null to create a pool
	 * **/
	public ParallelRenderer(int taskSize, ForkJoinPool pool) {
		if (pool == null) {pool = new ForkJoinPool(THREAD_POOL_SIZE);}
 
		this.taskSize = taskSize;
		this.pool = pool;
	}

	@Override
	public <I,G,A> Aggregates<A> aggregate(
			Glyphset<? extends G, ? extends I> glyphs, 
			Selector<G> selector,
			Aggregator<I,A> op,
			AffineTransform view, int width, int height) {
		
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
	
	
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer.Specialized<IN,OUT> t) {
		Aggregates<OUT> result = AggregateUtils.make(aggregates, t.emptyValue());
		PixelParallelTransfer<IN, OUT> task = new PixelParallelTransfer<>(aggregates, result, t, taskSize, aggregates.lowX(),aggregates.lowY(), aggregates.highX(), aggregates.highY());
		pool.invoke(task);
		return result;
	}
	
	public ProgressReporter progress() {return recorder;}
}
