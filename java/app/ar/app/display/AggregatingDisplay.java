package ar.app.display;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.ExecutorService;

import ar.*;
import ar.aggregates.AggregateUtils;
import ar.app.util.ActionProvider;
import ar.app.util.MostRecentOnlyExecutor;
import ar.app.util.ZoomPanHandler;
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

	protected volatile boolean fullRender = false;
	protected volatile boolean subsetRender = false;
	protected volatile boolean renderError = false;
	protected volatile Aggregates<?> aggregates;
	protected ExecutorService renderPool = new MostRecentOnlyExecutor(1,"FullDisplay Render Thread");//TODO: Redoing painting to use futures...
		
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
	public void refAggregates(Aggregates<?> aggregates) {display.refAggregates(aggregates);}
	
	public Renderer renderer() {return renderer;}
	
	public Glyphset<?,?> dataset() {return dataset;}
	public void dataset(Glyphset<?,?> data, Aggregator<?,?> aggregator, Transfer<?,?> transfer) {
		this.dataset = data;
		this.aggregator = aggregator;
		this.transfer(transfer);
		this.aggregates = null;
		this.repaint();
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
		fullRender=false;
		this.aggregates = aggregates;
		this.repaint();
		aggregatesChangedProvider.fireActionListeners();
	}
	
	public void renderAgain() {
		fullRender=true;
		subsetRender=true;
		renderError=false;
		repaint();
	}
	
	public void paintComponent(Graphics g) {
		Runnable action = null;
		if (renderer == null 
				|| dataset == null ||  dataset.isEmpty() 
				|| aggregator == null
				|| renderError == true) {
			g.setColor(Color.GRAY);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
 		} else if (fullRender) {
			action = new AggregateRender();
			fullRender = false;
		} else if (subsetRender) {
			action = new SubsetRender();
			subsetRender = false;
		} 

		if (action != null) {renderPool.execute(action);}
	}
		
	/**Set the subset that will be sent to transfer.**/
	public void subsetAggregates(Aggregates<?> aggregates) {
		display.aggregates(aggregates,null);
		repaint();
	}
	
	private final class SubsetRender implements Runnable {
		public void run() {
			long start = System.currentTimeMillis();
			try {
				Rectangle viewport = AggregatingDisplay.this.getBounds();				
				AffineTransform vt = viewTransform();
				int shiftX = (int) -(vt.getTranslateX()-renderTransform.getTranslateX());
				int shiftY = (int) -(vt.getTranslateY()-renderTransform.getTranslateY());
				
				Aggregates<?> subset = AggregateUtils.subset(
						aggregates, 
						shiftX, shiftY, 
						shiftX+viewport.width, shiftY+viewport.height);
				
				AggregatingDisplay.this.subsetAggregates(subset);
				if (subset == null) {return;}
				long end = System.currentTimeMillis();
				if (PERF_REP) {
					System.out.printf("%d ms (Subset render)\n",
							(end-start), subset.highX()-subset.lowX(), subset.highY()-subset.lowY());
				}
			} catch (ClassCastException e) {
				renderError = true;
				System.err.println(e.getMessage());
				//e.printStackTrace();
			}
			
			AggregatingDisplay.this.repaint();
		}	
	}
	
	private final class AggregateRender implements Runnable {
		
		public void run() {
			long start = System.currentTimeMillis();
			try {
				Rectangle databounds = viewTransform().createTransformedShape(dataset.bounds()).getBounds();
				renderTransform = Util.zoomFit(dataset.bounds(), databounds.width, databounds.height);
				
				@SuppressWarnings({"rawtypes"})
				Selector selector = TouchesPixel.make(dataset);
				
				@SuppressWarnings({"unchecked","rawtypes"})
				Aggregates<?> a = renderer.aggregate(dataset, selector, (Aggregator) aggregator, renderTransform, databounds.width, databounds.height);
				
				AggregatingDisplay.this.aggregates(a, renderTransform);
				long end = System.currentTimeMillis();
				if (PERF_REP) {
					System.out.printf("%d ms (Base aggregates render on %d x %d grid)\n",
							(end-start), aggregates.highX()-aggregates.lowX(), aggregates.highY()-aggregates.lowY());
				}
				
			} catch (Exception e) {
				renderError = true;
				String msg = e.getMessage() == null ? e.getClass().getName() : e.getMessage();
				System.err.println(msg);
				e.printStackTrace();
			}
			
			AggregatingDisplay.this.repaint();
		}
	}
	
	public String toString() {return String.format("AggregatingDisplay[Dataset: %1$s, Transfer: %2$s]", dataset, display.transfer(), aggregator);}
	
	
	
    /**Use this transform to convert values from the absolute system
     * to the screen system.
     */
	public AffineTransform viewTransform() {return new AffineTransform(viewTransformRef);}	
	public AffineTransform renderTransform() {return new AffineTransform(viewTransformRef);}	
	public void viewTransform(AffineTransform vt) throws NoninvertibleTransformException {
		//Only force full re-render if the zoom factor changed
		if (renderTransform == null 
				|| vt.getScaleX() != viewTransformRef.getScaleX()
				|| vt.getScaleY() != viewTransformRef.getScaleY()) {
			fullRender = true;
			subsetRender = true;
		} 
		
		if (renderTransform == null
				|| vt.getTranslateX() != viewTransformRef.getTranslateX()
				|| vt.getTranslateY() != viewTransformRef.getTranslateY()) {
			subsetRender=true;
		}
		
		this.viewTransformRef = vt;
		repaint();
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
}