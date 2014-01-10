package ar.rules.combinators;

import ar.Aggregates;
import ar.Transfer;
import ar.Renderer;
import ar.glyphsets.implicitgeometry.Valuer;

/** Repeatedly apply a transfer function until a predicate passes.
 *
 *  The actual iterative calculations are done on the entire set of aggregates,
 *  so the work is done once and cached.  Specialization is only done ONCE.
 *
 * TODO: Develop a specialize-on-each-iteration variant...but that might not be safe for region-based specialization...
 * TODO: Develop an item-wise variant... (like 'if')
 * @param <IN>
 */
public class While<IN> implements Transfer<IN,IN> {
    protected final Transfer<IN,IN> base;
    protected final Valuer<Aggregates<? extends IN>, Boolean> pred;

    public While(Valuer<Aggregates<? extends IN>, Boolean> pred, Transfer<IN,IN> base) {
        this.base=base;
        this.pred=pred;
    }
    public IN emptyValue() {return base.emptyValue();}
    public Specialized<IN> specialize(Aggregates<? extends IN> aggregates) {
        return new Specialized<>(base, pred, aggregates);
    }

    public static class Specialized<IN> extends While<IN> implements Transfer.Specialized<IN,IN> {
        protected final Transfer.Specialized<IN,IN> op;

        public Specialized(Transfer<IN,IN> base, Valuer<Aggregates<? extends IN>, Boolean> pred, Aggregates<? extends IN> aggs) {
            super(pred, base);
            op = base.specialize(aggs);
        }

        public Aggregates<IN> process(Aggregates<? extends IN> aggs, Renderer rend) {
            Aggregates<IN> out = rend.transfer(aggs, op);
        	while (!pred.value(out)) {out = rend.transfer(out, op);}
            return out;
        }        
    }
}
