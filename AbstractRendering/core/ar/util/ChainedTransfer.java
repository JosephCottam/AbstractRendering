package ar.util;

import ar.Aggregates;
import ar.Renderer;
import ar.Transfer;




/**Execute a sequence of transfers.
 * 
 * Specialization cost is includes executing the entire transfer chain for 
 * all points in the specialization aggregates and is paid at specialization time.
 * Therefore, specialization is probably more expensive than in most single-step 
 * or in nested transfers.
 * 
 * Furthermore, the first transfer performed will incur the cost of the whole chain
 * across all aggregates to accommodate locality-aware transfers.
 * 
 * TODO: Replace ARServer.execute code with this
 * TODO: Can this be done in a type-safe way?
 * 
 * @param <IN> Input type to the first transfer
 * @param <OUT> Output type of the last transfer
 */
@SuppressWarnings("unchecked")
public class ChainedTransfer<IN,OUT> implements Transfer<IN,OUT>{
	private final Transfer<?,?>[] transfers;
	protected final Renderer renderer;
	
	/**
	 * 
	 * @param renderer Rendering resources used for specialization of intermediate transfers
	 * @param transfers
	 */
	public ChainedTransfer(Renderer renderer, Transfer<?,?>... transfers) {
		this.transfers=transfers;
		this.renderer = renderer;
	}
	
	@Override
	public OUT emptyValue() {return (OUT) transfers[0].emptyValue();}

	@Override
	public Specialized<IN, OUT> specialize(
			Aggregates<? extends IN> aggregates) {
		return new Specialized<>(renderer, aggregates, transfers);
	}

	
	@SuppressWarnings("rawtypes")
	protected static class Specialized<IN,OUT> extends ChainedTransfer<IN,OUT> implements Transfer.Specialized<IN, OUT>  {
		private final Transfer.Specialized[] specialized;
		private final Object cacheGuard = new Object() {};
		private Aggregates cacheKey;
		private Aggregates cachedAggs;
		
		public Specialized(Renderer renderer, Aggregates rootAggregates, Transfer... transfers) {
			super(renderer, transfers);
			specialized = new Transfer.Specialized[transfers.length];
			
			Aggregates tempAggs = rootAggregates;
			for (int i=0; i<transfers.length; i++) {
				specialized[i] = transfers[i].specialize(tempAggs);
				tempAggs = renderer.transfer(tempAggs, specialized[i]);
			}
			
			//Store the results of specialization in case the whole set of aggregates was sent for specialization 
			cacheKey = rootAggregates;
			cachedAggs = tempAggs;
		}

		@Override
		public OUT at(int x, int y, Aggregates<? extends IN> rootAggregates) {
			synchronized(cacheGuard) {
				if (cacheKey == null || cacheKey != rootAggregates) {
					Aggregates tempAggs = rootAggregates;
					for (Transfer.Specialized ts: specialized) {
						tempAggs = renderer.transfer(tempAggs, ts);
					}
					cachedAggs = tempAggs;
				}
				cacheKey = rootAggregates;
			}
			return (OUT) cachedAggs.get(x,y);
		}
	}
}
