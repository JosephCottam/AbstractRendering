package ar.app.display;

import java.awt.*;
import java.awt.geom.AffineTransform;

import ar.*;
import ar.aggregates.FlatAggregates;
import ar.renderers.AggregationStrategies;
import ar.util.Util;

/**Panel that builds aggregates at a "base resolution" instead of the screen's native resolution.
 * 
 * NOTE: This panel is not fit for consumption yet due to issues surrounding zoom.
 * */
public class ARCascadeDisplay extends FullDisplay {
	private static final long serialVersionUID = 2549632552666062944L;
	
	private final int baseWidth = 1000;
	private final int baseHeight = 1000;
	private final AffineTransform renderTransform;
	
	private volatile Aggregates<?> baseAggregates;
	
	public ARCascadeDisplay(Aggregator<?,?> reduction, Transfer<?,?> transfer, Glyphset<?> glyphs, Renderer renderer) {
		super(reduction, transfer, glyphs, renderer);
		if (glyphs != null) {
			renderTransform = Util.zoomFit(glyphs.bounds(), baseWidth, baseHeight);
		} else {
			renderTransform = null;
		}
	}
	
	protected FullDisplay build(Aggregator<?,?> aggregator, Transfer<?,?> transfer, Glyphset<?> glyphs, Renderer renderer) {
		return new ARCascadeDisplay(aggregator, transfer, glyphs, renderer);
	}
	
	public void baseAggregates(Aggregates<?> aggregates) {
		this.baseAggregates = aggregates;
		super.aggregates(null);
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
		} else if (baseAggregates == null) {
			action = new AggregateRender();
		} else if (renderAgain || aggregates == null) {
			action = new CascadeRender();
		} 

		if (action != null) {
			renderPool.execute(action);
			renderAgain =false; 
		}
	}
	
	private final class CascadeRender implements Runnable {
		@SuppressWarnings({"unchecked","rawtypes"})
		public void run() {
			long start = System.currentTimeMillis();
			try {
				Rectangle viewport = ARCascadeDisplay.this.getBounds();
				AffineTransform vt = viewTransform();
				double scale = renderTransform.getScaleX()/vt.getScaleX();
				int shiftX = (int) -(vt.getTranslateX()-(renderTransform.getTranslateX()/scale));
				int shiftY = (int) -(vt.getTranslateY()-(renderTransform.getTranslateY()/scale));
				
				Aggregates subset = AggregationStrategies.verticalRollup((Aggregates) baseAggregates, aggregator, Math.ceil(scale));
				subset = FlatAggregates.subset(
						subset, 
						shiftX, shiftY, 
						shiftX+viewport.width, shiftY+viewport.height);
				
				ARCascadeDisplay.this.aggregates(subset);
				long end = System.currentTimeMillis();
				if (PERF_REP) {
					System.out.printf("%d ms (Cascade render)\n",
							(end-start), aggregates.highX()-aggregates.lowX(), aggregates.highY()-aggregates.lowY());
				}
			} catch (ClassCastException e) {
				renderError = true;
			}
			
			ARCascadeDisplay.this.repaint();
		}	
	}

	private final class AggregateRender implements Runnable {
		@SuppressWarnings({"unchecked","rawtypes"})
		public void run() {
			long start = System.currentTimeMillis();
			try {
				ARCascadeDisplay.this.baseAggregates(renderer.aggregate(dataset, (Aggregator) aggregator, renderTransform.createInverse(), baseWidth, baseHeight));
				long end = System.currentTimeMillis();
				if (PERF_REP) {
					System.out.printf("%d ms (Aggregates render on %d x %d grid)\n",
							(end-start), baseAggregates.highX()-baseAggregates.lowX(), baseAggregates.highY()-baseAggregates.lowY());
				}
			} catch (Exception e) {
				renderError = true;
			}
			
			ARCascadeDisplay.this.repaint();
		}
	}
	
	
	public String toString() {return String.format("ARCascadePanel[Dataset: %1$s, Ruleset: %2$s]", dataset, display.transfer(), aggregator);}
}
