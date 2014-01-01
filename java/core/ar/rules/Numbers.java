package ar.rules;

import java.awt.Color;

import ar.Aggregates;
import ar.Aggregator;
import ar.Renderer;
import ar.Transfer;
import ar.glyphsets.implicitgeometry.Valuer;
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
		public Integer combine(Integer left, V update) {return left+1;}
		public Integer rollup(Integer left, Integer right) {return left+right;}
		
		public Integer identity() {return 0;}
		public boolean equals(Object other) {return other instanceof Count;}
		public int hashCode() {return Count.class.hashCode();}
	}
	
	
	/**Retain the largest value seen.
	 * 
	 * TODO: Is there a general way to provide for two-argument operators?  That would take care of max/min/gt/lt/etc...
	 */
	public static final class Max<N extends Number> implements Aggregator<N, N> {
		private final Valuer<Double,N> wrapper;
		public Max(Valuer<Double,N> wrapper) {this.wrapper = wrapper;}

		public N combine(N current, N update) {
			 return wrapper.value(Math.max(current.doubleValue(), update.doubleValue()));
		}
		
		public N rollup(N left, N right) {
			return wrapper.value(Math.max(left.doubleValue(), right.doubleValue()));
		}
		
		public N identity() {return wrapper.value(0d);}
	}
	
	/**Interpolate between two colors with fixed upper and lower bounds.
	 * 
	 * If both colors are the same EXCEPT their alpha values, this is equivalent
	 * to a fixed alpha application.
	 * 
	 * @author jcottam
	 */
	public static final class FixedInterpolate<IN extends Number> implements Transfer.ItemWise<IN,Color> {
		private static final long serialVersionUID = -2583391379423930420L;
		final Color low, high, background;
		final double lowv, highv;

		public FixedInterpolate(Color low, Color high, double lowV, double highV) {
			this(low, high, lowV, highV, Color.white);
		}
		
		/**
		 * @param low Color to associated with lowV
		 * @param high Color to associate with highV
		 * @param lowV Expected lowest input value
		 * @param highV Expected highest input value
		 * @param background Background color (used for no-value)
		 */
		public FixedInterpolate(Color low, Color high, double lowV, double highV, Color background) {
			this.low = low;
			this.high = high;
			this.lowv = lowV;
			this.highv = highV;
			this.background = background;
		}

		@Override
		public Color at(int x, int y, Aggregates<? extends IN> aggregates) {
			return Util.interpolate(low, high, lowv, highv, aggregates.get(x, y).doubleValue());
		}

		@Override
		public Aggregates<Color> process(Aggregates<? extends IN> aggregates, Renderer rend) {
			return rend.transfer(aggregates,this);
		}
		
		@Override public Color emptyValue() {return background;}
		@Override public FixedInterpolate<IN>  specialize(Aggregates<? extends IN> aggregates) {return this;}
	}
	
	/**HD interpolation between two colors.**/
	public static class Interpolate<A extends Number> implements Transfer<A, Color> {
		private static final long serialVersionUID = 2878901447280244237L;
		protected final Color low, high, empty;
		
		/**
		 * @param low Color to associate with lowest input value
		 * @param high Color to associate with highest input value
		 */
		public Interpolate(Color low, Color high) {this(low,high, Color.white);}
		
		/**
		 * @param low Color to associate with lowest input value
		 * @param high Color to associate with highest input value
		 * @param empty Color to return when the default aggregate value is encountered
		 */
		public Interpolate(Color low, Color high, Color empty) {
			this.low = low;
			this.high = high;
			this.empty = empty;
		}
		
		@Override 
		public Transfer.Specialized<A,Color> specialize(Aggregates<? extends A> aggregates) {
			return new Specialized<>(aggregates, low, high, empty);
		}
		
		@Override public Color emptyValue() {return empty;}
		
		private static class Specialized<A extends Number> extends Interpolate<A> implements Transfer.ItemWise<A, Color> {
			private static final long serialVersionUID = 1106343839501609604L;
			protected final Util.Stats<? extends Number> extrema;

			public Specialized(Aggregates<? extends A> aggregates, Color low, Color high, Color empty) {
				super(low, high, empty);
				this.extrema = Util.stats(aggregates, false, false, false);
			}

			@Override
			public Color at(int x, int y, Aggregates<? extends A> aggregates) {
				Number v = aggregates.get(x,y);
				if (Util.isEqual(v, aggregates.defaultValue())) {return empty;}
				return Util.interpolate(low, high, extrema.min.doubleValue(), extrema.max.doubleValue(), v.doubleValue());
			}

			@Override
			public Aggregates<Color> process(Aggregates<? extends A> aggregates, Renderer rend) {
				return rend.transfer(aggregates, this);
			}
		}
	}
	
	
}
