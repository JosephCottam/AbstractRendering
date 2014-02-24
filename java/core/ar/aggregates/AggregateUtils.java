package ar.aggregates;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import ar.Aggregates;
import ar.aggregates.implementations.*;

/**Utilities for working with aggregates.
 * 
 * TODO: Move this stuff to the Aggregates interface when Java 1.8 comes out...
 */
public class AggregateUtils {

	/**Return a rectangle representing the bounds of this aggregate set.
	 * Bounds are based on the bounds of concern (low/high X/Y) not values set. 
	 * **/
	public static Rectangle bounds(Aggregates<?> aggs) {
		return new Rectangle(aggs.lowX(), aggs.lowY(), aggs.highX()-aggs.lowX(), aggs.highY()-aggs.lowY());
	}

	public static BufferedImage asImage(Aggregates<? extends Color> aggs) {
		return asImage(aggs, bounds(aggs).width, bounds(aggs).height, Color.white);
	}

	/**From a set of color aggregates, make a new image.**/
	public static BufferedImage asImage(Aggregates<? extends Color> aggs, int width, int height, Color background) {
		if (aggs instanceof ColorAggregates) {return ((ColorAggregates) aggs).image();}
		
		BufferedImage i = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics g = i.getGraphics();
		g.setColor(background);
		g.fillRect(0, 0, width, height);
		g.dispose();
		for (int x=Math.max(0, aggs.lowX()); x<Math.min(width, aggs.highX()); x++) {
			for (int y=Math.max(0, aggs.lowY()); y<Math.min(height, aggs.highY()); y++) {
				Color c = aggs.get(x, y);
				if (c != null) {i.setRGB(x, y, c.getRGB());}			
			}			
		}
		return i;
	}

	public static <A> Aggregates<A> make(Aggregates<?> like, A defVal) {return make(like.lowX(), like.lowY(), like.highX(), like.highY(),defVal);}

	public static <A> Aggregates<A> make(int width, int height, A defVal) {return make(0,0,width,height,defVal);}

	/**Create a set of aggregates for the given type.*/
	@SuppressWarnings("unchecked")
	public static <A> Aggregates<A> make(int lowX, int lowY, int highX, int highY, A defVal) {
		if (defVal != null && defVal instanceof Color) {
			return (Aggregates<A>) new ColorAggregates(lowX, lowY, highX, highY, (Color) defVal);
		} else if (defVal instanceof Integer) {
			return (Aggregates<A>) new IntAggregates(lowX, lowY, highX, highY, (Integer) defVal);
		} else if (defVal instanceof Double) {
			return (Aggregates<A>) new DoubleAggregates(lowX, lowY, highX, highY, (Double) defVal);
		} else if (defVal instanceof Boolean) {
			return (Aggregates<A>) new BooleanAggregates(lowX, lowY, highX, highY, (Boolean) defVal);
		} else if (size(lowX,lowY,highX,highY) > Integer.MAX_VALUE){
			return new Ref2DAggregates<>(lowX, lowY, highX, highY, defVal);
		} else {
			return new RefFlatAggregates<>(lowX, lowY, highX, highY, defVal);
		}
	}

	/**Grid-style printing of the aggregates.  
	 * Useful for debugging with small aggregate sets...**/
	public static String toString(Aggregates<?> aggs) {
		StringBuilder b = new StringBuilder();
		b.append(String.format("%d-%d by %d-%d\n", aggs.lowX(), aggs.highX(), aggs.lowY(), aggs.highY()));
		int len = 0;
		
		for (Object o: aggs) {
			if (o==null) {continue;}
			len = Math.max(len, o.toString().length());
		}

		for (int y=aggs.lowY(); y<aggs.highY(); y++) {
			for (int x=aggs.lowX(); x<aggs.highX(); x++) {
				b.append(String.format("%" + len + "s, ", aggs.get(x, y)));
			}
			b.deleteCharAt(b.length()-1);
			b.deleteCharAt(b.length()-1);
			b.append("\n");
		}
		return b.toString();
	}
	
	/**How many aggregate values are present here?**/
	public static final long size(Aggregates<?> aggs) {return size(aggs.lowX(), aggs.lowY(), aggs.highX(), aggs.highY());}

	/**How many aggregate values are present here?**/
	public static final long size(int lowX, int lowY, int highX, int highY) {
		return ((long) (highX-lowX)) * ((long) (highY-lowY));
	}
	
	@SuppressWarnings("unused") 
	/**Convert the x/y value to a single index based on the low/high x/y.**/
	public static final int idx(int x,int y, int lowX, int lowY, int highX, int highY) {
		int idx = ((highX-lowX)*(y-lowY))+(x-lowX);
		return idx;
	}

	
}
