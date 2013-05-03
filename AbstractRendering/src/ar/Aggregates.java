package ar;

import java.util.Arrays;
import java.util.Iterator;

public class Aggregates<A> implements Iterable<A> {
	private final A[] values;
	private final int width;
	private final int height;
	
	@SuppressWarnings("unchecked")
	public Aggregates(int width, int height) {
		values = (A[]) new Object[width*height];
		this.width = width;
		this.height = height;
	}
	public synchronized void set(int x, int y, A v) {values[idx(x,y)] = v;}
	public synchronized A at(int x, int y) {return (A) values[idx(x,y)];}
	public int width() {return width;}
	public int height() {return height;}
	
	private final int idx(int x,int y) {
		int idx = (height*x)+y;
		if (idx>=width*height) {
			throw new IllegalArgumentException(String.format("Cannot refer to item at (%1$d,%2$d) from aggregates.  Maximum is (%3$d,%4$d).", x, y, width-1, height-1));
		}
		return idx;
	}

	@Override
	public synchronized Iterator<A> iterator() {return (Iterator<A>) Arrays.asList(values).iterator();}
}
