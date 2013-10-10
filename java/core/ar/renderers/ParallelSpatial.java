package ar.renderers;

import java.awt.geom.AffineTransform;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;
import ar.aggregates.AggregateUtils;

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
		
		ReduceTask<I,A> t = new ReduceTask<I,A>(glyphs, inverseView, op, recorder, taskSize, aggregates, 0,0, width, height);
		pool.invoke(t);
		return aggregates;
	}
	
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer.Specialized<IN,OUT> t) {
		return ParallelSpatial.transfer(aggregates, t, taskSize, pool);
	}
	
	/** Utility method to enable other renderer implementations access to the parallel-by-blocks
	 * transfer implementation without instantiating a ParallelSpatial renderer.
	 * 
	 * @param aggregates Aggregates to perform transfer on
	 * @param t Transfer operation; Must be "specialized" before being passed in. 
	 * @param taskSize Maximum transfer task size (in number of aggregates) 
	 * @param pool Thread pool to use for parallelization
	 * @return Resulting aggregates
	 */
	public static <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer.Specialized<IN,OUT> t, int taskSize, ForkJoinPool pool) {
		Aggregates<OUT> result = AggregateUtils.make(aggregates, t.emptyValue());
		TransferTask<IN, OUT> task = new TransferTask<>(aggregates, result, t, taskSize, aggregates.lowX(),aggregates.lowY(), aggregates.highX(), aggregates.highY());
		pool.invoke(task);
		return result;
	}



	public double progress() {return recorder.percent();}

	private static final int center(int low, int high) {return low+((high-low)/2);}

	private static final class TransferTask<IN, OUT> extends RecursiveAction {
		private static final long serialVersionUID = 7512448648194530526L;
		
		private final int lowx, lowy, highx, highy;
		private final Aggregates<OUT> out;
		private final Aggregates<? extends IN> in;
		private final Transfer.Specialized<IN, OUT> t;
		private final int taskSize;
		
		public TransferTask(
				Aggregates<? extends IN> input, Aggregates<OUT> result, 
				Transfer.Specialized<IN, OUT> t,
				int taskSize,
				int lowX, int lowY, int highX, int highY
				) {
			
			this.lowx=lowX;
			this.lowy=lowY;
			this.highx=highX;
			this.highy=highY;
			this.out = result;
			this.in = input;
			this.t = t;
			this.taskSize = taskSize;
		}

		protected void compute() {
			int width = highx-lowx;
			int height = highy-lowy;
			if (width * height > taskSize) {
				int centerx = center(lowx, highx);
				int centery = center(lowy, highy);
				TransferTask<IN, OUT> SW = new TransferTask<>(in, out, t, taskSize, lowx,    lowy,    centerx, centery);
				TransferTask<IN, OUT> NW = new TransferTask<>(in, out, t, taskSize, lowx,    centery, centerx, highy);
				TransferTask<IN, OUT> SE = new TransferTask<>(in, out, t, taskSize, centerx, lowy,    highx,   centery);
				TransferTask<IN, OUT> NE = new TransferTask<>(in, out, t, taskSize, centerx, centery, highx,   highy);
				invokeAll(SW,NW,SE,NE);
			} else {
				for (int x=lowx; x<highx; x++) {
					for (int y=lowy; y<highy; y++) {
						OUT val = t.at(x, y, in);
						out.set(x, y, val);
					}
				}				
			}
		}
	}
	
	private static final class ReduceTask<I,A> extends RecursiveAction {
		private static final long serialVersionUID = -6471136218098505342L;

		private final int taskSize;
		private final int lowx, lowy, highx, highy;
		private final Aggregates<A> aggs;
		private final Aggregator<I,A> op;
		private final Glyphset<? extends I> glyphs;
		private final AffineTransform inverseView;
		private final RenderUtils.Progress recorder;
		
		public ReduceTask(Glyphset<? extends I> glyphs, AffineTransform view, 
				Aggregator<I,A> op, RenderUtils.Progress recorder, int taskSize,   
				Aggregates<A> aggs, int lowx, int lowy, int highx, int highy) {
			this.glyphs = glyphs;
			this.inverseView = view;
			this.recorder = recorder;
			this.op=op;
			this.taskSize = taskSize;
			this.aggs = aggs;
			this.lowx = lowx;
			this.lowy = lowy;
			this.highx = highx > aggs.highX() ? aggs.highX()-1 : highx;  //Roundoff...
			this.highy = highy > aggs.highY() ? aggs.highY()-1 : highy;//Roundoff...
			
			if (highx> aggs.highX()) {throw new RuntimeException(String.format("%d > width of %d",  highx, aggs.highX()));}
			if (highy> aggs.highY()) {throw new RuntimeException(String.format("%d > height of %d",  highy, aggs.highY()));}
		}

		@Override
		protected void compute() {
			int width = highx-lowx;
			int height = highy-lowy;

			if ((width*height) > taskSize) {
				int centerx = center(lowx, highx);
				int centery = center(lowy, highy);
				ReduceTask<I,A> SW = new ReduceTask<I,A>(glyphs, inverseView, op, recorder, taskSize, aggs, lowx,    lowy,    centerx, centery);
				ReduceTask<I,A> NW = new ReduceTask<I,A>(glyphs, inverseView, op, recorder, taskSize, aggs, lowx,    centery, centerx, highy);
				ReduceTask<I,A> SE = new ReduceTask<I,A>(glyphs, inverseView, op, recorder, taskSize, aggs, centerx, lowy,    highx,   centery);
				ReduceTask<I,A> NE = new ReduceTask<I,A>(glyphs, inverseView, op, recorder, taskSize, aggs, centerx, centery, highx,   highy);
				invokeAll(SW,NW,SE,NE);
			} else {
				for (int x=lowx; x<highx; x++) {
					for (int y=lowy; y<highy; y++) {
						A val = AggregationStrategies.pixel(aggs, op, glyphs, inverseView, x, y);
						aggs.set(x, y, val);
						recorder.update(1);
					}
				}
			}
		}
	}
}
