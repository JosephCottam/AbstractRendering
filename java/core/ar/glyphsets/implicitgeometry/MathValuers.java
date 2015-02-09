package ar.glyphsets.implicitgeometry;

/**Wrappers for mathematical functions.  
 * 
 * Many of these are pulled from java.lang.Math others are simple arithmetic operators.
 */
public class MathValuers {
	/**Perform log based on the double-value of the input.  
	 * By default, the add1 flag is set to true. 
	 * 
	 * If basis is set to 0, will just echo input values (convenient with interactive tools).
	 */
	public static final class Log<A extends Number> implements Valuer<A,Double> {
		final double base;
		final boolean add1;
		
		public Log(Integer base) {this((double) base.intValue(), true);}
		public Log(Integer base, boolean add1) {this((double) base.intValue(), add1);}
		
		public Log(Double base) {this(base, true);}
		public Log(Double base, boolean add1) {
			this.base = base; 
			this.add1=add1;
		}

		public Double apply(A from) {
			double val = from.doubleValue();
			if (base == 0) {return val;}
			else if (add1 && base == Math.E) {return Math.log1p(val);}
			else if (add1) {return Math.log1p(val)/Math.log(base);}
			else if (base == Math.E) {return Math.log(val);}
			else if (base == 10) {return Math.log10(val);}
			else {return Math.log(val)/Math.log(base);}
		}
	}
		

	public static final class ToDouble implements Valuer<Number, Double> {
		public Double apply(Number d) {return d.doubleValue();}
	}
	
	public static final class ToFloat implements Valuer<Number, Float> {
		public Float apply(Number d) {return d.floatValue();}
	}
	
	public static final class ToLong implements Valuer<Number, Long> {
		public Long apply(Number d) {return d.longValue();}
	}
	
	public static final class ToInteger implements Valuer<Number, Integer> {
		public Integer apply(Number d) {return d.intValue();}
	}

	public static final class ToShort implements Valuer<Double, Short> {
		public Short apply(Double d) {return d.shortValue();}
	}
	
	public static final class GT<N extends Number> implements Valuer<N, Boolean> {
		final double ref;
		public GT(Double ref) {this.ref = ref;} 
		public Boolean apply(N from) {return from.doubleValue() > ref;}
	}
	
	public static final class GTE<N extends Number> implements Valuer<N, Boolean> {
		final double ref;
		public GTE(Double ref) {this.ref = ref;} 
		public Boolean apply(N from) {return from.doubleValue() >= ref;}
	}
	
	public static final class LT<N extends Number> implements Valuer<N, Boolean> {
		final double ref;
		public LT(Double ref) {this.ref = ref;} 
		public Boolean apply(N from) {return from.doubleValue() < ref;}
	}
	
	public static final class LTE<N extends Number> implements Valuer<N, Boolean> {
		final double ref;
		public LTE(Double ref) {this.ref = ref;} 
		public Boolean apply(N from) {return from.doubleValue() <= ref;}
	}
	
	public static final class EQ<N extends Number> implements Valuer<N, Boolean> {
		final double ref;
		public EQ(Double ref) {this.ref = ref;} 
		public Boolean apply(N from) {return ref == from.doubleValue();}
	}
}
