package ar.glyphsets.implicitgeometry;

import java.awt.Shape;
import java.io.Serializable;

/**Convert a value into another value (often a color, but not always).
 * <I> Input value type
 * **/
public interface Shaper<I> extends Serializable {
	/**Create a shape from the passed item.**/
	public Shape shape (I from);
}