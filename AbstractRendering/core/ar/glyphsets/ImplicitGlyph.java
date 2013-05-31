package ar.glyphsets;

import java.awt.Shape;

/** Provide just-in-time conversion from a value to a glyph 
 * (or glyph components).  
 * 
 * To provide better control in implicit
 * glyphsets and the shape and value are handled separately,
 * though if both are implemented then a simple glyph creator is
 * return new SimpleGlyph<V>(transformer.shape(value), transformer.value(value)).
 * 
 * Neither operation is required as some Glyphset implementations
 * handle the shape and/or value in other ways.
 * (i.e., either method could throw "UnsupportedOperationException").
 * If only "value" is implemented and the value-type is Color, 
 * then the ImplicitGlyph used to be referred to as a "Painter".
 *  
 *  Be advised that these classes should be deterministic in their parameters
 *  because iteration order and quantity are not guaranteed by the renderers.
 * 
 * @param <I> Input type, convert value from this to a glyph
 * @param <V> Value of the "value" field from the glyph
 */
public interface ImplicitGlyph<I,V> {
	/**Shape of the glyph.**/
	public Shape shape (I from);
	/**Value of the glyph.**/
	public V value(I from);
	
	
	
	
}
