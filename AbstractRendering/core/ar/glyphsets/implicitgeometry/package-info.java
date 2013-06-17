/** Provide just-in-time conversion from a value to a glyph 
 * (or glyph components).  
 * 
 * To provide better control, flexibility, information in implicit
 * glyphsets and the shape and value are presented as separate interfaces.
 *  
 *  Be advised that classes implementing shaper/valuer should be deterministic 
 *  in their parameters because iteration order and access count are not guaranteed 
 *  by the renderers.
 */
package ar.glyphsets.implicitgeometry;