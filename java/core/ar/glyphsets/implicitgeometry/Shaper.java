package ar.glyphsets.implicitgeometry;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

/**Convert a value into a piece of geometry.
 * Geometry can be points, lines, rectangles, shapes, etc.
 * 
 * @param <IN> Input value type
 * @param <G> Geometry-type returned;
 * **/
public interface Shaper<IN,G> extends Serializable, Function<IN,G> {
	/**Tagging interface.  Indicates that the shaper implements a simple enough layout
	 * that the maximum/minimum values for each field will give a correct bounding box. 
	 */
	public static interface SafeApproximate<IN,G> extends Shaper<IN,G> {}
	
	/**Given a map entry, return the value.  Used for maps where the key determines the shape
	 * and the value determines the info.
	 * @author jcottam
	 */
	public static final class MapValue<K,G> implements Shaper<Map.Entry<K,G>, G> {
		@Override public G apply(Entry<K, G> from) {return from.getValue();}
	}

	/**Given a map entry, return the key.  Used for maps where the key determines the info
	 * and the value determines the shape.
	 * @author jcottam
	 * @param <V>
	 */
	public static final class MapKey<G,V> implements Shaper<Map.Entry<G, V>, G> {
		@Override public G apply(Entry<G, V> from) {return from.getKey();}
	}
}