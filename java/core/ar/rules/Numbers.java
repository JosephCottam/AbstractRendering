package ar.rules;

import java.awt.Color;

import ar.Aggregates;
import ar.Aggregator;
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

	

	/**Total value of items present**/
	public static final class Sum {
		@SuppressWarnings("unchecked")
		public static <N extends Number> Aggregator<N, N> make(N instance) {return (Aggregator<N, N>) make(instance.getClass());}
		
		@SuppressWarnings("unchecked")
		public static <N extends Number> Aggregator<N, N> make(Class<N> clss) {
			if (java.lang.Double.class.equals(clss)) {return (Aggregator<N, N>) new Double();}
			if (java.lang.Float.class.equals(clss)) {return (Aggregator<N, N>) new Float();}
			if (java.lang.Integer.class.equals(clss)) {return (Aggregator<N, N>) new Integer();}
			if (java.lang.Long.class.equals(clss)) {return (Aggregator<N, N>) new Long();}
			throw new IllegalArgumentException("No support for sum over " + clss.getName());
		}
		
		public static final class Double implements Aggregator<java.lang.Double, java.lang.Double> {
			@Override public java.lang.Double combine(java.lang.Double current, java.lang.Double update) {return current.doubleValue() + update.doubleValue();}
			@Override public java.lang.Double rollup(java.lang.Double left, java.lang.Double right) {return combine(left,right);}
			@Override public java.lang.Double identity() {return 0d;}
		}
		
		public static final class Float implements Aggregator<java.lang.Float, java.lang.Float> {
			@Override public java.lang.Float combine(java.lang.Float current, java.lang.Float update) {return current.floatValue() + update.floatValue();}
			@Override public java.lang.Float rollup(java.lang.Float left, java.lang.Float right) {return combine(left,right);}
			@Override public java.lang.Float identity() {return 0f;}
		}
		
		public static final class Integer implements Aggregator<java.lang.Integer , java.lang.Integer > {
			@Override public java.lang.Integer combine(java.lang.Integer current, java.lang.Integer update) {return current.intValue() + update.intValue();}
			@Override public java.lang.Integer rollup(java.lang.Integer left, java.lang.Integer right) {return combine(left,right);}
			@Override public java.lang.Integer identity() {return 0;}
		}
		
		public static final class Long implements Aggregator<java.lang.Long, java.lang.Long> {
			@Override public java.lang.Long combine(java.lang.Long current, java.lang.Long update) {return current.longValue() + update.longValue();}
			@Override public java.lang.Long rollup(java.lang.Long left, java.lang.Long right) {return combine(left,right);}
			@Override public java.lang.Long identity() {return 0L;}
		}		
	}
	
	
	/**Retain the largest value seen.*/
	public static final class Max<N extends Number> implements Aggregator<N, N> {
		private final Valuer<Double,N> wrapper;
		public Max(Valuer<Double,N> wrapper) {this.wrapper = wrapper;}

		public N combine(N current, N update) {
			 return wrapper.apply(Math.max(current.doubleValue(), update.doubleValue()));
		}
		
		public N rollup(N left, N right) {
			return wrapper.apply(Math.max(left.doubleValue(), right.doubleValue()));
		}
		
		public N identity() {return wrapper.apply(0d);}
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
			double v = aggregates.get(x, y).doubleValue();
			if (v < lowv) {return background;}
			return Util.interpolate(low, high, lowv, highv, v);
		}
		
		@Override public Color emptyValue() {return background;}
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
		public Transfer.ItemWise<A,Color> specialize(Aggregates<? extends A> aggregates) {
			return new Specialized<>(aggregates, low, high, empty);
		}
		
		@Override public Color emptyValue() {return empty;}
		
		private static class Specialized<A extends Number> extends Interpolate<A> implements Transfer.ItemWise<A, Color> {
			private static final long serialVersionUID = 1106343839501609604L;
			protected final Util.Stats<? extends Number> extrema;

			public Specialized(Aggregates<? extends A> aggregates, Color low, Color high, Color empty) {
				super(low, high, empty);
				this.extrema = Util.stats(aggregates, false, false, false, false);
			}

			@Override
			public Color at(int x, int y, Aggregates<? extends A> aggregates) {
				Number v = aggregates.get(x,y);
				if (Util.isEqual(v, aggregates.defaultValue())) {return empty;}
				return Util.interpolate(low, high, extrema.min.doubleValue(), extrema.max.doubleValue(), v.doubleValue());
			}
		}
	}
	
	
}
