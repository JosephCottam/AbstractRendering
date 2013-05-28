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
	public Aggregates<A> reduce(final GlyphSet<G> glyphs, final Aggregator<G,A> op, 
			final AffineTransform inverseView, final int width, final int height);
	
	
	/**Produces an image for rendering, converting a set of aggregates into a set of colors.
	 * 
	 * Since aggregates are produced with-respect-to a particular viewport, converting to colors
	 * is essentially producing an image.  Therefore, this image returns an image ready-to-display.
	 *   
	 * @param aggregates
	 * @param t
	 * @param width
	 * @param height
	 * @param background
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
