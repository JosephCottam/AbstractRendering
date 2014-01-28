package ar.rules;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import ar.util.Util;

/**Tools for working with associations between categories and counts.
 * @param <T> The type of the categories
 */
public class CategoricalCounts<T> implements Comparable<CategoricalCounts<T>> {
	private final Comparator<T> comp;
	private final int[] counts;
	private final Object[] labels;
	private final int fullSize;
	
	/**Create a new CoC with "natural" ordering.**/
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public CategoricalCounts() {this(new Util.ComparableComparator(), new Object[0], new int[0], 0);}
	
	/**@param comp Comparator used to order categories.**/
	public CategoricalCounts(Comparator<T> comp) {this(comp, new Object[0], new int[0], 0);}
	
	/**Create a Categorical count with a single categorical/count pair.**/
	public CategoricalCounts(Comparator<T> comp, T label, int count) {
		this(comp, new Object[]{label}, new int[]{count}, count);
	}
	
	/**@param counts Map backing this set of counts
	 * @param fullSize Total of the items in the counts (the relationship is not checked, but must hold for derivatives to work correctly)
	 ***/
	private CategoricalCounts(Comparator<T> comp, Object[] labels, int[] counts, int fullSize) {
		//System.out.printf("count with %d cats and %d total\n", counts.size(), fullSize);
		this.counts = counts;
		this.labels = labels;
		this.fullSize = fullSize;
		this.comp = comp;
	}
	
	@SuppressWarnings("unchecked")
	public CategoricalCounts<T> extend(T key, int count) {
		int idx = Arrays.binarySearch((T[]) labels, key, comp);
		if (idx >=0) {
			int[] newCounts = Arrays.copyOf(counts, counts.length);
			newCounts[idx] += count;
			return new CategoricalCounts<>(comp, labels, newCounts, fullSize+count);
		} else {
			idx = -(idx +1); 
			T[] newLabels = Util.insertInto((T[]) labels, key, idx);
			int[] newCounts = Util.insertInto(counts, count, idx);
			return new CategoricalCounts<>(comp, newLabels, newCounts, fullSize+count);
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
	
	@SuppressWarnings("unchecked")
	public T key(int i) {return (T) labels[i];}
	
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

	@Override
	public int hashCode() {return counts.hashCode();}
	
	public int count(int i) {return counts[i];}

	/**Create an empty version of the current thing.
	 * Does not remove any values,  but creates a new counter with the same construction parameters as the current one.
	 */
	public CategoricalCounts<T> empty() {return new CategoricalCounts<>(comp);} 


	/**Combine multiple CoC objects into a single CoC.
	 * 
	 * TODO: The array identity-equal almost never trips.  Can things be modified so the faster check trips?
	 * **/
	@SuppressWarnings({ "cast", "unchecked" })
	public static <T> CategoricalCounts<T> rollupTwo(CategoricalCounts<T> s1, CategoricalCounts<T> s2) {
		if (s1.labels == s2.labels || Arrays.deepEquals(s1.labels, s2.labels)) {
			int[] newCounts = Arrays.copyOf(s1.counts, s1.counts.length);
			for (int i=0; i< newCounts.length; i++) {newCounts[i] += s2.counts[i];}
			return new CategoricalCounts<>(s1.comp, (T[]) s1.labels, newCounts, s1.fullSize+s2.fullSize);
		} else {
			CategoricalCounts<T> combined = s1;
			for (T key: (T[]) s2.labels) {
				combined = combined.extend(key, s2.count(key));
			}
			return combined;
		}
	}

	/**Order by the full-size measurement.**/
	@Override public int compareTo(CategoricalCounts<T> o) {
		return Integer.compare(fullSize(), o.fullSize());
	}
	
	public static <T> CategoricalCounts<T> make(final Iterable<T> labels, final Iterable<Integer> counts) {
		CategoricalCounts<T> cc = new CategoricalCounts<T>();
		Iterator<T> labs = labels.iterator();
		Iterator<Integer> cnts = counts.iterator();
		
		while(labs.hasNext()) {
			T lab = labs.next();
			Integer c = cnts.next();
			cc = cc.extend(lab, c);
		}
		return cc;
	}
	
}
