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
	
	/**Interpolate between two colors with fixed upper and lower bounds.
	 * 
	 * If both colors are the same EXCEPT their alpha values, this is equivalent
	 * to a fixed alpha application.
	 * 
	 * @author jcottam
	 */
	public static final class FixedInterpolate implements Transfer.Specialized<Number,Color> {
		private static final long serialVersionUID = -2583391379423930420L;
		final Color low, high;
		final double lowv, highv;

		/**
		 * @param low Color to associated with lowV
		 * @param high Color to associate with highV
		 * @param lowV Expected lowest input value
		 * @param highV Expected highest input value
		 */
		public FixedInterpolate(Color low, Color high, double lowV, double highV) {
			this.low = low;
			this.high = high;
			this.lowv = lowV;
			this.highv = highV;
		}

		public Color at(int x, int y, Aggregates<? extends Number> aggregates) {
			return Util.interpolate(low, high, lowv, highv, aggregates.get(x, y).doubleValue());
		}

		public Color emptyValue() {return Color.WHITE;}
		public FixedInterpolate  specialize(Aggregates<? extends Number> aggregates) {return this;}
	}
	
	/**HD interpolation between two colors.**/
	public static class Interpolate implements Transfer<Number, Color> {
		private static final long serialVersionUID = 2878901447280244237L;
		protected final Color low, high, empty;
		protected final int logBasis;
		
		/**
		 * @param low Color to associate with lowest input value
		 * @param high Color to associate with highest input value
		 */
		public Interpolate(Color low, Color high) {this(low,high, Util.CLEAR, 0);}
		
		/**
		 * @param low Color to associate with lowest input value
		 * @param high Color to associate with highest input value
		 * @param empty Color to return when the default aggregate value is encountered
		 * @param logBasis Log basis to use; Value less than 1 signals linear interpolation
		 */
		public Interpolate(Color low, Color high, Color empty, int logBasis) {
			this.low = low;
			this.high = high;
			this.empty = empty;
			this.logBasis = logBasis;
		}
		

		public Transfer.Specialized<Number,Color> specialize(Aggregates<? extends Number> aggregates) {
			Util.Stats stats = Util.stats(aggregates, false);
			if (logBasis <=1) {
				return new SpecializedLinear(stats, low, high, empty, logBasis);
			} else {
				return new SpecializedLog(stats, low, high, empty, logBasis);
			}
		}
		
		public Color emptyValue() {return Util.CLEAR;}
		
		private static abstract class BaseSpecialized extends Interpolate implements Transfer.Specialized<Number, Color> {
			private static final long serialVersionUID = 1106343839501609604L;
			protected final Util.Stats extrema;

			public BaseSpecialized(Util.Stats extrema, Color low, Color high, Color empty, int logBasis) {
				super(low, high, empty, logBasis);
				this.extrema = extrema;
			}

			public Color at(int x, int y, Aggregates<? extends Number> aggregates) {
				Number v = aggregates.get(x,y);
				if (v.equals(aggregates.defaultValue())) {return empty;}
				return interpolate(v);
			}
			
			protected abstract Color interpolate(Number v);
		}

		public static final class SpecializedLog extends BaseSpecialized {
			private static final long serialVersionUID = -8820226527786085843L;

			public SpecializedLog(Util.Stats extrema, Color low, Color high, Color empty, int logBasis) {
				super(extrema, low, high, empty, logBasis);
			}

			protected Color interpolate(Number v) {
				return Util.logInterpolate(low,high, extrema.min, extrema.max, v.doubleValue(), logBasis);
			}
		}

		public static final class SpecializedLinear extends BaseSpecialized {
			private static final long serialVersionUID = 7114502132818604376L;

			public SpecializedLinear(Util.Stats extrema, Color low, Color high, Color empty, int logBasis) {
				super(extrema, low, high, empty, logBasis);
			}

			public Color interpolate(Number v) {
				return Util.interpolate(low, high, extrema.min, extrema.max, v.doubleValue());
			}
			
		}
	}
	
	
}
