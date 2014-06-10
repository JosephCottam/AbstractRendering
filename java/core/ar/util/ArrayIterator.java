package ar.util;

import java.util.Iterator;

public final class ArrayIterator<T> implements Iterator<T> {
	private final T[] base;
	private int index=0;
	
	public ArrayIterator(T[] base) {this.base = base;}
	@Override public boolean hasNext() {return index < base.length;}
	@Override public T next() {return base[index++];}
	@Override public void remove() {throw new UnsupportedOperationException();}
}
