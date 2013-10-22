package ar.renderers;

import java.awt.geom.AffineTransform;
import java.util.concurrent.ForkJoinPool;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.renderers.tasks.PixelParallelAggregate;
import ar.renderers.tasks.PixelParallelTransfer;

/**Task stealing renderer that operates on a per-pixel basis, designed to be used with a spatially-decomposed glyph set.
 * Divides aggregates space into regions and works on each region in isolation
 * (i.e., bin-driven iteration).
 * **/
public final class ParallelSpatial implements Renderer {
	private static final long serialVersionUID = -2626889612664504698L;
	
	/**Default task size for parallel operations.**/ 
	public static final int DEFAULT_TASK_SIZE = 100000;
	
	/**Thread pool size used for parallel operations.**/ 
	public static int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
	private final ForkJoinPool pool = new ForkJoinPool(THREAD_POOL_SIZE);

	private final int taskSize;
	private final RenderUtils.Progress recorder = RenderUtils.recorder();

	/**Render with task-size determined by DEFAULT_TASK_SIZE.**/
	public ParallelSpatial() {this(DEFAULT_TASK_SIZE);}

	/**Render with task-size determined by the passed parameter.**/
	public ParallelSpatial(int taskSize) {
		this.taskSize = taskSize;
	}
	protected void finalize() {pool.shutdownNow();}
	
	
	public <I,A> Aggregates<A> aggregate(final Glyphset<? extends I> glyphs, final Aggregator<I,A> op, 
			final AffineTransform view, final int width, final int height) {
		
		final Aggregates<A> aggregates = AggregateUtils.make(width, height, op.identity());

		AffineTransform inverseView;
		try {inverseView = view.createInverse();}
		catch (Exception e) {throw new IllegalArgumentException(e);}
		
		PixelParallelAggregate<I,A> t = new PixelParallelAggregate<I,A>(glyphs, inverseView, op, recorder, taskSize, aggregates, 0,0, width, height);
		pool.invoke(t);
		return aggregates;
	}
	
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer.Specialized<IN,OUT> t) {
		Aggregates<OUT> result = AggregateUtils.make(aggregates, t.emptyValue());
		PixelParallelTransfer<IN, OUT> task = new PixelParallelTransfer<>(aggregates, result, t, taskSize, aggregates.lowX(),aggregates.lowY(), aggregates.highX(), aggregates.highY());
		pool.invoke(task);
		return result;
	}
	
	public double progress() {return recorder.percent();}

}
