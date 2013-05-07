package ar;

import java.util.Arrays;
import java.util.Iterator;

public class Aggregates<A> implements Iterable<A> {
	private final A[] values;
	private final int offsetX, offsetY;
	private final int height,width;
	private final A defaultVal;
	
	public Aggregates(final int width, final int height, A defaultVal) {this(0,0,width,height, defaultVal);}
	
	@SuppressWarnings("unchecked")
	public Aggregates(final int x, final int y, final int width, final int height, A defaultVal) {
		this.width = width;
		this.height = height;
		this.offsetX = x;
		this.offsetY = y;
		this.defaultVal=defaultVal;
		values = (A[]) new Object[(width-offsetX)*(height-offsetY)];
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
	public int width() {return width;}
	public int height() {return height;}
	public int x() {return offsetX;}
	public int y() {return offsetY;}
	
	private final int idx(int x,int y) {
		int idx = (height*(x-offsetX))+(y-offsetY);
		if (idx>=width*height) {
			throw new IllegalArgumentException(String.format("Cannot refer to item at (%1$d,%2$d) from aggregates.  Maximum is (%3$d,%4$d).", x, y, width-1, height-1));
		}
		return idx;
	}

	@Override
	public synchronized Iterator<A> iterator() {return (Iterator<A>) Arrays.asList(values).iterator();}
}
