package ar.rules;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ar.Aggregates;
import ar.Aggregator;
import ar.Transfer;
import ar.glyphsets.implicitgeometry.Valuer;

/**Tools that don't apply to a particular data type.**/
public class General {
	/**Aggregator and Transfer that always returns the same value.**/
	public static final class Const<T> implements Aggregator<Object,T>, Transfer.Specialized<Object, T> {
		private static final long serialVersionUID = 2274344808417248367L;
		private final T val;
		/**@param val Value to return**/
		public Const(T val) {this.val = val;}
		public T combine(long x, long y, T left, Object update) {return val;}
		public T rollup(List<T> sources) {return val;}
		public T identity() {return val;}
		public T emptyValue() {return val;}
		public ar.Transfer.Specialized<Object, T> specialize(Aggregates<? extends Object> aggregates) {return this;}
		public T at(int x, int y, Aggregates<? extends Object> aggregates) {return val;}
	}


	/**Return what is found at the given location.**/
	public static final class Echo<T> implements Transfer.Specialized<T,T>, Aggregator<T,T> {
		private static final long serialVersionUID = -7963684190506107639L;
		private final T empty;
		/** @param empty Value used for empty; "at" always echos what's in the aggregates, 
		 *               but some methods need an empty value independent of the aggregates set.**/
		public Echo(T empty) {this.empty = empty;}
		public T at(int x, int y, Aggregates<? extends T> aggregates) {return aggregates.get(x, y);}

		public T emptyValue() {return empty;}
		
		public T combine(long x, long y, T left, T update) {return update;}
		public T rollup(List<T> sources) {
			if (sources.size() >0) {return sources.get(0);}
			return emptyValue();
		}
		public T identity() {return emptyValue();}
		public Echo<T> specialize(Aggregates<? extends T> aggregates) {return this;}
	}

	/**Return the given value when presented with a non-empty value.**/
	public static final class Present<IN, OUT> implements Transfer.Specialized<IN,OUT> {
		private static final long serialVersionUID = -7511305102790657835L;
		private final OUT present, absent;
		
		/**
		 * @param present Value to return on not-null
		 * @param absent Value to return on null
		 */
		public Present(OUT present, OUT absent) {
			this.present = present; 
			this.absent=absent;
		}
		
		public OUT at(int x, int y, Aggregates<? extends IN> aggregates) {
			Object v = aggregates.get(x, y);
			if (v != null && !v.equals(aggregates.defaultValue())) {return present;}
			return absent;
		}
		
		public Present<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {return this;}
		
		public OUT emptyValue() {return absent;}
	}
	
	/**Transfer function that wraps a java.util.map.**/
	public static class MapWrapper<IN,OUT> implements Transfer.Specialized<IN,OUT> {
		private static final long serialVersionUID = -4326656735271228944L;
		private final Map<IN, OUT> mappings;
		private final boolean nullIsValue;
		private final OUT other; 

		/**
		 * @param mappings Backing map
		 * @param other Value to return if the backing map does not include a requested key
		 * @param nullIsValue Should 'null' be considered a valid return value from the map, or should it be converted to 'other' instead
		 */
		public MapWrapper(Map<IN, OUT> mappings, OUT other, boolean nullIsValue) {
			this.mappings=mappings;
			this.nullIsValue = nullIsValue;
			this.other = other;
		}

		@Override
		public OUT at(int x, int y, Aggregates<? extends IN> aggregates) {
			IN key = aggregates.get(x, y);
			if (!mappings.containsKey(key)) {return other;}
			OUT val = mappings.get(key);
			if (val==null && !nullIsValue) {return other;}
			return val;
		}

		public OUT emptyValue() {return other;}
		public MapWrapper<IN,OUT> specialize(Aggregates<? extends IN> aggregates) {return this;}

		/**From a reader, make a map wrapper.  
		 * 
		 * This is stream-based, line-oriented conversion.
		 * Lines are read and processed one at time.
		 **/
		@SuppressWarnings("resource")
		public static <K,V> MapWrapper<K,V> fromReader(
				Reader in, Valuer<String,K> keyer, Valuer<String,V> valuer,
				V other, boolean nullIsValue) throws Exception {
			BufferedReader bf;

			if (in instanceof BufferedReader) {
				bf = (BufferedReader) in;
			} else {
				bf = new BufferedReader(in);
			}
			
			Map<K,V> dict = new HashMap<K,V>();
			String line = bf.readLine();
			while(line != null) {
				dict.put(keyer.value(line), valuer.value(line));
			}

			return new MapWrapper<K,V>(dict,other,nullIsValue);
		}
	}
	

	/**Implents "if" in a transfer function.  Applies one transfer if the predicate is true, another if it is false.**/
	public static class Switch<IN,OUT> implements Transfer<IN,OUT> {
		private static final long serialVersionUID = 9066005967376232334L;

		private final Predicate<IN> predicate;
		private final Transfer<IN,OUT> pass;
		private final Transfer<IN,OUT> fail;
		private final OUT empty;
		
		public Switch(Predicate<IN> predicate,
						Transfer<IN,OUT> pass,
						Transfer<IN,OUT> fail,
						OUT empty) {
			this.predicate = predicate;
			this.pass = pass;
			this.fail = fail;
			this.empty = empty;
		}

		@Override
		public OUT emptyValue() {return empty;}

		@Override
		public Transfer.Specialized<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {
			
			Transfer.Specialized<IN,OUT> ps= pass.specialize(aggregates);
			Transfer.Specialized<IN, OUT> fs = fail.specialize(aggregates);
			Predicate.Specialized<IN> preds = predicate.specialize(aggregates);
			return new Specialized<>(preds, ps, fs, empty);
		}
		
		protected static class Specialized<IN, OUT> extends Switch<IN, OUT> implements Transfer.Specialized<IN, OUT> {
			final Predicate.Specialized<IN> predicate;
			final Transfer.Specialized<IN, OUT> pass;
			final Transfer.Specialized<IN, OUT> fail;

			public Specialized(
					Predicate.Specialized<IN> predicate,
					Transfer.Specialized<IN, OUT> pass, 
					Transfer.Specialized<IN, OUT> fail, 
					OUT empty) {
				super(predicate, pass, fail, empty);
				this.predicate = predicate;
				this.pass = pass;
				this.fail = fail;
				// TODO Auto-generated constructor stub
			}


			@Override
			public OUT at(int x, int y,Aggregates<? extends IN> aggregates) {
				if (predicate.test(x, y, aggregates)) {
					return pass.at(x, y, aggregates);
				} else {
					return fail.at(x, y, aggregates);
				}
			}
		}
		
		/**Test on a specific location in a set of aggregates.**/
		public static interface Predicate<IN> {
			public Predicate.Specialized<IN> specialize(Aggregates<? extends IN> aggs);
			public interface Specialized<IN> extends Predicate<IN> {
				public boolean test(int x, int y, Aggregates<? extends IN> aggs);
			}
		}
	}
	
	
	public static final class Report<IN, OUT> implements Transfer<IN,OUT> {
		private final Transfer<IN,OUT> inner;
		private final String message;
		public Report(Transfer<IN,OUT> inner, String message) {
			this.inner = inner;
			this.message = message;
		}

		public OUT emptyValue() {return inner.emptyValue();}

		@Override
		public ar.Transfer.Specialized<IN, OUT> specialize(
				Aggregates<? extends IN> aggregates) {
			System.out.println(message);
			return inner.specialize(aggregates);
		}
		
	}
}
