package ar.rules.combinators;

import java.util.function.BiFunction;
import java.util.function.Function;

import ar.Aggregates;
import ar.Renderer;
import ar.Transfer;
import ar.aggregates.AggregateUtils;

/**Split into a number paths based on a function then merge...
 * All branches will have the same transfer applied to them.
 * 
 * This is a data-driven fan.  
 * For comparison, the vanilla Fan width is determined by the number of transfers passed.
 * 
 * @param IN Aggregates at input
 * @param BRANCH Aggregates type in each branch of the fan
 * @param MID Aggregates type post transfer application in each branch 
 * @param OUT Aggregates type of the (merged) result 
 * **/
public class DynamicFan<IN,BRANCH,MID,OUT> implements Transfer<IN,OUT> {
    protected final Transfer<BRANCH, MID> transfer;
    protected final Function<Aggregates<? extends IN>, Aggregates<BRANCH>[]> splitter;
    protected final BiFunction<OUT, MID, OUT> merge;
    protected final OUT empty;

    public DynamicFan(Transfer<BRANCH, MID> transfer,
    				  Function<Aggregates<? extends IN>, Aggregates<BRANCH>[]> splitter,
    				  OUT empty,
    				  BiFunction<OUT, MID, OUT> merge) {
        this.transfer = transfer;
        this.merge = merge;
        this.splitter = splitter;
        this.empty = empty;
    }
        
    @Override
    public OUT emptyValue() {return empty;}

    @Override
    public Specialized<IN,BRANCH, MID, OUT> specialize(Aggregates<? extends IN> aggregates) {
        return new Specialized<>(transfer, splitter, empty, merge, aggregates);
    }
    

    public static class Specialized<IN, BRANCH, MID, OUT> 
    	extends DynamicFan<IN,BRANCH, MID, OUT> implements Transfer.Specialized<IN,OUT> {
    	
        protected final Transfer.Specialized<BRANCH,MID>[] specialized;

        @SuppressWarnings("unchecked")
		public Specialized(Transfer<BRANCH, MID> transfer,
				  			Function<Aggregates<? extends IN>, Aggregates<BRANCH>[]> splitter,
				  			OUT empty,
				  			BiFunction<OUT, MID, OUT> merge,
				  			Aggregates<? extends IN> aggs) {
            super(transfer,  splitter, empty, merge);
            
            Aggregates<BRANCH>[] splitAggs = splitter.apply(aggs);
            
            specialized = new Transfer.Specialized[splitAggs.length]; 
            for (int i=0; i< splitAggs.length; i++) {
            	specialized[i] = transfer.specialize(splitAggs[i]);
            }
        }

        @Override
		public Aggregates<OUT> process(Aggregates<? extends IN> aggregates, Renderer rend) {
            Aggregates<BRANCH>[] splitAggs = splitter.apply(aggregates); 

            Aggregates<OUT> acc = AggregateUtils.make(1, 1, empty);
        	for (int i=0; i<specialized.length; i++) {
        		Aggregates<MID> right = rend.transfer(splitAggs[i], specialized[i]);
        		acc = AggregateUtils.alignedMerge(acc, right, empty, merge);
        	}
        	return acc;
        }
    }
    
    public static interface Split<IN, OUT> {
    	public Aggregates<OUT>[] split(Aggregates<? extends IN> aggs);
    }
}
