package ar.util;

/**TODO: Can the class arguments be enforced better with parameterized wild-cards?  Issues with parameterized types have been encountered...
 * **/
public interface Inspectable<IN,OUT> {
	/**The input class (e.g., the type of aggregates input).  
	 * This is used to guide configuration creation or validate configurations.
	 ***/
	public Class<?> input();
	
	/**The output class.  This is used to guide configuration creation or validate configurations.**/
	public Class<?> output();
	
}
