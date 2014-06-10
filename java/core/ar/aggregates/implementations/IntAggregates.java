package ar.aggregates.implementations;

import java.util.Iterator;

import ar.Aggregates;
import ar.aggregates.Iterator2D;

/**Set of Integer values.**/
public class IntAggregates extends IntegerBackingAggregates implements Aggregates<Integer> {
	public IntAggregates(int lowX,int lowY, int highX, int highY, int defVal) {
		super(lowX, lowY, highX, highY, defVal);
	}

	@Override public Iterator<Integer> iterator() {return new Iterator2D<>(this);}
	@Override public Integer get(int x, int y) {return super.getInt(x, y);}
	@Override public Integer defaultValue() {return super.defaultInt();}
}
