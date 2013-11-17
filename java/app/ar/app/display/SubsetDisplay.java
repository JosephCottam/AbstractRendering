package ar.app.display;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

import ar.*;
import ar.aggregates.AggregateUtils;
import ar.selectors.TouchesPixel;
import ar.util.Util;

/**Panel that renders the entire dataset at the current zoom resolution, so pan can happen quickly.*/
public class SubsetDisplay extends FullDisplay {
	private static final long serialVersionUID = 2549632552666062944L;
	
	private boolean fullRender;
	private boolean subsetRender;
	
	/**Affine transform used to render the full set of aggregates (data-space to 'aggregates')**/
	private AffineTransform renderTransform;
	
	/**Create a new instance.**/
	public SubsetDisplay(Aggregator<?,?> reduction, Transfer<?,?> transfer, Glyphset<?,?> glyphs, Renderer renderer) {
		super(reduction, transfer, glyphs, renderer);
	}
		
	/**Set the subset that will be sent to transfer.**/
	public void subsetAggregates(Aggregates<?> aggregates) {
		display.aggregates(aggregates,null);
		repaint();
	}
	
	public Aggregates<?> aggregates() {return aggregates;}
	public void aggregates(Aggregates<?> aggregates, AffineTransform renderTransform) {
		if (aggregates != this.aggregates) {display.refAggregates(null);}
		this.renderTransform=renderTransform;
		fullRender=false;
		this.aggregates = aggregates;
		this.repaint();
		aggregatesChangedProvider.fireActionListeners();
	}
	
	/**Get the affine transform currently being used to render from geometric space to aggregates.**/
	public AffineTransform renderTransform() {return renderTransform;}
	
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

	public void renderAgain() {
		fullRender=true;
		subsetRender=true;
		super.renderAgain();
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
	
	private final class SubsetRender implements Runnable {
		public void run() {
			long start = System.currentTimeMillis();
			try {
				Rectangle viewport = SubsetDisplay.this.getBounds();				
				AffineTransform vt = viewTransform();
				int shiftX = (int) -(vt.getTranslateX()-renderTransform.getTranslateX());
				int shiftY = (int) -(vt.getTranslateY()-renderTransform.getTranslateY());
				
				Aggregates<?> subset = AggregateUtils.subset(
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
				System.err.println(e.getMessage());
				//e.printStackTrace();
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
				
				@SuppressWarnings({"rawtypes"})
				Selector selector = TouchesPixel.make(dataset);
				
				@SuppressWarnings({"unchecked","rawtypes"})
				Aggregates<?> a = renderer.aggregate(dataset, selector, (Aggregator) aggregator, renderTransform, databounds.width, databounds.height);
				
				SubsetDisplay.this.aggregates(a, renderTransform);
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
			
			SubsetDisplay.this.repaint();
		}
	}
	
	
	public String toString() {return String.format("SubsetDisplay[Dataset: %1$s, Ruleset: %2$s]", dataset, display.transfer(), aggregator);}
}
