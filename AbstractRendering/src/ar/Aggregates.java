package ar;

import java.util.ArrayList;
import java.util.Iterator;

public class Aggregates<A> implements Iterable<A> {
	private final ArrayList<A> values;
	private final int width;
	private final int height;
	
	public Aggregates(int width, int height) {
		values = new ArrayList<A>(width*height);
		this.width = width;
		this.height = height;
	}
	public void set(int x, int y, A v) {values.add(idx(x,y),v);}
	public A at(int x, int y) {return values.get(idx(x,y));}
	public int width() {return width;}
	public int height() {return height;}
	
	private final int idx(int x,int y) {
		int idx = (height*x)+y;
		if (idx>=width*height) {
			throw new IllegalArgumentException(String.format("Cannot refer to item at (%1$d,%2$d) from aggregates.  Maximum is (%3$d,%4$d).", x, y, width, height));
		}
		return idx;
	}

	@Override
	public Iterator<A> iterator() {return values.iterator();}
}
