package ar.aggregates;

import java.util.Iterator;

import ar.Aggregates;

/**Wrap a set of aggregates, but make it have a different high/low X/Y than it did before.
 * This does not SHIFT values, so position x,y will have the same value in the original
 * and the subset.  It is only reporting that the area of concern (e.g., where non-default values
 *  are expected) is a different shape.  
 *  
 * This is used to make zero-copy subsets and thus its name.
 * 
 * Setting outside of the ORIGINAL bounds results in no change.
 * **/
public class SubsetWrapper<A> implements Aggregates<A> {
	private final Aggregates<A> base;
	private final int lowX, lowY, highX, highY;
	
	public SubsetWrapper(Aggregates<A> base, int lowX, int lowY, int highX, int highY) {
		this.base = base;
		this.lowX = lowX;
		this.lowY = lowY;
		this.highX = highX;
		this.highY = highY;
	}
	
	/**Return the backing aggregate set that this class wraps.**/
	public Aggregates<A> base() {return base;}
	
	public Iterator<A> iterator() {return new Iterator2D<>(this);}

	public A get(int x, int y) {return base.get(x, y);}

	public void set(int x, int y, A val) {base.set(x,y, val);}

	public A defaultValue() {return base.defaultValue();}
	
	public int lowX() {return lowX;}
	public int lowY() {return lowY;}
	public int highX() {return highX;}
	public int highY() {return highY;}

	public String toString() {return String.format("Subset Aggregates from %d,%d to %d,%d.", lowX, lowY, highX,highY);}
}
