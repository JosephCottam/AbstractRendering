package ar;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**A renderer implements a strategy for converting glyphs (geometry+data) into images.
 * Strategies can include parallelization, different iterations orders, synch/asynch return, 
 * different write orders, etc.
 *   
 * @author jcottam
 */
public interface Renderer<G,A> {
	
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
	public Aggregates<A> reduce(final Glyphset<G> glyphs, final Aggregator<G,A> op, 
			final AffineTransform inverseView, final int width, final int height);
	
	
	/**Produces an image for rendering, converting a set of aggregates into a set of colors.
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
	 * @param width Width of the image to render
	 * @param height Height of the image to render
	 * @param background Background color to put in the resulting image
	 * @return The resulting image 
	 */
	public BufferedImage transfer(Aggregates<A> aggregates, Transfer<A> t, int width, int height, Color background);
	
	
	/**For monitoring long-running render operations, this method provides a simple monitoring interface.
	 * Progress and progress reporting/recording are left up to the renderer to define.  This method may
	 * return -1 if progress is not being kept by the particular renderer.  Progress reports may be
	 * simple best-effort guesses and are not necessarily suitable for estimation or performance monitoring.
	 * 
	 * @return The percent of predicted work that has been completed.
	 */
	public double progress();
}
