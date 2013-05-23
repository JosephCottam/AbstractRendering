package ar;

import java.util.Arrays;
import java.util.Iterator;


/**Store of aggregate values.
 * 
 * This class does not require that the entire aggregate space be realized,
 * it implements sub-setting (though the subset region will still be stored
 * densely). 
 * 
 * @author jcottam
 *
 * @param <A>  Type of the values to be stored in the aggregate set.
 */
public class Aggregates<A> implements Iterable<A> {
	private final A[] values;
	private final int lowX, lowY;
	private final int highX,highY;
	private final A defaultVal;
	
	/**Create a region of aggregates from (0,0) to (highX,highY)**/
	public Aggregates(final int highX, final int highY, A defaultVal) {this(0,0,highX,highY, defaultVal);}
	
	/**Create a regional set of aggregates.
	 * 
	 * Though any integer-pair may be used as an index, the default value will be returned for
	 * any point outside of the region defined by (lowX, lowY) and (highX, highY).
	 * This class treats row of highX and the column highY as the outside of the 
	 * region of interest (much like length is used in arrays).
	 * 
	 * @param lowX Lowest valid x
	 * @param lowY Lowest valid y 
	 * @param highX Lowest invalid x > lowX 
	 * @param highY Lowest invalid y > highX
	 * @param defaultVal 
	 */
	@SuppressWarnings("unchecked")
	public Aggregates(final int lowX, final int lowY, final int highX, final int highY, A defaultVal) {
		if (lowX >= highX) {throw new IllegalArgumentException(String.format("Inverted bounds: lowX (%d) must be lower than highX (%d)", lowX, highX));}
		if (lowY >= highY) {throw new IllegalArgumentException(String.format("Inverted bounds: lowY (%d) must be lower than highY (%d)", lowY, highY));}
		
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

	/**Set the value at the given (x,y).**/
	public synchronized void set(int x, int y, A v) {
		int idx = idx(x,y);
		if (idx<0||idx>=values.length) {return;}
		values[idx] = v;
	}
	
	
	/**Get the value at the given (x,y).**/
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
		int idx = ((highY-lowY)*(x-lowX))+(y-lowY);
		return idx;
	}

	
	/**Iterates over the values in the region defined by (lowX,lowY) and (highX, highY).**/
	public synchronized Iterator<A> iterator() {return (Iterator<A>) Arrays.asList(values).iterator();}
}
