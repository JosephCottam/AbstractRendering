package ar.app.display;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.ExecutorService;

import ar.*;
import ar.app.util.ActionProvider;
import ar.app.util.MostRecentOnlyExecutor;
import ar.app.util.ZoomPanHandler;
import ar.selectors.TouchesPixel;
import ar.util.HasViewTransform;
import ar.util.Util;

/**Render and display exactly what fits on the screen.
 */
public class FullDisplay extends ARComponent.Aggregating implements HasViewTransform {
	protected static final long serialVersionUID = 1L;

	protected final ActionProvider aggregatesChangedProvider = new ActionProvider();
	
	protected final SimpleDisplay display;
	
	protected Aggregator<?,?> aggregator;
	protected Glyphset<?,?> dataset;
	
	protected AffineTransform viewTransformRef = new AffineTransform();

	protected volatile boolean renderAgain = false;
	protected volatile boolean renderError = false;
	protected volatile Aggregates<?> aggregates;
	protected ExecutorService renderPool = new MostRecentOnlyExecutor(1,"FullDisplay Render Thread");//TODO: Redoing painting to use futures...
		
	protected final Renderer renderer;

	/**Create a new instance.
	 * 
	 * TODO: Investigate just taking in 'renderer' since that is the only immutable thing.
	 */
	public FullDisplay(Aggregator<?,?> aggregator, Transfer<?,?> transfer, Glyphset<?,?> glyphs, Renderer renderer) {
		super();
		display = new SimpleDisplay(null, transfer, renderer);
		this.setLayout(new BorderLayout());
		this.add(display, BorderLayout.CENTER);
		this.invalidate();
		this.aggregator = aggregator;
		this.dataset = glyphs;
		this.renderer = renderer;
		
		ZoomPanHandler h = new ZoomPanHandler();
		super.addMouseListener(h);
		super.addMouseMotionListener(h);
	}
	
	protected void finalize() {renderPool.shutdown();}
	public void addAggregatesChangedListener(ActionListener l) {aggregatesChangedProvider.addActionListener(l);}

	public Aggregates<?> refAggregates() {return display.refAggregates();}
	public void refAggregates(Aggregates<?> aggregates) {display.refAggregates(aggregates);}
	
	public Renderer renderer() {return renderer;}
	
	public Glyphset<?,?> dataset() {return dataset;}
	public void dataset(Glyphset<?,?> data) {
		this.dataset = data;
		this.aggregates = null;
		this.repaint();
	}
	
	public Transfer<?,?> transfer() {return display.transfer();}
	public void transfer(Transfer<?,?> t) {display.transfer(t);}
	
	public Aggregator<?,?> aggregator() {return aggregator;}
	public void aggregator(Aggregator<?,?> aggregator) {
		this.aggregator = aggregator;
		aggregates(null,null);
		this.repaint();
	}
	
	public Aggregates<?> aggregates() {return aggregates;}
	public void aggregates(Aggregates<?> aggregates, AffineTransform renderTransform) {
		if (aggregates != this.aggregates) {display.refAggregates(null);}

		this.display.aggregates(aggregates, renderTransform);
		this.aggregates = aggregates;
		this.repaint();
		aggregatesChangedProvider.fireActionListeners();
	}
	
	public void renderAgain() {
		renderAgain=true;
		renderError=false;
		repaint();
	}
	
	public void paintComponent(Graphics g) {
		Runnable action = null;
		if (renderer == null 
				|| dataset == null ||  dataset.isEmpty() 
				|| aggregator == null
				|| renderError == true) {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
		} else if (renderAgain || aggregates == null) {
			action = new RenderAggregates();
		} 

		if (action != null) {
			renderPool.execute(action);
			renderAgain =false; 
		} 
		super.paintComponent(g);
	}
		
	/**Calculate aggregates for a given region.**/
	protected final class RenderAggregates implements Runnable {
		@SuppressWarnings({"unchecked","rawtypes"})
		public void run() {
			int width = FullDisplay.this.getWidth();
			int height = FullDisplay.this.getHeight();
			long start = System.currentTimeMillis();
			AffineTransform vt = viewTransform();
			Selector selector = TouchesPixel.make(dataset);

			try {
				aggregates = renderer.aggregate(dataset, selector, (Aggregator) aggregator, vt, width, height);
				display.aggregates(aggregates, viewTransformRef);
				long end = System.currentTimeMillis();
				if (PERF_REP) {
					System.out.printf("%d ms (Aggregates render on %d x %d grid)\n",
							(end-start), aggregates.highX()-aggregates.lowX(), aggregates.highY()-aggregates.lowY());
				}
			} catch (ClassCastException e) {
				renderError = true;
			}
			
			FullDisplay.this.repaint();
		}
	}
	
	
	public String toString() {return String.format("ARPanel[Dataset: %1$s, Ruleset: %2$s]", dataset, display.transfer(), aggregator);}
	
	
	
    /**Use this transform to convert values from the absolute system
     * to the screen system.
     */
	public AffineTransform viewTransform() {return new AffineTransform(viewTransformRef);}	
	public AffineTransform renderTransform() {return new AffineTransform(viewTransformRef);}	
	public void viewTransform(AffineTransform vt) throws NoninvertibleTransformException {
		if (this.viewTransformRef.equals(vt)) {return;}
		this.viewTransformRef = vt;
		this.renderAgain = true;
		this.repaint();
	}

	
	public void zoomFit() {
		try {
			Rectangle2D content = (dataset == null ? null : dataset().bounds());
			if (dataset() == null || content ==null || content.isEmpty()) {return;}
			
			AffineTransform vt = Util.zoomFit(content, getWidth(), getHeight());
			viewTransform(vt);
		} catch (Exception e) {} //Ignore all zoom-fit errors...they are usually caused by under-specified state
	}
}
