/**Abstract Rendering core interfaces.
 * 
 * Abstract Rendering is a framework for considering
 * pixel-level effects when rendering traditional abstract canvas geometries.
 * It can be though of as an application of binning at the pixel level.
 * 
 * As an input, Abstract Rendering takes a set of geometry and associated values.
 * These are converted into pixels via a multi-staged transformation.
 * Abstract rendering has four core function types:
 * + Select -- For each pixel, what pieces of geometry should be considered
 * + Info -- For each piece of geometry touching a pixel, what pieces of information matter
 * + Aggregate -- How to combine multiple pieces of information
 * + Transfer -- Transform combined values (Eventually into pixels)
 * 
 * The four functions are combined as follows
 * pixel = Transfer( Aggregate( {Info(glyph) | glyph in Select(Glyphs,x,y)}))
 * 
 * A "glyph" is a pieces of geometry associated with some data.
 * "Glyphs" is a collection of many glyph entries.
 * The x/y value is a location of a given pixel.
 * "Info" extracts a specific piece of data associated with the glyph.
 * Aggregate then takes a collection of such values and merges them together.
 * Finally, Transfer converts the aggregate values into other values.
 * 
 * In this implementation, the Glyph is explicitly represented and the info function
 * is a method on that class.  The only selection function currently supported is
 * "contains" which returns all of the geometric objects that contain some part of the given pixel.
 * Aggregate and transfer are fully generalized and represented in this framework. 
 */
package ar;