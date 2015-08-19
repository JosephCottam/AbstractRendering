package ar.ext.server;

import java.util.Iterator;

import ar.Aggregates;
import ar.aggregates.Iterator2D;

/**Merge two sets of aggregates, giving preference to the 'top' on in returning values.
 * Does not make copies, handling the overlay with logic and references instaed.
 * **/
public class OverlayWrapper<A> implements Aggregates<A> {
	private final Aggregates<A> top, bottom;
	private final A defVal;
	
	public OverlayWrapper(Aggregates<A> top, Aggregates<A> bottom) {this(top, bottom, bottom.defaultValue());}
	public OverlayWrapper(Aggregates<A> top, Aggregates<A> bottom, A defVal) {
		this.top = top;
		this.bottom = bottom;
		this.defVal = defVal;
	}
	
	
	public Aggregates<A> top() {return top;}
	public Aggregates<A> bottom() {return bottom;}
	
	/**True if nothing was changed on the base aggregates via this wrapper.**/
	@Override public boolean empty() {return top.empty() && bottom.empty();}
	
	@Override public Iterator<A> iterator() {return new Iterator2D<>(this);}

	@Override public A get(int x, int y) {
		if (x >= top.lowX() && x < top.highX() && y >= top.lowY() && y < top.highY()) {return top.get(x,y);}
		else if (x >= bottom.lowX() && x < bottom.highX() && y >= bottom.lowY() && y < bottom.highY()) {return bottom.get(x, y);}
		else {return defaultValue();}
	}

	/**Unsupported, cannot guarantee set is available in full bounds.**/
	@Override public void set(int x, int y, A val) {throw new UnsupportedOperationException();}

	@Override public A defaultValue() {return defVal;}
	
	@Override public int lowX() {return Math.min(top.lowX(), bottom.lowX());}
	@Override public int lowY() {return Math.min(top.lowY(), bottom.lowY());}
	@Override public int highX() {return Math.max(top.highX(), bottom.highX());}
	@Override public int highY() {return Math.max(top.highY(), bottom.highY());}

	@Override public String toString() {return String.format("Overlay wrapper Aggregates from (%d, %d) to (%d, %d).", lowX(), lowY(), highX(), highY());}
}
