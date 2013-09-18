package ar.app.display;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

import javax.swing.JComponent;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;

/**Root interactive display interface.**/
public abstract class ARComponent extends JComponent {	
	private static final long serialVersionUID = 4454152912475936102L;
	
	/**Flag to enable/disable performance reporting messages to system.out (defaults to false)**/
	public static boolean PERF_REP = false;



	public abstract Transfer<?,?> transfer();
	public abstract void transfer(Transfer<?, ?> t);

	public abstract Aggregates<?> aggregates();
	public abstract void aggregates(Aggregates<?> aggregates);
	
	public abstract Aggregates<?> refAggregates();
	public abstract void refAggregates(Aggregates<?> aggregates);
	
	public abstract Renderer renderer();

	public static abstract class Aggregating extends ARComponent {
		private static final long serialVersionUID = 404081973530563354L;
		public abstract Glyphset<?> dataset();
		public abstract void dataset(Glyphset<?> data);
	
		public abstract Aggregator<?, ?> aggregator();
		public abstract void aggregator(Aggregator<?, ?> aggregator);
		
		public abstract void zoomFit();
		public abstract AffineTransform viewTransform();
		public abstract void viewTransform(AffineTransform vt) throws NoninvertibleTransformException;
	}
	
	public interface Holder {
		public ARComponent getARComponent();
	}
}