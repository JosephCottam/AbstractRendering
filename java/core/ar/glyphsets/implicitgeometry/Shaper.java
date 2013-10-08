package ar.glyphsets.implicitgeometry;

import java.awt.Shape;
import java.io.Serializable;

/**Convert a value into a shpae. 
 * @param <I> Input value type
 * **/
public interface Shaper<I> extends Serializable {
	/**Create a shape from the passed item.**/
	public Shape shape (I from);
	
	/**Tagging interface.  Indicates that the shaper implements a simple enough layout
	 * that the maximum/minimum values for each field will give a correct bounding box. 
	 */
	public static interface SafeApproximate<I> extends Shaper<I> {}
}