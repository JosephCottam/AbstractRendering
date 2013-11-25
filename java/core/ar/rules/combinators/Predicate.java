package ar.rules.combinators;

import ar.Aggregates;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.Util;

/**Simple true/false test.
 * 
 * TODO: Should this really be a valuer of type boolean (saves the VPred class...).
 * @param <IN>  The type of the value to be tested
 */
public interface Predicate<IN> {
    public boolean test(IN arg);
    
    
    /**Predicate to wrap a valuer.**/
    public static final class VPred<IN> implements Predicate<IN> {
    	private final Valuer<IN,Boolean> valuer;
    	public VPred(Valuer<IN,Boolean> valuer) {this.valuer = valuer;}
    	public boolean test(IN arg) {return valuer.value(arg);}
    	
    	public static <IN> VPred<IN> m(Valuer<IN, Boolean> pred) {return new VPred<>(pred);}
    }
    
    /**Check if a predicate is true for all items in an aggregate set.**/
    public static final class All<IN> implements Predicate<Aggregates<? extends IN>> {
    	private final Predicate<IN> pred;
    	private final boolean ignoreDefault;
    	public All(Predicate<IN> pred) {this(pred, false);}
    	public All(Predicate<IN> pred, boolean ignoreDefault) {
    		this.pred = pred;
    		this.ignoreDefault = ignoreDefault;
    	}
		public boolean test(Aggregates<? extends IN> arg) {
			for (IN v: arg) {
				boolean empty = Util.isEqual(v, arg.defaultValue());
				if (ignoreDefault && empty) {continue;}
				if (!pred.test(v)) {return false;}
			}
			return true;
		}
    }
    
    /**Check if a predicate is true for at least one item in an aggregate set.**/
    public static final class Some<IN> implements Predicate<Aggregates<? extends IN>> {
    	private final Predicate<IN> pred;
    	public Some(Predicate<IN> pred) {this.pred = pred;}
		public boolean test(Aggregates<? extends IN> arg) {
			for (IN v: arg) {if (pred.test(v)) {return true;}}
			return false;
		}
    }
    
    /**Negate a predicate.**/
    public static final class Not<IN> implements Predicate<IN> {
    	private final Predicate<IN> base;
    	public Not(Predicate<IN> base) {this.base = base;}
		public boolean test(IN arg) {return !base.test(arg);}
    }
    
    /**Echo for boolean values.**/
    public static final class True implements Predicate<Boolean> {
		public boolean test(Boolean arg) {return arg;}
    }
}
