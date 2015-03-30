package ar.app.display;

import java.awt.geom.AffineTransform;

import javax.swing.JComponent;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;
import ar.util.HasViewTransform;
import ar.util.axis.DescriptorPair;

/**Root interactive display interface.**/
public abstract class ARComponent extends JComponent implements HasViewTransform {	
	private static final long serialVersionUID = 4454152912475936102L;
	
	/**Flag to enable/disable performance reporting messages to system.out (defaults to false)**/
	public static boolean PERFORMANCE_REPORTING = false;

	public abstract Transfer<?,?> transfer();
	public abstract void transfer(Transfer<?, ?> t);

	/**Aggregates that result after transfer is performed (if any)*/
	public abstract Aggregates<?> transferAggregates();

	/**Aggregates before transfer.**/
	public abstract Aggregates<?> aggregates();

	/**Set Aggregates before transfer and the transform used to create them.**/
	public abstract void aggregates(Aggregates<?> aggregates, AffineTransform renderTransform, DescriptorPair<?,?> axes);
	
	public abstract Aggregates<?> refAggregates();
	public abstract void refAggregates(Aggregates<?> aggregates);
	
	public abstract Renderer renderer();

	public abstract void includeAxes(boolean include);
	
	/**Force a full re-render.**/
	public abstract void renderAgain();
	
	public static abstract class Aggregating extends ARComponent {
		private static final long serialVersionUID = 404081973530563354L;

		public abstract Glyphset<?,?> dataset();
		public abstract void dataset(Glyphset<?,?> data, Aggregator<?,?> agg, Transfer<?,?> t);
	
		public abstract Aggregator<?,?> aggregator();
		public abstract Transfer<?,?> transfer();
		
		@Override public abstract AffineTransform viewTransform();
		
		/**Get a copy of the current render transform.  Should return a copy, not a reference.**/
		public abstract AffineTransform renderTransform();
	}
	
	public interface Holder {
		public ARComponent getARComponent();
	}
}