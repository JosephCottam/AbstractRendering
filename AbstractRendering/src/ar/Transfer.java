package ar;

import java.awt.Color;


/**Transfer functions converts an aggregate into a pixel.**/
public interface Transfer<A> {
	
	/**What color should be used for the pixel at location X/Y.
	 * 
	 * This function accepts the full set of aggregates so context 
	 * (as determined by the full set of aggregates)
	 * can be employed in determining a specific pixel.
	 * 
	 * These functions are not guaranteed to be  called from 
	 * a single thread, so implementations must provide for thread safety.
	 */
	public Color at(int x, int y, Aggregates<A> aggregates);
}
