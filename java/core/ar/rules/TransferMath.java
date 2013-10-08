package ar.rules;

import ar.Aggregates;
import ar.Transfer;

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
}
