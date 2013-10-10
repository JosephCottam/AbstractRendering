package ar.aggregates;

import java.util.Iterator;

import ar.Aggregates;

/**Iterate over a set of aggregates by accessing the get and low/high methods.
 * 
 * Optimized for row-major iteration.
 */
public final class Iterator2D<A> implements Iterator<A> {
	private final Aggregates<A> source;
	int x,y;
	public Iterator2D(Aggregates<A> source) {
		this.source=source;
		this.x = source.lowX();
		this.y = source.lowY();
	}

	public boolean hasNext() {return x<source.highX() && y<source.highY();}

	public A next() {
		A val = source.get(x, y);
		x = x +1;
		if (x >=source.highX()) {
			x=source.lowX();
			y = y+1;
		}
		return val;
	}
	public void remove() { throw new UnsupportedOperationException(); }
}