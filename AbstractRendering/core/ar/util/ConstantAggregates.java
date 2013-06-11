package ar.util;

import java.util.Iterator;

import ar.Aggregates;

public final class ConstantAggregates<A> implements Aggregates<A> {
	private final A val;
	private final int lowX, lowY, highX, highY;
	public ConstantAggregates(A value) {this(0,0,0,0, value);}
	public ConstantAggregates(int lowX, int lowY, int highX, int highY, A value) {
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
	public int lowX() {return 0;}
	public int lowY() {return 0;}
	public int highX() {return 0;}
	public int highY() {return 0;}
}