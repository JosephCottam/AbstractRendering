package ar.glyphsets;

import java.awt.Shape;

import ar.Glyphset.Glyph;

/**Basic glyph that stores literal values passed during its construction.
 * @param <V> Type of the value stored
 */
public class SimpleGlyph<V> implements Glyph<V>{
	public final Shape shape;
	public final V value;
	
	/**Create a glyph with the given shape (value is null).**/
	public SimpleGlyph(Shape shape) {this(shape, null);}
	
	/**Create a glyph with the given shape/value.**/
	public SimpleGlyph(Shape shape, V value) {
		this.shape=shape; 
		this.value = value;
	}

	public Shape shape() {return shape;}
	public V value() {return value;}
}
