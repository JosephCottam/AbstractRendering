package ar.rules.combinators;

import ar.Aggregates;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.Util;

/**Simple true/false test.
 * 
 * TODO: Should this really be a valuer of return-type boolean (saves the VPred class...).
 * @param <IN>  The type of the value to be tested
 */
public interface Predicates<IN> {
    
    /**Check if a predicate is true for all items in an aggregate set.**/
    public static final class All<IN> implements Valuer<Aggregates<? extends IN>, Boolean> {
    	private final Valuer<IN, Boolean> pred;
    	private final boolean ignoreDefault;
    	public All(Valuer<IN, Boolean> pred) {this(pred, false);}
    	public All(Valuer<IN, Boolean> pred, boolean ignoreDefault) {
    		this.pred = pred;
    		this.ignoreDefault = ignoreDefault;
    	}
		public Boolean value(Aggregates<? extends IN> arg) {
			for (IN v: arg) {
				boolean empty = Util.isEqual(v, arg.defaultValue());
				if (ignoreDefault && empty) {continue;}
				if (!pred.value(v)) {return Boolean.FALSE;}
			}
			return Boolean.TRUE;
		}
    }
    
    /**Check if a predicate is true for at least one item in an aggregate set.**/
    public static final class Some<IN> implements Valuer<Aggregates<? extends IN>, Boolean> {
    	private final Valuer<IN, Boolean> pred;
    	public Some(Valuer<IN, Boolean> pred) {this.pred = pred;}
		public Boolean value(Aggregates<? extends IN> arg) {
			for (IN v: arg) {if (pred.value(v)) {return Boolean.TRUE;}}
			return Boolean.FALSE;
		}
    }
    
    /**Negate a predicate.**/
    public static final class Not<IN> implements Valuer<IN, Boolean> {
    	private final Valuer<IN, Boolean> base;
    	public Not(Valuer<IN, Boolean> base) {this.base = base;}
		public Boolean value(IN arg) {return !base.value(arg);}
    }
}
