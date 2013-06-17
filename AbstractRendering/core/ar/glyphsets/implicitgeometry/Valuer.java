package ar.glyphsets.implicitgeometry;

import java.awt.Color;

/**Converts values from one type to another.
 * The common scenario is to select a single field from a
 *  complex, heterogenous object (such as selecting the "name" field from a user-class).
 * However, this class can also be used to do general-purpose
 *  conversions during data load.
 *  
 * <I> Input value type
 * <V> Output value type
 * **/
public interface Valuer<I,V> {
	public V value(I from);
	
	/**Pass-through valuer.  Value-in=value-out.*/
	public static class IdentityValuer<I> implements Valuer<I,I> {public I value(I v) {return v;}}

	/**Convert a value to an integer via Integer.parseInt.**/
	public final class ToInt<V> implements Valuer<V,Integer> {
		public Integer value(V from) {return Integer.parseInt(from.toString());}
	}
	
	
	/**Give everything the same value (default value is the color red).*/
	public final class Constant<T> implements Valuer<T,Color> {
		private final Color c;
		public Constant() {this.c = Color.red;}
		public Constant(Color c) {this.c = c;}
		public Color value(T item) {return c;}
	}
	
	/**Binary valuation scheme.  
	 * If an item equals the stored value, return value 'a'.
	 * Otherwise return value 'b'.
	 */
	public final class Binary<T,V> implements Valuer<T,V> {
		private final V a;
		private final V b;
		private final T v;
		public Binary(T v, V a, V b) {this.v = v; this.a = a; this.b=b;}
		public V value(T item) {
			if (item == v || (v != null && v.equals(item))) {return a;}
			return b;
		}
	}
}