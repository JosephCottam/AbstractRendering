/**Renderer implementations (and supporting utilities).
 * 
 * Renderers work most efficiently when paired with an appropriate glyphset.
 * The principal factor is the spatial arrangement of the glyphset vs
 * the iteration pattern of the renderer.  Renderers follow two basic pattern: pixel and glyph based iteration.
 * All renderers use pixel-based transfer calculation (since the glyphs are no longer considered),
 * so this division has to do with aggregate creation strategies.
 * 
 * Pixel-based renderers look at each pixel and find glyphs that associate with that pixel.
 * Once a pixel's aggregate value has been determined, it is not returned to.
 * In contrast, glyph-based renderers look at each glyph in the glyphset and update all aggregate cells
 * that the glyph would influence.
 * 
 * In general, spatial glyphset work better with pixel-based renderers while
 * sequential glyphsets work better with glyph-based renderers.  (Aside: there are extensions
 * to quad-trees that would make this less cut-and-dry).    Glyph-based renderers
 * are more complicated that pixel-based renderers in that they require a 
 * third function type ("Aggregate Reducer"), however this additional function
 * enables overlapping updates without complex locking strategies.  In contrast,
 * pixel-based renderers are simpler but require that the glyphs be divided spatially
 * to be efficient (which incurs additional load-time costs).
 * 
 * **/
package ar.renderers;