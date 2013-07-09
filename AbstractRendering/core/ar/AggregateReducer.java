package ar;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ar.aggregates.ConstantAggregates;
import ar.aggregates.FlatAggregates;
import ar.util.Util;

/**Combine aggregate values.  
 * 
 * The methods defined in this interface
 * as the inner work for a iteration strategy loops.  Actual iteration
 * strategies must be defined elsewhere.  Two core strategies are included
 * in the inner-class "Strategies".
 *
 * @param <LEFT>  Left-hand aggregate type
 * @param <RIGHT> Right-hand aggregate type
 * @param <OUT> Resulting aggregate type
 */
public interface AggregateReducer<LEFT,RIGHT,OUT> {

	/**Combine two aggregate values.
	 * 
	 * This is a combination point-wise of the aggregate values, not of the aggregate sets.
	 * (If you have a use case for combining aggregate sets instead of just aggregate values, please let me know.)
	 * AggregateReducers.
	 * 
	 * @param left Left-hand aggregate value
	 * @param right Right-hand aggregate value
	 * @return Combination of left and right aggregate values as a new aggregate value
	 */
	public OUT combine(LEFT left, RIGHT right);
	

	/**Reduce a few aggregate values into a single value.  
	 * This is used for reducing the resolution of a single set of aggregates (e.g., for zooming without recomputing aggregates). 
	 * 
	 * @param sources Values from the base aggregates
	 * @return Combination of the passed aggregates
	 */
	public OUT rollup(List<LEFT> sources);
	
	/** Value that is an identity under this operation in the output space.
	 * This value is used when no data is present for reduction in either aggregate or for padding in rollup.**/
	public OUT zero();
	
	public Class<LEFT> left();
	public Class<RIGHT> right();
	public Class<OUT> output();
	
	
	
	/**Core iteration strategies for using the AggregateReducer functions.*/
	public static class Strategies {
		/**Combine two aggregate sets according to the passed reducer.
		 * 
		 * The resulting aggregate set will have a realized subset region sufficient to
		 * cover the realized sbuset region of both source aggregate sets (regardless of 
		 * the values found in those sources).  If one of the two aggregate sets provided
		 * is already of sufficient size, it will be used as both a source and a target.
		 * 
		 * 
		 * @param left Aggregate set to use for left-hand arguments
		 * @param right Aggregate set to use for right-hand arguments
		 * @param red Reduction operation
		 * @return Resulting aggregate set (may be new or a destructively updated left or right parameter) 
		 */
		public static <T> Aggregates<T> foldLeft(Aggregates<T> left, Aggregates<T> right, AggregateReducer<T,T,T> red) {
			if (left == null) {return right;}
			if (right == null) {return left;}
			
			T identity = red.zero();
			
			if ((left instanceof ConstantAggregates) && Util.isEqual(identity, left.defaultValue())) {return right;}
			if ((right instanceof ConstantAggregates) && Util.isEqual(identity, right.defaultValue())) {return right;}
			
			List<Aggregates<T> >sources = new ArrayList<Aggregates<T>>();
			Aggregates<T> target;
			Rectangle rb = new Rectangle(right.lowX(), right.lowY(), right.highX()-right.lowX(), right.highY()-right.lowY());
			Rectangle lb = new Rectangle(left.lowX(), left.lowY(), left.highX()-left.lowX(), left.highY()-left.lowY());
			Rectangle bounds = rb.union(lb);
			
			if (lb.contains(bounds)) {
				sources.add(right);
				target = left;
			} else if (rb.contains(bounds)) {
				sources.add(left);
				target = right;
			} else {
				sources.add(right);
				sources.add(left);
				target = new FlatAggregates<T>(bounds.x, bounds.y, bounds.x+bounds.width, bounds.y+bounds.height, red.zero());
			}

			for (Aggregates<T> source: sources) {
				for (int x=Math.max(0, source.lowX()); x<source.highX(); x++) {
					for (int y=Math.max(0, source.lowY()); y<source.highY(); y++) {
						target.set(x,y, red.combine(target.at(x,y), source.at(x,y)));
					}
				}
			}
			return target;
		}
		
		/**Performs a 2x2 fold-up of the passed aggregate set.
		 * 
		 * <p> The incoming aggregates are tessellated with a 2x2 grid 
		 * (odd lengths are handled by padding with start's default value.). 
		 * 
		 * TODO: Extend to dxd rollup
		 * **/
		public static <T> Aggregates<T> foldUp(Aggregates<T> start, AggregateReducer<T,T,T> red) {
			Aggregates<T> end = new FlatAggregates<T>(start.lowX()/2, start.lowY()/2, start.highX()/2, start.highY()/2, red.zero());
			
			for (int x = start.lowX(); x < start.highX(); x=x+2) {
				for (int y=start.lowY(); y < start.highY(); y=y+2) {
					T one = start.at(x,y);
					T two = start.at(x+1,y);
					T three = start.at(x,y+1);
					T four = start.at(x+1,y+1);
					
					T value = red.rollup(Arrays.asList(one, two, three, four));
					end.set(x/2, y/2, value);
				}
			}
			return end;
		}
	}
}
