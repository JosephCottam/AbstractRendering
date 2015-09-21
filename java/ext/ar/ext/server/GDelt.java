package ar.ext.server;

import static ar.ext.lang.BasicLibrary.put;
import static ar.ext.lang.BasicLibrary.get;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ar.Aggregates;
import ar.Aggregator;
import ar.Transfer;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.Util;

public class GDelt {
	public static interface Cell {public double value();}
	public static BiFunction<? extends Number, ? extends Number, Double> ABS_MAX = (a,b) -> Math.max(Math.abs(a.doubleValue()), Math.abs(b.doubleValue()));
	public static BiFunction<? extends Number, ? extends Number, Double> ABS_MIN = (a,b) -> Math.min(Math.abs(a.doubleValue()), Math.abs(b.doubleValue()));
	public static BiFunction<? extends Number, ? extends Number, Double> RANDOM = (a,b) -> Math.random() > .5 ? a.doubleValue() : b.doubleValue();
	

	
	public static final  Map<String, Function<List<Object>, Object>> LIBRARY = new HashMap<>();
	static {
		put(LIBRARY, "avg", "Average of a set of categories (Map<C,Double> -> Double)", args -> new Avg<>());
		put(LIBRARY, "trend", "Direction and magnitude of a trend across categories (Map<C,Double> -> Double)", args -> new Trend<>());
		put(LIBRARY, "averageCat", "Average value for a category. (Aggregator, Map<C,Double>)", args -> new AverageCat<>());
		put(LIBRARY, "yearTone", "GDelt year/tone (info, Map<C,Double>)", args -> new KeyValue(0,2));
		put(LIBRARY, "hueInterpolate", "Rainbow color map", args -> new HueInterpolate<>(get(args, 0, Util.CLEAR)));
		put(LIBRARY, "rescale", "Linearly rescale a set of numbers so they land bewteen min/max (args: low, high, empty)", args -> new Rescale<>(get(args, 0, 0d), get(args, 1, 1d), get(args, 2, 0d)));
		put(LIBRARY, "absMax", "Keep the value farthest from zero (preserves sign in output, but not in comparison)", args -> ABS_MAX);
		put(LIBRARY, "absMin", "Keep the value closest to zero (preserves sign in output, but not in comparison)", args -> ABS_MIN);
		put(LIBRARY, "random", "Pick one of two value sot keep", args -> RANDOM);
		put(LIBRARY, "nan", "NaN", args -> Double.NaN);
		put(LIBRARY, "inf", "positive infinity", args -> Double.POSITIVE_INFINITY);
		put(LIBRARY, "ninf", "negative infinity", args -> Double.NEGATIVE_INFINITY);
	}
	
	
	/**Delta first and last gives overall trend direction/magnitude.**/
	public static class Trend<K> implements Transfer.ItemWise<Map<K, Cell>, Double> {
		@Override public Double emptyValue() {return 0d;}

		@Override
		public Double at(int x, int y, Aggregates<? extends Map<K, Cell>> input) {
			Map<K, Cell> items = input.get(x,y);
			if (items.size() == 0) {return 0d;}

			Iterator<Cell> vals = items.values().iterator();
			double first = vals.next().value();
			double last = first;
			while (vals.hasNext()) {last = vals.next().value();}
			return last-first;
		}
	}
	
	/**Combine all categories, and return the average over the whole.**/
	public static class Avg<K> implements Transfer.ItemWise<Map<K, Cell>, Double> {
		@Override public Double emptyValue() {return 0d;}

		@Override
		public Double at(int x, int y, Aggregates<? extends Map<K, Cell>> input) {
			Map<K, Cell> items = input.get(x,y);
			if (items.size() == 0) {return 0d;}
			
			Iterator<Cell> vals = items.values().iterator();
			double total = vals.next().value();
			while (vals.hasNext()) {total = total + vals.next().value();}
			return total/items.size();
		}
	}

	
	public static class KeyValue implements Valuer<Indexed, Map<Integer, Double>> {
		final int keyIdx, valIdx;
		
		public KeyValue(int keyIdx, int valIdx) {this.keyIdx = keyIdx; this.valIdx = valIdx;}
		
		@Override
		public Map<Integer, Double> apply(Indexed t) {
			Map<Integer, Double> v = new HashMap<>();
			v.put((Integer) t.get(keyIdx), ((Number) t.get(valIdx)).doubleValue());
			return v;
		}
		
	}
	
	public static class Rescale<N extends Number> implements Transfer<N, Double> {
		protected final double low, high, empty;
		
		public Rescale(double low, double high, double empty) {
			this.low = low;
			this.high = high;
			this.empty = empty;
		}
		
		@Override public Double emptyValue() {return empty;}
		@Override
		public Specialized<N> specialize(Aggregates<? extends N> aggregates) {
			return new Specialized<>(low, high, empty, aggregates);
		}
		
		public static class Specialized<N extends Number> extends Rescale<N> implements Transfer.ItemWise<N, Double> {			
			final Util.Stats<N> stats; 
			public Specialized(double low, double high, double empty, Aggregates<? extends N> aggs) {
				super(low, high, empty);
				this.stats = Util.stats(aggs);
			}

			@Override
			public Double at(int x, int y, Aggregates<? extends N> input) {
				double v = input.get(x,y).doubleValue();
				if (Util.isEqual(v, input.defaultValue())) {return empty;}
				double p = 1-((stats.max.doubleValue()-v)/(stats.max.doubleValue()-stats.min.doubleValue()));
				return Util.weightedAverage(low, high, p);				
			}
		}
	}
	
	public static class HueInterpolate<N extends Number> implements Transfer<N, Color> {
		protected final Color background;
		protected final float saturation, value;
		public HueInterpolate(Color background) {this(background, 1f, 1f);}
		public HueInterpolate(Color background, float sat, float value) {
			this.background = background;
			this.saturation = sat;
			this.value = value;
		}
		@Override public Color emptyValue() {return background;}
		
		public Specialized<N> specialize(Aggregates<? extends N> aggregates) {
			return new Specialized<>(background, saturation, value, aggregates);
		}
		
		public static class Specialized<N extends Number> extends HueInterpolate<N> implements Transfer.ItemWise<N, Color> {			
			final Util.Stats<N> stats; 
			public Specialized(Color background, float saturation, float value, Aggregates<? extends N> aggs) {
				super(background, saturation, value);
				this.stats = Util.stats(aggs);
			}

			@Override
			public Color at(int x, int y, Aggregates<? extends N> input) {
				if (Util.isEqual(input.get(x,y), input.defaultValue())) {return background;}
				float v = input.get(x,y).floatValue();

				float p = .75f*(1-((stats.max.floatValue()-v)/(stats.max.floatValue()-stats.min.floatValue())));
				return new Color(Color.HSBtoRGB(p, saturation, value));
			}
		}
		
	}
		
	public static class AverageCat<K, T extends Map<K, Double>> implements Aggregator<T, Map<K, AverageCat.Avg>> {
		public static final class Avg implements Cell {
			public final int count;
			public final double val;
			public final double avg;
			public Avg(int count, double val) {
				this.count = count;
				this.val = val;
				this.avg = val/count;
			}
			
			@Override public double value() {return avg;}
			public Avg update(double val) {return new Avg(count+1, this.val+val);}
			public Avg update(Avg update) {return new Avg(count+update.count, val+update.val);}
		}
		
		private final Supplier<Map<K, Avg>> allocator;
		private final Map<K, Avg> identity;
		public AverageCat() {this(() -> new TreeMap<>());}
		
		/**@param allocator Function to invoke to create a new entry.**/
		public AverageCat(Supplier<Map<K, Avg>> allocator) {
			this.allocator = allocator;
			identity = allocator.get();
		}

		@Override public Map<K, Avg> identity() {return identity;}

		@Override
		public Map<K, Avg> combine(Map<K, Avg> current, T update) {
			if (update.isEmpty()) {return current;}
			
			Map<K,Avg> target = allocator.get();
			target.putAll(current);
			for (Map.Entry<K, Double> up: update.entrySet()) {
				Avg avg = current.getOrDefault(up.getKey(), new Avg(0,0));
				target.put(up.getKey(), avg.update(up.getValue()));
			}
			return target;
		}


		@Override
		public Map<K, Avg> rollup(Map<K, Avg> left, Map<K, Avg> right) {
			if (left.isEmpty()) {return right;}
			if (right.isEmpty()) {return left;}
			
			Map<K, Avg> into = right, from = left;
			if (left.size() > right.size()) {
				into = left;
				from = right;
			}
			
			for (Map.Entry<K, Avg> e: from.entrySet()) {
				K key = e.getKey();
				if (into.containsKey(key)) {
					into.put(key, into.get(key).update(e.getValue()));
				} else {
					into.put(key, e.getValue());
				}
			}
			return into;
		}
	}
	
	
}
