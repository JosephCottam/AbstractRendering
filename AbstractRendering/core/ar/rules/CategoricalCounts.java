package ar.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public interface CategoricalCounts<T> {
	
	public void add(T key, int qty);
	public int count(int i);
	public int count(Object key);
	public int fullSize();
	public int size();


	/**Count categories.  Categoreis are stored in sorted order (so "nth" makes sense).**/
	public static final class CoC<T> implements CategoricalCounts<T>{
		private int fullSize=0;
		final SortedMap<T, Integer> counts;
		
		public CoC() {counts = new TreeMap<T,Integer>();}
		public CoC(Comparator<T> comp) {counts = new TreeMap<T,Integer>(comp);}
		
		public void add(T key, int count) {
			if (!counts.containsKey(key)) {counts.put(key, 0);}
			int v = counts.get(key);
			counts.put(key, v+count);
			fullSize+=count;
		}
		public int count(Object key) {
			if (counts.containsKey(key)) {return counts.get(key);}
			else {return 0;}
		}
		
		public int size() {return counts.size();}
		public int fullSize() {return fullSize;}
		public String toString() {return "COC: " + Arrays.deepToString(counts.keySet().toArray());}
		public int val(Object category) {return counts.get(category);}
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
	public static final class RLE {
		public final List<Object> keys = new ArrayList<Object>();
		public final List<Integer> counts = new ArrayList<Integer>();
		public int fullSize =0;
		public void add(Object key, int count) {
			keys.add(key);
			counts.add(count);
			fullSize+=count;
		}
		
		public int count(int i) {return counts.get(i);}
		public Object key(int i) {return keys.get(i);}
		public int size() {return keys.size();}
		public int fullSize() {return fullSize;}
		public String toString() {return "RLE: " + Arrays.deepToString(counts.toArray());}
		public int val(Object category) {
			for (int i=0; i<keys.size();i++) {
				if (keys.get(i).equals(category)) {return counts.get(i);}
			}
			return 0;
		}
		
		public boolean equals(Object other) {
			if (!(other instanceof RLE)) {return false;}
			RLE alter = (RLE) other;
			return counts.equals(alter.counts) && keys.equals(alter.keys);
		}
	}
}
