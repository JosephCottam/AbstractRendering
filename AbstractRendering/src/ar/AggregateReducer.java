package ar;

/**Combine two aggregate sets into a third composite aggregate.
 * 
 * @author jcottam
 *
 * @param <LEFT>  Left-hand aggregate type
 * @param <RIGHT> Right-hand aggregate type
 * @param <OUT> Resulting aggregate type
 */
public interface AggregateReducer<LEFT,RIGHT,OUT> {
	
	/**Combine two aggregate values.
	 * This is a combination point-wise of the aggregate values, not of the aggregate sets.
	 * 
	 * (NOTE: If you come up with a useful case for combining aggregate sets, please let me know.)
	 * 
	 * @param left Left-hand aggregate value
	 * @param right Right-hand aggregate value
	 * @return Combination of left and right aggregate values as a new aggregate value
	 */
	public OUT combine(LEFT left, RIGHT right);
}
