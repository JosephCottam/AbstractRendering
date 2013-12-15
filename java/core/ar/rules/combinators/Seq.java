package ar.rules.combinators;

import ar.Aggregates;
import ar.Renderer;
import ar.Resources;
import ar.Transfer;
import ar.util.CacheProvider;

public class Seq<IN,MID,OUT> implements Transfer<IN,OUT> {
    protected final Transfer<IN,MID> first;
    protected final Transfer<MID,OUT> second;
    protected final Renderer renderer;

    public Seq(Transfer<IN,MID> first, Transfer<MID,OUT> second) {this(Resources.DEFAULT_RENDERER, first, second);}
    public Seq(Renderer renderer, Transfer<IN,MID> first, Transfer<MID,OUT> second) {
        this.renderer = renderer;
        this.first = first;
        this.second = second;
    }
    @Override
    public OUT emptyValue() {return second.emptyValue();}

    @Override
    public Transfer.Specialized<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {
        return new Specialized<>(renderer, first, second, aggregates);
    }

    //TODO: Put some smarts here about caching.  
    //TODO: Maybe pair this up with chain (or get rid of chain, or make chain use this...) 
    public <OUT2> Seq<IN,?,OUT2> then(Transfer<OUT,OUT2> next) {return new Seq<>(this, next);}

    public static class Specialized<IN,MID,OUT> extends Seq<IN,MID, OUT> implements Transfer.Specialized<IN,OUT>,CacheProvider.CacheTarget<IN,OUT> {
        protected final Transfer.Specialized<IN,MID> first;
        protected final Transfer.Specialized<MID,OUT> second;
        protected final CacheProvider<IN,OUT> cache;

        public Specialized(final Renderer renderer,
                           final Transfer<IN, MID> first,
                           final Transfer<MID, OUT> second,
                           final Aggregates<? extends IN> aggs) {
            super(renderer, first, second);
            this.first = first.specialize(aggs);
            cache = new CacheProvider<>(this);

            Aggregates<MID> tempAggs = renderer.transfer(aggs, this.first);
            this.second = second.specialize(tempAggs);
            Aggregates<OUT> tempAggs2 = renderer.transfer(tempAggs, this.second);
            cache.set(aggs, tempAggs2);
        }

        @Override
        public OUT at(int x, int y, Aggregates<? extends IN> aggs) {
            return cache.get(aggs).get(x,y);
        }

        @Override
        public Aggregates<OUT> build(Aggregates<? extends IN> aggs) {
            Aggregates<MID> tempAggs1 = renderer.transfer(aggs, first);
            Aggregates<OUT> tempAggs2 = renderer.transfer(tempAggs1, second);
            return tempAggs2;
        }

        @Override
        public boolean localOnly() {
        	return first.localOnly() && second.localOnly();
        }
    }
}
