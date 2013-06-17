package ar.glyphsets.implicitgeometry;

import java.awt.Shape;

/**Convert a value into another value (often a color, but not always).
 * <I> Input value type
 * **/
public interface Shaper<I> {public Shape shape (I from);}