package ar.rules;

import java.util.List;

import ar.Aggregates;
import ar.Aggregator;
import ar.Transfer;

public class General {
	public static final class Const<T> implements Aggregator<Object,T> {
		private final T val;
		public Const(T val) {this.val = val;}
		public Class<?> input() {return Object.class;}
		public Class<?> output() {return Object.class;}
		public T combine(long x, long y, T left, Object update) {return val;}
		public T rollup(List<T> sources) {return val;}
		public T identity() {return val;}
	}


	/**Return what is found at the given location.**/
	public static final class Echo<T> implements Transfer<T,T> {
		private final Class<T> type;
		private final T empty;
		public Echo(T empty, Class<T> type) {this.empty = empty; this.type = type;}
		public T at(int x, int y, Aggregates<? extends T> aggregates) {return aggregates.at(x, y);}

		public T emptyValue() {return empty;}
		public Class<T> input() {return type;}
		public Class<T> output() {return type;}
	}

	/**Return the given value when presented with a non-empty value.**/
	public static final class Present<IN, OUT> implements Transfer<IN,OUT> {
		private final OUT present, absent;
		private final Class<OUT> outputType;
		
		public Present(OUT present, OUT absent, Class<OUT> outType) {
			this.present = present; 
			this.absent=absent;
			outputType = outType;
		}
		
		public OUT at(int x, int y, Aggregates<? extends IN> aggregates) {
			Object v = aggregates.at(x, y);
			if (v != null && !v.equals(aggregates.defaultValue())) {return present;}
			return absent;
		}
		
		public OUT emptyValue() {return absent;}
		public Class<Object> input() {return Object.class;}
		public Class<OUT> output() {return outputType;}
	}
	
}
