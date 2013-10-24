package ar;

import java.io.Serializable;

/**An Aggregator convert glyphs into aggregate items for a specific view.
 * 
 * The aggregator is defined such that it may be used in a fold-like operation
 * where agg.combine(x,y, agg.combine(...), V) is a valid construction.
 * 
 * The intended usagage model is to instantiate an aggregator once per glyphset
 * and repeatedly apply the aggregator to each pixel in the space.
 * 
 * 
 * IN -- The type of the data element on the glyph
 * OUT -- The type of aggregates produced
 * **/
public interface Aggregator<IN,OUT> extends Serializable {
	
	/**
	 * Compute an aggregate value from an existing aggregate value and a 
	 * new input values.  If the return value is not equal-to "current,"
	 * the return value must be a distinct object from "current" to ensure correct behavior.
	 * 
	 * The x and y position are provided as arguments so position-sensitive
	 * aggregation can be performed.  Any other contextual information
	 * needs to be provided through the class in some other way.
	 * 
	 * @param x The x-position being operated on
	 * @param y The y-position being operated on
	 * @param current  An existing aggregate value
	 * @param update A new input value
	 * @return The aggregate value
	 */
	public OUT combine(long x, long y, OUT current, IN update);
	
	
	/**Reduce aggregate values into a single value.
	 * 
	 * If there is any sequential notion, the "first" item should go into left and the "second" into right.
	 * If semantically appropriate, accumulators should be passed as "left" and implementing classes may optimize for that case.
	 * This is used for reducing the resolution of a single set of aggregates (e.g., for zooming without recomputing aggregates). 
	 * 
	 * @return Combination of the passed aggregates
	 */
	public OUT rollup(OUT left, OUT right);

	/**What value is an mathematical identity value for this operation?
	 * Value V is an identity is op(V, x) = x for all V.
	 * 
	 * This method is used to initialize the aggregate set in many circumstances.
	 * Because of aggregate reducers, this initial value needs to 
	 * be an identity (thus the name).  However, not all renderers rely on this
	 * property (for example, pixel-serial rendering just uses it for the background).
	 **/
	public OUT identity();
}
