package ar.app.display;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.ExecutorService;

import ar.*;
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
	
	private AffineTransform renderedTransform = new AffineTransform();

	protected volatile boolean fullRender = false;
	protected volatile boolean renderError = false;
	protected volatile Aggregates<?> aggregates;
	protected ExecutorService renderPool = new MostRecentOnlyExecutor(1,"FullDisplay Render Thread");
		
	protected final Renderer renderer;

	public AggregatingDisplay(Renderer renderer) {
		super();
		this.renderer = renderer;
		display = new TransferDisplay(renderer);
		this.setLayout(new BorderLayout());
		this.add(display, BorderLayout.CENTER);
		
		ZoomPanHandler.installOn(this);
	}

	/**Create a new instance.
	 * 
	 */
	public AggregatingDisplay(Aggregator<?,?> aggregator, Transfer<?,?> transfer, Glyphset<?,?> glyphs, Renderer renderer) {
		this(renderer);
		this.aggregator = aggregator;
		this.dataset = glyphs;		
		display.transfer(transfer);
		ZoomPanHandler.installOn(this);
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
		aggregates(null, null);
		fullRender = true;
		renderError = false;
		this.repaint();
	}
	
	public Transfer<?,?> transfer() {return display.transfer();}
	public void transfer(Transfer<?,?> t) {
		display.transfer(t);
	}
	
	public Aggregator<?,?> aggregator() {return aggregator;}
	
	public Aggregates<?> transferAggregates() {return display.transferAggregates();}
	public Aggregates<?> aggregates() {return aggregates;}
	public void aggregates(Aggregates<?> aggregates, AffineTransform renderedTransform) {
		display.aggregates(aggregates, renderedTransform);
		display.refAggregates(null);

		this.renderedTransform=renderedTransform;
		this.aggregates = aggregates;
		fullRender=false;
		aggregatesChangedProvider.fireActionListeners();
	}

	public void renderAgain() {
		fullRender=true;
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
			renderPool.execute(action);
			fullRender = false;
		} 

	}
	
	public String toString() {return String.format("AggregatingDisplay[Dataset: %1$s, Transfer: %2$s]", dataset, display.transfer(), aggregator);}
	
    /**Use this transform to convert values from the absolute system
     * to the screen system.
     */
	@Override public AffineTransform viewTransform() {return display.viewTransform();}	
	@Override public AffineTransform renderTransform() {return new AffineTransform(renderedTransform);}	
	@Override public void viewTransform(AffineTransform vt, boolean provisional) {
		//Only force full re-render if the zoom factor changed non-provisionally
		fullRender = !provisional 
				&& (renderedTransform == null 
					|| vt.getScaleX() != renderedTransform.getScaleX()
					|| vt.getScaleY() != renderedTransform.getScaleY());
		display.viewTransform(vt, provisional); 		
		repaint();
	}

	
	public void zoomFit() {
		try {
			Rectangle2D content = (dataset == null ? null : dataset().bounds());
			if (content ==null || content.isEmpty()) {return;}
			
			AffineTransform vt = Util.zoomFit(content, getWidth(), getHeight());
			viewTransform(vt, false);
		} catch (Exception e) {} //Ignore all zoom-fit errors...they are usually caused by under-specified state
	}
	
	public Rectangle2D dataBounds() {return dataset.bounds();}
	
	private final class AggregateRender implements Runnable {
		
		public void run() {
			long start = System.currentTimeMillis();
			try {
				Rectangle databounds = viewTransform().createTransformedShape(dataset.bounds()).getBounds();
				AffineTransform rt = Util.zoomFit(dataset.bounds(), databounds.width, databounds.height);
				
				@SuppressWarnings({"rawtypes"})
				Selector selector = TouchesPixel.make(dataset);
				
				@SuppressWarnings({"unchecked","rawtypes"})
				Aggregates<?> a = renderer.aggregate(dataset, selector, (Aggregator) aggregator, rt, databounds.width, databounds.height);
				
				AggregatingDisplay.this.aggregates(a, rt);
				long end = System.currentTimeMillis();
				if (PERFORMANCE_REPORTING) {
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
}
