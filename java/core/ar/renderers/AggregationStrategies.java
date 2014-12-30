package ar.renderers;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import ar.Aggregates;
import ar.Aggregator;
import ar.aggregates.AggregateUtils;
import ar.aggregates.implementations.ConstantAggregates;
import ar.rules.combinators.Split.Merge;
import ar.util.Util;


/**Core iteration strategies for build aggregates from various source values.
 * These are used internally by various renderers and probably only
 * need to be used if you are implementing a renderer.*/
public class AggregationStrategies {
	
	/**Take a set of aggreagtes on the left and on the right, combine them with the merge.
	 * 
	 * This method differs from HorizontalRollup in that horizontal rollup in to significant ways.
	 * First, rollup assumes that left, right and end result are all of the same type, enabling certain optimizations.
	 * Second, rollup may destructively **modify** one of the passed aggregates. 
	 * 
	 * @param left
	 * @param right
	 * @param merge
	 * @return
	 */
	public static <L,R,OUT> Aggregates<OUT> horizontalMerge(Aggregates<L> left, Aggregates<R> right, Merge<L,R,OUT> merge) {
		Rectangle rb = new Rectangle(right.lowX(), right.lowY(), right.highX()-right.lowX(), right.highY()-right.lowY());
		Rectangle lb = new Rectangle(left.lowX(), left.lowY(), left.highX()-left.lowX(), left.highY()-left.lowY());
		Rectangle bounds = rb.union(lb);

		Aggregates<OUT> target = AggregateUtils.make((int) bounds.getMinX(), (int) bounds.getMinY(), 
													(int) bounds.getMaxX(), (int) bounds.getMaxY(),
													merge.identity());

		OUT identity = merge.identity();
		for (int x=Math.max(0, target.lowX()); x<target.highX(); x++) {
			for (int y=Math.max(0, target.lowY()); y<target.highY(); y++) {
				L l = left.get(x,y);
				R r = right.get(x,y);
				OUT v = merge.merge(l, r);
				if (Util.isEqual(identity, v)) {continue;}
				target.set(x,y, v); 
			}
		}
		return target;
	}
	
	
	/**Combine two aggregate sets according to the passed reducer. 
	 * Aligns coordinates between the two sets. 
	 * 
	 * The resulting aggregate set will have a realized subset region sufficient to
	 * cover the realized subset region of both source aggregate sets (regardless of 
	 * the values found in those sources).  If one of the two aggregate sets provided
	 * is already of sufficient size, it will be used as both a source and a target.
	 * Therefore, this may involve a **DESTRUCTIVE** update of one of the sets of aggregates.
	 * 
	 * 
	 * TODO: Move to GlyphParallelAggregation task with Java8 and hide.  Replace current other uses with horizontalMerge   
	 * 
	 * @param left Aggregate set to use for left-hand arguments
	 * @param right Aggregate set to use for right-hand arguments
	 * @param red Reduction operation
	 * @return Resulting aggregate set (may be new or a destructively updated left or right parameter) 
	 */
	@Deprecated
	public static <T> Aggregates<T> horizontalRollup(Aggregates<T> left, Aggregates<T> right, Aggregator<?,T> red) {
		if (left == null) {return right;}
		if (right == null) {return left;}

		T identity = red.identity();

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
					(int) bounds.getMaxX(), (int) bounds.getMaxY(), red.identity());
		}
	
		for (Aggregates<T> source: sources) {
			for (int x=Math.max(0, source.lowX()); x<source.highX(); x++) {
				for (int y=Math.max(0, source.lowY()); y<source.highY(); y++) {
					T newVal = source.get(x,y);
					if (Util.isEqual(identity, newVal)) {continue;}
					T comb = red.rollup(target.get(x,y), source.get(x,y));
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
}
