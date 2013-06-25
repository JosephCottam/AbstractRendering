package ar.app.util;

import java.awt.Color;

import ar.Aggregator;
import ar.rules.Aggregators;

public interface WrappedAggregator<G,A> extends Wrapped<Aggregator<G,A>> {
	public Aggregator<G,A> op();
	
	public class SolidBlue implements WrappedAggregator<Object, Color> {
		public Aggregator<Object, Color> op() {return new Aggregators.IDColor(Color.BLUE);}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Blue (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	} 

	public class Gradient implements WrappedAggregator<Object, Color> {
		public Aggregator<Object, Color> op() {return new Aggregators.Gradient(500, 500);}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Gradient 500 (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	} 

	public class OverplotFirst implements WrappedAggregator<Color, Color> {
		public Aggregator<Color, Color> op() {return new Aggregators.First();}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Overplot First (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	} 

	public class OverplotLast implements WrappedAggregator<Color, Color> {
		public Aggregator<Color, Color> op() {return new Aggregators.Last();}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Overplot Last (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	} 

	public class Count implements WrappedAggregator<Object, Integer> {
		public Aggregator<Object, Integer> op() {return new Aggregators.Count();}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Count (int)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	}
	
  public class RLEColorsTL implements WrappedAggregator<Color, Aggregators.RLE> {
		public Aggregator<Color, Aggregators.RLE> op() {return new Aggregators.RLEColor(true,true);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "Color RLE sorted & TL (RLE)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	}
	
	public class RLEColors implements WrappedAggregator<Color, Aggregators.RLE> {
		public Aggregator<Color, Aggregators.RLE> op() {return new Aggregators.RLEColor(true);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "Color RLE sorted (RLE)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	}
	
	public class RLEUnsortColors implements WrappedAggregator<Color, Aggregators.RLE> {
		public Aggregator<Color, Aggregators.RLE> op() {return new Aggregators.RLEColor(false);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "Color RLE (RLE)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	}
	
	public class DeltaNeighbors implements WrappedAggregator<Object, Integer> {
		public Aggregator<Object, Integer> op() {return new Aggregators.DeltaNeighbors(5);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Delta neighbors 5 (int)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	}

	
}
