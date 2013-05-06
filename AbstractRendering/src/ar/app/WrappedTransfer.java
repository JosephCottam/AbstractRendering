package ar.app;

import java.awt.Color;

import ar.Transfer;
import ar.rules.Aggregators;
import ar.rules.Transfers;

public interface WrappedTransfer<A> {
	public Transfer<A> op();
	public Class<A> type();
	
	public class RedWhiteInterpolate implements WrappedTransfer<Integer> {
		public Transfer<Integer> op() {return new Transfers.Direct(Color.white, Color.red);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Red/White Linear (int)";}
	}
	
	public class RedBlueInterpolate implements WrappedTransfer<Integer> {
		public Transfer<Integer> op() {return new Transfers.Direct(Color.blue, Color.red);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Red/Blue Linear (int)";}
	}
	
	public class FixedAlpha implements WrappedTransfer<Integer> {
		public Transfer<Integer> op() {return new Transfers.FixedAlpha(Color.white, Color.red, 0, 25.5);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "10% Alpha (int)";}
	}
	
	public class OutlierHighlight implements WrappedTransfer<Integer> {
		public Transfer<Integer> op() {return new Transfers.ZScore(Color.white, Color.red, true);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Outlier Highlight (int)";}
	}
	
	public class OutlierHighlightB implements WrappedTransfer<Integer> {
		public Transfer<Integer> op() {return new Transfers.ZScore(Color.white, Color.red, false);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Outlier Highlight w/0's (int)";}
	}

	
	public class Percent90 implements WrappedTransfer<Aggregators.RLE> {
		public Transfer<Aggregators.RLE> op() {return new Transfers.FirstPercent(.9, Color.white, Color.blue, Color.red);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "90% Percent (RLE)";}
	}

	public class Percent95 implements WrappedTransfer<Aggregators.RLE> {
		public Transfer<Aggregators.RLE> op() {return new Transfers.FirstPercent(.95, Color.white, Color.blue, Color.red);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "95% Percent (RLE)";}
	}

	public class Percent25 implements WrappedTransfer<Aggregators.RLE> {
		public Transfer<Aggregators.RLE> op() {return new Transfers.FirstPercent(.25, Color.white, Color.blue, Color.red);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "25% Percent (RLE)";}
	}
	
	public class EchoColor implements WrappedTransfer<Color> {
		public Transfer<Color> op() {return new Transfers.IDColor();}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Echo (Color)";}
	}
	
	public class HighAlphaLog implements WrappedTransfer<Aggregators.RLE> {
		public Transfer<Aggregators.RLE> op() {return new Transfers.HighAlpha(Color.white, .1, true);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "Log HD Alpha (RLE)";}
	}
	
	public class HighAlphaLin implements WrappedTransfer<Aggregators.RLE> {
		public Transfer<Aggregators.RLE> op() {return new Transfers.HighAlpha(Color.white, .1, false);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "Linear HD Alpha (RLE)";}
	}
}
