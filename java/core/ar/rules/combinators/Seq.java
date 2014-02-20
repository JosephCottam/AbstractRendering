package ar.rules.combinators;

import ar.Aggregates;
import ar.Renderer;
import ar.Transfer;
import ar.renderers.ParallelRenderer;

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
        return new Specialized<>(rend, first, second, aggregates);
    }
    
    /**Extend the sequence of transfers with a new step.**/ 
    public <OUT2> Seq<IN,?,OUT2> then(Transfer<OUT,OUT2> next) {return new Seq<>(this, next);}
    
    /**Create a new sequence with the given transfer and renderer.**/
    public static <IN, OUT> SeqStart<IN,OUT> start(Renderer rend, Transfer<IN,OUT> start) {return new SeqStart<>(rend, start);}
    
    /**Create a new sequence with the given transfer and default renderer.**/
    public static <IN, OUT> SeqStart<IN,OUT> start(Transfer<IN,OUT> start) {return new SeqStart<>(null, start);}
    
    /**Initiates a sequence with a single transfer function.
     * Behaves just like the provided transfer, but can be extended with another transfer with the 'then' method.
     * **/
    public static class SeqStart<IN,OUT> extends Seq<IN, OUT, OUT> {
    	public SeqStart(Renderer rend, Transfer<IN, OUT> base) {
    		super(rend, base,null);
    	}

		@Override public OUT emptyValue() {return first.emptyValue();}

		@Override
		public ar.Transfer.Specialized<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {
			return first.specialize(aggregates);
		}
		
	    @Override public <OUT2> Seq<IN,?,OUT2> then(Transfer<OUT,OUT2> next) {return new Seq<>(rend, first, next);}
    }

    public static class Specialized<IN,MID,OUT> extends Seq<IN,MID, OUT> implements Transfer.Specialized<IN,OUT> {
        protected final Transfer.Specialized<IN,MID> first;
        protected final Transfer.Specialized<MID,OUT> second;

        public Specialized(final Renderer rend,
        				   final Transfer<IN, MID> first,
                           final Transfer<MID, OUT> second,
                           final Aggregates<? extends IN> aggs) {
            super(first, second);
            this.first = first.specialize(aggs);

            Aggregates<MID> tempAggs = rend.transfer(aggs, this.first); 
            this.second = second.specialize(tempAggs);
        }

		@Override
		public Aggregates<OUT> process(Aggregates<? extends IN> aggs, Renderer rend) {
            Aggregates<MID> tempAggs1 = rend.transfer(aggs, first);
            Aggregates<OUT> tempAggs2 = rend.transfer(tempAggs1, second);
            return tempAggs2;
		}
    }
}
