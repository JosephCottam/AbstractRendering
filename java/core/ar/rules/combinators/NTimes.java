package ar.rules.combinators;

import ar.Aggregates;
import ar.Resources;
import ar.Transfer;
import ar.Renderer;
import ar.util.CacheProvider;

/** Apply an operator N times.
 * Specializes the operator each time around the loop.
 * */
public class NTimes<IN> implements Transfer<IN,IN> {
    protected final Transfer<IN,IN> base;
    protected final int n;
    protected final Renderer renderer;

    public NTimes(int n, Transfer<IN,IN> base) {
        this(Resources.DEFAULT_RENDERER, n, base);
    }

    public NTimes(Renderer renderer, int n, Transfer<IN,IN> base) {
        this.renderer=renderer;
        this.base=base;
        this.n = n;
    }
    public IN emptyValue() {return base.emptyValue();}
    public Specialized<IN> specialize(Aggregates<? extends IN> aggregates) {
        return new Specialized<>(renderer, base, n, aggregates);
    }

    public static class Specialized<IN> extends NTimes<IN> implements Transfer.Specialized<IN,IN>, CacheProvider.CacheTarget<IN,IN> {
        protected final CacheProvider<IN,IN> cache;
        protected final Transfer.Specialized<IN,IN> op;


        public Specialized(Renderer renderer, Transfer<IN,IN> base, int n, Aggregates<? extends IN> aggs) {
            super(renderer, n, base);
            this.cache = new CacheProvider<>(this);
            op = base.specialize(aggs);
        }

        public Aggregates<IN> build(Aggregates<? extends IN> aggs) {
        	Transfer.Specialized<IN, IN> spec = op.specialize(aggs);
            Aggregates<IN> out = renderer.transfer(aggs, spec);

            for (int i=0; i<n-1; i++){  //Did it once to initialize the loop
            	spec = op.specialize(out);
            	out = renderer.transfer(out, spec);
            }
            return out;
        }

        @Override
        public IN at(int x, int y, Aggregates<? extends IN> aggregates) {
            return cache.get(aggregates).get(x,y);
        }
    }
}
