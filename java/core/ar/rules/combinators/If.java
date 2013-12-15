package ar.rules.combinators;

import ar.Aggregates;
import ar.Transfer;
import ar.glyphsets.implicitgeometry.Valuer;

/**If/Then/Else implementation.
 * 
 * Unless otherwise specified, the empty value will be taken from either pass or fail.
 * **/
public class If<IN,OUT> implements Transfer<IN,OUT> {
    protected final Valuer<IN, Boolean> pred;
    protected final Transfer<IN,OUT> pass;
    protected final Transfer<IN,OUT> fail;
    protected final OUT empty;

    public If(Valuer<IN, Boolean> pred, Transfer<IN,OUT> pass, Transfer<IN,OUT> fail) {
        this(pred, pass, fail, pass.emptyValue());
    }

    public If(Valuer<IN, Boolean> pred, Transfer<IN,OUT> pass, Transfer<IN,OUT> fail, OUT empty) {
        this.pred = pred;
        this.pass = pass;
        this.fail = fail;
        this.empty = empty;
    }
    @Override public OUT emptyValue() {return empty;}

    @Override
    public Specialized<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {
        return new Specialized<>(pred, pass, fail, empty, aggregates);
    }

    public static class Specialized<IN,OUT> extends If<IN,OUT> implements Transfer.Specialized<IN,OUT> {
        final Transfer.Specialized<IN,OUT> pass;
        final Transfer.Specialized<IN,OUT> fail;

        public Specialized(
        		Valuer<IN, Boolean> pred,
                Transfer<IN,OUT> pass,
                Transfer<IN,OUT> fail,
                OUT empty,
                Aggregates<? extends IN> aggregates) {

            super(pred,pass,fail,empty);
            this.pass = pass.specialize(aggregates);
            this.fail = fail.specialize(aggregates);
        }

        @Override
        public OUT at(int x, int y, Aggregates<? extends IN> aggregates) {
            IN val = aggregates.get(x,y);
            if (pred.value(val)) {return pass.at(x,y,aggregates);}
            else {return fail.at(x,y,aggregates);}
        }
        
        @Override
        public boolean localOnly() {
        	return false; //TODO: Always false on account of the predicate.  Re-examine... 
        }
    }
}
