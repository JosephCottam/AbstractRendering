package ar.rules;

import ar.Aggregates;
import ar.Transfer;
import ar.util.Util;

/**Simple mathematical operations encoded as transfer functions
 * @author jcottam
 *
 */
public final class TransferMath {
	private TransferMath() {}

	
	/**Divide all items by a given value.*/
	public static class DivideInt implements Transfer.Specialized<Integer, Integer> {
		private final int denominator;
		
		/**@param denominator Value to divide by*/
		public DivideInt(int denominator) {this.denominator = denominator;}
		public Integer emptyValue() {return 0;}
		public ar.Transfer.Specialized<Integer, Integer> specialize(
				Aggregates<? extends Integer> aggregates) {return this;}
		public Integer at(int x, int y, Aggregates<? extends Integer> aggregates) {
			int val = aggregates.get(x, y).intValue()/denominator;
			return val;
		}
	}
	
	public static class DivideDouble implements Transfer.Specialized<Number, Double> {
		private final double denominator;

		/**@param denominator Value to divide by*/
		public DivideDouble(double denominator) {this.denominator = denominator;}
		public Double emptyValue() {return 0d;}
		public ar.Transfer.Specialized<Number, Double> specialize(
				Aggregates<? extends Number> aggregates) {return this;}
		public Double at(int x, int y, Aggregates<? extends Number> aggregates) {
			return aggregates.get(x, y).doubleValue()/denominator;
		}
	}
	
	/**Perform log based on the double-value of the input.  
	 * 
	 * If basis is set to 0, will just echo input values.
	 */
	public static class LogDouble implements Transfer.Specialized<Number, Double> {
		private final double basis;
		private final Number emptyInput;
		public LogDouble(double base, Number emptyInput) {
			this.basis = base;
			this.emptyInput = emptyInput;
		}
		
		public Double emptyValue() {return 0d;}
		
		public ar.Transfer.Specialized<Number, Double> specialize(
				Aggregates<? extends Number> aggregates) {return this;}
		
		public Double at(int x, int y, Aggregates<? extends Number> aggregates) {
			Number val = aggregates.get(x,y);
			if (Util.isEqual(val, emptyInput)) {return emptyValue();}
			else if (basis == 0) {return val.doubleValue();}
			else if (basis == Math.E) {return Math.log(val.doubleValue());}
			else if (basis == 10) {return Math.log10(val.doubleValue());}
			else {return Math.log(val.doubleValue())/Math.log(basis);}
		}		
	}
}
