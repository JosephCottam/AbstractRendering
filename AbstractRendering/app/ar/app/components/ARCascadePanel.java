package ar.app.components;

import java.awt.*;
import java.awt.geom.AffineTransform;

import ar.*;
import ar.aggregates.FlatAggregates;
import ar.util.Util;

/**Panel that builds aggregates at a "base resolution" instead of the screen's native resolution.*/
public class ARCascadePanel extends ARPanel {
	private static final long serialVersionUID = 2549632552666062944L;
	
	private final int baseWidth = 1000;
	private final int baseHeight = 1000;
	private final AffineTransform renderTransform;
	
	public ARCascadePanel(Aggregator<?,?> reduction, Transfer<?,?> transfer, Glyphset<?> glyphs, Renderer renderer) {
		super(reduction, transfer, glyphs, renderer);
		if (glyphs != null) {
			renderTransform = Util.zoomFit(glyphs.bounds(), baseWidth, baseHeight);
		} else {
			renderTransform = null;
		}
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
		} else if (renderAgain || aggregates == null) {
			action = new FullRender();
		} 

		if (action != null && (renderThread == null || !renderThread.isAlive())) {
			renderAgain =false; 
			renderThread = new Thread(action, "Render Thread");
			renderThread.setDaemon(true);
			renderThread.start();
		}
		super.paint(g);
	
	}
	
	public final class FullRender implements Runnable {
		@SuppressWarnings({"unchecked","rawtypes"})
		public void run() {
			long start = System.currentTimeMillis();
			try {
				aggregates = renderer.aggregate(dataset, (Aggregator) aggregator, renderTransform, baseWidth, baseHeight);
				
				//Aggregates zoomed = AggregationStrategies.foldUp(aggregates, (Aggregator) aggregator);
				//PREVENT RE-RENDER ON ZOOM CHANGE OR PAN...
				
				Rectangle viewBounds = ARCascadePanel.this.getBounds();
				AffineTransform ivt = inverseViewTransform();
				viewBounds = ivt.createTransformedShape(viewBounds).getBounds();

				Aggregates subset = FlatAggregates.subset(aggregates, viewBounds.x, viewBounds.y, viewBounds.x+viewBounds.width, viewBounds.y+viewBounds.height);
				
				display.setAggregates(subset);
				long end = System.currentTimeMillis();
				if (PERF_REP) {
					System.out.printf("%d ms (Aggregates render on %d x %d grid\n",
							(end-start), aggregates.highX()-aggregates.lowX(), aggregates.highY()-aggregates.lowY());
				}
			} catch (ClassCastException e) {
				renderError = true;
			}
			
			ARCascadePanel.this.repaint();
		}
	}
	
	
	public String toString() {return String.format("ARCascadePanel[Dataset: %1$s, Ruleset: %2$s]", dataset, display.transfer(), aggregator);}
}
