package ar.app;

import java.awt.Color;

import ar.Reduction;
import ar.rules.Reductions;

public interface WrappedReduction<A> {
	public Reduction<A> op();
	public Class<A> type();
	
	public class SolidBlue implements WrappedReduction<Color> {
		public Reduction<Color> op() {return new Reductions.IDColor(Color.BLUE);}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Blue (color)";}
	} 

	public class Gradient implements WrappedReduction<Color> {
		public Reduction<Color> op() {return new Reductions.Gradient(500, 500);}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Gradient 500 (color)";}
	} 

	public class OverplotFirst implements WrappedReduction<Color> {
		public Reduction<Color> op() {return new Reductions.First();}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Overplot First (color)";}
	} 

	public class OverplotLast implements WrappedReduction<Color> {
		public Reduction<Color> op() {return new Reductions.Last();}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Overplot Last (color)";}
	} 

	public class Count implements WrappedReduction<Integer> {
		public Reduction<Integer> op() {return new Reductions.Count();}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Count (int)";}
	}
	
	public class CountPairs implements WrappedReduction<Reductions.CountPair> {
		public Reduction<Reductions.CountPair> op() {return new Reductions.RLESortedColor();}
		public Class<Reductions.CountPair> type() {return Reductions.CountPair.class;}
		public String toString() {return "RLE Pairs (Pairs)";}

	}
	


	
}
