package ar.rules;

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
	/**Aggregator that always returns the same value.**/
	public static final class Const<T> implements Aggregator<Object,T> {
		private static final long serialVersionUID = 2274344808417248367L;
		private final T val;
		/**@param val Value to return**/
		public Const(T val) {this.val = val;}
		public T combine(long x, long y, T left, Object update) {return val;}
		public T rollup(List<T> sources) {return val;}
		public T identity() {return val;}
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
