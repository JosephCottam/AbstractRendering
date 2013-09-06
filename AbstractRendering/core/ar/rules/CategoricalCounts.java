package ar.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**Tools for working with associations between categories and counts.
 * @param <T> The type of the categories
 */
public interface CategoricalCounts<T> {
	
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


	/**Count categories.  Categories are stored in sorted order (so "nth" makes sense).**/
	public static final class CoC<T> implements CategoricalCounts<T> {
		final SortedMap<T, Integer> counts;
		private final int fullSize;
		
		/**Create a new CoC with "natural" ordering.**/
		public CoC() {this(new TreeMap<T,Integer>(),0);}
		
		/**@param comp Comparator used to order categories.**/
		public CoC(Comparator<T> comp) {this(new TreeMap<T,Integer>(comp),0);}
		
		/**@param counts Map backing this set of counts
		 * @param fullSize Total of the items in the counts (the relationship is not checked, but must hold for derivatives to work correctly)
		 ***/
		public CoC(SortedMap<T, Integer> counts, int fullSize) {
			this.counts = counts;
			this.fullSize = fullSize;
		}
		
		public CoC<T> extend(T key, int count) {
			SortedMap<T,Integer> ncounts = new TreeMap<T,Integer>(counts.comparator());
			ncounts.putAll(counts);
			if (!ncounts.containsKey(key)) {ncounts.put(key, 0);}
			int v = ncounts.get(key);
			ncounts.put(key, v+count);
			int fullSize = this.fullSize + count;
			return new CoC<T>(ncounts, fullSize);
		}
		
		public int count(Object key) {
			if (counts.containsKey(key)) {return counts.get(key);}
			else {return 0;}
		}
		
		public int size() {return counts.size();}
		public int fullSize() {return fullSize;}
		public String toString() {return "COC: " + counts.toString();}
		public T key(int i) {
			Iterator<T> it = counts.keySet().iterator();
			for (; i>0; i--) {it.next();}
			return it.next();			
		}
		public int count(int i) {
			Iterator<Integer> it = counts.values().iterator();
			for (; i>0; i--) {it.next();}
			return it.next();
		}
	}
	
	/**Encapsulation of run-length encoding information.
	 * A run-length encoding describes the counts of the items found
	 * in the order they were found.  The same category may appear 
	 * multiple times if items of the category are interspersed with
	 * items from other categories.
	 */
	public static final class RLE<T> implements CategoricalCounts<T> {
		private final List<T> keys;
		private final List<Integer> counts;
		private final int fullSize;
		
		/**Empty run-length encoding.**/
		public RLE() {this(new ArrayList<T>(), new ArrayList<Integer>(), 0);}
		
		/**Create a new RLE with the given keys and counts.
		 * The two lists must be of the same length.
		 * @param keys
		 * @param counts
		 */
		public RLE(List<T> keys, List<Integer> counts) {
			this(keys, counts, sum(counts));
		}
		
		private static int sum(Iterable<Integer> vals) {
			int acc=0;
			for (Integer v: vals) {acc+=v;}
			return acc;
		}
		
		private RLE(List<T> keys, List<Integer> counts, int fullSize) {
			assert keys.size() == counts.size() : "keys vs. counts size mismatch";
			this.keys = keys;
			this.counts = counts;
			this.fullSize=fullSize;
		}
		
		public RLE<T> extend(T key, int count) {
			List<T> nkeys;
			List<Integer> ncounts = new ArrayList<Integer>();
			
			
			ncounts.addAll(counts);
			
			int last = keys.size()-1;
			if (last >=0 && key.equals(keys.get(last))) {
				nkeys = keys;
				ncounts.set(last, counts.get(last)+count);
			} else {
				nkeys = new ArrayList<T>();
				nkeys.addAll(keys); 
				nkeys.add(key);
				ncounts.add(count);
			}
			return new RLE<T>(nkeys, ncounts, fullSize+count);
		}
		
		public int count(int i) {return counts.get(i);}
		public T key(int i) {return keys.get(i);}
		public int size() {return keys.size();}
		public int fullSize() {return fullSize;}
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
	}
}
