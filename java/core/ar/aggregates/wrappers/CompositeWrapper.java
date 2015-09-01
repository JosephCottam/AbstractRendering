package ar.aggregates.wrappers;

import java.util.Iterator;
import java.util.function.BiFunction;

import ar.Aggregates;
import ar.aggregates.AggregateUtils;
import ar.aggregates.Iterator2D;

/**Create one set of aggregates from two sets of aggregates, 
 *   but the two inputs can be pulled out again later.
 *   
 * At the heart of this class's operations is the composer, a bi-function that takes
 * an item from the left, an item from the right and composes them to make the reutrn value. 
 *   
 */
public class CompositeWrapper<L,R,A> implements Aggregates<A> {
	private final Aggregates<L> left;
	private final Aggregates<R> right;
	private final BiFunction<L,R,A> composer;
	
	public CompositeWrapper(final Aggregates<L> left, final Aggregates<R> right, final BiFunction<L,R,A> composer) {
		this.left = left;
		this.right = right;
		this.composer = composer;
	}
	
	@Override public A get(int x, int y) {return composer.apply(left.get(x, y), right.get(x,y));}
	@Override public A defaultValue() {return composer.apply(left.defaultValue(), right.defaultValue());}
	@Override public int lowX() {return Math.min(left.lowX(), right.lowX());}
	@Override public int lowY() {return Math.min(left.lowY(), right.lowY());}
	@Override public int highX() {return Math.max(left.highX(), right.highX());}
	@Override public int highY() {return Math.max(left.highY(), right.highY());}
	@Override public Iterator<A> iterator() {return new Iterator2D<>(this);}

	@Override
	public void set(int x, int y, A val) {throw new UnsupportedOperationException("Cannot set in composite aggregates.");}
	
	public Aggregates<L> left() {return left;}
	public Aggregates<R> right() {return right;}

	/**Utility to create a Pair composite.**/
	public static <L,R> CompositeWrapper<L,R, Pair<L,R>> wrap(final Aggregates<L> left, final Aggregates<R> right) {
		return wrap(left, right, new Pairer<L,R>());
	}

	/**Construction method to ease transitions between default and non-default composer usage.**/
	public static <L,R,A> CompositeWrapper<L,R,A> wrap(final Aggregates<L> left, final Aggregates<R> right, final BiFunction<L,R,A> composer) {
		return new CompositeWrapper<>(left, right, composer);
	}
	
	/**Simple utility to pair up the left and right items.**/
	public static final class Pairer<L,R> implements BiFunction<L,R, Pair<L,R>> {
		@Override public Pair<L,R> apply(L left, R right) {return new Pair<>(left, right);}
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
	
	/**Tool to convert "aggregate of pairs" into "pair of aggregates."
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

	/**Return the left item if it is not the identity, otherwise return the right item.**/
	public static final class LeftBiased<A> implements BiFunction<A,A,A> {
		private final A leftID;
		public LeftBiased(A leftID) {this.leftID = leftID;}
		
		@Override
		public A apply(A left, A right) {
			if (leftID == left || (leftID != null && leftID.equals(left))) {return right;}
			return left;
		}
		
	}


}
