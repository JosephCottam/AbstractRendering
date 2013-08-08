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
		public int hashCode() {return this.getClass().hashCode();}
		public Class<?> input() {return Object.class;}
		public Class<?> output() {return Color.class;}
	} 

	public class Gradient implements WrappedAggregator<Object, Color> {
		public Aggregator<Object, Color> op() {return new Debug.Gradient(500, 500);}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Gradient 500 (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
		public int hashCode() {return this.getClass().hashCode();}
		public Class<?> input() {return Object.class;}
		public Class<?> output() {return Color.class;}
	} 

	public class OverplotFirst implements WrappedAggregator<Color, Color> {
		public Aggregator<Color, Color> op() {return new Categories.First();}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Overplot First (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
		public int hashCode() {return this.getClass().hashCode();}
		public Class<?> input() {return Color.class;}
		public Class<?> output() {return Color.class;}
	} 

	public class OverplotLast implements WrappedAggregator<Color, Color> {
		public Aggregator<Color, Color> op() {return new Categories.Last();}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Overplot Last (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
		public int hashCode() {return this.getClass().hashCode();}
		public Class<?> input() {return Color.class;}
		public Class<?> output() {return Color.class;}
	} 

	public class Count implements WrappedAggregator<Object, Integer> {
		public Aggregator<Object, Integer> op() {return new Numbers.Count<Object>();}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Count (int)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
		public int hashCode() {return this.getClass().hashCode();}
		public Class<?> input() {return Object.class;}
		public Class<?> output() {return Integer.class;}
	}
	
	public class RLEColors implements WrappedAggregator<Color, CategoricalCounts.CoC<Color>> {
		public Aggregator<Color, CategoricalCounts.CoC<Color>> op() {
			return new Categories.CountCategories<Color>(Util.COLOR_SORTER);
		}
		public String toString() {return "Color RLE sorted (RLE)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
		public int hashCode() {return this.getClass().hashCode();}
		public Class<?> input() {return Color.class;}
		public Class<?> output() {return CategoricalCounts.CoC.class;}
	}
	
	public class RLEUnsortColors implements WrappedAggregator<Color, CategoricalCounts.RLE<Color>> {
		public Aggregator<Color, CategoricalCounts.RLE<Color>> op() {
			return new Categories.RunLengthEncode<Color>();
		}
		public String toString() {return "Color RLE (RLE)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
		public int hashCode() {return this.getClass().hashCode();}
		public Class<?> input() {return Color.class;}
		public Class<?> output() {return CategoricalCounts.RLE.class;}

	}
}
