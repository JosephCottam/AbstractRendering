package ar;

import java.awt.geom.AffineTransform;

/**A renderer implements a strategy for converting glyphs (geometry+data) into images.
 * Strategies can include parallelization, different iterations orders, synch/asynch return, 
 * different write orders, etc.
 *   
 * @author jcottam
 */
public interface Renderer {
	
	/**Produces the aggregates for a specific set of glyphs given the current view.
	 * 
	 * This method uses an inverse view transform because iteration is usually driven from the
	 * pixel-space but glyph selection occurs in canvas space. Therefore, the most common transformation
	 * is from pixels to canvas (not the more common canvas to pixels).
	 *  
	 * @param glyphs  The items to render
	 * @param op Means to convert the glyphset into an aggregate for a specific position
	 * @param inverseView The *inverse* view transform 
	 * @param width The width of the current viewport
	 * @param height The height of the current viewport
	 * @return Resulting aggregate set
	 */
	public <V,A> Aggregates<A> aggregate(final Glyphset<? extends V> glyphs, final Aggregator<V,A> op, 
			final AffineTransform inverseView, final int width, final int height);
	
	
	/**Produces an new set of aggregates for rendering, converting a set of aggregates into a set of colors.
	 * 
	 * Since aggregates are produced with-respect-to a particular viewport, converting to colors
	 * is essentially producing an image.  Therefore, this image returns an image ready-to-display.
	 * 
	 * The width and height must be passed to transfer separately from the aggregates 
	 * because (1) Aggregates support sub-regions and therefore might not be the same size as the space 
	 * to render in and (2) a set of aggregates might extend off the rendering window if over-sampling
	 * was done in aggregation (i.e., to support fast panning or maintain a view invariant property 
	 * by aggregating over the entire canvas instead of just the view window).
	 *   
	 * @param aggregates Set of aggregates to perform transfer on 
	 * @param t Transfer function to apply
	 * @return A resulting set of aggregates
	 */
	public <IN,OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Transfer<IN, OUT> t);
	
	
	/**For monitoring long-running render operations, this method provides a simple monitoring interface.
	 * Progress and progress reporting/recording are left up to the renderer to define.  This method may
	 * return -1 if progress is not being kept by the particular renderer.  Progress reports may be
	 * simple best-effort guesses and are not necessarily suitable for estimation or performance monitoring.
	 * 
	 * @return The percent of predicted work that has been completed.
	 */
	public double progress();
	
	
}
