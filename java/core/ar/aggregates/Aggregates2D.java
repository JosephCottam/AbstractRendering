package ar.aggregates;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

import ar.Aggregates;

/** Aggregates implemented as nested arrays.
 * This class efficiently supports subset regions.
 */
public class Aggregates2D<A> implements Aggregates<A>{
	private static final long serialVersionUID = 7143994707761884518L;
	private final A[][] values;
	private final int lowX, lowY;
	private final int highX,highY;
	private final A defaultVal;
	
	/**Create a an aggregates instance with the same high/low values as the parameter. 
	 */
	public Aggregates2D(Aggregates<?> like, A defaultVal) {this(like.lowX(), like.lowY(), like.highX(),  like.highY(), defaultVal);}
	
	/**Create a region of aggregates from (0,0) to (highX,highY)**/
	public Aggregates2D (final int highX, final int highY, A defaultVal) {this(0,0,highX,highY, defaultVal);}
	
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
	public Aggregates2D(final int lowX, final int lowY, final int highX, final int highY, A defaultVal) {
		if (lowX > highX) {throw new BoundsInversionException(lowX, highX, "X");}
		if (lowY > highY) {throw new BoundsInversionException(lowY, highY, "Y");}
		
		this.lowX = lowX;
		this.lowY = lowY;
		this.highX = highX;
		this.highY = highY;
		this.defaultVal=defaultVal;
		
		long width = highX-lowX;
		long height = highY-lowY;
		
		
		if (width > Integer.MAX_VALUE || height > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(String.format("Aggregates of size %dx%d exceeds the implementation capacity.", (highX-lowX), (highY-lowY)));
		}
		
		values = (A[][]) new Object[(int) width][(int) height];
		for (A[] row : values) {Arrays.fill(row, defaultVal);}
	}

	/**Set the value at the given (x,y).**/
	public synchronized void set(int x, int y, A v) {
		if (x<lowX || y < lowY || x >= highX || y > highY) {return;}
		values[x-lowX][y-lowY] = v;
	}
	
	
	/**Get the value at the given (x,y).**/
	public synchronized A get(int x, int y) {
		if (x<lowX || y < lowY || x >= highX || y >= highY) {return defaultVal;}
		return values[x-lowX][y-lowY];
	}

	public A defaultValue() {return defaultVal;}

	/**What are the bounds that can actually be stored in this aggregates object?*/
	public int lowX() {return lowX;}
	public int lowY() {return lowY;}
	public int highX() {return highX;}
	public int highY() {return highY;}
	
	
	/**Iterates over the values in the region defined by (lowX,lowY) and (highX, highY).**/
	public synchronized Iterator<A> iterator() {return new Iterator2D<>(values);}
	
	public static final class Iterator2D<E> implements Iterator<E> {
		  private Queue<Iterator<E>> queue = new LinkedList<Iterator<E>>();
		  public Iterator2D(E[][] sources) {
			  for (E[] source: sources) {queue.add(Arrays.asList(source).iterator());}
		  }
		  
		  public boolean hasNext() {
		    // If this returns true, the head of the queue will have a next element
		    while(!queue.isEmpty()) {
		      if(queue.peek().hasNext()) {
		        return true;
		      }
		      queue.poll();
		    }
		    return false;
		  }
		  public E next() {
		    if(!hasNext()) throw new NoSuchElementException();
		    Iterator<E> iter = queue.poll();
		    E result = iter.next();
		    queue.offer(iter);
		    return result;
		  }
		  public void remove() { throw new UnsupportedOperationException(); }
		};
}
