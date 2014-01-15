package ar.glyphsets.implicitgeometry;

/**Wrappers for mathematical functions.  
 * 
 * Many of these are pulled from java.lang.Math others are simple arithmetic operators.
 */
public class MathValuers {
	public static final class AddInt<A extends Number> implements Valuer<A, Integer> {
		final int val;
		public AddInt(Integer val) {this.val = val;}
		public Integer value(A from) {return from.intValue()+val;}
	}

	public static final class AddDouble<A extends Number> implements Valuer<A, Double> {
		final double val;
		public AddDouble(Double val) {this.val = val;}
		public Double value(A from) {return from.doubleValue()+val;}
	}
	
	public static final class SubtractInt<A extends Number> implements Valuer<A, Integer> {
		final int val;
		public SubtractInt(Integer val) {this.val = val;}
		public Integer value(A from) {return from.intValue()-val;}
	}

	public static final class SubtractDouble<A extends Number> implements Valuer<A, Double> {
		final double val;
		public SubtractDouble(Double val) {this.val = val;}
		public Double value(A from) {return from.doubleValue()-val;}
	}

	
	public static final class DivideInt<A extends Number> implements Valuer<A,Integer> {
		final int denominator;
		public DivideInt(Integer denominator) {this.denominator = denominator;}
		public Integer value(A from) {return from.intValue()/denominator;}		
	}
	
	public static final class DivideDouble<A extends Number> implements Valuer<A,Double> {
		final double denominator;
		public DivideDouble(Double denominator) {this.denominator = denominator;}
		public Double value(A from) {return from.doubleValue()/denominator;}		
	}
	
	public static final class MultiplyInt<A extends Number> implements Valuer<A, Integer> {
		final int multiplier;
		public MultiplyInt(Integer multiplier) {this.multiplier = multiplier;}
		public Integer value(A from) {return from.intValue()*multiplier;}
	}

	public static final class MultiplyDouble<A extends Number> implements Valuer<A, Double> {
		final double multiplier;
		public MultiplyDouble(Double multiplier) {this.multiplier = multiplier;}
		public Double value(A from) {return from.doubleValue()*multiplier;}
	}
		
	/**Perform log based on the double-value of the input.  
	 * If basis is set to 0, will just echo input values (convenient with interactive tools).
	 */
	public static final class Log<A extends Number> implements Valuer<A,Double> {
		final double base;
		final boolean add1;
		final boolean ignoreZeros;
		
		public Log(Double base) {this(base, false, false);}
		public Log(Double base, boolean add1, boolean ignoreZeros) {
			this.base = base; 
			this.add1=add1;
			this.ignoreZeros = ignoreZeros;
		}

		public Double value(A from) {
			double val = from.doubleValue();
			if (base == 0) {return val;}
			else if (val == 0d && ignoreZeros) {return 0d;}
			else if (add1 && base == Math.E) {return Math.log1p(val);}
			else if (add1) {return Math.log1p(val)/Math.log(base);}
			else if (base == Math.E) {return Math.log(val);}
			else if (base == 10) {return Math.log10(val);}
			else {return Math.log(val)/Math.log(base);}
		}
	}
		
	/**Raise a base to the passed values.**/
	public static final class Pow<A extends Number> implements Valuer<A,Double> {
		final double base;
		public Pow(double base) {this.base = base;} 
		public Double value(A from) {return Math.pow(base, from.doubleValue());}
	}
	
	/**Raise a value to a set power.**/
	public static final class Raise<A extends Number> implements Valuer<A,Double> {
		final double pow;
		public Raise(double pow) {this.pow = pow;} 
		public Double value(A from) {return Math.pow(from.doubleValue(), pow);}
	}
	
	public static final class Sqrt<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.sqrt(from.doubleValue());}
	}
	

	public static final class AbsLong<A extends Number> implements Valuer<A,Long> {
		public Long value(A from) {return Math.abs(from.longValue());}
	}

	public static final class AbsFloat<A extends Number> implements Valuer<A,Float> {
		public Float value(A from) {return Math.abs(from.floatValue());}
	}

	
	public static final class AbsInt<A extends Number> implements Valuer<A,Integer> {
		public Integer value(A from) {return Math.abs(from.intValue());}
	}
	
	public static final class AbsDouble<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.abs(from.doubleValue());}
	}
	
	public static final class ACos<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.acos(from.doubleValue());}
	}
	
	public static final class ASin<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.asin(from.doubleValue());}
	}
	public static final class ATan<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.atan(from.doubleValue());}
	}
	public static final class CBRT<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.cbrt(from.doubleValue());}
	}
	
	public static final class ceil<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.ceil(from.doubleValue());}
	}

	public static final class cos<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.cos(from.doubleValue());}
	}
	
	public static final class cosh<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.cosh(from.doubleValue());}
	}
	
	public static final class exp<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.exp(from.doubleValue());}
	}
	
	public static final class expm1<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.expm1(from.doubleValue());}
	}
	
	public static final class floor<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.floor(from.doubleValue());}
	}
	
	public static final class Exponent<A extends Number> implements Valuer<A,Integer> {
		public Integer value(A from) {return Math.getExponent(from.doubleValue());}
	}
	
	public static final class NextUp<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.nextUp(from.doubleValue());}
	}
	public static final class RInt<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.rint(from.doubleValue());}
	}
	public static final class RoundDouble<A extends Number> implements Valuer<A,Long> {
		public Long value(A from) {return Math.round(from.doubleValue());}
	}
	public static final class RoundFloat<A extends Number> implements Valuer<A,Integer> {
		public Integer value(A from) {return Math.round(from.floatValue());}
	}
	public static final class Signum<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.signum(from.doubleValue());}
	}
	public static final class Sin<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.sin(from.doubleValue());}
	}
	public static final class Sinh<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.sinh(from.doubleValue());}
	}
	public static final class Tan<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.tan(from.doubleValue());}
	}
	public static final class Tanh<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.tanh(from.doubleValue());}
	}
	public static final class ToDegrees<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.toDegrees(from.doubleValue());}
	}
	public static final class ToRadians<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.toRadians(from.doubleValue());}
	}
	public static final class ULP<A extends Number> implements Valuer<A,Double> {
		public Double value(A from) {return Math.ulp(from.doubleValue());}
	}
	
	public static final class ToDouble implements Valuer<Number, Double> {
		public Double value(Number d) {return d.doubleValue();}
	}
	public static final class ToFloat implements Valuer<Number, Float> {
		public Float value(Number d) {return d.floatValue();}
	}
	public static final class ToLong implements Valuer<Number, Long> {
		public Long value(Number d) {return d.longValue();}
	}
	public static final class ToInteger implements Valuer<Number, Integer> {
		public Integer value(Number d) {return d.intValue();}
	}
	public static final class ShortWrapper implements Valuer<Double, Short> {
		public Short value(Double d) {return d.shortValue();}
	}
	
	public static final class GT<N extends Number> implements Valuer<N, Boolean> {
		final double ref;
		public GT(Double ref) {this.ref = ref;} 
		public Boolean value(N from) {return from.doubleValue() > ref;}
	}
	
	public static final class GTE<N extends Number> implements Valuer<N, Boolean> {
		final double ref;
		public GTE(Double ref) {this.ref = ref;} 
		public Boolean value(N from) {return from.doubleValue() >= ref;}
	}
	
	public static final class LT<N extends Number> implements Valuer<N, Boolean> {
		final double ref;
		public LT(Double ref) {this.ref = ref;} 
		public Boolean value(N from) {return from.doubleValue() < ref;}
	}
	
	public static final class LTE<N extends Number> implements Valuer<N, Boolean> {
		final double ref;
		public LTE(Double ref) {this.ref = ref;} 
		public Boolean value(N from) {return from.doubleValue() <= ref;}
	}
	
	public static final class EQ<N extends Number> implements Valuer<N, Boolean> {
		final double ref;
		public EQ(Double ref) {this.ref = ref;} 
		public Boolean value(N from) {return ref == from.doubleValue();}
	}
}
