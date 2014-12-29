package ar.rules.combinators;

import ar.Aggregates;
import ar.Renderer;
import ar.Transfer;
import ar.renderers.ParallelRenderer;
import ar.rules.General;

/**Do one transfer, then pipe its results into another.
 * The first transfer finishes before the second one starts.
 *
 * @param <IN> The expected input type
 * @param <MID> The result type of the first transfer and input type of the second 
 * @param <OUT> Output type of the second transfer
 */
public class Seq<IN,MID,OUT> implements Transfer<IN,OUT> {
	public static final Renderer SHARED_RENDERER = new ParallelRenderer(); 
    protected final Transfer<IN,MID> first;
    protected final Transfer<MID,OUT> second;
    protected final Renderer rend;

    public Seq(Transfer<IN,MID> first, Transfer<MID,OUT> second) {this(null, first, second);}
    public Seq(Renderer rend, Transfer<IN,MID> first, Transfer<MID,OUT> second) {
        this.first = first;
        this.second = second;
        this.rend = rend == null ? SHARED_RENDERER : rend;
    }
    
    @Override public OUT emptyValue() {return second.emptyValue();}

    @Override
    public Transfer.Specialized<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {
        Transfer.Specialized<IN, MID> f = first.specialize(aggregates);

        Aggregates<MID> tempAggs = rend.transfer(aggregates, f); 
        Transfer.Specialized<MID,OUT> s = second.specialize(tempAggs);

        return new Specialized<>(f,s);
    }
      
    public static class Specialized<IN,MID,OUT> extends Seq<IN,MID, OUT> implements Transfer.Specialized<IN,OUT> {
        protected final Transfer.Specialized<IN,MID> first;
        protected final Transfer.Specialized<MID,OUT> second;

        public Specialized(final Transfer.Specialized<IN, MID> first,
                           final Transfer.Specialized<MID, OUT> second) {
            super(first, second);
            this.first = first;
            this.second = second;
        }

		@Override
		public Aggregates<OUT> process(Aggregates<? extends IN> aggs, Renderer rend) {
            Aggregates<MID> tempAggs1 = rend.transfer(aggs, first);
            Aggregates<OUT> tempAggs2 = rend.transfer(tempAggs1, second);
            return tempAggs2;
		}
    }
    
    /**Extend the sequence of transfers with a new step.**/ 
    public <OUT2> Seq<IN,?,OUT2> then(Transfer<OUT,OUT2> next) {return new Seq<>(this, next);}
    
    /**Full empty start.  Behaves like echo if applied.**/
    @SuppressWarnings({"rawtypes", "unchecked"})
	public static class SeqEmpty implements Transfer {
    	private final Renderer rend;
		public SeqEmpty(Renderer rend) {this.rend = rend;}
		public <IN, OUT> SeqStub<IN,OUT> then(Transfer<IN,OUT> next) {return new SeqStub<>(rend, next);}
		@Override public Object emptyValue() {return null;}
		@Override public Specialized specialize(Aggregates aggregates) {return new General.Echo(aggregates.defaultValue());}
    }
    
    /** Initiates a sequence with a single transfer function.
     *  Behaves just like the provided transfer, but can be extended with another transfer with the 'then' method.
     * **/
    public static class SeqStub<IN,OUT> implements Transfer<IN, OUT> {
    	private final Renderer rend;
    	private final Transfer<IN,OUT> base;
    	
    	public SeqStub(Renderer rend, Transfer<IN, OUT> base) {
    		this.rend = rend;
    		this.base = base; 
    	}

    	public <OUT2> Seq<IN,OUT,OUT2> then(Transfer<OUT,OUT2> next) {return new Seq<>(rend, base, next);}

		@Override public OUT emptyValue() {return base.emptyValue();}

		@Override
		public ar.Transfer.Specialized<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {
			return base.specialize(aggregates);
		}
		
    }

}
