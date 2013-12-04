package ar.aggregates.implementations;

import java.util.Iterator;

import ar.Aggregates;
import ar.aggregates.Iterator2D;

/**Set of color aggregates backed by a buffered image.**/
public class BooleanAggregates implements Aggregates<Boolean> {
	private final boolean[] values;
	private final Boolean defVal;
	private final int lowX, lowY, highX, highY;

	public BooleanAggregates(int lowX,int lowY, int highX, int highY, Boolean defVal) {
		this.defVal = defVal;
		this.lowX = lowX;
		this.lowY = lowY;
		this.highX = highX;
		this.highY = highY;
		
		int size = (highX-lowX)*(highY-lowY);
		this.values = new boolean[size];
	}

	public Boolean get(int x, int y) {
		if (x<lowX || x >=highX || y<lowY || y>=highY) {return defVal;}
		return values[idx(x,y)];
	}

	public void set(int x, int y, Boolean val) {
		if (x<lowX || x >=highX || y<lowY || y>=highY) {return;}
		values[idx(x,y)] = val;
	}

	public Iterator<Boolean> iterator() {return new Iterator2D<>(this);};
	public Boolean defaultValue() {return defVal;}
	public int lowX() {return lowX;}
	public int lowY() {return lowY;}
	public int highX() {return highX;}
	public int highY() {return highY;}

	private final int idx(int x,int y) {
		int idx = ((highX-lowX)*(y-lowY))+(x-lowX);
		return idx;
	}
}
