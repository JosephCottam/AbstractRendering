package ar.rules;

import java.awt.Color;

import ar.Aggregates;
import ar.Renderer;
import ar.Transfer;
import static ar.rules.combinators.Combinators.*;
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
	public static class Gradient implements Transfer.ItemWise<Object, Color> {
		private static final long serialVersionUID = 2620947228987431184L;

		public boolean equals(Object other) {return other instanceof Gradient;}
		public int hashCode() {return Gradient.class.hashCode();}

		@Override public Color emptyValue() {return Util.CLEAR;}

		@Override 
		public ar.Transfer.Specialized<Object, Color> specialize(
				Aggregates<? extends Object> aggregates) {return this;}
		
		@Override
		public Aggregates<Color> process(
				Aggregates<? extends Object> aggregates, Renderer rend) {
			return rend.transfer(aggregates, this);
		}
		@Override
		public Color at(int x, int y, Aggregates<? extends Object> aggregates) {
			float width =aggregates.highX()-aggregates.lowX();
			float height = aggregates.highY()-aggregates.lowY();
			return new Color((x-aggregates.lowX())/width, (y-aggregates.lowY())/height,.5f ,1.0f); 
		}
	}
	

	/**Throw an error during some phase.**/
	public static class TransferError<IN,OUT> implements Transfer<IN,OUT> {
		public enum PHASE {ID, SPEC, COMBINE, ROLLUP, PROCESS}
		protected final RuntimeException toThrow = new RuntimeException("Forced Error");
		protected final PHASE phase;
		protected final Transfer<IN,OUT> base;
		
		public TransferError(Transfer<IN,OUT> base, PHASE phase) {
			this.phase = phase;
			this.base = base;
		}
		
		public OUT emptyValue() {
			if (phase != PHASE.ID) {throw toThrow;}
			return base.emptyValue();
		}

		public ar.Transfer.Specialized<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {
			if (phase == PHASE.SPEC) {throw toThrow;}
			return new Specialized<>(base.specialize(aggregates), phase);
		}

		public static class Specialized<IN,OUT> extends TransferError<IN,OUT> implements Transfer.Specialized<IN, OUT> {
			private final Transfer.Specialized<IN, OUT> base;
			public Specialized(Transfer.Specialized<IN, OUT> base,
					ar.rules.Debug.TransferError.PHASE phase) {
				super(base, phase);
				this.base = base;
			}

			@Override
			public Aggregates<OUT> process(Aggregates<? extends IN> aggregates, Renderer rend) {
				if (phase == PHASE.PROCESS) {throw toThrow;}
				return rend.transfer(aggregates, base);
			}
		}
	}
	

	/**Print out statistics on an aggregate set.**/
	public static final class Stats<IN extends Number, OUT> implements Transfer<IN,OUT> {
		private final Transfer<IN,OUT> inner;
		
		private Stats(Transfer<IN,OUT> inner) {this.inner = inner;}
		
		/**Print out statistics before the passed function's specialization occurs.**/
		public static <IN extends Number, OUT> Stats<IN, OUT> before(Transfer<IN,OUT> inner) {
			return new Stats<>(inner);
		}
		
		/**Print out statistics before after passed function has executed.**/
		public static <IN, OUT extends Number> Transfer<IN, OUT> after(Transfer<IN,OUT> inner, OUT val) {
			return seq().then(inner).then(new Stats<>(new General.Echo<>(val)));
		}

		public OUT emptyValue() {return inner.emptyValue();}

		@Override
		public ar.Transfer.Specialized<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {
			Util.Stats<?> s= Util.stats(aggregates, false, false, false);
			System.out.println(s.toString());
			return inner.specialize(aggregates);
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
