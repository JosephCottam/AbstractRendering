package ar.aggregates.implementations;

import java.io.Serializable;
import java.util.Arrays;

import ar.aggregates.AggregateUtils;

/**Set of aggregates backed by ints.  Used for things
 * that have a storage type of int, though a representation type
 * might be different.  This includes colors and actual ints.
 **/
abstract class IntegerBackingAggregates implements Serializable {
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

	public Integer getInteger(int x, int y) {return getInt(x,y);}
	
	public void setInt(int x, int y, int val) {
		if (x<lowX || x >=highX || y<lowY || y>=highY) {return;}
		values[AggregateUtils.idx(x,y, lowX, lowY, highX, highY)] = val;
	}
	
	public int getInt(int x, int y) {
		if (x<lowX || x >=highX || y<lowY || y>=highY) {return defVal;}
		return values[AggregateUtils.idx(x,y, lowX, lowY, highX, highY)];
	}

	
	public void set(int x, int y, Integer val) {setInt(x, y, val.intValue());}

	public Integer defaultInt() {return defVal;}
	public int lowX() {return lowX;}
	public int lowY() {return lowY;}
	public int highX() {return highX;}
	public int highY() {return highY;}
}
