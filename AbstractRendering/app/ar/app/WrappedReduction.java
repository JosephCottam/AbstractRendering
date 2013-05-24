package ar.app;

import java.awt.Color;

import ar.Aggregator;
import ar.rules.Aggregators;

public interface WrappedReduction<A> {
	public Aggregator<A> op();
	public Class<A> type();
	
	public class SolidBlue implements WrappedReduction<Color> {
		public Aggregator<Color> op() {return new Aggregators.IDColor(Color.BLUE);}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Blue (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	} 

	public class Gradient implements WrappedReduction<Color> {
		public Aggregator<Color> op() {return new Aggregators.Gradient(500, 500);}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Gradient 500 (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	} 

	public class OverplotFirst implements WrappedReduction<Color> {
		public Aggregator<Color> op() {return new Aggregators.First();}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Overplot First (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	} 

	public class OverplotLast implements WrappedReduction<Color> {
		public Aggregator<Color> op() {return new Aggregators.Last();}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Overplot Last (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	} 

	public class Count implements WrappedReduction<Integer> {
		public Aggregator<Integer> op() {return new Aggregators.Count();}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Count (int)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	}
	
  public class RLEColorsTL implements WrappedReduction<Aggregators.RLE> {
		public Aggregator<Aggregators.RLE> op() {return new Aggregators.RLEColor(true,true);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "Color RLE sorted & TL (RLE)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	}
	
	public class RLEColors implements WrappedReduction<Aggregators.RLE> {
		public Aggregator<Aggregators.RLE> op() {return new Aggregators.RLEColor(true);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "Color RLE sorted (RLE)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	}
	
	public class RLEUnsortColors implements WrappedReduction<Aggregators.RLE> {
		public Aggregator<Aggregators.RLE> op() {return new Aggregators.RLEColor(false);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "Color RLE (RLE)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	}
	
	public class DeltaNeighbors implements WrappedReduction<Integer> {
		public Aggregator<Integer> op() {return new Aggregators.DeltaNeighbors(5);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Delta neighbors 5 (int)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	}

	
}
