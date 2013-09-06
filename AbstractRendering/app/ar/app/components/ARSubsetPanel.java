package ar.app.components;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

import ar.*;
import ar.aggregates.FlatAggregates;
import ar.util.Util;

/**Panel that renders more than just what's visible on the screen so pan can happen quickly.
 * */
public class ARSubsetPanel extends ARPanel {
	private static final long serialVersionUID = 2549632552666062944L;
	
	private AffineTransform renderTransform;
	
	private boolean fullRender;
	private boolean subsetRender;
	
	private boolean viewRelativeTransfer = true; 
	
	private volatile Aggregates<?> baseAggregates;
	
	public ARSubsetPanel(Aggregator<?,?> reduction, Transfer<?,?> transfer, Glyphset<?> glyphs, Renderer renderer) {
		super(reduction, transfer, glyphs, renderer);
	}
	
	protected ARPanel build(Aggregator<?,?> aggregator, Transfer<?,?> transfer, Glyphset<?> glyphs, Renderer renderer) {
		return new ARSubsetPanel(aggregator, transfer, glyphs, renderer);

	}
	
	public void baseAggregates(Aggregates<?> aggregates) {
		this.baseAggregates = aggregates;
		if (!viewRelativeTransfer) {display.setRefAggregates(aggregates);}
		else {display.setRefAggregates(null);}
		aggregates(null);
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
		
		transferViewTransform(vt);
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
		} else if (fullRender || baseAggregates == null || renderTransform == null) {
			action = new AggregateRender();
			fullRender = false;
		} else if (subsetRender || aggregates == null) {
			action = new SubsetRender();
			subsetRender = false;
		} 

		if (action != null) {
			renderPool.execute(action);
		}
	}
	
	private final class SubsetRender implements Runnable {
		public void run() {
			long start = System.currentTimeMillis();
			try {
				Rectangle viewport = ARSubsetPanel.this.getBounds();				
				AffineTransform vt = viewTransform();
				int shiftX = (int) -(vt.getTranslateX()-renderTransform.getTranslateX());
				int shiftY = (int) -(vt.getTranslateY()-renderTransform.getTranslateY());
				
				Aggregates<?> subset = FlatAggregates.subset(
						baseAggregates, 
						shiftX, shiftY, 
						shiftX+viewport.width, shiftY+viewport.height);
				
				ARSubsetPanel.this.aggregates(subset);
				long end = System.currentTimeMillis();
				if (PERF_REP) {
					System.out.printf("%d ms (Subset render)\n",
							(end-start), aggregates.highX()-aggregates.lowX(), aggregates.highY()-aggregates.lowY());
				}
			} catch (ClassCastException e) {
				renderError = true;
			}
			
			ARSubsetPanel.this.repaint();
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
				
				ARSubsetPanel.this.baseAggregates(a);
				long end = System.currentTimeMillis();
				if (PERF_REP) {
					System.out.printf("%d ms (Base aggregates render on %d x %d grid)\n",
							(end-start), baseAggregates.highX()-baseAggregates.lowX(), baseAggregates.highY()-baseAggregates.lowY());
				}
				
			} catch (Exception e) {
				renderError = true;
			}
			
			ARSubsetPanel.this.repaint();
		}
	}
	
	
	public String toString() {return String.format("ARCascadePanel[Dataset: %1$s, Ruleset: %2$s]", dataset, display.transfer(), aggregator);}
}
