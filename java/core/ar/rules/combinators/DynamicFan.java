package ar.rules.combinators;

import ar.Aggregates;
import ar.Aggregator;
import ar.Renderer;
import ar.Transfer;
import ar.rules.combinators.Fan.Merge;
import ar.rules.combinators.Fan.AggregatorMerge;

/**Split into a number paths based on a function then merge...
 * All branches will have the same transfer applied to them.
 * **/
public class DynamicFan<IN,SPLIT,OUT> implements Transfer<IN,OUT> {
    protected final Transfer<SPLIT,OUT> transfer;
    protected final Merge<OUT> merge;
    protected final Split<IN, SPLIT> split;

    public DynamicFan(Split<IN, SPLIT> split, Transfer<SPLIT, OUT> transfer, Merge<OUT> merge) {
        this.transfer = transfer;
        this.merge = merge;
        this.split = split;
    }
    
    public DynamicFan(Split<IN, SPLIT> split,  Transfer<SPLIT,OUT> transfer, Aggregator<?, OUT> aggregator) {
    	this(split, transfer, new AggregatorMerge<>(aggregator));
    }
    
    @Override
    public OUT emptyValue() {return merge.identity();}

    @Override
    public Specialized<IN,SPLIT,OUT> specialize(Aggregates<? extends IN> aggregates) {
        return new Specialized<>(split, transfer, merge, aggregates);
    }
    

    public static class Specialized<IN,SPLIT,OUT> 
    	extends DynamicFan<IN,SPLIT,OUT> implements Transfer.Specialized<IN,OUT> {
    	
        protected final Transfer.Specialized<SPLIT,OUT>[] specialized;

        @SuppressWarnings("unchecked")
		public Specialized(
                Split<IN, SPLIT> split,
                Transfer<SPLIT, OUT> transfer,
                Merge<OUT> merge,
                Aggregates<? extends IN> aggs) {
            super(split, transfer, merge);
            
            Aggregates<SPLIT>[] splitAggs = split.split(aggs);
            
            specialized = new Transfer.Specialized[splitAggs.length]; 
            for (int i=0; i< splitAggs.length; i++) {
            	specialized[i] = transfer.specialize(splitAggs[i]);
            }
        }

        @Override
		public Aggregates<OUT> process(Aggregates<? extends IN> aggregates, Renderer rend) {
            Aggregates<SPLIT>[] splitAggs = split.split(aggregates); 

        	Aggregates<OUT> left = rend.transfer(splitAggs[0], specialized[0]);
        	for (int i=1; i<specialized.length; i++) {
        		Aggregates<OUT> right = rend.transfer(splitAggs[i], specialized[i]);
        		left = merge.merge(left, right);
        	}
        	return left;
        }
    }
    
    public static interface Split<IN, OUT> {
    	public Aggregates<OUT>[] split(Aggregates<? extends IN> aggs);
    }
}
