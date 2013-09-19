package ar.app.display;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

import ar.*;
import ar.aggregates.FlatAggregates;
import ar.app.util.ZoomPanHandler;
import ar.util.Util;

/**Panel that renders more than just what's visible on the screen so pan can happen quickly.
 * */
public class SubsetDisplay extends FullDisplay implements ZoomPanHandler.HasViewTransform {
	private static final long serialVersionUID = 2549632552666062944L;
	
	private AffineTransform renderTransform;
	
	private boolean fullRender;
	private boolean subsetRender;
	
	/**Should transfer specialization be done view-relative or always based on full-zoom?
	 * Default is to be based on full-zoom.**/
	private boolean viewRelativeTransfer = false; 
	
	private volatile Aggregates<?> subsetAggregates;
	
	public SubsetDisplay(Aggregator<?,?> reduction, Transfer<?,?> transfer, Glyphset<?> glyphs, Renderer renderer) {
		super(reduction, transfer, glyphs, renderer);
	}
		
	public boolean viewRelativeTransfer() {return viewRelativeTransfer;}
	public void viewRelativeTransfer(boolean viewRelativeTransfer) {
		this.viewRelativeTransfer =viewRelativeTransfer;
		repaint();
	}
	
	public void subsetAggregates(Aggregates<?> aggregates) {
		this.subsetAggregates = aggregates;
		if (!viewRelativeTransfer) {display.refAggregates(aggregates);}
		else {display.refAggregates(null);}
		aggregates(null);
		repaint();
	}
	
	public void setViewTransform(AffineTransform vt) throws NoninvertibleTransformException {
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
		
		viewTransform(vt);
	}

	
	@Override
	protected void panelPaint(Graphics g) {
		Runnable action = null;
		if (renderer == null 
				|| dataset == null ||  dataset.isEmpty() 
				|| aggregator == null
				|| renderError == true) {
			g.setColor(Color.GRAY);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
		} else if (fullRender || aggregates == null || renderTransform == null) {
			action = new AggregateRender();
			fullRender = false;
		} else if (subsetRender || subsetAggregates == null) {
			action = new SubsetRender();
			subsetRender = false;
		} 

		if (action != null) {
			renderPool.execute(action);
		}
	}
	
	
	public void viewTransform(AffineTransform vt) throws NoninvertibleTransformException {		
		this.viewTransformRef = vt;
		inverseViewTransformRef  = new AffineTransform(vt);
		inverseViewTransformRef.invert();
		this.subsetAggregates(null);
	}

	
	private final class SubsetRender implements Runnable {
		public void run() {
			long start = System.currentTimeMillis();
			try {
				Rectangle viewport = SubsetDisplay.this.getBounds();				
				AffineTransform vt = viewTransform();
				int shiftX = (int) -(vt.getTranslateX()-renderTransform.getTranslateX());
				int shiftY = (int) -(vt.getTranslateY()-renderTransform.getTranslateY());
				
				Aggregates<?> subset = FlatAggregates.subset(
						aggregates, 
						shiftX, shiftY, 
						shiftX+viewport.width, shiftY+viewport.height);
				
				SubsetDisplay.this.subsetAggregates(subset);
				if (subset == null) {return;}
				long end = System.currentTimeMillis();
				if (PERF_REP) {
					System.out.printf("%d ms (Subset render)\n",
							(end-start), subset.highX()-subset.lowX(), subset.highY()-subset.lowY());
				}
			} catch (ClassCastException e) {
				renderError = true;
			}
			
			SubsetDisplay.this.repaint();
		}	
	}

	private final class AggregateRender implements Runnable {
		public void run() {
			long start = System.currentTimeMillis();
			try {
				Rectangle databounds = viewTransform().createTransformedShape(dataset.bounds()).getBounds();
				renderTransform = Util.zoomFit(dataset.bounds(), databounds.width, databounds.height);

				@SuppressWarnings({"unchecked","rawtypes"})
				Aggregates<?> a = renderer.aggregate(dataset, (Aggregator) aggregator, renderTransform.createInverse(), databounds.width, databounds.height);
				
				SubsetDisplay.this.aggregates(a);
				long end = System.currentTimeMillis();
				if (PERF_REP) {
					System.out.printf("%d ms (Base aggregates render on %d x %d grid)\n",
							(end-start), aggregates.highX()-aggregates.lowX(), aggregates.highY()-aggregates.lowY());
				}
				
			} catch (Exception e) {
				renderError = true;
			}
			
			SubsetDisplay.this.repaint();
		}
	}
	
	
	public String toString() {return String.format("SubsetDisplay[Dataset: %1$s, Ruleset: %2$s]", dataset, display.transfer(), aggregator);}
}
