package ar.renderers;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.StreamSupport;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.renderers.tasks.GlyphParallelAggregation;
import ar.renderers.tasks.PixelParallelTransfer;


/**Renderer based around streams and parallel iteration found in Java 1.8.**/
public class StreamRenderer implements Renderer {
	private static final long serialVersionUID = 1103433143653202677L;
	
	/**How small can a transfer task get before it won't be subdivided anymore.**/
	public static final long DEFAULT_TRANSFER_TASK_SIZE = 100000;
	//-------------------------------------------------------------------------------------
	
	private final ProgressRecorder recorder;
	private final long transferTaskSize;
	private final ForkJoinPool pool = new ForkJoinPool(); //Used in transfer...

	public StreamRenderer() {this(null);}

	public StreamRenderer(ProgressRecorder recorder) {this(DEFAULT_TRANSFER_TASK_SIZE, recorder);}
	
	/**Render that uses the given thread pool for parallel operations.
	 * 
	 * @param pool -- Thread pool to use.  Null to create a pool
	 * **/
	public StreamRenderer(long transferTaskSize, ProgressRecorder recorder) {
		this.transferTaskSize = transferTaskSize > 0 ? transferTaskSize : DEFAULT_TRANSFER_TASK_SIZE;
		this.recorder = recorder == null ? new ProgressRecorder.Counter() : recorder;
	}

	@Override
	public <I,G,A> Aggregates<A> aggregate(
			Glyphset<? extends G, ? extends I> glyphs, 
			Selector<G, I> selector,
			Aggregator<I,A> op,
			AffineTransform view, int width, int height) {
		
		return StreamSupport.stream(glyphs.spliterator(), true)
			.map(selector)
			.collect(op);
			
			
		//Map each glyph to a set of aggregates
		//Reduce all aggregates into one set of aggreagtes
		

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
	
	public ProgressRecorder recorder() {return recorder;}
}
