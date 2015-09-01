package ar.renderers;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
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
	private final List<Future<?>> tasks = Collections.synchronizedList(new ArrayList<>());
	private final int threadLoad;


	public ThreadpoolRenderer() {this(null, RENDER_THREAD_LOAD, null);}

	public ThreadpoolRenderer(ProgressRecorder recorder) {this(null, RENDER_THREAD_LOAD, recorder);}
	
	private static final AtomicInteger threadCounter = new AtomicInteger(0); 
	/**Render that uses the given thread pool for parallel operations.
	 * 
	 * @param pool -- Thread pool to use.  Null to create a pool
	 * **/
	public ThreadpoolRenderer(ExecutorService pool, int threadLoad, ProgressRecorder recorder) {
		this.pool = pool != null ? pool : Executors.newFixedThreadPool(RENDER_POOL_SIZE,
				(Runnable r) -> {
					Thread t = new Thread(r, "AR Renderer Pool -- " + threadCounter.getAndIncrement());
					t.setDaemon(true);
			        return t;
			    });
		this.threadLoad = threadLoad > 0 ? threadLoad : RENDER_THREAD_LOAD;
		this.recorder = recorder == null ? new ProgressRecorder.Counter() : recorder;
	}


	@Override
	public <I, G, A> Aggregates<A> aggregate(
			Glyphset<? extends G, ? extends I> glyphs, Selector<G> selector,
			Aggregator<I, A> aggregator, AffineTransform viewTransform,
			Function<A, Aggregates<A>> allocator, 
			BiFunction<Aggregates<A>, Aggregates<A>, Aggregates<A>> merge) {
		return oneStep(glyphs, selector, aggregator, viewTransform, allocator, merge);
	}
	
	@Override
	public <I,G,A> Aggregates<A> aggregate(
			Glyphset<? extends G, ? extends I> glyphs, 
			Selector<G> selector,
			Aggregator<I,A> aggregator,
			AffineTransform viewTransform) {

		return aggregate(glyphs, selector, aggregator, viewTransform, 
				Renderer.simpleAllocator(glyphs, viewTransform),
				Renderer.simpleMerge(aggregator.identity(), aggregator::rollup)
				);
	}
	
	//Exists to make the types work out right
	private <I, G, A, GG extends G, II extends I> Aggregates<A> oneStep(
			Glyphset<GG, II> glyphs, 
			Selector<G> selector,
			Aggregator<I,A> op,
			AffineTransform view,
			Function<A, Aggregates<A>> allocator,
			BiFunction<Aggregates<A>, Aggregates<A>, Aggregates<A>> merge) {

		
		int taskCount = threadLoad * RENDER_POOL_SIZE;
		long ticks = GlyphParallelAggregation.ticks(taskCount);
		recorder.reset(ticks);
		ExecutorCompletionService<Aggregates<A>> service = new ExecutorCompletionService<>(pool);
		
		Collection<Glyphset<GG, II>> segments = glyphs.segment(taskCount);
		List<Future<Aggregates<A>>> futures = new ArrayList<>(); 
		
		for (Glyphset<GG, II> segment: segments) {
			AggregateTask<G,I,A> task = new AggregateTask<>(
					recorder, view,
					segment, selector, op, allocator);
			futures.add(service.submit(task));
		}
		
		tasks.addAll(futures);
		
		Aggregates<A> result = allocator.apply(op.identity());	//TODO: Maybe remove, not necessarily needed
		try {
			for (int i=0; i<segments.size(); i++) {
				Future<Aggregates<A>> future = service.take();
				if (future.isCancelled()) {throw new Renderer.StopSignaledException();}
				
				Aggregates<A> from = future.get();
				result = merge.apply(result, from);
			}
		} 
		catch (Renderer.StopSignaledException e) {throw e;} 
		catch (Exception e) {throw new RuntimeException("Error completing aggregation", e);}

		return result;
	}
	
	@Override 
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
		catch (InterruptedException e) {
			throw new RuntimeException("Error completing transfer", e);
		} 
		
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
	@Override public void stop() {
		for(Future<?> task: tasks) {task.cancel(true);}
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
		private final Function<A, Aggregates<A>> allocator;
		
		public AggregateTask(
				ProgressRecorder recorder, 
				AffineTransform viewTransform,
				Glyphset<? extends G, ? extends I> glyphs,
				Selector<G> selector,
				Aggregator<I,A> op,
				Function<A, Aggregates<A>> allocator
				) {
			this.recorder = recorder;
			this.glyphset = glyphs;
			this.selector = selector;
			this.viewTransform = viewTransform;
			this.op = op;
			this.allocator = allocator;
		}
		
		
		@Override
		public Aggregates<A> call() throws Exception {
			Aggregates<A> target = allocator.apply(op.identity());
			recorder.update(1);
			selector.processSubset(glyphset, viewTransform, target, op);
						
			if (target.empty()) {return null;}
			else {return target;}
		}
	}
}
