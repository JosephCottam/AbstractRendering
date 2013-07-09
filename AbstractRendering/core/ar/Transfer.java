package ar;

import ar.util.Inspectable;

/**Transfer functions converts an aggregate into a pixel.**/
public interface Transfer<IN,OUT> extends Inspectable<IN,OUT> {
	
	/**What color should be used for the pixel at location X/Y.
	 * 
	 * This function accepts the full set of aggregates so context 
	 * (as determined by the full set of aggregates)
	 * can be employed in determining a specific pixel.
	 * 
	 * These functions are not guaranteed to be  called from 
	 * a single thread, so implementations must provide for thread safety.
	 */
	public OUT at(int x, int y, Aggregates<? extends IN> aggregates);
	
	/**What value that represents "empty" from this transfer function?*/
	public OUT emptyValue();
}
