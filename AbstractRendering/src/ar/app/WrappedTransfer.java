package ar.app;

import java.awt.Color;

import ar.Transfer;
import ar.rules.Reductions;
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
	
	public class OutlierHighlight implements WrappedTransfer<Integer> {
		public Transfer<Integer> op() {return new Transfers.ZScore(Color.white, Color.red);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Outlier Highlight (int)";}
	}
	
	public class Percent90 implements WrappedTransfer<Reductions.RLE> {
		public Transfer<Reductions.RLE> op() {return new Transfers.FirstPercent(.9, Color.white, Color.blue, Color.red);}
		public Class<Reductions.RLE> type() {return Reductions.RLE.class;}
		public String toString() {return "90% Percent (RLE)";}
	}

	public class Percent95 implements WrappedTransfer<Reductions.RLE> {
		public Transfer<Reductions.RLE> op() {return new Transfers.FirstPercent(.95, Color.white, Color.blue, Color.red);}
		public Class<Reductions.RLE> type() {return Reductions.RLE.class;}
		public String toString() {return "95% Percent (RLE)";}
	}

	public class Percent25 implements WrappedTransfer<Reductions.RLE> {
		public Transfer<Reductions.RLE> op() {return new Transfers.FirstPercent(.25, Color.white, Color.blue, Color.red);}
		public Class<Reductions.RLE> type() {return Reductions.RLE.class;}
		public String toString() {return "25% Percent (RLE)";}
	}
	
	public class EchoColor implements WrappedTransfer<Color> {
		public Transfer<Color> op() {return new Transfers.IDColor();}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Echo (Color)";}
	}
	
	public class HighAlphaLog implements WrappedTransfer<Reductions.RLE> {
		public Transfer<Reductions.RLE> op() {return new Transfers.HighAlpha(Color.white, .1, true);}
		public Class<Reductions.RLE> type() {return Reductions.RLE.class;}
		public String toString() {return "High-Def Alpha (log)";}
	}
	
	public class HighAlphaLin implements WrappedTransfer<Reductions.RLE> {
		public Transfer<Reductions.RLE> op() {return new Transfers.HighAlpha(Color.white, .1, false);}
		public Class<Reductions.RLE> type() {return Reductions.RLE.class;}
		public String toString() {return "High-Def Alpha (Linear)";}
	}
}
