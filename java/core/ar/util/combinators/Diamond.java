package ar.util.combinators;

import ar.Aggregates;
import ar.Aggregator;
import ar.Renderer;
import ar.Transfer;
import ar.renderers.AggregationStrategies;
import ar.util.CacheProvider;

/**Split, then merge...**/
public class Diamond<IN,OUT> implements Transfer<IN,OUT> {
    protected final Transfer<IN,OUT> left;
    protected final Transfer<IN,OUT> right;
    protected final Aggregator<?, OUT> merge;
    protected final Renderer renderer;

    public Diamond(Transfer<IN, OUT> left, Transfer<IN, OUT> right, Aggregator<?, OUT> merge) {
        this(Resources.DEFAULT_RENDERER, left, right, merge);
    }

    public Diamond(Renderer renderer, Transfer<IN, OUT> left, Transfer<IN, OUT> right, Aggregator<?, OUT> merge) {
        this.renderer = renderer;
        this.left = left;
        this.right = right;
        this.merge = merge;
    }

    @Override
    public OUT emptyValue() {return merge.identity();}

    @Override
    public Specialized<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {
        return new Specialized<>(renderer, left, right, merge, aggregates);
    }

    public static class Specialized<IN,OUT> extends Diamond<IN,OUT> implements Transfer.Specialized<IN,OUT>, CacheProvider.CacheTarget<IN,OUT> {
        protected final Transfer.Specialized<IN,OUT> left;
        protected final Transfer.Specialized<IN,OUT> right;
        protected final CacheProvider<IN,OUT> cache;

        public Specialized(
                Renderer renderer,
                Transfer<IN, OUT> left,
                Transfer<IN, OUT> right,
                Aggregator<?, OUT> merge,
                Aggregates<? extends IN> aggs) {
            super(renderer, left, right, merge);
            this.left = left.specialize(aggs);
            this.right = right.specialize(aggs);
            this.cache = new CacheProvider<>(this);
        }

        @Override
        public OUT at(int x, int y, Aggregates<? extends IN> aggregates) {
            return cache.get(aggregates).get(x,y);
        }

        @Override
        public Aggregates<? extends OUT> build(Aggregates<? extends IN> aggs) {
            Aggregates<OUT> lAggs = renderer.transfer(aggs, left);
            Aggregates<OUT> rAggs = renderer.transfer(aggs, right);
            return AggregationStrategies.horizontalRollup(lAggs, rAggs, merge);
        }
    }


}
