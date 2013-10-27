package ar;

/**Selectors associate glyphs with bins.  
 * 
 * In the simple theoretical model, the selector takes a bin
 * and returns a list of glyphs that are associated with that bin.
 * However, in this execution model, the selector associates
 * a glyph to a set of bins.  This alternative arrangement
 * is driven by the data-parallel and aggregate-reduction based
 * execution model this implementation actually uses.
 */
public interface Selector {

}
