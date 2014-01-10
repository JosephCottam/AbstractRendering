package ar.rules.combinators;

import ar.Aggregates;
import ar.Renderer;
import ar.Transfer;
import ar.renderers.ParallelRenderer;

//TODO: Investigate if specialize generally should take a renderer as an argument...

public class Seq<IN,MID,OUT> implements Transfer<IN,OUT> {
	private static final Renderer SHARED_RENDERER = new ParallelRenderer(); 
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
    public <OUT2> Seq<IN,?,OUT2> then(Transfer<OUT,OUT2> next) {return new Seq<>(this, next);}
    
    public static <IN, OUT> SeqStart<IN,OUT> start(Renderer rend, Transfer<IN,OUT> start) {return new SeqStart<>(rend, start);}
    public static <IN, OUT> SeqStart<IN,OUT> start(Transfer<IN,OUT> start) {return new SeqStart<>(null, start);}
    
    public static class SeqStart<IN,OUT> implements Transfer<IN,OUT> {
    	public final Transfer<IN,OUT> base;
    	public final Renderer rend;

    	public SeqStart(Renderer rend, Transfer<IN, OUT> base) {
    		this.base = base;
    		this.rend = rend;
    	}

		@Override
		public OUT emptyValue() {return base.emptyValue();}

		@Override
		public ar.Transfer.Specialized<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {
			return base.specialize(aggregates);
		}
		
	    public <OUT2> Seq<IN,?,OUT2> then(Transfer<OUT,OUT2> next) {return new Seq<>(rend, base, next);}
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
