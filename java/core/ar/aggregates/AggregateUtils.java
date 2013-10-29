package ar.aggregates;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import ar.Aggregates;

/**Utilities for working with aggregates.
 * 
 * TODO: Move this stuff to the Aggregates interface when Java 1.8 comes out...
 */
public class AggregateUtils {

	/**From a set of color aggregates, make a new image.**/
	public static BufferedImage asImage(Aggregates<Color> aggs, int width, int height, Color background) {
		if (aggs instanceof ImageAggregates) {return ((ImageAggregates) aggs).image();}
		
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

	/**Create a new aggregate set that is a subset of the old aggregate set.
	 * 
	 * The new aggregate set will have the same indices as the old aggregate set
	 * (so 100,100 in the old one will have the same value as 100,100 in the old),
	 * however the new aggregate set will not necessarily have the same lowX/lowY or highX/highY
	 * as the old set.  The new aggregate set will have the same default value as the old.
	 */
	public static  <A> Aggregates<A> alignedSubset(Aggregates<A> source, int lowX, int lowY, int highX, int highY) {
		Aggregates<A> target = make(lowX, lowY, highX, highY, source.defaultValue());
		for (int x=lowX; x<highX; x++) {
			for (int y=lowY; y<highY; y++) {
				target.set(x, y, source.get(x, y));
			}
		}
		return target;
	}

	public static <A> Aggregates<A> make(Aggregates<?> like, A defVal) {return make(like.lowX(), like.lowY(), like.highX(), like.highY(),defVal);}

	public static <A> Aggregates<A> make(int width, int height, A defVal) {return make(0,0,width,height,defVal);}

	/**Create a set of aggregates for the given type.
	 */
	@SuppressWarnings("unchecked")
	public static <A> Aggregates<A> make(int lowX, int lowY, int highX, int highY, A defVal) {
		if (defVal instanceof Color) {
			return (Aggregates<A>) new ImageAggregates(lowX, lowY, highX, highY, (Color) defVal);
		} else {
			return new FlatAggregates<>(lowX, lowY, highX, highY, defVal);
		}
	}

	/**Produce an independent aggregate set that has a lowX/Y value of 0,0 and contains
	 * values from the source as determined by the passed low/high values.
	 * 
	 * On null, returns null.
	 * **/
	public static <A> Aggregates<A> subset(Aggregates<A> source, int lowX, int lowY, int highX, int highY) {
		if (source == null) {return null;}
		
		int width = highX-lowX;
		int height = highY-lowY;
		
		Aggregates<A> aggs= make(0, 0, width, height, source.defaultValue());
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				A val = source.get(x+lowX,y+lowY);
				aggs.set(x, y, val);
			}
		}
		return aggs;
	}

	/**Grid-style printing of the aggregates.  
	 * Useful for debugging with small aggregate sets...**/
	public static String toString(Aggregates<?> aggs) {
		StringBuilder b = new StringBuilder();
		b.append(String.format("%d-%d by %d-%d\n", aggs.lowX(), aggs.highX(), aggs.lowY(), aggs.highY()));
		int len = 0;
		for (Object o: aggs) {len = Math.max(len, o.toString().length());}
		
		for (int x=aggs.lowX(); x<aggs.highX(); x++) {
			for (int y=aggs.lowY(); y<aggs.highY(); y++) {
				b.append(String.format("%" + len + "s, ", aggs.get(x, y)));
			}
			b.deleteCharAt(b.length()-1);
			b.deleteCharAt(b.length()-1);
			b.append("\n");
		}
		return b.toString();
	}
	
	/**How many aggregate values are present here?**/
	public static final long size(Aggregates<?> aggs) {
		return ((long) (aggs.highX()-aggs.lowX())) * ((long) (aggs.highY()-aggs.lowY()));
	}

	
}
