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
	
	
	/**Determine control parameter values for the passed set of a aggregates.
	 * This method should be called at least once before the first
	 * value of an aggregate set is presented to the transfer function. 
	 * 
	 * Some transfer functions rely on relationships that exist inside of an 
	 * aggregate set.  This method allows the transfer function to compute
	 * relevant relationship quantities once, and then use them mulitple times
	 * in the "at" method.
	 * 
	 * For example, high-definition alpha composition needs to know the maximum
	 * and minimum value in the dataset.  "Specialize" will compute 
	 * those max/min values.
	 * 
	 * TODO: Should this be a function that returns a new transfer function instead?
	 * 
	 * @param aggregates Aggregates to determine the paramters for.
	 * **/
	public void specialize(Aggregates<? extends IN> aggregates);
}
