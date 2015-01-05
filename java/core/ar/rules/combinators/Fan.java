package ar.rules.combinators;

import java.util.function.BiFunction;

import ar.Aggregates;
import ar.Renderer;
import ar.Transfer;
import ar.aggregates.AggregateUtils;

/**Split (arbitrary width), apply transfer and merge results.
 * 
 * The aggregates are passed to each transfer function in the list
 * Order of execution and merge is unspecified.
 * 
 * All branches have to produce the same type of aggregates.
 * **/
public class Fan<IN,OUT> implements Transfer<IN,OUT> {
    protected final Transfer<IN,OUT>[] transfers;
    protected final BiFunction<Aggregates<OUT>,Aggregates<OUT>,Aggregates<OUT>> merge;
    protected final OUT empty;

    /**@param empty The empty value on the transfer
     * @param merge Operation to merge two sets of aggregates
     * @param transfers Transfers to apply.
     */
    @SafeVarargs
    public Fan(OUT empty, BiFunction<Aggregates<OUT>,Aggregates<OUT>,Aggregates<OUT>> merge, Transfer<IN, OUT>... transfers) {
        this.transfers = transfers;
        this.merge = merge;
        this.empty = empty;
    }

    
    @Override
    public OUT emptyValue() {return empty;}

    @Override
    public Specialized<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {
        return new Specialized<>(empty, merge, transfers, aggregates);
    }
    

    public static class Specialized<IN,OUT> 
    	extends Fan<IN,OUT> implements Transfer.Specialized<IN,OUT> {
    	
        protected final Transfer.Specialized<IN,OUT>[] specialized;

        @SuppressWarnings("unchecked")
		public Specialized(
				OUT empty, 
                BiFunction<Aggregates<OUT>,Aggregates<OUT>,Aggregates<OUT>> merge,
                Transfer<IN, OUT>[] transfers,
                Aggregates<? extends IN> aggs) {
            super(empty, merge, transfers);
            
            specialized = new Transfer.Specialized[transfers.length]; 
            for (int i=0; i< transfers.length; i++) {
            	specialized[i] = transfers[i].specialize(aggs);
            }
        }

        @Override
		public Aggregates<OUT> process(Aggregates<? extends IN> aggregates, Renderer rend) {
        	Aggregates<OUT> left = rend.transfer(aggregates, specialized[0]);
        	for (int i=1; i<specialized.length; i++) {
        		Aggregates<OUT> right = rend.transfer(aggregates, specialized[i]);
        		left = merge.apply(left, right);
        	}
        	return left;
        }
    }
    
    /**Utility to convert an element-wise function into a merge of a full set of aggregates.
     * 
     * NOTE: Default value is pulled from left argument, so the left argument must be built
     * with the default value from the transfer.  
     * 
     * TODO: Should this live in aggregateUtils instead?
     * **/
    public static final class AlignedMerge<A> implements BiFunction<Aggregates<A>, Aggregates<A>, Aggregates<A>> {
    	private final BiFunction<A,A,A> base;
    	
    	public AlignedMerge(BiFunction<A,A,A> base) {this.base = base;}
		
    	@Override
		public Aggregates<A> apply(Aggregates<A> l, Aggregates<A> r) {
    		return AggregateUtils.alignedMerge(l,r, l.defaultValue(), base);
		}
    }
}
