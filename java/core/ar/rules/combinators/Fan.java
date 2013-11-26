package ar.rules.combinators;

import ar.Aggregates;
import ar.Aggregator;
import ar.Renderer;
import ar.Transfer;
import ar.renderers.AggregationStrategies;
import ar.util.CacheProvider;

/**Split (arbitrary width), then merge...
 * 
 * TODO: To faciliate easier ad-hoc merge, have a single-method "combiner" interface instead of "Aggregator".  All Aggregator are combiners. 
 * **/
public class Fan<IN,OUT> implements Transfer<IN,OUT> {
    protected final Transfer<IN,OUT>[] transfers;
    protected final Aggregator<?, OUT> merge;
    protected final Renderer renderer;

    @SafeVarargs
	public Fan(Aggregator<?, OUT> merge, Transfer<IN, OUT>... transfers) {
        this(Resources.DEFAULT_RENDERER, merge, transfers);
    }

    @SafeVarargs
    public Fan(Renderer renderer, Aggregator<?, OUT> merge, Transfer<IN, OUT>... transfers) {
        this.renderer = renderer;
        this.transfers = transfers;
        this.merge = merge;
    }

    @Override
    public OUT emptyValue() {return merge.identity();}

    @Override
    public Specialized<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {
        return new Specialized<>(renderer, merge, transfers, aggregates);
    }

    public static class Specialized<IN,OUT> extends Fan<IN,OUT> implements Transfer.Specialized<IN,OUT>, CacheProvider.CacheTarget<IN,OUT> {
        protected final Transfer.Specialized<IN,OUT>[] specialized;
        protected final CacheProvider<IN,OUT> cache;

        @SuppressWarnings("unchecked")
		public Specialized(
                Renderer renderer,
                Aggregator<?, OUT> merge,
                Transfer<IN, OUT>[] transfers,
                Aggregates<? extends IN> aggs) {
            super(renderer, merge, transfers);
            
            specialized = new Transfer.Specialized[transfers.length]; 
            for (int i=0; i< transfers.length; i++) {
            	specialized[i] = transfers[i].specialize(aggs);
            }
            this.cache = new CacheProvider<>(this);
        }

        @Override
        public OUT at(int x, int y, Aggregates<? extends IN> aggregates) {
            return cache.get(aggregates).get(x,y);
        }

        @Override
        public Aggregates<? extends OUT> build(Aggregates<? extends IN> aggs) {
        	Aggregates<OUT> left = null;
        	for (int i=0; i<specialized.length; i++) {
        		Aggregates<OUT> right = renderer.transfer(aggs, specialized[i]);
        		left = AggregationStrategies.horizontalRollup(left, right, merge);
        	}
        	return left;
        }
    }


}
