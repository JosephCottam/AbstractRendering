package ar.rules.combinators;

import ar.Aggregates;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.Util;

/**Simple true/false tests.
 * Predicates are valuers that return true or false.
 * 
 * The 'predicates' should never be used, but it is a convenient place
 * to put several common predicates.  
 * 
 * @param <IN>  The type of the value to be tested
 */
public abstract class Predicates<IN> {
	private Predicates() {/*Prevent Instantiation*/}
    
    /**Check if a predicate is true for all items in an aggregate set.**/
    public static final class All<IN> implements Valuer<Aggregates<? extends IN>, Boolean> {
    	private final Valuer<IN, Boolean> pred;
    	private final boolean ignoreDefault;
    	public All(Valuer<IN, Boolean> pred) {this(pred, false);}
    	public All(Valuer<IN, Boolean> pred, boolean ignoreDefault) {
    		this.pred = pred;
    		this.ignoreDefault = ignoreDefault;
    	}
    	
		public Boolean value(Aggregates<? extends IN> arg) {return value(arg, pred, ignoreDefault);}
		
		public static <IN> Boolean value(Aggregates<? extends IN> arg, Valuer<IN, Boolean> pred, boolean ignoreDefault) {
			for (IN v: arg) {
				boolean empty = Util.isEqual(v, arg.defaultValue());
				if (ignoreDefault && empty) {continue;}
				if (!pred.value(v)) {return Boolean.FALSE;}
			}
			return Boolean.TRUE;			
		}
		
		/**Check if all values are true.  Returns true iff all are true.**/
		public static Boolean all(Aggregates<Boolean> arg) {
			for (Boolean v: arg) {if (!v) {return false;}}
			return true;
		}
    }
    
    /**Check if a predicate is true for at least one item in an aggregate set.**/
    public static final class Any<IN> implements Valuer<Aggregates<? extends IN>, Boolean> {
    	private final Valuer<IN, Boolean> pred;
    	public Any(Valuer<IN, Boolean> pred) {this.pred = pred;}
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
