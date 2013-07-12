package ar;

import java.awt.geom.Rectangle2D;
import java.awt.Shape;
import java.util.Collection;


/**A collection of glyphs for rendering.
 * A glyph is a geometric description and accompanying data values.
 */
public interface Glyphset<T> {
	
	/**Return all glyphs that intersect the passed rectangle.**/
	public Collection<? extends Glyph<T>> intersects(Rectangle2D r);
	
	/**Is this glyphset empty?*/
	public boolean isEmpty();
	
	/**How many items in this glyphset?*/
	public long size();
	
	/**What are the overall bounds of the items in this glyphset?**/
	public Rectangle2D bounds();
	
	/**Add a new item to this glyphset**/
	public void add(Glyph<T> g);
	
	/**Glyphsets that support random access.
	 * This interface is largely to support parallel execution.
	 */
	public static interface RandomAccess<T> extends Glyphset.Segementable<T> {public Glyph<T> get(long l);}
	
	/**
	 * Means of subdividing glyphsets without implications for precision.
	 * Correct implementation and use of these methods will non-overlapping
	 * subsets.
	 * 
	 * Some parallel processing strategies work best on a subset of the data.
	 * The simplest way to subset data is to find out how much data there is and
	 * exactly divide it among the parts. For example, if there are 100 data
	 * points and 10 tests, give task 0 gets data values 0-9, task 1 values
	 * 10-19, etc. This type of division is easy to do with random access data
	 * (as are other convenient strategies), but not always feasible in other
	 * container types.
	 * 
	 * Concretely, the quad-tree containers don't impose a full ordering on the
	 * data set. So providing and index into the data isn't convenient. However,
	 * it is discrete at the level of leaf-quads. Similarly, the Avro
	 * serialization format provides segments, but not random-access to those
	 * segments. The data can still be divided, but providing the notion of
	 * "a precise division" that random access structures provide is
	 * unattractive. The segment system provides a vocabulary for specifying
	 * data subsets without implying precision.
	 */
	public static interface Segementable<T> extends Glyphset<T>, Iterable<Glyph<T>> {
		/**One greater than the highest value for top.
		 * Analogous to "length" or "size" but without the semantic interpretation. 
		 **/
		public int limit();

		/**
		 * Get a subset of the data.
		 * 
		 * The precise meaning of "bottom" and "top" is left up to the
		 * implementation. To simplify requests, bottom is considered inclusive
		 * and top as non-inclusive. This enables array-slicing conventions to
		 * be followed. (Old top becomes new bottom.) Similarly, the highest
		 * valid value from top is returned by the limit method.
		 * 
		 * Bottom must be lower than top. A value above limit should be an
		 * exception.
		 * 
		 * Equally spaced bottom/top pairs **do not** need to return subsets of
		 * the same size.
		 * 
		 * @param bottom  Lower marker. This is an inclusive bound.
		 * @param top Upper marker. This is an exclusive bound.
		 * @return A subset of the data.
		 */
		public Glyphset.Segementable<T> segement(int bottom, int top) throws IllegalArgumentException;
	}
	
	/**Return type type of values held by the glyphs. 
	 * This is used for configuration validation.
	 * **/
	public Class<T> valueType();
	
	/**Simple wrapper class glyphs.**/
	public static interface Glyph<V> {
		public Shape shape();
		public V value();
	}
}
