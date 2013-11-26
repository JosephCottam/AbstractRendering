package ar.rules.combinators;

import ar.Aggregates;
import ar.Resources;
import ar.Transfer;
import ar.Renderer;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.CacheProvider;

/** Repeatedly apply a transfer function until a predicate passes.
 *
 *  The actual iterative calculations are done on the entire set of aggregates,
 *  so the work is done once and cached.  Specialization is only done ONCE.
 *
 * TODO: Develop a specialize-on-each-iteration variant
 * @param <IN>
 */
public class While<IN> implements Transfer<IN,IN> {
    protected final Transfer<IN,IN> base;
    protected final Valuer<Aggregates<? extends IN>, Boolean> pred;
    protected final Renderer renderer;

    public While(Valuer<Aggregates<? extends IN>, Boolean> pred, Transfer<IN,IN> base) {
        this(Resources.DEFAULT_RENDERER, pred, base);
    }

    public While(Renderer renderer, Valuer<Aggregates<? extends IN>, Boolean> pred, Transfer<IN,IN> base) {
        this.renderer=renderer;
        this.base=base;
        this.pred=pred;
    }
    public IN emptyValue() {return base.emptyValue();}
    public Specialized<IN> specialize(Aggregates<? extends IN> aggregates) {
        return new Specialized<>(renderer, base, pred, aggregates);
    }

    public static class Specialized<IN> extends While<IN> implements Transfer.Specialized<IN,IN>, CacheProvider.CacheTarget<IN,IN> {
        protected final CacheProvider<IN,IN> cache;
        protected final Transfer.Specialized<IN,IN> op;


        public Specialized(Renderer renderer, Transfer<IN,IN> base, Valuer<Aggregates<? extends IN>, Boolean> pred, Aggregates<? extends IN> aggs) {
            super(renderer, pred, base);
            this.cache = new CacheProvider<>(this);
            op = base.specialize(aggs);
        }

        public Aggregates<? extends IN> build(Aggregates<? extends IN> aggs) {
            while (!pred.value(aggs)) {aggs = renderer.transfer(aggs, op);}
            return aggs;
        }

        @Override
        public IN at(int x, int y, Aggregates<? extends IN> aggregates) {
            return cache.get(aggregates).get(x,y);
        }
    }
}
