package ar;

import java.awt.geom.AffineTransform;

public interface Aggregator<A> {
	public A at(int x, int y, GlyphSet glyphs, AffineTransform inverseView);
	
	/**What value is an identity for this operation?
	 * Value V is an identity is op(V, x) = x for all V.
	 **/
	public A identity();
}
