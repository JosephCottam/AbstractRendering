package ar;

import java.awt.geom.Rectangle2D;
import java.util.Collection;

/**
 * A collection of glyphs for rendering. A glyph is a geometric description and
 * accompanying data values.
 * 
 * Segmentation
 * ------------
 * 
 * To support parallelization, the glyphset interface also supports
 * "segmentation".  This enables sub-setting without the implication of
 * precise divisions.  The segmentation system is made up of the "limit" 
 * and "segment" methods.
 * 
 * Some parallel processing strategies work best on a subset of the data. The
 * simplest way to subset data is to find out how much data there is and exactly
 * divide it among the parts. For example, if there are 100 data points and 10
 * tests, give task 0 gets data values 0-9, task 1 values 10-19, etc. This type
 * of division is easy to do with random access data (as are other convenient
 * strategies), but not always feasible in other container types.
 * 
 * Concretely, the quad-tree containers don't impose a full ordering on the data
 * set. So providing and index into the data isn't convenient. However, it is
 * discrete at the level of leaf-quads. Similarly, the Avro serialization format
 * provides segments, but not random-access to those segments. The data can
 * still be divided, but providing the notion of "a precise division" that
 * random access structures provide is unattractive. The segment system provides
 * a vocabulary for specifying data subsets without implying precision.
 * 
 * 
 * Random Access
 * -------------
 * A particularly common way to support segmentation is to support random access.
 * When random access is present, a number of additional classes for segmentation
 * can be used.
 */
public interface Glyphset<T> extends Iterable<Glyph<T>> {
	
	/**Return all glyphs that intersect the passed rectangle.**/
	public Collection<Glyph<T>> intersects(Rectangle2D r);
	
	/**Is this glyphset empty?*/
	public boolean isEmpty();
	
	/**What are the overall bounds of the items in this glyphset?**/
	public Rectangle2D bounds();

	/**How many items in this glyphset?
	 * */
	public long size();
	
	/**Add a new item to this glyphset.
	 * TODO: Is this required? It is only used in utility spaces, most of which already "know" the glyphset type.
	 * @throws UnsupportedOperationException -- Not all glyphsets support adding items.
	 ***/
	public void add(Glyph<T> g);
	
	/**Glyphsets that support random access.
	 * This interface is largely to support parallel execution.
	 */
	public static interface RandomAccess<T> extends Glyphset<T> {
		/**Return the item at the specified index.**/
		public Glyph<T> get(long l);
	}
		
	
	/**One greater than the highest value for "top" in "segment".
	 * 
	 * Analogous to "length" or "size" but without the semantic interpretation on what underlies
	 * this glyphset.  This only controls how many segments can be reliably made. 
	 **/
	public long segments();

	/**
	 * Get a subset of the data.
	 * 
	 * The precise meaning of "bottom" and "top" is left up to the
	 * implementation. To simplify requests, bottom is considered inclusive
	 * and top as non-inclusive. This enables array-subset conventions to
	 * be followed. (Old top becomes new bottom.) Similarly, the highest
	 * valid value from top is returned by the "segments" method.
	 * 
	 * Bottom must be lower than top. A value above "segments" should be an
	 * exception.
	 * 
	 * Equally spaced bottom/top pairs **do not** need to return subsets of
	 * the same size.  Similarly, if s1=segment(a,b) and s2=segment(b,c)
	 * then s1 and s2 do not need to contain contiguous chunks.  
	 * 
	 * @param bottom  Lower marker. This is an inclusive bound.
	 * @param top Upper marker. This is an exclusive bound.
	 * @return A subset of the data.
	 */
	public Glyphset<T> segment(long bottom, long top) throws IllegalArgumentException;
}
