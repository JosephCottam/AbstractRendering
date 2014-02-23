package ar.aggregates.wrappers;

import java.util.Iterator;

import ar.Aggregates;
import ar.aggregates.Iterator2D;


/**Transpose the aggregate set (without copying).**/
public class TransposeWrapper<A> implements Aggregates<A> {
	private final Aggregates<A> base;
	
	public TransposeWrapper(Aggregates<A> base) {this.base = base;}
	
	/**Return the backing aggregate set that this class wraps.**/
	public Aggregates<A> base() {return base;}
	
	@Override public Iterator<A> iterator() {return new Iterator2D<>(this);}
	@Override public A get(int x, int y) {return base.get(y,x);}
	@Override public void set(int x, int y, A val) {base.set(y,x, val);}
	@Override public A defaultValue() {return base.defaultValue();}
	@Override public int lowX() {return base.lowY();}
	@Override public int lowY() {return base.lowX();}
	@Override public int highX() {return base.highY();}
	@Override public int highY() {return base.highX();}
	
	@Override public String toString() {return String.format("Transpose Aggregates from %d,%d to %d,%d.", lowX(), lowY(), highX(), highY());}

	/**Transpose a set of aggregates.**/
	public static <A> Aggregates<A> transpose(Aggregates<A> aggs) {
		if (aggs instanceof TransposeWrapper) {
			return ((TransposeWrapper<A>) aggs).base();
		} else {
			return new TransposeWrapper<>(aggs);
		}
	}
}
