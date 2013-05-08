package ar;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Iterator;

public class Aggregates<A> implements Iterable<A> {
	private final A[] values;
	private final int offsetX, offsetY;
	private final int height,width;
	private final A defaultVal;
	
	public Aggregates(final int width, final int height, A defaultVal) {this(0,0,width,height, defaultVal);}
	public Aggregates(final Rectangle bounds, A defaultVal) {this(bounds.x, bounds.y, bounds.width, bounds.height, defaultVal);}
	
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

	/**What are the bounds that can actually be stored in this aggregates object?*/
	public Rectangle bounds() {return new Rectangle(offsetX, offsetY, width, height);}
	public int width() {return width;}
	public int height() {return height;}
	public int offsetX() {return offsetX;}
	public int offsetY() {return offsetY;}
	
	private final int idx(int x,int y) {
		int idx = (height*(x-offsetX))+(y-offsetY);
		return idx;
	}

	@Override
	public synchronized Iterator<A> iterator() {return (Iterator<A>) Arrays.asList(values).iterator();}
}
