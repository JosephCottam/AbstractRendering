package ar.aggregates;

import java.util.Iterator;

import ar.Aggregates;

public final class ConstantAggregates<A> implements Aggregates<A> {
	private final A val;
	private final int lowX, lowY, highX, highY;
	public ConstantAggregates(A value) {this(0,0,0,0, value);}
	public ConstantAggregates(int lowX, int lowY, int highX, int highY, A value) {
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
			@Override
			public void remove() {throw new UnsupportedOperationException();}
		};
	}

	public A at(int x, int y) {return val;}
	public void set(int x, int y, A val) {throw new UnsupportedOperationException();}
	public A defaultValue() {return val;}
	public int lowX() {return lowX;}
	public int lowY() {return lowY;}
	public int highX() {return highX;}
	public int highY() {return highY;}
}