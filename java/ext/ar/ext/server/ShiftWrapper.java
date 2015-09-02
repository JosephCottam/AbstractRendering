package ar.ext.server;

import java.util.Iterator;

import ar.Aggregates;

/**Shifts the coordinates by a given amount.  
 * 
 * Does not copy, does not scale or subset, just modifies the indexing calculations.
 * **/
public class ShiftWrapper<A> implements Aggregates<A> {
	private final Aggregates<A> base;
	private final int tx, ty;
	
	public ShiftWrapper(Aggregates<A> base, int tx, int ty) {
		this.base = base;
		this.tx = tx;
		this.ty = ty;
	}
	
	
	public Aggregates<A> base() {return base;}
	
	/**True if nothing was changed on the base aggregates via this wrapper.**/
	@Override public boolean empty() {return base.empty();}
	
	@Override public Iterator<A> iterator() {return base.iterator();}

	@Override public A get(int x, int y) {return base.get(x+tx, y+ty);}

	@Override public void set(int x, int y, A val) {base.set(x+tx,y+ty,val);}

	@Override public A defaultValue() {return base.defaultValue();}
	
	@Override public int lowX() {return base.lowX()+tx;}
	@Override public int lowY() {return base.lowY()+ty;}
	@Override public int highX() {return base.highX()+tx;}
	@Override public int highY() {return base.highY()+ty;}

	@Override public String toString() {return String.format("Overlay wrapper Aggregates from (%d, %d) to (%d, %d).", lowX(), lowY(), highX(), highY());}
}
