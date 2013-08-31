package ar.app.components;

import java.awt.*;
import java.awt.geom.AffineTransform;

import ar.*;
import ar.aggregates.FlatAggregates;
import ar.renderers.AggregationStrategies;
import ar.util.Util;

/**Panel that builds aggregates at a "base resolution" instead of the screen's native resolution.*/
public class ARCascadePanel extends ARPanel {
	private static final long serialVersionUID = 2549632552666062944L;
	
	private final int baseWidth = 500;
	private final int baseHeight = 500;
	private final AffineTransform renderTransform;
	
	private volatile Aggregates<?> baseAggregates;
	
	public ARCascadePanel(Aggregator<?,?> reduction, Transfer<?,?> transfer, Glyphset<?> glyphs, Renderer renderer) {
		super(reduction, transfer, glyphs, renderer);
		if (glyphs != null) {
			renderTransform = Util.zoomFit(glyphs.bounds(), baseWidth, baseHeight);
		} else {
			renderTransform = null;
		}
	}
	
	protected ARPanel build(Aggregator<?,?> aggregator, Transfer<?,?> transfer, Glyphset<?> glyphs, Renderer renderer) {
		return new ARCascadePanel(aggregator, transfer, glyphs, renderer);
	}
	
	public void baseAggregates(Aggregates<?> aggregates) {
		this.baseAggregates = aggregates;
		super.aggregates(null);
	}
	
	@Override
	public void paint(Graphics g) {
		Runnable action = null;
		if (renderer == null 
				|| dataset == null ||  dataset.isEmpty() 
				|| aggregator == null
				|| renderError == true) {
			g.setColor(Color.GRAY);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
		} else if (baseAggregates == null) {
			action = new AggregateRender();
		} else if (renderAgain || aggregates == null) {
			action = new CascadeRender();
		} 

		if (action != null && (renderThread == null || !renderThread.isAlive())) {
			renderAgain =false; 
			renderThread = new Thread(action, "Render Thread");
			renderThread.setDaemon(true);
			renderThread.start();
		}
		super.paint(g);
	
	}
	
	private final class CascadeRender implements Runnable {
		@SuppressWarnings({"unchecked","rawtypes"})
		public void run() {
			long start = System.currentTimeMillis();
			try {
				Rectangle viewportBounds = ARCascadePanel.this.getBounds();
				AffineTransform vt = viewTransform();
				int shiftX = (int) -vt.getTranslateX();
				int shiftY = (int) -vt.getTranslateY();
				double scaleX = vt.getScaleX()/renderTransform.getScaleX();
				double scaleY = vt.getScaleY()/renderTransform.getScaleY();
//				int width = (int) (viewportBounds.width * scaleX);
//				int height = (int) (viewportBounds.height * scaleY);
				int width = (int) (viewportBounds.width);
				int height = (int) (viewportBounds.height);
				
				Aggregates subset = FlatAggregates.subset(
						baseAggregates, 
						shiftX, shiftY, 
						shiftX+width, shiftY+height);
				
				//subset = AggregationStrategies.verticalRollup(subset, aggregator, viewportBounds.width, viewportBounds.height);
				
				ARCascadePanel.this.aggregates(subset);
				long end = System.currentTimeMillis();
				if (PERF_REP) {
					System.out.printf("%d ms (Cascade render)\n",
							(end-start), aggregates.highX()-aggregates.lowX(), aggregates.highY()-aggregates.lowY());
				}
			} catch (ClassCastException e) {
				renderError = true;
			}
			
			ARCascadePanel.this.repaint();
		}	
	}

	private final class AggregateRender implements Runnable {
		@SuppressWarnings({"unchecked","rawtypes"})
		public void run() {
			long start = System.currentTimeMillis();
			try {
				ARCascadePanel.this.baseAggregates(renderer.aggregate(dataset, (Aggregator) aggregator, renderTransform.createInverse(), baseWidth, baseHeight));
				long end = System.currentTimeMillis();
				if (PERF_REP) {
					System.out.printf("%d ms (Aggregates render on %d x %d grid)\n",
							(end-start), baseAggregates.highX()-baseAggregates.lowX(), baseAggregates.highY()-baseAggregates.lowY());
				}
			} catch (Exception e) {
				renderError = true;
			}
			
			ARCascadePanel.this.repaint();
		}
	}
	
	
	public String toString() {return String.format("ARCascadePanel[Dataset: %1$s, Ruleset: %2$s]", dataset, display.transfer(), aggregator);}
}
