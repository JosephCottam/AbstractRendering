/**Glyphset implementations (and supporting utilities).
 * 
 * Glyphsets are categorized as either Spatial or Sequential arrangements and as either Explicit or Implicit  geometries.
 * 
 * Spatial arrangements glyphsets are the quad-trees and the matrix glyphset.  
 * These divide the logical canvas space into regions and store glyph elements 
 * in their corresponding regions. Sequential arrangements keep lists of the 
 * glyphs without regard to the location of those glyphs.  Generally speaking,
 * spatial arrangements require more memory but enable efficient pixel-parallel 
 * operation while sequential arrangements are smaller.   Sequential arrangements
 * can be parallelized by glyph instead of by pixel (glyph vs. pixel rendering 
 * has its own tradeoffs).
 * 
 * Explicit geometries store the actual shape objects for rapid access.  This
 * requires more RAM but is faster to access.  Implicit geometries are created out
 * of a dataset and several functions.  When a geometry is required, the functions
 * are invoked to create geometry out of the data.  This is slower than an explicit
 * geometry but more space efficient.  In one data set of 7 million items, the
 * explicit geometry took 9GB of space while the implicit geometry took only 1GB,
 * however access times were twice as long for the implicit geometry.
 * 
 * 
 * Other glyphset support tools are also found in this package (such as glyph implementations
 *  and the interfaces/support-classes for the implicit geometry system).
 * 
 * TODO: Investigate a glyphset that wraps a shape and determines its low/high X/Y values 
 *       based on the shape bounds, also the value is the glyph value 
 * 
 * **/
package ar.glyphsets;