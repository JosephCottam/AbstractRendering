package ar.app;

import java.awt.Color;

import ar.Reduction;
import ar.Transfer;
import ar.rules.*;

public interface Ruleset<A> {
	public Reduction<A> R();
	public Transfer<A> T();
	
	
	
	
	public static final class AlphaCount implements Ruleset<Integer> {
		public Reduction<Integer> R() {return HomoAlpha.R();}
		public Transfer<Integer> T() {return HomoAlpha.T(Color.white, Color.red, false);}
		public String toString() {return "Count-based Alpha";}
	}

	
	public static final class OutlierAlpha implements Ruleset<Integer> {
		public Reduction<Integer> R() {return HomoAlpha.R();}
		public Transfer<Integer> T() {return HomoAlpha.T(Color.white, Color.red, true);}
		public String toString() {return "Outlier Alpha";}
	}

	public static final class OverplotLast implements Ruleset<Color> {
		public Reduction<Color> R() {return Overplot.R(false);}
		public Transfer<Color> T() {return Overplot.T();}
		public String toString() {return "Overplot (last write wins)";}
	}

	public static final class OverplotFirst implements Ruleset<Color> {
		public Reduction<Color> R() {return Overplot.R(true);}
		public Transfer<Color> T() {return Overplot.T();}
		public String toString() {return "Overplot (first write wins)";}
	}
	
	public static final class SolidBlue implements Ruleset<Color> {
		public Reduction<Color> R() {return TestPatterns.R(Color.BLUE);}
		public Transfer<Color> T() {return TestPatterns.T();}
		public String toString() {return "Solid Blue";}		
	}
 }
