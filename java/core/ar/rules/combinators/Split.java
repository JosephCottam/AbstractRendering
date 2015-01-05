package ar.rules.combinators;

import java.util.function.BiFunction;

import ar.Aggregates;
import ar.Renderer;
import ar.Transfer;
import ar.aggregates.AggregateUtils;

/**Split into two transfers then merge.
 * Branches may produce different types of output.
 * **/
public class Split<IN,L,R,OUT> implements Transfer<IN,OUT> {
    protected final Transfer<IN,L> left;
    protected final Transfer<IN,R> right;
    protected final OUT empty;
    protected final BiFunction<L,R,OUT> merge; 

    public Split(Transfer<IN, L> left, Transfer<IN,R> right, OUT empty, BiFunction<L,R,OUT> merge) {
        this.left = left;
        this.right = right;
        this.empty = empty;
        this.merge = merge;
    }
        
    @Override
    public OUT emptyValue() {return empty;}

    @Override
    public Specialized<IN, L,R, OUT> specialize(Aggregates<? extends IN> aggregates) {
        return new Specialized<>(left, right, empty, merge, aggregates);
    }
    

    public static class Specialized<IN,L,R,OUT> 
    	extends Split<IN,L,R,OUT> implements Transfer.Specialized<IN,OUT> {
    	
        protected final Transfer.Specialized<IN,L> left;
        protected final Transfer.Specialized<IN,R> right;

		public Specialized(
                final Transfer<IN, L> left,
                final Transfer<IN, R> right,
                final OUT empty,
                BiFunction<L,R,OUT> merge,
                final Aggregates<? extends IN> aggs) {
            super(left, right, empty, merge);
             
            this.left = left.specialize(aggs);
            this.right = right.specialize(aggs);
        }

        @Override
		public Aggregates<OUT> process(Aggregates<? extends IN> aggregates, Renderer rend) {
        	Aggregates<L> ll = rend.transfer(aggregates, left);
        	Aggregates<R> rr = rend.transfer(aggregates, right);
        	
        	int minX = Math.min(ll.lowX(), rr.lowX());
        	int minY = Math.min(ll.lowY(), rr.lowY());
        	int maxX = Math.max(ll.highX(), rr.highX());
        	int maxY = Math.max(ll.highY(), rr.highY());
        	
        	Aggregates<OUT> out = AggregateUtils.make(minX, minY, maxX, maxY, empty);
        	for (int x=minX; x<maxX; x++) {
        		for (int y=minY; y<maxY;y ++) {
        			L l = ll.get(x,y);
        			R r = rr.get(x,y);
        			out.set(x, y, merge.apply(l, r));
        		}
        	}
        	
        	return out;
        }
    }
}
