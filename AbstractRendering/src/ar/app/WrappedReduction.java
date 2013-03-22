package ar.app;

import java.awt.Color;

import ar.Reduction;
import ar.rules.HomoAlpha;
import ar.rules.Overplot;
import ar.rules.TestPatterns;

public interface WrappedReduction<A> {
	public Reduction<A> op();
	public Class<A> type();
	
	public class SolidBlue implements WrappedReduction<Color> {
		public Reduction<Color> op() {return TestPatterns.R(Color.BLUE);}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Blue (color)";}
	} 


	public class OverplotFirst implements WrappedReduction<Color> {
		public Reduction<Color> op() {return Overplot.R(true);}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Overplot First (color)";}
	} 

	public class OverplotLast implements WrappedReduction<Color> {
		public Reduction<Color> op() {return Overplot.R(false);}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Overplot Last (color)";}
	} 

	public class Count implements WrappedReduction<Integer> {
		public Reduction<Integer> op() {return HomoAlpha.R();}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Count (Integer)";}
	}
	


	
}
