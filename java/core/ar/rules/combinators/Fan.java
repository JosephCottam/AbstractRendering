package ar.rules.combinators;

import ar.Aggregates;
import ar.Aggregator;
import ar.Renderer;
import ar.Transfer;
import ar.renderers.AggregationStrategies;

/**Split (arbitrary width), then merge...**/
public class Fan<IN,OUT> implements Transfer<IN,OUT> {
    protected final Transfer<IN,OUT>[] transfers;
    protected final Aggregator<?, OUT> merge;	//Only rollup is used, so only OUT matters 

    @SafeVarargs
    public Fan(Aggregator<?, OUT> merge, Transfer<IN, OUT>... transfers) {
        this.transfers = transfers;
        this.merge = merge;
    }

    @Override
    public OUT emptyValue() {return merge.identity();}

    @Override
    public Specialized<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {
        return new Specialized<>(merge, transfers, aggregates);
    }

    public static class Specialized<IN,OUT> extends Fan<IN,OUT> implements Transfer.Specialized<IN,OUT> {
        protected final Transfer.Specialized<IN,OUT>[] specialized;

        @SuppressWarnings("unchecked")
		public Specialized(
                Aggregator<?, OUT> merge,
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
        		left = AggregationStrategies.horizontalRollup(left, right, merge);
        	}
        	return left;
        }
    }


}
