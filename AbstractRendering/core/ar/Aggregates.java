package ar;

import java.io.Serializable;


/**Store of aggregate values.
 * Aggregate implementations are required to report the upper and lower end
 * of their bounds so region-based operations can be performed without
 * realizing the entire aggregate space.  Efficient sub-setting
 * is not required of any aggregate sets, only that they accurately
 * report the region that they represent.
 * 
 * @author jcottam
 *
 * @param <A>  Type of the values to be stored in the aggregate set.
 */
public interface Aggregates<A> extends Iterable<A>, Serializable {
	public A at(int x, int y);
	public void set(int x, int y, A val);
	public A defaultValue();
	public int lowX();
	public int lowY();
	public int highX();
	public int highY();
}
