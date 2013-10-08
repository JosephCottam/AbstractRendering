package ar.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import ar.util.Util;

/**Tools for working with associations between categories and counts.
 * @param <T> The type of the categories
 */
public interface CategoricalCounts<T> extends Comparable<CategoricalCounts<T>> {
	
	/**Add a new value at the given key.**/
	public CategoricalCounts<T> extend(T key, int qty);
	
	/**What are the counts in the i-th category?**/
	public int count(int i);
	
	/**What is the the i-th category?**/
	public T key(int i);
	
	/**What is the count for the given category?**/
	public int count(T key);
	
	/**What is the total of all counts?**/
	public int fullSize();
	
	/**How many category entries are there?
	 * 
	 * In classes that tolerate multiple associations with the same key,
	 * each association should be counted. 
	 */
	public int size();
	
	/**Return a new categorical counts object of the same type, but empty.**/
	public CategoricalCounts<T> empty();


	/**Count categories.  Categories are stored in sorted order (so "nth" makes sense).**/
	public static final class CoC<T> implements CategoricalCounts<T> {
		private final Comparator<T> comp;
		private final int[] counts;
		private final Object[] labels;
		private final int fullSize;
		
		/**Create a new CoC with "natural" ordering.**/
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public CoC() {this(new Util.ComparableComparator(), new Object[0], new int[0], 0);}
		
		/**@param comp Comparator used to order categories.**/
		public CoC(Comparator<T> comp) {this(comp, new Object[0], new int[0], 0);}
		
		/**Create a Categorical count with a single categorical/count pair.**/
		public CoC(Comparator<T> comp, T label, int count) {
			this(comp, new Object[]{label}, new int[]{count}, count);
		}
		
		/**@param counts Map backing this set of counts
		 * @param fullSize Total of the items in the counts (the relationship is not checked, but must hold for derivatives to work correctly)
		 ***/
		private CoC(Comparator<T> comp, Object[] labels, int[] counts, int fullSize) {
			//System.out.printf("count with %d cats and %d total\n", counts.size(), fullSize);
			this.counts = counts;
			this.labels = labels;
			this.fullSize = fullSize;
			this.comp = comp;
		}
		
		@SuppressWarnings("unchecked")
		public CoC<T> extend(T key, int count) {
			int idx = Arrays.binarySearch((T[]) labels, key, comp);
			if (idx >=0) {
				int[] newCounts = Arrays.copyOf(counts, counts.length);
				newCounts[idx] += count;
				return new CoC<>(comp, labels, newCounts, fullSize+count);
			} else {
				idx = -(idx +1); 
				T[] newLabels = Util.insertInto((T[]) labels, key, idx);
				int[] newCounts = Util.insertInto(counts, count, idx);
				return new CoC<>(comp, newLabels, newCounts, fullSize+count);
			}
		}
		
		@SuppressWarnings("unchecked")
		public int count(T key) {
			int idx = Arrays.binarySearch((T[]) labels, key, comp);
			if (idx >= 0) {return counts[idx];}
			return 0;
		}
		
		public int size() {return labels.length;}
		public int fullSize() {return fullSize;}
		public String toString() {return "COC: " + counts.toString();}
		
		@SuppressWarnings("unchecked")
		public T key(int i) {return (T) labels[i];}
		
		public boolean equals(Object other) {
			if (!(other instanceof CoC)) {return false;}
			CoC<?> alter = (CoC<?>) other;
			if (alter.size() != size()) {return false;}
			for (int i=0; i<size(); i++) {
				if (!alter.key(i).equals(key(i))) {return false;}
				if (alter.count(i) != count(i)) {return false;}
			}
			return true;
		}

		@Override
		public int hashCode() {return counts.hashCode();}
		
		public int count(int i) {return counts[i];}

		public CoC<T> empty() {return new CoC<>(comp);} 


		/**Combine multiple CoC objects into a single CoC.
		 * 
		 * TODO: The array identity-equal almost never trips.  Can things be modified so the faster check trips?
		 * **/
		@SuppressWarnings({ "cast", "unchecked" })
		public static <T> CoC<T> rollupTwo(CoC<T> s1, CoC<T> s2) {
			if (s1.labels == s2.labels || Arrays.deepEquals(s1.labels, s2.labels)) {
				int[] newCounts = Arrays.copyOf(s1.counts, s1.counts.length);
				for (int i=0; i< newCounts.length; i++) {newCounts[i] += s2.counts[i];}
				return new CoC<>(s1.comp, (T[]) s1.labels, newCounts, s1.fullSize+s2.fullSize);
			} else {
				CoC<T> combined = s1;
				for (T key: (T[]) s2.labels) {
					combined = combined.extend(key, s2.count(key));
				}
				return combined;
			}
		}

		@Override
		public int compareTo(CategoricalCounts<T> o) {
			return Integer.compare(fullSize(), o.fullSize());
		}
	}
	
	/**Encapsulation of run-length encoding information.
	 * A run-length encoding describes the counts of the items found
	 * in the order they were found.  The same category may appear 
	 * multiple times if items of the category are interspersed with
	 * items from other categories.
	 * 
	 * A comparator may (optionally) be supplied through some constructors.
	 * If supplied, equality of keys is determined to the comparator.
	 * Otherwise (e.g., if the comparator is null), equality is determined
	 * by the equals-method of the earlier added key. 
	 */
	public static final class RLE<T> implements CategoricalCounts<T> {
		private final List<T> keys;
		private final List<Integer> counts;
		private final Comparator<T> comp;
		private int fullSize = -1;
		
		/**Empty run-length encoding.**/
		public RLE() {this(null, new ArrayList<T>(), new ArrayList<Integer>());}
		
		/**Create a new RLE with the given keys and counts.
		 * The two lists must be of the same length.
		 * @param keys
		 * @param counts
		 */
		public RLE(List<T> keys, List<Integer> counts) {
			this(null, keys, counts);
		}
		
		/**New RLE object with.
		 * 
		 * @param comp Comparitor to use to determine equality (may be null)
		 * @param keys Keyset to associate with counts
		 * @param counts Counts to associate with keyset
		 */
		public RLE(Comparator<T> comp, List<T> keys, List<Integer> counts) {
			assert keys.size() == counts.size() : "keys vs. counts size mismatch";
			this.keys = keys;
			this.counts = counts;
			this.comp = comp;
		}
		
		public RLE<T> extend(T key, int count) {
			List<T> nkeys;
			List<Integer> ncounts = new ArrayList<Integer>();
			
			
			ncounts.addAll(counts);
			
			int last = keys.size()-1;
			if (last >=0 && equal(keys.get(last), key)) {
				nkeys = keys;
				ncounts.set(last, counts.get(last)+count);
			} else {
				nkeys = new ArrayList<T>();
				nkeys.addAll(keys); 
				nkeys.add(key);
				ncounts.add(count);
			}
			return new RLE<T>(comp, nkeys, ncounts);
		}

		private final boolean equal(T existingKey, T newKey) {
			if (comp == null) {return existingKey.equals(newKey);}
			else {return comp.compare(existingKey, newKey) == 0;}
		}
		
		public int count(int i) {return counts.get(i);}
		public T key(int i) {return keys.get(i);}
		public int size() {return keys.size();}
		public int fullSize() {
			if (fullSize == -1) {
				int acc=0;
				for (Integer v: counts) {acc+=v;}
				fullSize = acc;
			}
			return fullSize;
		}

		public String toString() {return "RLE: " + Arrays.deepToString(counts.toArray());}
		
		public boolean equals(Object other) {
			if (!(other instanceof RLE)) {return false;}
			RLE<?> alter = (RLE<?>) other;
			return counts.equals(alter.counts) && keys.equals(alter.keys);
		}
		
		public int hashCode() {return counts.hashCode()+keys.hashCode();}

		@Override
		public int count(T key) {
			for (int i=0; i<keys.size();i++) {
				if (keys.get(i).equals(key)) {return counts.get(i);}
			}
			return 0;
		}
		
		public CategoricalCounts<T> empty() {return new RLE<>();}
		
		@Override
		public int compareTo(CategoricalCounts<T> o) {
			return Integer.compare(fullSize(), o.fullSize());
		}
	}
}
