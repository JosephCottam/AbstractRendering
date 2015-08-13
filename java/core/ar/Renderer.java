package ar;

import java.awt.geom.AffineTransform;
import java.io.Serializable;
import java.util.function.BiFunction;
import java.util.function.Function;

import ar.renderers.ProgressRecorder;
import ar.Selector;

/**A renderer implements a strategy for converting glyphs (geometry+data) into images.
 * Strategies can include parallelization, different iterations orders, synch/asynch return, 
 * different write orders, etc.
 *   
 * @author jcottam
 */
public interface Renderer extends Serializable {
	
	public <I,G,A> Aggregates<A> aggregate(
			final Glyphset<? extends G, ? extends I> glyphs, 
			final Selector<G> selector,
			final Aggregator<I,A> aggregator, 
			final AffineTransform viewTransform);

	/**Produces the aggregates for a specific set of glyphs given the current view.
	 * 
	 * The aggregate allocator function may be invoked multiple times during parallel 
	 * aggregating and should return a new instance each time (as parallel aggregation
	 * assumes independent aggregate instances as targets).
	 * 
	 * 
	 * @param glyphs  The items to render
	 * @param selector Associates glyphs with positions
	 * @param aggregator Converts a glyph into an aggregate for a specific position
	 * @param viewTransform The view transform (e.g., geometry to screen) 
	 * @param width The width of the current viewport
	 * @param height The height of the current viewport
	 * @param allocator Function to allocate aggregates
	 * @return Resulting aggregate set
	 */
	public <I,G,A> Aggregates<A> aggregate(
			final Glyphset<? extends G, ? extends I> glyphs, 
			final Selector<G> selector,
			final Aggregator<I,A> aggregator, 
			final AffineTransform viewTransform,
			Function<A, Aggregates<A>> allocator,
			BiFunction<Aggregates<A>, Aggregates<A>, Aggregates<A>> merge);
	
	/**Produces an new set of aggregates from an existing one.
	 * 
	 * Since aggregates are produced with-respect-to a particular viewport, converting to colors
	 * is essentially producing an image.  Therefore, this function may be used to prepare an image
	 * that is ready-to-display.
	 * 
	 * TODO: Why is this Transfer.Specialized?  Could it be just Transfer?  It has to do with the .Specialized/.ItemWise split.
	 * 
	 * @param aggregates Set of aggregates to perform transfer on 
	 * @param t Transfer function to apply
	 * @return A resulting set of aggregates
	 */
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, 
											 Transfer.Specialized<IN, OUT> t);
	

	/**Produces a set of aggregates based on an item-wise specialization.**/
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, 
											 Transfer.ItemWise<IN, OUT> t);

	
	/**For monitoring long-running render operations, this method provides a simple monitoring interface.
	 * Progress and progress reporting/recording are left up to the renderer to define.  This method may
	 * return -1 if progress is not being kept by the particular renderer.  Progress reports may be
	 * simple best-effort guesses and are not necessarily suitable for estimation or performance monitoring.
	 * 
	 * @return The percent of predicted work that has been completed.
	 */
	public ProgressRecorder recorder();
	
	public static Renderer defaultInstance() {return new ar.renderers.ThreadpoolRenderer();}
}
