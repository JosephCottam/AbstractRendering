package ar.ext.tiles;

import java.util.List;

import ar.AggregateReducer;

/**Aggregate reducer for combining non-overlapping aggregate sets.*/
public class CopyReducer<A> implements AggregateReducer<A, A, A>{
	private final Class<A> type;
	public CopyReducer(Class<A> type) {this.type = type;}
	
	/**Always takes the left-value if it is non-null, otherwise takes the right.**/
	public A combine(A left, A right) {
		if (left != null) {return left;}
		else {return right;}
	}

	public A rollup(List<A> sources) {throw new UnsupportedOperationException();}
	public A zero() {return null;}
	public Class<A> left() {return type;}
	public Class<A> right() {return type;}
	public Class<A> output() {return type;}
}
