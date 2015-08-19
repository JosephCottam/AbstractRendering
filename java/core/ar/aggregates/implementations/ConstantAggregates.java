package ar.aggregates.implementations;

import java.util.Iterator;

import ar.Aggregates;


/**An aggregate set that all cells have the same value.
 * Still tracks the low/high bounds, but the in-bounds and out-of-bounds values are the same.
 * 
 * This class is often used to present a specific region as having a particular value
 * (thus the region is tracked, even though it does not represent a value).
 */
public final class ConstantAggregates<A> implements Aggregates<A> {
	private static final long serialVersionUID = -6013315833630311053L;
	
	private final A val;
	private final int lowX, lowY, highX, highY;
	
	/**Create a constant aggregates with zero-extent, but a default value.**/
	public ConstantAggregates(A value) {this(value,0,0,0,0);}
	
	/**Create an aggregate set with the given value at all locations
	 * (i.e., default value IS the held value).
	 */
	public ConstantAggregates(A value, int lowX, int lowY, int highX, int highY) {
		if (lowX > highX) {
			throw new IllegalArgumentException(String.format("Inverted bounds: lowX (%d) must be lower than highX (%d)", lowX, highX));}
		if (lowY > highY) {
			throw new IllegalArgumentException(String.format("Inverted bounds: lowY (%d) must be lower than highY (%d)", lowY, highY));}

		this.val = value;
		this.lowX = lowX;
		this.lowY = lowY;
		this.highX = highX;
		this.highY = highY;
	}
	
	public Iterator<A> iterator() {
		final int size = (highX - lowX) * (highY-lowY);
		return new Iterator<A>() {
			int count = 0;
			public boolean hasNext() {return count < size;}
			public A next() {count++; return val;}
			public void remove() {throw new UnsupportedOperationException();}
		};
	}

	public A get(int x, int y) {return val;}
	public void set(int x, int y, A val) {throw new UnsupportedOperationException();}
	public A defaultValue() {return val;}
	public int lowX() {return lowX;}
	public int lowY() {return lowY;}
	public int highX() {return highX;}
	public int highY() {return highY;}
}