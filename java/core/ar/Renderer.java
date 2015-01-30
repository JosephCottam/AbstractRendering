package ar;

import java.awt.geom.AffineTransform;
import java.io.Serializable;

import ar.renderers.ProgressRecorder;
import ar.Selector;

/**A renderer implements a strategy for converting glyphs (geometry+data) into images.
 * Strategies can include parallelization, different iterations orders, synch/asynch return, 
 * different write orders, etc.
 *   
 * @author jcottam
 */
public interface Renderer extends Serializable {
	
	/**Produces the aggregates for a specific set of glyphs given the current view.
	 * 
	 * @param glyphs  The items to render
	 * @param selector Associates glyphs with positions
	 * @param aggregator Converts a glyph into an aggregate for a specific position
	 * @param viewTransform The view transform (e.g., geometry to screen) 
	 * @param width The width of the current viewport
	 * @param height The height of the current viewport
	 * @return Resulting aggregate set
	 */
	public <I,G,A> Aggregates<A> aggregate(
			final Glyphset<? extends G, ? extends I> glyphs, 
			final Selector<G> selector,
			final Aggregator<I,A> aggregator, 
			final AffineTransform viewTransform, final int width, final int height);
	
	
	/**Produces an new set of aggregates from an existing one.
	 * 
	 * Since aggregates are produced with-respect-to a particular viewport, converting to colors
	 * is essentially producing an image.  Therefore, this function may be used to prepare an image
	 * that is ready-to-display.
	 * 
	 * @param aggregates Set of aggregates to perform transfer on 
	 * @param t Transfer function to apply
	 * @return A resulting set of aggregates
	 */
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer.Specialized<IN, OUT> t);
	

	/**Produces a set of aggregates based on an item-wise specialization.**/
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer.ItemWise<IN, OUT> t);

	
	/**For monitoring long-running render operations, this method provides a simple monitoring interface.
	 * Progress and progress reporting/recording are left up to the renderer to define.  This method may
	 * return -1 if progress is not being kept by the particular renderer.  Progress reports may be
	 * simple best-effort guesses and are not necessarily suitable for estimation or performance monitoring.
	 * 
	 * @return The percent of predicted work that has been completed.
	 */
	public ProgressRecorder recorder();
}
