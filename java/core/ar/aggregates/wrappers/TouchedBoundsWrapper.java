package ar.aggregates.wrappers;

import java.util.Iterator;

import ar.Aggregates;
import ar.aggregates.Iterator2D;
import ar.util.Util;


/**Wrap a set of aggregates.  Report min/max X/Y based on values set instead of the region that is set-able.**/
public class TouchedBoundsWrapper<A> implements Aggregates<A> {
	private final Aggregates<A> base;
	private int lowX = Integer.MAX_VALUE;
	private int lowY = Integer.MAX_VALUE;
	private int highX = Integer.MIN_VALUE;
	private int highY = Integer.MIN_VALUE;
	
	public TouchedBoundsWrapper(Aggregates<A> base) {this(base, true);}
	public TouchedBoundsWrapper(Aggregates<A> base, boolean discoverTouched) {
		this.base = base;
		if (discoverTouched) {
			for (int x=base.lowX(); x<base.highX(); x++) {
				for (int y= base.lowY(); y<base.highY(); y++) {
					if (!Util.isEqual(base.get(x,y), base.defaultValue())) {
						lowX = Math.min(lowX, x);
						lowY = Math.min(lowY, y);
						highX = Math.max(highX, x);
						highY = Math.max(highY, y);
					}
				}
			}
		}
		
	}
	
	
	/**Return the backing aggregate set that this class wraps.**/
	public Aggregates<A> base() {return base;}
	
	/**True if nothing was changed on the base aggregates via this wrapper.**/
	public boolean untouched() {return lowX == Integer.MAX_VALUE && lowY == Integer.MAX_VALUE && highX == Integer.MIN_VALUE && highY == Integer.MIN_VALUE;}
	
	@Override public Iterator<A> iterator() {return new Iterator2D<>(this);}

	@Override public A get(int x, int y) {return base.get(x, y);}

	@Override public void set(int x, int y, A val) {
		base.set(x,y, val);
		
		if (x < lowX && x >= base.lowX()) {lowX = x;}
		if (y < lowY && y >= base.lowY()) {lowY = y;}
		if (x >= highX && x < base.highX()) {highX = x+1;}
		if (y >= highY && y < base.highY()) {highY = y+1;}
	}

	@Override public A defaultValue() {return base.defaultValue();}
	
	@Override public int lowX() {return Math.max(lowX, base.lowX());}
	@Override public int lowY() {return Math.max(lowY, base.lowY());}
	@Override public int highX() {return Math.min(highX, base.highX());}
	@Override public int highY() {return Math.min(highY, base.highY());}

	@Override public String toString() {return String.format("Touched Bounds (Wrapped) Aggregates from %d,%d to %d,%d.", lowX, lowY, highX,highY);}
}
