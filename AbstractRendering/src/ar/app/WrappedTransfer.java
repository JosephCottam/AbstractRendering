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
	
	public class Percent90 implements WrappedTransfer<Reductions.CountPair> {
		public Transfer<Reductions.CountPair> op() {return new Transfers.CountPercent(.9, Color.white, Color.blue, Color.red);}
		public Class<Reductions.CountPair> type() {return Reductions.CountPair.class;}
		public String toString() {return "Percent 90% (Pairs)";}
	}

	public class Percent95 implements WrappedTransfer<Reductions.CountPair> {
		public Transfer<Reductions.CountPair> op() {return new Transfers.CountPercent(.95, Color.white, Color.blue, Color.red);}
		public Class<Reductions.CountPair> type() {return Reductions.CountPair.class;}
		public String toString() {return "Percent 95% (Pairs)";}
	}

	
	public class EchoColor implements WrappedTransfer<Color> {
		public Transfer<Color> op() {return new Transfers.IDColor();}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Echo (Color)";}
	}
}
