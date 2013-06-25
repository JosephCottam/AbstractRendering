package ar;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import ar.util.Inspectable;

/**Aggregators convert glyphs into aggregate items for a specific view.
 * 
 * FROM -- The type of the data element on the glyph
 * TO -- The type of aggregates produced
 * **/
public interface Aggregator<FROM, TO> extends Inspectable<FROM, TO> {
	
	/**
	 * Compute the aggregate for the given pixel in the give glyphset and view.
	 * 
	 * The inverse view transform is included in this signature to support 
	 * aggregators that consider neighboring pixels.  Including the view transform
	 * enables a more direct expression of accessing additional pixels.
	 * 
	 * @param pixel Rectangle of the current current pixel in screen-space
	 * @param glyphs Set of glyphs in canvas space
	 * @param inverseView Transformation from screen space to canvas space
	 * @return The aggregate value
	 */
	public TO at(Rectangle pixel, Glyphset<FROM> glyphs, AffineTransform inverseView);
	
	
	/**What value is an mathematical identity value for this operation?
	 * Value V is an identity is op(V, x) = x for all V.
	 * 
	 * This method is used to initialize the aggregate set in many circumstances.
	 * Because of aggregate reducers, this initial value needs to 
	 * be an identity (thus the name).  However, not all renderers rely on this
	 * property (for example, pixel-serial rendering just uses it for the background).
	 **/
	public TO identity();
}
