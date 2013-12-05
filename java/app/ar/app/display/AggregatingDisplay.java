package ar.app.display;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.ExecutorService;

import ar.*;
import ar.aggregates.SubsetWrapper;
import ar.app.util.ActionProvider;
import ar.app.util.MostRecentOnlyExecutor;
import ar.app.util.ZoomPanHandler;
import ar.renderers.AggregationStrategies;
import ar.selectors.TouchesPixel;
import ar.util.Util;

/**Render and display exactly what fits on the screen.
 */
public class AggregatingDisplay extends ARComponent.Aggregating {
	protected static final long serialVersionUID = 1L;

	protected final ActionProvider aggregatesChangedProvider = new ActionProvider();
	
	protected final TransferDisplay display;
	
	protected Aggregator<?,?> aggregator;
	protected Glyphset<?,?> dataset;
	
	protected AffineTransform viewTransformRef = new AffineTransform();
	private AffineTransform renderTransform = new AffineTransform();

	protected volatile Aggregates<?> aggregates;
	protected ExecutorService renderPool = new MostRecentOnlyExecutor(1,"FullDisplay Render Thread");
		
	protected final Renderer renderer;

	public AggregatingDisplay(Renderer renderer) {
		super();
		this.renderer = renderer;
		display = new TransferDisplay(null, null, renderer);
		this.setLayout(new BorderLayout());
		this.add(display, BorderLayout.CENTER);
		
		new ZoomPanHandler().register(this);	
	}

	/**Create a new instance.
	 * 
	 */
	public AggregatingDisplay(Aggregator<?,?> aggregator, Transfer<?,?> transfer, Glyphset<?,?> glyphs, Renderer renderer) {
		this(renderer);
		this.aggregator = aggregator;
		this.dataset = glyphs;		
		new ZoomPanHandler().register(this);
	}
	
	protected void finalize() {renderPool.shutdown();}
	public void addAggregatesChangedListener(ActionListener l) {aggregatesChangedProvider.addActionListener(l);}

	public Aggregates<?> refAggregates() {return display.refAggregates();}
	public void refAggregates(Aggregates<?> aggregates) {
		display.refAggregates(aggregates);
	}
	
	public Renderer renderer() {return renderer;}
	
	public Glyphset<?,?> dataset() {return dataset;}
	public void dataset(Glyphset<?,?> data, Aggregator<?,?> aggregator, Transfer<?,?> transfer) {
		this.dataset = data;
		this.aggregator = aggregator;
		this.transfer(transfer);
		this.aggregates = null;
		renderAgain();
	}
	
	public Transfer<?,?> transfer() {return display.transfer();}
	public void transfer(Transfer<?,?> t) {
		display.transfer(t);
	}
	
	public Aggregator<?,?> aggregator() {return aggregator;}
	
	public Aggregates<?> aggregates() {return aggregates;}
	public void aggregates(Aggregates<?> aggregates, AffineTransform renderTransform) {
		if (aggregates != this.aggregates) {display.refAggregates(null);}
		this.renderTransform=renderTransform;
		this.aggregates = aggregates;
		subsetAggregates();
		aggregatesChangedProvider.fireActionListeners();
	}
	
	public void renderAgain() {
		if (renderer != null 
				&& dataset != null 
				&& !dataset.isEmpty() 
				&& aggregator != null) {
			renderPool.execute(new AggregateRender());
			repaint();
		}
	}
	
//	public void paintComponent(Graphics g) {
//		Runnable action = null;
//		if (renderer == null 
//				|| dataset == null ||  dataset.isEmpty() 
//				|| aggregator == null
//				|| renderError == true) {
//			g.setColor(Color.GRAY);
//			g.fillRect(0, 0, this.getWidth(), this.getHeight());
// 		} else if (fullRender) {
//			action = new AggregateRender();
//			renderPool.execute(action);
//			fullRender = false;
//			subsetRender = false;
//		} else if (subsetRender) {
//			System.out.println("Subset again");
//			subsetAggregates();
//			subsetRender = false;
//		} 
//	}
		
	/**Set the subset that will be sent to transfer.**/
	public void subsetAggregates() {
		Rectangle viewport = AggregatingDisplay.this.getBounds();				
		AffineTransform vt = viewTransform();
		int shiftX = (int) -(vt.getTranslateX()-renderTransform.getTranslateX());
		int shiftY = (int) -(vt.getTranslateY()-renderTransform.getTranslateY());
		Aggregates<?> subset = new SubsetWrapper<>(aggregates, shiftX, shiftY, shiftX+viewport.width, shiftY+viewport.height);

		display.aggregates(subset,null);
	}
	
	public String toString() {return String.format("AggregatingDisplay[Dataset: %1$s, Transfer: %2$s]", dataset, display.transfer(), aggregator);}
	
    /**Use this transform to convert values from the absolute system
     * to the screen system.
     */
	public AffineTransform viewTransform() {return new AffineTransform(viewTransformRef);}	
	public AffineTransform renderTransform() {return new AffineTransform(renderTransform);}	
	public void viewTransform(AffineTransform vt) throws NoninvertibleTransformException {
		//Only force full re-render if the zoom factor changed
		if (renderTransform == null 
				|| vt.getScaleX() != viewTransformRef.getScaleX()
				|| vt.getScaleY() != viewTransformRef.getScaleY()) {
			renderAgain();
		} 
		
		if (renderTransform == null
				|| vt.getTranslateX() != viewTransformRef.getTranslateX()
				|| vt.getTranslateY() != viewTransformRef.getTranslateY()) {
			subsetAggregates();
		}
		this.viewTransformRef = vt;
	}
	
	public void zoomFit() {
		try {
			Rectangle2D content = (dataset == null ? null : dataset().bounds());
			if (dataset() == null || content ==null || content.isEmpty()) {return;}
			
			AffineTransform vt = Util.zoomFit(content, getWidth(), getHeight());
			viewTransform(vt);
		} catch (Exception e) {} //Ignore all zoom-fit errors...they are usually caused by under-specified state
	}
	
	public Rectangle2D dataBounds() {return dataset.bounds();}
	
	private final class AggregateRender<G,I,A> implements Runnable {
		public void run() {
			long start = System.currentTimeMillis();
			try {
				Rectangle databounds = viewTransform().createTransformedShape(dataset.bounds()).getBounds();
				AffineTransform rt = Util.zoomFit(dataset.bounds(), databounds.width, databounds.height);
				
				@SuppressWarnings("unchecked")
				Glyphset<G,I> data = (Glyphset<G,I>) dataset;
				
				@SuppressWarnings("unchecked")
				Aggregator<I,A> op = (Aggregator<I,A>) aggregator;

				Selector<G> selector = TouchesPixel.make(data);
				
				Aggregates<?> a = renderer.aggregate(data, selector, op, rt, databounds.width, databounds.height);

//				Rectangle renderbounds = rt.createTransformedShape(data.bounds()).getBounds();
//				Aggregates<A> a = AggregateUtils.make(renderbounds.x, renderbounds.y,
//						renderbounds.x+renderbounds.width, renderbounds.y+renderbounds.height, 
//						op.identity());
//
//				IncrementalTask<G,I,A> t = 
//					 new IncrementalTask<>(
//							 AggregatingDisplay.this, 
//							a, 
//							renderer,
//							10,
//							data, 
//							selector, 
//							op, 
//							rt, 
//							databounds.width, 
//							databounds.height);
//					 
//				Thread th = new Thread(t, "Incremental");
//				th.start();
				
				AggregatingDisplay.this.aggregates(a, rt);
				long end = System.currentTimeMillis();
				if (PERF_REP) {
					System.out.printf("%d ms (Base aggregates render on %d x %d grid)\n",
							(end-start), aggregates.highX()-aggregates.lowX(), aggregates.highY()-aggregates.lowY());
				}
				AggregatingDisplay.this.subsetAggregates();
				
			} catch (Exception e) {
				String msg = e.getMessage() == null ? e.getClass().getName() : e.getMessage();
				System.err.println(msg);
				e.printStackTrace();
			}
			
			AggregatingDisplay.this.repaint();
		}
	}
	
	public static final class IncrementalTask<G,I,A> implements Runnable {
		final Aggregates<A> acc;
		final Renderer renderer;
		final int steps;
		final Glyphset<? extends G, ? extends I> glyphs; 
		final Selector<G> selector;
		final Aggregator<I,A> op;
		final AffineTransform viewTransform;
		final int width;
		final int height;
		final AggregatingDisplay target;
		
		public IncrementalTask(
				AggregatingDisplay target,
				Aggregates<A> acc, Renderer renderer, int steps,
				Glyphset<? extends G, ? extends I> glyphs,
				Selector<G> selector, Aggregator<I, A> op,
				AffineTransform viewTransform, int width, int height) {
			super();
			this.target = target;
			this.acc = acc;
			this.renderer = renderer;
			this.steps = steps;
			this.glyphs = glyphs;
			this.selector = selector;
			this.op = op;
			this.viewTransform = viewTransform;
			this.width = width;
			this.height = height;
		}



		@Override
		public void run() {
			long step = glyphs.segments()/steps;
			for (long bottom=0; bottom<glyphs.segments(); bottom+=step) {
				Glyphset<? extends G, ? extends I> subset = glyphs.segment(bottom, Math.min(bottom+step, glyphs.size()));
				//Glyphset<? extends G, ? extends I> subset = glyphs;
				Aggregates<A> update = renderer.aggregate(subset, selector, op, viewTransform, width, height);
				AggregationStrategies.horizontalRollup(acc, update, op);
				target.subsetAggregates();
				System.out.println("Updated...");
			}			
		}
		
	}
}
