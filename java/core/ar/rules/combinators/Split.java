package ar.rules.combinators;

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
    protected final Merge<L,R,OUT> merge; 

    public Split(Transfer<IN, L> left, Transfer<IN,R> right, Merge<L,R, OUT> merge) {
        this.left = left;
        this.right = right;
        this.merge = merge;
    }
        
    @Override
    public OUT emptyValue() {return merge.identity();}

    @Override
    public Specialized<IN, L,R, OUT> specialize(Aggregates<? extends IN> aggregates) {
        return new Specialized<>(left, right, merge, aggregates);
    }
    

    public static class Specialized<IN,L,R,OUT> 
    	extends Split<IN,L,R,OUT> implements Transfer.Specialized<IN,OUT> {
    	
        protected final Transfer.Specialized<IN,L> left;
        protected final Transfer.Specialized<IN,R> right;

		public Specialized(
                final Transfer<IN, L> left,
                final Transfer<IN, R> right,
                final Merge<L,R, OUT> merge,
                final Aggregates<? extends IN> aggs) {
            super(left, right, merge);
             
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
        	
        	Aggregates<OUT> out = AggregateUtils.make(minX, minY, maxX, maxY, merge.identity());
        	for (int x=minX; x<maxX; x++) {
        		for (int y=minY; y<maxY;y ++) {
        			L l = ll.get(x,y);
        			R r = rr.get(x,y);
        			out.set(x, y, merge.merge(l, r));
        		}
        	}
        	
        	return out;
        }
    }
    

    /**Operator to merge a single value from a left-side aggregate and
     * a single value from a right-side aggregate.
     * 
     * TODO: Investigate this WRT the merge in Fan.  
     *       Maybe make into merge(Aggregates<L> and Aggregates<R>) with a
     *       default implementation that iterates the space and calls mergeOne on each L/R.
     *       But that might cause problems with vectorization by loop-in-loop issues.
     * TODO: Can an aligned flattening be expressed in java?  That might allow vectorization or at least reduce loop issues....  
     * 
     * **/
    public static interface Merge<L,R,OUT>  {
    	public OUT merge(L left, R right);
    	public OUT identity();
    }
}
