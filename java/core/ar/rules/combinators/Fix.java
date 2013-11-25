package ar.rules.combinators;

import ar.Aggregates;
import ar.Transfer;
import ar.Renderer;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.CacheProvider;

/** Calculate a fixed point by repeatedly applying a function
 *  until the predicate returns true.
 *
 *  The actual fixed-point calculation is done for the entire set of aggregates,
 *  so the work is done once and cached.  Specialization is only done ONCE.
 *
 * TODO: Develop a specialize-on-each-iteration variant
 * @param <IN>
 */
public class Fix<IN> implements Transfer<IN,IN> {
    protected final Transfer<IN,IN> base;
    protected final Valuer<Aggregates<? extends IN>, Boolean> pred;
    protected final Renderer renderer;

    public Fix(Valuer<Aggregates<? extends IN>, Boolean> pred, Transfer<IN,IN> base) {
        this(Resources.DEFAULT_RENDERER, pred, base);
    }

    public Fix(Renderer renderer, Valuer<Aggregates<? extends IN>, Boolean> pred, Transfer<IN,IN> base) {
        this.renderer=renderer;
        this.base=base;
        this.pred=pred;
    }
    public IN emptyValue() {return base.emptyValue();}
    public Specialized<IN> specialize(Aggregates<? extends IN> aggregates) {
        return new Specialized<>(renderer, base, pred, aggregates);
    }

    public static class Specialized<IN> extends Fix<IN> implements Transfer.Specialized<IN,IN>, CacheProvider.CacheTarget<IN,IN> {
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
