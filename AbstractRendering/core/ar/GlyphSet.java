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
	public static interface RandomAccess<T> extends Glyphset<T> ,Iterable<Glyph<T>> {public Glyph<T> get(long l);}
	
	/**Simple wrapper class glyphs.**/
	public static interface Glyph<V> {
		public Shape shape();
		public V value();
	}
}
