package ar;

import java.awt.geom.AffineTransform;
import java.io.Serializable;

/**Selectors associate glyphs with bins.  
 * 
 * In the simple theoretical model, the selector takes a bin
 * and returns a list of glyphs that are associated with that bin.
 * However, in this execution model, the selector associates
 * a glyph to a set of bins.  This alternative arrangement
 * is driven by the data-parallel and aggregate-reduction based
 * execution model this implementation actually uses.
 */
public interface Selector<G> extends Serializable {
	/**Process all items in a glyphset towards a given aggregates target.
	 * 
	 * This method is used in glyph-parallel aggregation because it is can be efficiently
	 * used on a group of glyphs without allocating lots of aggregates.
	 * 
	 * @param glyphset Glyphs to process
	 * @param view View transform to apply
	 * @param existing Existing aggregates MAY be destructively updated 
	 * @param op Aggregator operation to use.
	 * @return Aggregates representing the existing aggregates updated by the glyphset
	 */
	public <I,A> Aggregates<A> processSubset(
			Iterable<? extends Glyph<? extends G, ? extends I>> glyphset, 
			AffineTransform view,
			Aggregates<A> existing, 
			Aggregator<I,A> op);
	
	/***Does the given shape touch the indicated bin?
	 * 
	 * This method is used in pixel-parallel aggregation.
	 *  
	 * @param glyph Glyph to consider
	 * @param view Under this view transform
	 * @param x X Location of the bin
	 * @param y Y Location of the bin
	 * @return Does the glyph impact the indicated bin?
	 */
	public boolean hitsBin(Glyph<? extends G, ?> glyph, AffineTransform view, int x, int y);
}
