package ar;

import java.util.Arrays;
import java.util.Iterator;

public class Aggregates<A> implements Iterable<A> {
	private final A[] values;
	private final int lowX, lowY;
	private final int highX,highY;
	private final A defaultVal;
	
	public Aggregates(final int highX, final int highY, A defaultVal) {this(0,0,highX,highY, defaultVal);}
	
	@SuppressWarnings("unchecked")
	public Aggregates(final int lowX, final int lowY, final int highX, final int highY, A defaultVal) {
		this.lowX = lowX;
		this.lowY = lowY;
		this.highX = highX;
		this.highY = highY;
		this.defaultVal=defaultVal;
		int size = (highX-lowX)*(highY-lowY);
		size = Math.max(0, size);
		values = (A[]) new Object[size];
		Arrays.fill(values, defaultVal);
	}
	
	public synchronized void set(int x, int y, A v) {
		int idx = idx(x,y);
		if (idx<0||idx>=values.length) {return;}
		values[idx] = v;
	}
	
	public synchronized A at(int x, int y) {
		int idx = idx(x,y);
		if (idx<0||idx>=values.length) {return defaultVal;}
		return values[idx];
	}
	
	public A defaultValue() {return defaultVal;}

	/**What are the bounds that can actually be stored in this aggregates object?*/
	public int lowX() {return lowX;}
	public int lowY() {return lowY;}
	public int highX() {return highX;}
	public int highY() {return highY;}
	
	private final int idx(int x,int y) {
		int idx = (highY-lowY)*(x-lowX)+(y-lowY);
		return idx;
	}

	@Override
	public synchronized Iterator<A> iterator() {return (Iterator<A>) Arrays.asList(values).iterator();}
}
