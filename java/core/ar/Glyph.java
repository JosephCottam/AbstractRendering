package ar;

/**A glyph is the input data type for the general abstract rendering system
 * (the ImplicitGeometry subsystem is a way to produce glyphs, essentially
 * a simple way to mediate the transition into the Abstract Rendering system).
 * 
 * A glyph is a piece of geometry (such as a circle, square, line or point), 
 * combined with a value (traditionally a category or level, sometimes a color).
 * 
 * @param <G> The type of the geometry carried by this glyph.  Generally is Point, Line or Shape (or their 2D counterparts)
 * @param <I> The type of the value carried by this glyph.
 */
public interface Glyph<G,I> {
	/**What is the geometry associated with this glyph?*/
	public G shape();

	/**What is the value associated with this glyph?*/
	public I info();
}