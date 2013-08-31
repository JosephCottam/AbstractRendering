package ar.aggregates;

import java.util.Arrays;
import java.util.Iterator;

import ar.Aggregates;

/** Aggregates implementation backed by a single array.
 * This class efficiently supports subset regions.
 */
public class FlatAggregates<A> implements Aggregates<A>{
	private static final long serialVersionUID = 7143994707761884518L;
	private final A[] values;
	private final int lowX, lowY;
	private final int highX,highY;
	private final A defaultVal;
	
	/**Create a an aggregates instance with the same high/low values as the parameter. 
	 */
	public FlatAggregates(Aggregates<?> like, A defaultVal) {this(like.lowX(), like.lowY(), like.highX(),  like.highY(), defaultVal);}
	
	/**Create a region of aggregates from (0,0) to (highX,highY)**/
	public FlatAggregates (final int highX, final int highY, A defaultVal) {this(0,0,highX,highY, defaultVal);}
	
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
	public FlatAggregates(final int lowX, final int lowY, final int highX, final int highY, A defaultVal) {
		if (lowX > highX) {throw new IllegalArgumentException(String.format("Inverted bounds: lowX (%d) must be lower than highX (%d)", lowX, highX));}
		if (lowY > highY) {throw new IllegalArgumentException(String.format("Inverted bounds: lowY (%d) must be lower than highY (%d)", lowY, highY));}
		
		this.lowX = lowX;
		this.lowY = lowY;
		this.highX = highX;
		this.highY = highY;
		this.defaultVal=defaultVal;
		long size = ((long) highX-lowX)*(highY-lowY);
		if (size > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(String.format("Aggregates of size %dx%d exceeds the implementation capacity.", (highX-lowX), (highY-lowY)));
		}
		
		size = Math.max(0, size);
		values = (A[]) new Object[(int)size];
		Arrays.fill(values, defaultVal);
	}

	/**Set the value at the given (x,y).**/
	public synchronized void set(int x, int y, A v) {
		if (x<lowX || x>=highX || y<lowY || y>=highY) {return;}
		int idx = idx(x,y);
		values[idx] = v;
	}
	
	
	/**Get the value at the given (x,y).**/
	public synchronized A get(int x, int y) {
		if (x<lowX || x>=highX || y<lowY || y>=highY) {return defaultVal;}
		int idx = idx(x,y);
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
	public synchronized Iterator<A> iterator() {return Arrays.asList(values).iterator();}

	/**Produce an independent aggregate set that has a lowX/Y value of 0,0 and contains
	 * values from the source as determined by the passed low/high values.
	 * 
	 * On null, returns null.
	 * 
	 * TODO: Move to the Aggregates interface when Java 1.8 comes out...
	 * **/
	public static <A> Aggregates<A> subset(Aggregates<A> source, int lowX, int lowY, int highX, int highY) {
		if (source == null) {return null;}
		
		int width = highX-lowX;
		int height = highY-lowY;
		
		Aggregates<A> aggs= new FlatAggregates<>(0, 0, width, height, source.defaultValue());
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				A val = source.get(x+lowX,y+lowY);
				aggs.set(x, y, val);
			}
		}
		return aggs;
	}
}
