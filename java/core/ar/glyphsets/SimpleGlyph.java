package ar.glyphsets;

import java.awt.Shape;
import java.io.Serializable;

import ar.Glyph;

/**Basic glyph that stores literal values passed during its construction.
 * @param <I> Type of the value stored
 */
public class SimpleGlyph<I> implements Glyph<I>, Serializable {
	private static final long serialVersionUID = -7121921034999419091L;
	private final Shape shape;
	private final I value;
	
	/**Create a glyph with the given shape (value is null).**/
	public SimpleGlyph(Shape shape) {this(shape, null);}
	
	/**Create a glyph with the given shape/value.**/
	public SimpleGlyph(Shape shape, I value) {
		this.shape=shape; 
		this.value = value;
	}

	public Shape shape() {return shape;}
	public I info() {return value;}
}
