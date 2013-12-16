package ar.rules.combinators;

import ar.Aggregates;
import ar.Renderer;
import ar.Transfer;
import ar.util.CacheProvider;

/**Execute a sequence of transfers.
 * 
 * A transfer function takes a set of aggregates and produces a set of aggregates.
 * In the simple case, the aggregates produced are colors, but they need not be.
 * Chaining to transfers together is simply piping the output of one to the input
 * of the other (conceptually).  This class takes care of that piping.
 * 
 * Specialization cost is includes executing the entire transfer chain for 
 * all points in the specialization aggregates and is paid at specialization time.
 * Therefore, specialization is probably more expensive than in most single-step 
 * or in nested transfers.
 * 
 * Furthermore, the first transfer performed will incur the cost of the whole chain
 * across all aggregates to accommodate locality-aware transfers.
 * 
 * TODO: Can this be done in a type-safe way?
 * 
 * @param <IN> Input type to the first transfer
 * @param <OUT> Output type of the last transfer
 */
@SuppressWarnings("unchecked")
public class Chain<IN,OUT> implements Transfer<IN,OUT>{
	private final Transfer<?,?>[] transfers;
	protected final Renderer renderer;
	
	/**
	 * 
	 * @param renderer Rendering resources used for specialization of intermediate transfers
	 * @param transfers
	 */
	public Chain(Renderer renderer, Transfer<?, ?>... transfers) {
		this.transfers=transfers;
		this.renderer = renderer;
	}
	
	/**Default output is the default of the last item in the chain.**/
	@Override
	public OUT emptyValue() {return (OUT) transfers[transfers.length-1].emptyValue();}

	@Override
	public Specialized<IN, OUT> specialize(
			Aggregates<? extends IN> aggregates) {
		return new Specialized<>(renderer, aggregates, transfers);
	}

	
	@SuppressWarnings("rawtypes")
	protected static class Specialized<IN,OUT> extends Chain<IN,OUT> implements Transfer.Specialized<IN, OUT>,CacheProvider.CacheTarget {
		private final Transfer.Specialized[] specialized;
        private final CacheProvider<IN,OUT> cache;
		
		public Specialized(Renderer renderer, Aggregates rootAggregates, Transfer... transfers) {
			super(renderer, transfers);
            cache = new CacheProvider(this);

			specialized = new Transfer.Specialized[transfers.length];
			Aggregates tempAggs = rootAggregates;
			for (int i=0; i<transfers.length; i++) {
				specialized[i] = transfers[i].specialize(tempAggs);
				tempAggs = renderer.transfer(tempAggs, specialized[i]);
			}
            cache.set(rootAggregates, tempAggs);
		}

		@Override
		public OUT at(int x, int y, Aggregates<? extends IN> aggs) {return cache.get(aggs).get(x,y);}

        @Override
        public Aggregates build(Aggregates aggs) {
            Aggregates tempAggs = aggs;
            for (Transfer.Specialized ts: specialized) {
                tempAggs = renderer.transfer(tempAggs, ts);
            }
            return tempAggs;
        }
        
        public boolean localOnly() {return false;}
    }
}
