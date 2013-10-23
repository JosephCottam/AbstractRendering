package ar.glyphsets;

import java.io.Serializable;

import ar.Glyph;

/**Basic glyph that stores literal values passed during its construction.
 * @param <I> Type of the value stored
 */
public class SimpleGlyph<G,I> implements Glyph<G,I>, Serializable {
	private static final long serialVersionUID = -7121921034999419091L;
	private final G shape;
	private final I value;
	
	/**Create a glyph with the given shape (value is null).**/
	public SimpleGlyph(G shape) {this(shape, null);}
	
	/**Create a glyph with the given shape/value.**/
	public SimpleGlyph(G shape, I value) {
		this.shape=shape; 
		this.value = value;
	}

	public G shape() {return shape;}
	public I info() {return value;}
}
