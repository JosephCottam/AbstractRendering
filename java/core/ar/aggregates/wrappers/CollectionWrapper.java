package ar.aggregates.wrappers;

import java.util.AbstractCollection;
import java.util.Iterator;

import ar.Aggregates;
import ar.aggregates.AggregateUtils;

public class CollectionWrapper<T> extends AbstractCollection<T> {
	private final Aggregates<T> base;
	
	public CollectionWrapper(Aggregates<T> aggs) {this.base = aggs;}

	@Override public Iterator<T> iterator() {return base.iterator();}
	@Override public int size() {
		long size = AggregateUtils.size(base);
		if (size > Integer.MAX_VALUE) {throw new RuntimeException("Cannot return size because aggregates size exceeds integer limites.");}
		return (int) size;
	}
	
}
