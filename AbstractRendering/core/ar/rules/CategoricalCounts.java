package ar.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public interface CategoricalCounts<T> {
	
	public CategoricalCounts<T> extend(T key, int qty);
	public int count(int i);
	public T key(int i);
	public int count(T key);
	public int fullSize();
	public int size();


	/**Count categories.  Categories are stored in sorted order (so "nth" makes sense).**/
	public static final class CoC<T> implements CategoricalCounts<T> {
		final SortedMap<T, Integer> counts;
		private final int fullSize;
		
		public CoC() {this(new TreeMap<T,Integer>(),0);}
		public CoC(Comparator<T> comp) {this(new TreeMap<T,Integer>(comp),0);}
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
		public final List<T> keys;
		public final List<Integer> counts;
		public final int fullSize;
		
		public RLE() {this(new ArrayList<T>(), new ArrayList<Integer>(), 0);}
		
		public RLE(List<T> keys, List<Integer> counts, int fullSize) {
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

		@Override
		public int count(T key) {
			for (int i=0; i<keys.size();i++) {
				if (keys.get(i).equals(key)) {return counts.get(i);}
			}
			return 0;
		}
	}
}
