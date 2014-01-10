package ar.rules.combinators;

import ar.Aggregates;
import ar.Aggregator;
import ar.Renderer;
import ar.Transfer;
import ar.renderers.AggregationStrategies;

/**Split (arbitrary width), then merge...
 * 
 * All branches have to produce the same type...
 * **/
public class Fan<IN,OUT> implements Transfer<IN,OUT> {
    protected final Transfer<IN,OUT>[] transfers;
    protected final Merge<OUT> merge; 

    @SafeVarargs
    public Fan(Merge<OUT> merge, Transfer<IN, OUT>... transfers) {
        this.transfers = transfers;
        this.merge = merge;
    }
    
    @SafeVarargs
    public Fan(Aggregator<?, OUT> aggregator, Transfer<IN, OUT>... transfers) {
        this.transfers = transfers;
        this.merge = new AggregatorMerge<>(aggregator);
    }
    
    @Override
    public OUT emptyValue() {return merge.identity();}

    @Override
    public Specialized<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {
        return new Specialized<>(merge, transfers, aggregates);
    }
    

    public static class Specialized<IN,OUT> 
    	extends Fan<IN,OUT> implements Transfer.Specialized<IN,OUT> {
    	
        protected final Transfer.Specialized<IN,OUT>[] specialized;

        @SuppressWarnings("unchecked")
		public Specialized(
                Merge<OUT> merge,
                Transfer<IN, OUT>[] transfers,
                Aggregates<? extends IN> aggs) {
            super(merge, transfers);
            
            specialized = new Transfer.Specialized[transfers.length]; 
            for (int i=0; i< transfers.length; i++) {
            	specialized[i] = transfers[i].specialize(aggs);
            }
        }

        @Override
		public Aggregates<OUT> process(Aggregates<? extends IN> aggregates, Renderer rend) {
        	Aggregates<OUT> left = null;
        	for (int i=0; i<specialized.length; i++) {
        		Aggregates<OUT> right = rend.transfer(aggregates, specialized[i]);
        		left = merge.merge(left, right);
        	}
        	return left;
        }
    }

    
    /**Extension of the Aggregator merging concept to work at the aggregates level,
     * not the individual aggregate value level.  This is may be used to preserve metadata.
     * 
     * WARNING: The existing types system does not guarantee a specific aggregates-set type at any time.
     * Therefore, if merging is aggregates-type specific, tests MUST be made and unexpected aggregates-set
     * types should not result in an exception. 
     * 
     * TODO: Investigate adding the aggregates type as a parameter...probably requires having transfers parameterized by aggregate return type as well...
     * TODO: Should this interface be pushed back into the Aggregator, and item-wise aggregation be a special case?  (Similar to Transfer.Specialized vs Transfer.ItemWise).
     * TODO: Investigate merge into a new type...merge(AGG1 acc, AGG2 new)  
     *
     * @param <IN>
     * @param <OUT>
     */
    public static interface Merge<A>  {
    	public Aggregates<A> merge(Aggregates<A> left, Aggregates<A> right);
    	public A identity();
    }

    /**Simple wrapper for an aggregator's 'rollup' to be used as the merge strategy.**/
    public static class AggregatorMerge<A> implements Merge<A> {
    	protected final Aggregator<?, A> aggregator;    	
    	public AggregatorMerge(Aggregator<?, A> aggregator) {this.aggregator = aggregator;}
		@Override public A identity() {return aggregator.identity();}
		
		@Override
		public Aggregates<A> merge(Aggregates<A> left, Aggregates<A> right) {
    		return AggregationStrategies.horizontalRollup(left, right, aggregator);
		}
    }

}
