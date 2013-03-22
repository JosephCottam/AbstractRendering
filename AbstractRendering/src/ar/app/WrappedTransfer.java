package ar.app;

import java.awt.Color;

import ar.Transfer;
import ar.rules.HomoAlpha;
import ar.rules.Overplot;

public interface WrappedTransfer<A> {
	public Transfer<A> op();
	public Class<A> type();
	
	public class RedWhiteInterpolate implements WrappedTransfer<Integer> {
		public Transfer<Integer> op() {return HomoAlpha.T(Color.white, Color.red, false);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Red/White Linear (int)";}
	}
	
	public class RedBlueInterpolate implements WrappedTransfer<Integer> {
		public Transfer<Integer> op() {return HomoAlpha.T(Color.blue, Color.red, false);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Red/Blue Linear (int)";}
	}
	
	public class OutlierHighlight implements WrappedTransfer<Integer> {
		public Transfer<Integer> op() {return HomoAlpha.T(Color.white, Color.red, true);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Outlier Highlight (int)";}
	}
	
	public class EchoColor implements WrappedTransfer<Color> {
		public Transfer<Color> op() {return Overplot.T();}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Echo (Color)";}
	}
}
