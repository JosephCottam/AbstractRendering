package ar.renderers;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.concurrent.ForkJoinPool;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.renderers.tasks.GlyphParallelAggregation;
import ar.renderers.tasks.PixelParallelTransfer;


/**Task-stealing renderer that works on a per-glyph basis, designed for use with a linear stored glyph-set.
 * Iterates the glyphs and produces many aggregate sets that are then combined
 * (i.e., glyph-driven iteration).
 */
public class ParallelGlyphs implements Renderer {
	private static final long serialVersionUID = 1103433143653202677L;
	
	/**Default task size for parallel operations.**/ 
	public static int DEFAULT_TASK_SIZE = 100000;

	/**Thread pool size used for parallel operations.**/ 
	public static int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
	private final ForkJoinPool pool = new ForkJoinPool(THREAD_POOL_SIZE);

	private final int taskSize;
	private final RenderUtils.Progress recorder = RenderUtils.recorder();

	/**Render with task-size determined by DEFAULT_TASK_SIZE.**/
	public ParallelGlyphs() {this(DEFAULT_TASK_SIZE);}
	
	
	/**Render with task-size determined by the passed parameter.**/
	public ParallelGlyphs(int taskSize) {
		this.taskSize = taskSize;
	}
	
	protected void finalize() {pool.shutdownNow();}

	@Override
	public <I,A> Aggregates<A> aggregate(Glyphset<? extends I> glyphs, Aggregator<I,A> op, 
			AffineTransform view, int width, int height) {
		
		recorder.reset(glyphs.size());

		GlyphParallelAggregation<I,A> t = GlyphParallelAggregation.make(
				glyphs.iterator().next().shape().getClass(),	//HACK: This lookup may be fragile.  It may better to get the aggregates to tell the geometry type...
				glyphs, 
				view, 
				op, 
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
	
	public double progress() {return recorder.percent();}
}
