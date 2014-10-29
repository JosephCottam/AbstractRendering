package ar.rules;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import ar.util.Util;

/**Tools for working with associations between categories and counts.
 * @param <T> The type of the categories
 */
public class CategoricalCounts<T> implements Serializable {
	private final Comparator<T> comp;
	private final int[] counts;
	private final T[] labels;
	private final int fullSize;
	
	/**Create a new CoC with "natural" ordering.**/
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public CategoricalCounts() {this(new Util.ComparableComparator(), (T[]) new Object[0], new int[0], 0);}
	
	/**@param comp Comparator used to order categories.**/
	@SuppressWarnings({ "unchecked"})
	public CategoricalCounts(Comparator<T> comp) {this(comp, (T[]) new Object[0], new int[0], 0);}
	
	/**Create a Categorical count with a single categorical/count pair.**/
	@SuppressWarnings({ "unchecked"})
	public CategoricalCounts(Comparator<T> comp, T label, int count) {
		this(comp, (T[]) new Object[]{label}, new int[]{count}, count);
	}
	
	/**@param counts Map backing this set of counts
	 * @param fullSize Total of the items in the counts (the relationship is not checked, but must hold for derivatives to work correctly)
	 ***/
	private CategoricalCounts(Comparator<T> comp, T[] labels, int[] counts, int fullSize) {
		//System.out.printf("count with %d cats and %d total\n", counts.size(), fullSize);
		this.counts = counts;
		this.labels = labels;
		this.fullSize = fullSize;
		this.comp = comp;
	}
	
	public CategoricalCounts<T> extend(T key, int count) {
		int idx = Arrays.binarySearch(labels, key, comp);
		if (idx >=0) {
			int[] newCounts = Arrays.copyOf(counts, counts.length);
			newCounts[idx] += count;
			return new CategoricalCounts<>(comp, labels, newCounts, fullSize+count);
		} else {
			idx = -(idx +1); 
			T[] newLabels = Util.insertInto(labels, key, idx);
			int[] newCounts = Util.insertInto(counts, count, idx);
			return new CategoricalCounts<>(comp, newLabels, newCounts, fullSize+count);
		}
	}
	
	public int count(T key) {
		int idx = Arrays.binarySearch(labels, key, comp);
		if (idx >= 0) {return counts[idx];}
		return 0;
	}
	
	public int size() {return labels.length;}
	public int fullSize() {return fullSize;}
	public String toString() {
		if (size() ==0) {return "<empty>";}
		
		StringBuilder b = new StringBuilder();
		for (int i=0; i<size(); i++) {
			b.append(labels[i]);
			b.append(": ");
			b.append(counts[i]);
			b.append("; ");
		}
		b.deleteCharAt(b.length()-1);
		b.deleteCharAt(b.length()-1);
		return b.toString();
	}
	
	public T key(int i) {return labels[i];}
	
	public boolean hasKey(T key) {
		for (Object k: labels) {if (Util.isEqual(k, key)) {return true;}}
		return false;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof CategoricalCounts)) {return false;}
		CategoricalCounts<?> alter = (CategoricalCounts<?>) other;
		if (alter.size() != size()) {return false;}
		for (int i=0; i<size(); i++) {
			if (!alter.key(i).equals(key(i))) {return false;}
			if (alter.count(i) != count(i)) {return false;}
		}
		return true;
	}
	
	@Override public int hashCode() {return Arrays.hashCode(counts);}
	
	public int count(int i) {return counts[i];}

	public Comparator<T> comparator() {return comp;}
	
	/**Create an empty version of the current thing.
	 * Does not remove any values,  but creates a new counter with the same construction parameters as the current one.
	 */
	public CategoricalCounts<T> empty() {return new CategoricalCounts<>(comp);} 


	/**Combine multiple CoC objects into a single CoC.
	 * **/
	public static <T> CategoricalCounts<T> rollupTwo(CategoricalCounts<T> s1, CategoricalCounts<T> s2) {
		if (s1.labels == s2.labels || Arrays.deepEquals(s1.labels, s2.labels)) {
			int[] newCounts = Arrays.copyOf(s1.counts, s1.counts.length);
			for (int i=0; i< newCounts.length; i++) {newCounts[i] += s2.counts[i];}
			return new CategoricalCounts<>(s1.comp, s1.labels, newCounts, s1.fullSize+s2.fullSize);
		} else {
			CategoricalCounts<T> combined = s1;
			for (T key: s2.labels) {
				combined = combined.extend(key, s2.count(key));
			}
			return combined;
		}
	}
	
	public static <T> CategoricalCounts<T> make(final Iterable<T> labels, final Iterable<Integer> counts, Comparator<T> comp) {
		CategoricalCounts<T> cc = new CategoricalCounts<T>(comp);
		Iterator<T> labs = labels.iterator();
		Iterator<Integer> cnts = counts.iterator();
		
		while(labs.hasNext()) {
			T lab = labs.next();
			Integer c = cnts.next();
			cc = cc.extend(lab, c);
		}
		return cc;
	}
	
	/**Sort categorical counts based on their full size.**/
	public static final class MangitudeComparator<K> implements Comparator<CategoricalCounts<K>> {
		@Override
		public int compare(CategoricalCounts<K> o1, CategoricalCounts<K> o2) {
			return Integer.compare(o1.fullSize(), o2.fullSize());
		}
	}
	
}
