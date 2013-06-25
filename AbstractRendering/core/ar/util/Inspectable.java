package ar.util;

public interface Inspectable<IN,OUT> {
	/**The input class (e.g., the type of aggregates input).  
	 * This is used to guide configuration creation or validate configurations.
	 ***/
	public Class<IN> input();
	
	/**The output class.  This is used to guide configuration creation or validate configurations.**/
	public Class<OUT> output();
	
}
