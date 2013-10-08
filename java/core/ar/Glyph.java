package ar;

import java.awt.Shape;

/**A glyph is the input data type for the general abstract rendering system
 * (the ImplicitGeometry subsystem is a way to produce glyphs, essentially
 * a simple way to mediate the transition into the Abstract Rendering system).
 * 
 * A glyph is a piece of geometry (such as a circle, square, line or point), 
 * combined with a value (traditionally a category or level, sometimes a color).
 * **/
public interface Glyph<V> {
	/**What is the geometry associated with this glyph?*/
	public Shape shape();

	/**What is the value associated with this glyph?*/
	public V info();
}