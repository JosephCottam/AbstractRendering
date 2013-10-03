package ar.glyphsets.implicitgeometry;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;

import ar.rules.CategoricalCounts;

/**Converts values from one type to another.
 * The common scenario is to select a single field from a
 *  complex, heterogeneous object (such as selecting the "name" field from a user-class).
 * However, this class can also be used to do general-purpose
 *  conversions during data load.
 *  
 * <I> Input value type
 * <V> Output value type
 * **/
public interface Valuer<I,V> extends Serializable {
	/**Create a value from the passed item.**/
	public V value(I from);
	
	/**Pass-through valuer.  Value-in=value-out.*/
	public static class IdentityValuer<I> implements Valuer<I,I> {
		private static final long serialVersionUID = 6961888682185387204L;

		public I value(I v) {return v;}
	}

	/**Convert a value to an integer via Integer.parseInt.**/
	public final class ToInt<V> implements Valuer<V,Integer> {
		private static final long serialVersionUID = 2540867051146887184L;

		public Integer value(V from) {return Integer.valueOf(from.toString());}
	}
	
	
	/**Give everything the same value (default value is the color red).
	 * @param <I> Input type
	 * @param <V> Value return type
	 */
	public final class Constant<I,V> implements Valuer<I,V> {
		private static final long serialVersionUID = -8933986990047616101L;
		private final V c;
		
		@SuppressWarnings("javadoc")
		public Constant(V c) {this.c = c;}
		public V value(I item) {return c;}
	}
	
	/**Binary valuation scheme.  
	 * If an item equals the stored value, return value 'a'.
	 * Otherwise return value 'b'.
	 */
	public final class Binary<T,V> implements Valuer<T,V> {
		private static final long serialVersionUID = -3348263722682722360L;
		private final V a;
		private final V b;
		private final T v;
		
		@SuppressWarnings("javadoc")
		public Binary(T v, V a, V b) {this.v = v; this.a = a; this.b=b;}
		public V value(T item) {
			if (item == v || (v != null && v.equals(item))) {return a;}
			return b;
		}
	}

	/**Load the data as a key/value pair.  The key is the category, the value is an integer count.**/
	public static class CategoryCount<T> implements Valuer<Indexed,CategoricalCounts.CoC<T>> {
		private static final long serialVersionUID = 1L;
		private final int catIdx, valIdx;
		private final Comparator<T> comp;
		
		
		/** @param catIdx Index to get the category label from.
		 *  @param valIdx Index to get the count value from.
		 */
		public CategoryCount(Comparator<T> comp, int catIdx, int valIdx) {
			this.catIdx = catIdx;
			this.valIdx = valIdx;
			this.comp = comp;
		}
		
		public CategoricalCounts.CoC<T> value(Indexed from) {
			@SuppressWarnings("unchecked")
			T key = (T) from.get(catIdx);
			int val = ((Integer) from.get(valIdx)).intValue();
			return new CategoricalCounts.CoC<>(comp, key, val); 
		}
	}
	
}