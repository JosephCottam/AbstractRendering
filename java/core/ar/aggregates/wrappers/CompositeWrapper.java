package ar.aggregates.wrappers;

import java.util.Iterator;

import ar.Aggregates;
import ar.aggregates.AggregateUtils;
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

	/**Utility to create a Pair composite.**/
	public static <L,R> CompositeWrapper<L,R, Pair<L,R>> wrap(final Aggregates<L> left, final Aggregates<R> right) {
		return wrap(left, right, new Pairer<L,R>());
	}

	/**Construction method to ease transitions between default and non-default composer usage.**/
	public static <L,R,A> CompositeWrapper<L,R,A> wrap(final Aggregates<L> left, final Aggregates<R> right, final Composer<L,R,A> composer) {
		return new CompositeWrapper<>(left, right, composer);
	}
	
	/**Simple utility to pair up the left and right items.**/
	public static final class Pairer<L,R> implements CompositeWrapper.Composer<L,R, Pair<L,R>> {
		@Override public Pair<L,R> compose(L left, R right) {return new Pair<>(left, right);}
	}
	
	/**Simple utility to represent pairs of values.**/
	public static final class Pair<L,R> {
		public final L left;
		public final R right;
		
		public Pair(L left, R right) {
			this.left = left;
			this.right = right;
		}
	}
	
	/**Tool to aggregate of pairs into pair of aggregates.
	 * In some ways, this is the converse of wrapping two aggregates into a composite with a Pairer.
	 * 
	 * TODO: Is it possible to test if aggs already is a CompositeWrapper with the requested left/right values?
	 * **/
	public static final <L,R> CompositeWrapper<L,R, Pair<L,R>> convert(Aggregates<Pair<L,R>> aggs, L leftEmpty, R rightEmpty) {
		Aggregates<L> leftAggs = AggregateUtils.make(aggs, leftEmpty);
		Aggregates<R> rightAggs = AggregateUtils.make(aggs, rightEmpty);
		
		for (int x=aggs.lowX(); x<aggs.highX(); x++) {
			for (int y=aggs.lowY(); y<aggs.highY();y++) {
				Pair<L,R> v = aggs.get(x, y);
				leftAggs.set(x, y, v.left);
				rightAggs.set(x, y, v.right);
			}
		}
		return new CompositeWrapper<>(leftAggs, rightAggs, new Pairer<L,R>());
	}


}
