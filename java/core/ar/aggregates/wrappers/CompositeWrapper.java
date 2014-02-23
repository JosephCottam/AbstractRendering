package ar.aggregates.wrappers;

import java.util.Iterator;

import ar.Aggregates;
import ar.aggregates.Iterator2D;

/**Create one set of aggregates from two sets of aggregates, 
 *   but the two inputs can be pulled out again later.
 */
public class CompositeWrapper<L,R,A> implements Aggregates<A> {
	private final Aggregates<L> left;
	private final Aggregates<R> right;
	private final Composer<L,R,A> composer;
	
	public CompositeWrapper(final Aggregates<L> left, final Aggregates<R> right, final Composer<L,R,A> composer) {
		this.left = left;
		this.right = right;
		this.composer = composer;
	}
	
	@Override public A get(int x, int y) {return composer.compose(left.get(x, y), right.get(x,y));}
	@Override public A defaultValue() {return composer.compose(left.defaultValue(), right.defaultValue());}
	@Override public int lowX() {return Math.min(left.lowX(), right.lowX());}
	@Override public int lowY() {return Math.min(left.lowY(), right.lowY());}
	@Override public int highX() {return Math.max(left.highX(), right.highX());}
	@Override public int highY() {return Math.max(left.highY(), right.highY());}
	@Override public Iterator<A> iterator() {return new Iterator2D<>(this);}

	@Override @SuppressWarnings("unused")
	public void set(int x, int y, A val) {throw new UnsupportedOperationException("Cannot set in composite aggregates.");}
	
	public Aggregates<L> left() {return left;}
	public Aggregates<R> right() {return right;}
	
	/**Take two aggregate arguments to create a new aggregate value.**/
	public static interface Composer<L,R,A> {public A compose(L left, R right);}
}
