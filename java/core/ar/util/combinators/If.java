package ar.util.combinators;

import ar.Aggregates;
import ar.Transfer;

//TODO: Merge with ar.rules.General.Switch...investigate the difference between the two predicate representations

public class If<IN,OUT> implements Transfer<IN,OUT> {
    protected final Predicate<IN> pred;
    protected final Transfer<IN,OUT> pass;
    protected final Transfer<IN,OUT> fail;
    protected final OUT empty;

    public If(Predicate<IN> pred, Transfer<IN,OUT> pass, Transfer<IN,OUT> fail) {
        this(pred, pass, fail, pass.emptyValue());
    }

    public If(Predicate<IN> pred, Transfer<IN,OUT> pass, Transfer<IN,OUT> fail, OUT empty) {
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
                Predicate<IN> pred,
                Transfer<IN,OUT> pass,
                Transfer<IN,OUT> fail,
                OUT empty,
                Aggregates<? extends IN> aggregates) {

            super(pred,pass,fail,empty);
            this.pass = pass.specialize(aggregates);
            this.fail = pass.specialize(aggregates);
        }

        @Override
        public OUT at(int x, int y, Aggregates<? extends IN> aggregates) {
            IN val = aggregates.get(x,y);
            if (pred.test(val)) {return pass.at(x,y,aggregates);}
            else {return fail.at(x,y,aggregates);}
        }
    }
}
