package ar.glyphsets.implicitgeometry;

import java.io.Serializable;

/**Convert a value into a piece of geometry.
 * Geometry can be points, lines, rectangles, shapes, etc.
 * 
 * @param <G> Geometry-type returned;
 * @param <I> Input value type
 * **/
public interface Shaper<G,I> extends Serializable {
	/**Create a shape from the passed item.**/
	public G shape (I from);
	
	/**Tagging interface.  Indicates that the shaper implements a simple enough layout
	 * that the maximum/minimum values for each field will give a correct bounding box. 
	 */
	public static interface SafeApproximate<G,I> extends Shaper<G,I> {}
}