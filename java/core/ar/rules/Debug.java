package ar.rules;

import java.awt.Color;

import ar.Aggregates;
import ar.Aggregator;
import ar.Transfer;
import ar.util.Util;

/**Classes used largely for debugging.
 * May be instructive for reference though.
 * 
 */
@SuppressWarnings("javadoc")
public class Debug {

	/**Compute a gradient across the 2D space.  
	 * This class was used largely for debugging; it ignores its inputs. 
	 */
	public static final class Gradient implements Aggregator<Object, Color> {
		private static final long serialVersionUID = 2620947228987431184L;
		private final float width,height;

		public Gradient(int width, int height) {this.width=width; this.height=height;}

		public Color identity() {return Util.CLEAR;}
		public boolean equals(Object other) {return other instanceof Gradient;}
		public int hashCode() {return Gradient.class.hashCode();}

		public Color combine(long x, long y, Color left, Object update) {
			return new Color(x/width, y/height,.5f ,1.0f); 
		}

		public Color rollup(Color left, Color right) {
			int r = left.getRed() + right.getRed();
			int g = left.getGreen() + right.getGreen();
			int b = left.getBlue() + right.getBlue();
			int a = left.getAlpha() + right.getAlpha();
			return new Color(r/2,g/2,b/2,a/2);
		}
	}
	

	
	/**Will print a message on specialization.**/
	public static final class Report<IN, OUT> implements Transfer<IN,OUT> {
		private final Transfer<IN,OUT> inner;
		private final String message;
		
		
		/**
		 * @param inner Transfer function to actually perform.
		 * @param message Message to print at specialization time.
		 */
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
