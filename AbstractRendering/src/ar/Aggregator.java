package ar;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

public interface Aggregator<A> {
	public A at(Rectangle pixel, GlyphSet glyphs, AffineTransform inverseView);
	
	/**What value is an identity for this operation?
	 * Value V is an identity is op(V, x) = x for all V.
	 **/
	public A identity();
}
