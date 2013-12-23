package ar.rules.combinators;

import ar.Aggregates;
import ar.Renderer;
import ar.Resources;
import ar.Transfer;

//TODO: Add a 'Seq.Start' to compliment 'Seq.then'.  You can then do Seq.start(t).then(a).then(b)
public class Seq<IN,MID,OUT> implements Transfer<IN,OUT> {
    protected final Transfer<IN,MID> first;
    protected final Transfer<MID,OUT> second;

    public Seq(Transfer<IN,MID> first, Transfer<MID,OUT> second) {
        this.first = first;
        this.second = second;
    }
    
    @Override public OUT emptyValue() {return second.emptyValue();}

    @Override
    public Transfer.Specialized<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {
        return new Specialized<>(first, second, aggregates);
    }

    //TODO: Put some smarts here about caching.  
    //TODO: Maybe pair this up with chain (or get rid of chain, or make chain use this...) 
    public <OUT2> Seq<IN,?,OUT2> then(Transfer<OUT,OUT2> next) {return new Seq<>(this, next);}

    public static class Specialized<IN,MID,OUT> extends Seq<IN,MID, OUT> implements Transfer.Specialized<IN,OUT> {
        protected final Transfer.Specialized<IN,MID> first;
        protected final Transfer.Specialized<MID,OUT> second;

        public Specialized(final Transfer<IN, MID> first,
                           final Transfer<MID, OUT> second,
                           final Aggregates<? extends IN> aggs) {
            super(first, second);
            this.first = first.specialize(aggs);

            Aggregates<MID> tempAggs = Resources.DEFAULT_RENDERER.transfer(aggs, this.first); //TODO: Maybe specialize needs to take the renderer as an argument...
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
