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
	
	/**Thread pool size used for parallel operations.**/ 
	public static int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
	private final ForkJoinPool pool;

	private final RenderUtils.Progress recorder = RenderUtils.recorder();
	
	private final Class<?> geometryType;

	public ParallelGlyphs() {this(null, null);}
	public ParallelGlyphs(ForkJoinPool pool) {this(pool, null);}

	/**Render with task-size determined by the passed parameter and use the given thread pool for parallel operations.
	 * 
	 * @param geometryType -- The geometry type renderers should expect to us.  Null means "auto detect from passed glyphs"...which may fail at runtime.
	 * @param taskSize -- Granularity of tasks 
	 * @param ForkJoinPool -- Pool to use.  Null to create a pool
	 * **/
	public ParallelGlyphs(ForkJoinPool pool, Class<?> geometryType) {
		if (pool == null) {pool = new ForkJoinPool(THREAD_POOL_SIZE);}
	
		this.geometryType = geometryType;
		this.pool = pool;
	}

	@Override
	public <I,G,A> Aggregates<A> aggregate(Glyphset<? extends G, ? extends I> glyphs, Aggregator<I,A> op, 
			AffineTransform view, int width, int height) {
		
		long taskSize = glyphs.segments()/THREAD_POOL_SIZE;
		recorder.reset(glyphs.size());
				
		Class<?> geometryType = this.geometryType;
		if (geometryType == null) {geometryType = glyphs.iterator().next().shape().getClass();}

		GlyphParallelAggregation<I,G,A> t = GlyphParallelAggregation.make(
				geometryType,
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
		
		long taskSize = AggregateUtils.size(aggregates)/THREAD_POOL_SIZE;
		
		PixelParallelTransfer<IN, OUT> task = new PixelParallelTransfer<>(aggregates, result, t, taskSize, aggregates.lowX(),aggregates.lowY(), aggregates.highX(), aggregates.highY());
		pool.invoke(task);
		return result;
	}	
	
	public double progress() {return recorder.percent();}
}
