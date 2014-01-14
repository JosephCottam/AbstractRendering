package ar.app.display;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

import javax.swing.JComponent;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;
import ar.util.HasViewTransform;

/**Root interactive display interface.**/
public abstract class ARComponent extends JComponent {	
	private static final long serialVersionUID = 4454152912475936102L;
	
	/**Flag to enable/disable performance reporting messages to system.out (defaults to false)**/
	public static boolean PERF_REP = false;

	public abstract Transfer<?,?> transfer();
	public abstract void transfer(Transfer<?, ?> t);

	public abstract Aggregates<?> aggregates();
	public abstract void aggregates(Aggregates<?> aggregates, AffineTransform renderTransform);
	
	public abstract Aggregates<?> refAggregates();
	public abstract void refAggregates(Aggregates<?> aggregates);
	
	public abstract Renderer renderer();

	/**Force a full re-render.**/
	public abstract void renderAgain();
	
	public static abstract class Aggregating extends ARComponent implements HasViewTransform {
		private static final long serialVersionUID = 404081973530563354L;

		public abstract Glyphset<?,?> dataset();
		public abstract void dataset(Glyphset<?,?> data, Aggregator<?,?> agg, Transfer<?,?> t);
	
		public abstract Aggregator<?,?> aggregator();
		public abstract Transfer<?,?> transfer();
		
		public abstract AffineTransform viewTransform();
		public abstract AffineTransform renderTransform();
		public abstract void viewTransform(AffineTransform vt) throws NoninvertibleTransformException;
	}
	
	public interface Holder {
		public ARComponent getARComponent();
	}
}