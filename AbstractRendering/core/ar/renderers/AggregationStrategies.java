package ar.renderers;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyph;
import ar.Glyphset;
import ar.aggregates.ConstantAggregates;
import ar.aggregates.FlatAggregates;
import ar.util.Util;


/**Core iteration strategies for build aggregates from various source values.
 * These are used internally by various renderers and probably only
 * need to be used if you are implementing a renderer.*/
public class AggregationStrategies {
	/**Combine two aggregate sets according to the passed reducer.
	 * 
	 * The resulting aggregate set will have a realized subset region sufficient to
	 * cover the realized subset region of both source aggregate sets (regardless of 
	 * the values found in those sources).  If one of the two aggregate sets provided
	 * is already of sufficient size, it will be used as both a source and a target.
	 * 
	 * 
	 * @param left Aggregate set to use for left-hand arguments
	 * @param right Aggregate set to use for right-hand arguments
	 * @param red Reduction operation
	 * @return Resulting aggregate set (may be new or a destructively updated left or right parameter) 
	 */
	public static <T> Aggregates<T> horizontalRollup(Aggregates<T> left, Aggregates<T> right, Aggregator<?,T> red) {
		if (left == null) {return right;}
		if (right == null) {return left;}

		T identity = red.identity();

		if ((left instanceof ConstantAggregates) && Util.isEqual(identity, left.defaultValue())) {return right;}
		if ((right instanceof ConstantAggregates) && Util.isEqual(identity, right.defaultValue())) {return right;}

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
			target = new FlatAggregates<T>(bounds.x, bounds.y, bounds.x+bounds.width, bounds.y+bounds.height, red.identity());
		}

		//TODO: Is there a faster way to do this?  Maybe the inner-list creation can be avoided by changing rollup to take two values
		for (Aggregates<T> source: sources) {
			for (int x=Math.max(0, source.lowX()); x<source.highX(); x++) {
				for (int y=Math.max(0, source.lowY()); y<source.highY(); y++) {
					
					T comb = red.rollup(Arrays.asList(target.get(x,y), source.get(x,y)));
					target.set(x,y, comb); 
				}
			}
		}
		return target;
	}

	/**Performs a nxn fold-up of the passed aggregate set.
	 * 
	 * The incoming aggregates are tessellated with a nxn grid.
	 * Over-the-edge-values for the tesselation are taken as  
	 * 
	 * TODO: Add fractional value support (signature becomes <IN, OUT> Aggregates<OUT> vr(Aggregates<IN>, Aggregator<?, IN>, Fractioner<IN,OUT>)"; Default fractioner is "majority"
	 * 
	 * @param factor Requested roll-up factor.  
	 * **/
	public static <T> Aggregates<T> verticalRollup(Aggregates<T> start, Aggregator<?,T> red, double factor) {
		int size = (int) Math.round(factor);
		if (size < 1) {return start;}
		Aggregates<T> end = new FlatAggregates<T>(start.lowX()/size, start.lowY()/size, start.highX()/size, start.highY()/size, red.identity());

		ArrayList<T> l = new ArrayList<>();
		for (int x = start.lowX(); x < start.highX(); x=x+size) {
			for (int y=start.lowY(); y < start.highY(); y=y+size) {

				l.clear();
				for (int xx=0; xx<size; xx++) {
					for (int yy=0; yy<size; yy++) {
						l.add(start.get(x+xx,y+yy));
					}
				}

				T value = red.rollup(l);
				end.set(x/size, y/size, value);
			}
		}
		return end;
	}

	
	/**Rollup until the aggregates are close to the requested width and height, then subset.
	 * 
	 * "Close" is a width or height where halving them again would make the size below the requested width/height.
	 * 
	 * @param start Initial set of aggregates
	 * @param red Aggregator operation to use for rollup
	 * @param targetWidth Width to target
	 * @param targetHeight Height to target
	 * @return Aggregates of the requested width/height
	 */
	public static <T> Aggregates<T> verticalRollup(Aggregates<T> start, Aggregator<?,T> red, int targetWidth, int targetHeight) {
		final int startWidth = start.highX()-start.lowX();
		final int startHeight = start.highY()-start.lowY();		
		final double wspan = startWidth/(double) targetWidth;
		final double hspan = startHeight/(double) targetHeight;
		int span = (int) Math.ceil(Math.max(wspan, hspan));
 		return verticalRollup(start, red, span);
	}

	
	
	/**Perform aggregation for a single pixel.**/
	public static <A,V> A pixel(
			Aggregates<A> aggregates, 
			Aggregator<V,A> op,
			Glyphset<? extends V> glyphset,
			AffineTransform inverseView, 
			int x, int y) {
		
		//TODO: investigate a special-purpose rectangle transform
		//TODO: investigate taking in a rectangle to avoid per-loop iteration allocation
		Rectangle2D pixel = new Rectangle(x,y,1,1);
		pixel = inverseView.createTransformedShape(pixel).getBounds2D(); 
		
		Collection<? extends Glyph<? extends V>> glyphs = glyphset.intersects(pixel);
		A acc = aggregates.get(x, y);
		for (Glyph<? extends V> g: glyphs) {
			V val = g.value();
			acc = op.combine(x, y, acc, val);
		}
		return acc;

	}
}
