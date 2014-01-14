package ar.aggregates.implementations;

import java.util.Arrays;

import ar.aggregates.AggregateUtils;

/**Set of color aggregates backed by a buffered image.**/
abstract class IntegerBackingAggregates {
	protected final int[] values;
	protected final Integer defVal;
	protected final int lowX, lowY, highX, highY;

	public IntegerBackingAggregates(int lowX,int lowY, int highX, int highY, int defVal) {
		this.defVal = defVal;
		this.lowX = lowX;
		this.lowY = lowY;
		this.highX = highX;
		this.highY = highY;
		
		int size = (highX-lowX)*(highY-lowY);
		this.values = new int[size];
		Arrays.fill(values, defVal);
	}

	public Integer getInt(int x, int y) {
		if (x<lowX || x >=highX || y<lowY || y>=highY) {return defVal;}
		return values[AggregateUtils.idx(x,y, lowX, lowY, highX, highY)];
	}

	public void set(int x, int y, Integer val) {
		if (x<lowX || x >=highX || y<lowY || y>=highY) {return;}
		values[AggregateUtils.idx(x,y, lowX, lowY, highX, highY)] = val;
	}

	public Integer defaultInt() {return defVal;}
	public int lowX() {return lowX;}
	public int lowY() {return lowY;}
	public int highX() {return highX;}
	public int highY() {return highY;}
}
