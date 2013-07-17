package ar.app.util;

import java.awt.Color;

import ar.Aggregator;
import ar.rules.CategoricalCounts;
import ar.rules.Debug;
import ar.rules.General;
import ar.rules.Categories;
import ar.rules.Numbers;
import ar.util.Util;

public interface WrappedAggregator<G,A> extends Wrapped<Aggregator<G,A>> {
	public Aggregator<G,A> op();
	
	public class SolidBlue implements WrappedAggregator<Object, Color> {
		public Aggregator<Object, Color> op() {return new General.Const<Color>(Color.BLUE);}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Blue (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	} 

	public class Gradient implements WrappedAggregator<Object, Color> {
		public Aggregator<Object, Color> op() {return new Debug.Gradient(500, 500);}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Gradient 500 (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	} 

	public class OverplotFirst implements WrappedAggregator<Color, Color> {
		public Aggregator<Color, Color> op() {return new Categories.First();}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Overplot First (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	} 

	public class OverplotLast implements WrappedAggregator<Color, Color> {
		public Aggregator<Color, Color> op() {return new Categories.Last();}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Overplot Last (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	} 

	public class Count implements WrappedAggregator<Object, Integer> {
		public Aggregator<Object, Integer> op() {return new Numbers.Count();}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Count (int)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	}
	
	public class RLEColors implements WrappedAggregator<Color, CategoricalCounts.CoC<Color>> {
		public Aggregator<Color, CategoricalCounts.CoC<Color>> op() {
			return new Categories.CountCategories<Color>(Util.COLOR_SORTER, Color.class);
		}
		public Class<CategoricalCounts.CoC> type() {return CategoricalCounts.CoC.class;}
		public String toString() {return "Color RLE sorted (RLE)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	}
	
	public class RLEUnsortColors implements WrappedAggregator<Color, CategoricalCounts.RLE<Color>> {
		public Aggregator<Color, CategoricalCounts.RLE<Color>> op() {
			return new Categories.RunLengthEncode<Color>(Color.class);
		}
		public Class<CategoricalCounts.RLE> type() {return CategoricalCounts.RLE.class;}
		public String toString() {return "Color RLE (RLE)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
	}
}
