package ar.aggregates.implementations;

import java.util.Iterator;

import ar.Aggregates;
import ar.aggregates.Iterator2D;

/**Set of color aggregates backed by a buffered image.**/
public class IntAggregates extends IntegerBackingAggregates implements Aggregates<Integer> {
	public IntAggregates(int lowX,int lowY, int highX, int highY, int defVal) {
		super(lowX, lowY, highX, highY, defVal);
	}

	public Iterator<Integer> iterator() {return new Iterator2D<>(this);}
	public Integer get(int x, int y) {return super.getInt(x, y);}
	public Integer defaultValue() {return super.defaultInt();}
}
