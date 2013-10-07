package ar;

import java.io.Serializable;

/**Transfer functions converts an aggregate into a pixel.
 * 
 * Transfer functions have a two-phase life cycle: generic and specialized.
 * Often a transfer function needs information about the data it is about
 * to process before it can process the first pixel (such as the bounds on the values).
 * Generic transfers don't know this information yet, specialized ones do.
 * The "Specialize" method converts a generic function into a specialized one
 * OR a specialized one into another one (specialized transfer functions retain the capabilities of generic ones).  
 * The Transfer.Specialized interface
 * indicates that a transfer function is ready for use.
 *   
 * **/
public interface Transfer<IN,OUT> extends Serializable {
	
	/**What value that represents "empty" from this transfer function?*/
	public OUT emptyValue();
	
	
	/**Determine control parameter values for the passed set of a aggregates.
	 * This method should be called at least once before the first
	 * value of an aggregate set is presented to the transfer function. 
	 * 
	 * Some transfer functions rely on relationships that exist inside of an 
	 * aggregate set.  This method allows the transfer function to compute
	 * relevant relationship quantities once, and then use them multiple times
	 * in the "at" method.
	 * 
	 * For example, high-definition alpha composition needs to know the maximum
	 * and minimum value in the dataset.  "Specialize" will compute 
	 * those maximum/minimum values.
	 * 
	 * @param aggregates Aggregates to determine the parameters for.
	 * **/
	public Specialized<IN,OUT> specialize(Aggregates<? extends IN> aggregates);

	
	/**Indicate that a transfer function is "ready to run".**/
	public static interface Specialized<IN,OUT> extends Transfer<IN,OUT> { 
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
	}
}
