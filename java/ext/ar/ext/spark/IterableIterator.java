package ar.ext.spark;

import java.util.Iterator;

/**Wraps an iterator as an iterable. 
 * 
 * WARNING: This is not illegal, but violates some conventions.  The iterator
 * returned by this class is the original argument.  Since iterators are stateful,
 * multiple class to 'iterator' may cause problems as the same iterator may be used
 * in multiple locations....BEWARE!
 * 
 * @param <E>
 */
public class IterableIterator<E> implements Iterable<E> {
	final Iterator<E> iterator;
	
	public IterableIterator(Iterator<E> iterator) {
	    if (iterator == null) {throw new NullPointerException();}
		this.iterator = iterator ;
	}

	@Override public Iterator<E> iterator() {return iterator;}
}
