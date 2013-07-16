package ar.rules;

import java.awt.Color;

import ar.Aggregates;
import ar.Transfer;
import ar.util.Util;

public class General {
	/**Return the color stored in the aggregate set;
	 * essentially a pass-through.**/
	public static final class ID<T> implements Transfer<T,T> {
		private final Class<T> type;
		private final T empty;
		public ID(T empty, Class<T> type) {this.empty = empty; this.type = type;}
		public T at(int x, int y, Aggregates<? extends T> aggregates) {return aggregates.at(x, y);}

		public T emptyValue() {return empty;}
		public Class<T> input() {return type;}
		public Class<T> output() {return type;}
	}

	

	/**Return the given value when presented with a non-empty value.**/
	public static final class Present<IN,OUT> implements Transfer<IN,OUT> {
		private final OUT present, absent;
		private final Class<IN> inputType;
		private final Class<OUT> outputType;
		
		public Present(OUT present, OUT absent, Class<IN> inType, Class<OUT> outType) {
			this.present = present; 
			this.absent=absent;
			inputType = inType;
			outputType = outType;
		}
		
		public OUT at(int x, int y, Aggregates<? extends IN> aggregates) {
			Object v = aggregates.at(x, y);
			if (v != null && !v.equals(aggregates.defaultValue())) {return present;}
			return absent;
		}
		
		public OUT emptyValue() {return absent;}
		public Class<IN> input() {return inputType;}
		public Class<OUT> output() {return outputType;}
	}
	
}
