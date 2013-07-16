package ar.rules;

import java.awt.Color;
import java.util.List;

import ar.Aggregates;
import ar.Aggregator;
import ar.Transfer;
import ar.util.Util;

public final class Numbers {
	private Numbers() {/*Prevent instantiation*/}
	
	/**How many items present?**/
	public static final class Count implements Aggregator<Object, Integer> {
		public Integer combine(long x, long y, Integer left, Object update) {return left+1;}
		public Integer rollup(List<Integer> integers) {
			int acc=0;
			for (Integer v: integers) {acc+=v;}
			return acc;
		}
		
		public Integer identity() {return 0;}
		public boolean equals(Object other) {return other instanceof Count;}
		public Class<Object> input() {return Object.class;}
		public Class<Integer> output() {return Integer.class;}
	}
	

	public static final class FixedAlpha implements Transfer<Number,Color> {
		final Color low, high;
		final double lowv, highv;

		public FixedAlpha(Color low, Color high, double lowV, double highV) {
			this.low = low;
			this.high = high;
			this.lowv = lowV;
			this.highv = highV;
		}

		public Color at(int x, int y, Aggregates<? extends Number> aggregates) {
			return Util.interpolate(low, high, lowv, highv, aggregates.at(x, y).doubleValue());
		}

		public Color emptyValue() {return Color.WHITE;}
		public Class<Number> input() {return Number.class;}
		public Class<Color> output() {return Color.class;}
	}
	
	public static final class Interpolate implements Transfer<Number, Color> {
		private final Color low, high, empty;
		private final int logBasis;
		private Aggregates<? extends Number> cacheKey;	//Could be a weak-reference instead...
		private Util.Stats extrema;
		
		public Interpolate(Color low, Color high) {this(low,high, Util.CLEAR, 0);}
		public Interpolate(Color low, Color high, Color empty, int logBasis) {
			this.low = low;
			this.high = high;
			this.empty = empty;
			this.logBasis = logBasis;
		}
		
		public Color at(int x, int y, Aggregates<? extends Number> aggregates) {
			if (cacheKey == null || cacheKey != aggregates) {
				extrema = Util.stats(aggregates, false);
				cacheKey = aggregates;
			}
			
			Number v = aggregates.at(x,y);
			if (v.equals(aggregates.defaultValue())) {
				return empty;
			}
			
			if (logBasis <= 1) {
				return Util.interpolate(low, high, extrema.min, extrema.max, v.doubleValue());
			} else {
				return Util.logInterpolate(low,high, extrema.min, extrema.max, v.doubleValue(), logBasis);
			}
		}
		
		public Color emptyValue() {return Util.CLEAR;}
		public Class<Number> input() {return Number.class;}
		public Class<Color> output() {return Color.class;}
	}
	
	
}
