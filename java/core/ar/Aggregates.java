package ar;

import java.io.Serializable;


/** Store of aggregate values.
 * 
 * Aggergates are the results of rendering geometry into a bin.  They are
 * the core unique conceptual data structure of Abstract Rendering.  In
 * essence, a set of aggregates is a data space derived from a source
 * data space and a set of geometry.  (The source data and geometry
 * are provided via Glyphsets).  
 * 
 * Aggregate implementations are required to report the upper and lower end
 * of their bounds so region-based operations can be performed without
 * realizing the entire aggregate space.  Efficient sub-setting
 * is not required of any aggregate sets, only that they accurately
 * report the region that they represent.
 * 
 * Note: The current aggreages implementation is based on Cartesian grids
 * (notice how all indexing is done via X/Y).  We are exploring alternative 
 * aggregate arrangements and will be updating this interface to reflect a 
 * more general addressing scheme. 
 * 
 * @author jcottam
 *
 * @param <A>  Type of the values to be stored in the aggregate set.
 */
public interface Aggregates<A> extends Serializable, Iterable<A> {
	/**Get the aggregate value at the given position.**/
	public A get(int x, int y);
	
	/**Set the aggregate value at the given position.**/
	public void set(int x, int y, A val);
	
	/**What is the default value in this set of aggregates?
	 * 
	 * This value is usually derived from the aggregator function 
	 * that was applied to construct it.  The default value 
	 * is a valid value if there was no data associated with 
	 * the given aggregate location.  However, the default
	 * value may not actually indicate that no values were had
	 * (e.g., it is not a "null" value, it is more like a "zero").
	 */
	public A defaultValue();
	
	/**What is the lowest X-Value that this in the range of this Aggregates instance? 
	 * 
	 * Values below this value are not guaranteed to have a sensible value,
	 * though it is often the case that they can be assumed to be "defaultValue"
	 * and it is permissible for an implementing class to return the default
	 * value when queries.  
	 * 
	 * NOTE: We are exploring policy options for requesting items outside of the
	 * aggregates range.  In the future, requests outside of range may have a specified
	 * behavior.  For now, the implementation MAY return the default value, or throw an
	 * exception or return null.
	 **/
	public int lowX();

	/**What is the lowest Y-Value that this in the range of this Aggregates instance?
	 * (See notes on lowX for boundary behaviors).
	 * */
	public int lowY();

	/**What is the lowest X-Value that this above the range of this Aggregates instance?
	 * (See notes on lowX for boundary behaviors).
	 * */
	public int highX();
	
	/**What is the lowest Y-Value that this above the range of this Aggregates instance?
	 * (See notes on lowX for boundary behaviors).
	 * */
	public int highY();
}
