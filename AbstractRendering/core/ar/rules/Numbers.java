package ar.rules;

import java.awt.Color;
import java.util.List;

import ar.Aggregates;
import ar.Aggregator;
import ar.Transfer;
import ar.util.Util;

/**Aggregators and Transfers that work with scalar numbers.**/
public final class Numbers {
	private Numbers() {/*Prevent instantiation*/}
	
	/**How many items present?
	 * 
	 * Input type does not matter, always produces integer outputs.
	 ***/
	public static final class Count<V> implements Aggregator<V, Integer> {
		private static final long serialVersionUID = 5984959309743633510L;
		public Integer combine(long x, long y, Integer left, V update) {return left+1;}
		public Integer rollup(List<Integer> integers) {
			int acc=0;
			for (Integer v: integers) {acc+=v;}
			return acc;
		}
		
		public Integer identity() {return 0;}
		public boolean equals(Object other) {return other instanceof Count;}
		public int hashCode() {return Count.class.hashCode();}
	}
	
	/**Multiply the input value to determine the alpha value.  Equivalent
	 * of having each number apply a certain amount of alpha.
	 * 
	 * @author jcottam
	 */
	public static final class FixedAlpha implements Transfer<Number,Color> {
		private static final long serialVersionUID = -2583391379423930420L;
		final Color low, high;
		final double lowv, highv;

		public FixedAlpha(Color low, Color high, double lowV, double highV) {
			this.low = low;
			this.high = high;
			this.lowv = lowV;
			this.highv = highV;
		}

		public Color at(int x, int y, Aggregates<? extends Number> aggregates) {
			return Util.interpolate(low, high, lowv, highv, aggregates.get(x, y).doubleValue());
		}

		public Color emptyValue() {return Color.WHITE;}
		public void specialize(Aggregates<? extends Number> aggregates) {/**No work to perform.**/}
	}
	
	/**HD interpolation between two colors.**/
	public static final class Interpolate implements Transfer<Number, Color> {
		private static final long serialVersionUID = 2878901447280244237L;
		private final Color low, high, empty;
		private final int logBasis;
		private Util.Stats extrema;
		
		public Interpolate(Color low, Color high) {this(low,high, Util.CLEAR, 0);}
		public Interpolate(Color low, Color high, Color empty, int logBasis) {
			this.low = low;
			this.high = high;
			this.empty = empty;
			this.logBasis = logBasis;
		}
		
		public Color at(int x, int y, Aggregates<? extends Number> aggregates) {
			Number v = aggregates.get(x,y);
			if (v.equals(aggregates.defaultValue())) {
				return empty;
			}
			
			if (logBasis <= 1) {
				return Util.interpolate(low, high, extrema.min, extrema.max, v.doubleValue());
			} else {
				return Util.logInterpolate(low,high, extrema.min, extrema.max, v.doubleValue(), logBasis);
			}
		}

		public void specialize(Aggregates<? extends Number> aggregates) {
			this.extrema = Util.stats(aggregates, false);
		}
		
		public Color emptyValue() {return Util.CLEAR;}
	}
	
	
}
