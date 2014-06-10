package ar.glyphsets.implicitgeometry;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

import ar.rules.CategoricalCounts;
import ar.util.Util;

/**Converts values from one type to another.
 * The common scenario is to select a single field from a
 *  complex, heterogeneous object (such as selecting the "name" field from a user-class).
 * However, this class can also be used to do general-purpose
 *  conversions during data load.
 *  
 *  This class can be used as a generalized "info" function in conjunction with the Wrapped Collection.
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
		public Integer value(V from) {
			if (from instanceof Number) {return ((Number) from).intValue();}
			else {return Integer.valueOf(from.toString());}
		}
	}

	/**Convert a value to an integer via Integer.parseInt.**/
	public final class ToDouble<V> implements Valuer<V,Double> {
		private static final long serialVersionUID = 2540867051146887184L;
		public Double value(V from) {
			if (from instanceof Number) {return ((Number) from).doubleValue();}
			return Double.valueOf(from.toString());
		}
	}

	
	
	/**Give everything the same value
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
	public static class CategoryCount<T> implements Valuer<Indexed,CategoricalCounts<T>> {
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
		
		public CategoricalCounts<T> value(Indexed from) {
			@SuppressWarnings("unchecked")
			T key = (T) from.get(catIdx);
			int val = ((Integer) from.get(valIdx)).intValue();
			return new CategoricalCounts<>(comp, key, val); 
		}
	}
	
	/**Convert a value to a true/false based on equality to a reference value.**/
	public static final class Equals<IN> implements Valuer<IN, Boolean> {
		private final IN ref;
		public Equals(IN ref) {this.ref = ref;}
		public Boolean value(IN from) {return Util.isEqual(ref, from);}		
	}


	
	/**Given a map entry, return the value.  Used for maps where the key determines the shape
	 * and the value determines the info.
	 * @author jcottam
	 * @param <V>
	 */
	public static final class MapValue<K,V> implements Valuer<Map.Entry<K,V>, V> {
		@Override public V value(Entry<K,V> from) {return from.getValue();}		
	}

	/**Given a map entry, return the key.  Used for maps where the key determines the info
	 * and the value determines the shape.
	 * @author jcottam
	 * @param <V>
	 */
	public static final class MapKey<K,V> implements Valuer<Map.Entry<K,V>, K> {
		@Override public K value(Entry<K,V> from) {return from.getKey();}		
	}
}