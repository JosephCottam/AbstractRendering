package ar.aggregates;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import ar.Aggregates;
import ar.Aggregator;
import ar.aggregates.implementations.*;
import ar.util.Util;

/**Utilities for working with aggregates.
 * 
 * TODO: Move this stuff to the Aggregates interface when Java 1.8 comes out...
 */
public class AggregateUtils {

	/**Return a rectangle representing the bounds of this aggregate set.
	 * Bounds are based on the bounds of concern (low/high X/Y) not values set.
	 * Null aggs have null bounds. 
	 * **/
	public static Rectangle bounds(Aggregates<?> aggs) {
		if (aggs == null) {return null;}
		return new Rectangle(aggs.lowX(), aggs.lowY(), aggs.highX()-aggs.lowX(), aggs.highY()-aggs.lowY());
	}

	public static BufferedImage asImage(Aggregates<? extends Color> aggs) {
		return asImage(aggs, Color.white);
	}

	public static BufferedImage asImage(Aggregates<? extends Color> aggs, Color background) {
		Rectangle bounds = bounds(aggs);
		return asImage(aggs, bounds.width, bounds.height, background);
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
	

	/**Merge two sets of aggregates according to some function. 
	 * The results in position (x,y) is derived from position (x,y) in both 
	 * input aggregates.  Therefore, the result with have bounds sufficient to
	 * enclose both input sets. 
	 *   
	 * NOTE: Aligned merge is also the basis of aggregates differencing (aka, subtractive rendering)
	 *   
	 * @param left Set of aggregates to use as the first argument to the merge operator.
	 * @param right Set of aggregates to use as the second argument to the merge operator
	 * @param op Merge operator
	 * @param defVal Default value for results aggregates.
	 * @return Results of the merge.
	 */
	public static <L,R,OUT> Aggregates<OUT> alignedMerge(
								Aggregates<L> left, 
								Aggregates<R> right,
								OUT defVal,
								BiFunction<L,R,OUT> op) {
		
		Rectangle rb = new Rectangle(right.lowX(), right.lowY(), right.highX()-right.lowX(), right.highY()-right.lowY());
		Rectangle lb = new Rectangle(left.lowX(), left.lowY(), left.highX()-left.lowX(), left.highY()-left.lowY());
		Rectangle bounds = rb.union(lb);

		Aggregates<OUT> target = AggregateUtils.make((int) bounds.getMinX(), (int) bounds.getMinY(), 
													(int) bounds.getMaxX(), (int) bounds.getMaxY(),
													defVal);

		for (int x=Math.max(0, target.lowX()); x<target.highX(); x++) {
			for (int y=Math.max(0, target.lowY()); y<target.highY(); y++) {
				L l = left.get(x,y);
				R r = right.get(x,y);
				OUT v = op.apply(l, r);
				if (Util.isEqual(defVal, v)) {continue;}
				target.set(x,y, v); 
			}
		}
		return target;
	}
	
	/**FOR INTERNAL USE ONLY.  Aligned merge two sets of aggregaets...possibly modifying one of them.
	 * Can only be used if BOTH sets of aggregates are only held in the current scope AND
	 * going out of scope immediately after the merge.  
	 * 
	 * Combine two aggregate sets according to the passed reducer. 
	 * Aligns coordinates between the two sets. 
	 * 
	 * The resulting aggregate set will have a realized subset region sufficient to
	 * cover the realized subset region of both source aggregate sets (regardless of 
	 * the values found in those sources).  If one of the two aggregate sets provided
	 * is already of sufficient size, it will be used as both a source and a target.
	 * Therefore, this may involve a **DESTRUCTIVE** update of one of the sets of aggregates.
	 * 
	 * @param left Aggregate set to use for left-hand arguments
	 * @param right Aggregate set to use for right-hand arguments
	 * @param identity Identity value for the rollup function
	 * @param rollup Reduction operation
	 * @return Resulting aggregate set (may be new or a destructively updated left or right parameter) 
	 */
	public static <T> Aggregates<T> __unsafeMerge(Aggregates<T> left, Aggregates<T> right, T identity, BiFunction<T,T,T> rollup) {
		if (left == null) {return right;}
		if (right == null) {return left;}

		if ((left instanceof ConstantAggregates) && Util.isEqual(identity, left.defaultValue())) {return right;}
		if ((right instanceof ConstantAggregates) && Util.isEqual(identity, right.defaultValue())) {return left;}

		List<Aggregates<T> >sources = new ArrayList<Aggregates<T>>();
		Aggregates<T> target;
		Rectangle rb = new Rectangle(right.lowX(), right.lowY(), right.highX()-right.lowX(), right.highY()-right.lowY());
		Rectangle lb = new Rectangle(left.lowX(), left.lowY(), left.highX()-left.lowX(), left.highY()-left.lowY());
		Rectangle bounds = rb.union(lb);

		if (lb.contains(bounds)) {
			target = left;
			sources.add(right);
		} else if (rb.contains(bounds)) {
			target = right;
			sources.add(left);
		} else {
			sources.add(left);
			sources.add(right);
			target = AggregateUtils.make((int) bounds.getMinX(), (int) bounds.getMinY(), 
					(int) bounds.getMaxX(), (int) bounds.getMaxY(), identity);
		}
	
		for (Aggregates<T> source: sources) {
			for (int x=Math.max(0, source.lowX()); x<source.highX(); x++) {
				for (int y=Math.max(0, source.lowY()); y<source.highY(); y++) {
					T newVal = source.get(x,y);
					if (Util.isEqual(identity, newVal)) {continue;}
					T comb = rollup.apply(target.get(x,y), source.get(x,y));
					target.set(x,y, comb); 
				}
			}
		}
		return target;
	}
	
	/**Create a new set of aggregates with smaller bounds
	 * by making each cell in the new aggregates cover multiple cells
	 * in the old aggregates. 
	 * 
	 * TODO: Add fractional value support via a fractioner function
	 * TODO: Provide selector-like functionality
	 * 
	 * @param factor Requested roll-up factor (each output cell is a factorxfactor region of the input) 
	 * **/
	public static <T> Aggregates<T> coarsen(Aggregates<T> start, Aggregator<?,T> red, double factor) {
		int size = (int) Math.round(factor);
		if (size < 1) {return start;}
		Aggregates<T> end = AggregateUtils.make(start.lowX()/size, start.lowY()/size, start.highX()/size, start.highY()/size, red.identity());

		for (int x = start.lowX(); x < start.highX(); x=x+size) {
			for (int y=start.lowY(); y < start.highY(); y=y+size) {
				
				T acc = red.identity();
				for (int xx=0; xx<size; xx++) {
					for (int yy=0; yy<size; yy++) {
						acc = red.rollup(acc, start.get(x+xx,y+yy));
					}
				}

				end.set(x/size, y/size, acc);
			}
		}
		return end;
	}
	
	/**Make a new set of aggregates with the same values in the same positions as the old one.
	 * This is a new set of aggregates, with a new set of backing data. 
	 * **/
	public static <A> Aggregates<A> copy(Aggregates<? extends A> source, A defVal) {
		Aggregates<A> target = make(source, defVal);
		for (int x=source.lowX(); x<source.highX(); x++) {
			for (int y=source.lowY(); y<source.highY(); y++) {
				target.set(x, y, source.get(x,y));
			}
		}
		return target;
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
