package ar.app.util;

import java.awt.Color;

import ar.Aggregator;
import ar.rules.CategoricalCounts;
import ar.rules.General;
import ar.rules.Categories;
import ar.rules.Numbers;
import ar.util.Util;

public interface WrappedAggregator<G,A> extends Wrapped<Aggregator<G,A>> {
	public Aggregator<G,A> op();
	
	public class SolidBlue implements WrappedAggregator<Object, Color> {
		public Aggregator<Object, Color> op() {return new General.Const<>(Color.BLUE);}
		public String toString() {return "Blue (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
		public int hashCode() {return this.getClass().hashCode();}
	} 

	public class OverplotFirst implements WrappedAggregator<Color, Color> {
		public Aggregator<Color, Color> op() {return new Categories.First();}
		public String toString() {return "Overplot First (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
		public int hashCode() {return this.getClass().hashCode();}
	} 

	public class OverplotLast implements WrappedAggregator<Color, Color> {
		public Aggregator<Color, Color> op() {return new General.Last<>(Util.CLEAR);}
		public String toString() {return "Overplot Last (color)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
		public int hashCode() {return this.getClass().hashCode();}
	} 

	public class Count implements WrappedAggregator<Object, Integer> {
		public Aggregator<Object, Integer> op() {return new Numbers.Count<Object>();}
		public String toString() {return "Count (int)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
		public int hashCode() {return this.getClass().hashCode();}
	}
	
	public class CoCColors implements WrappedAggregator<Color, CategoricalCounts<Color>> {
		public Aggregator<Color, CategoricalCounts<Color>> op() {
			return new Categories.CountCategories<Color>(Util.COLOR_SORTER);
		}
		public String toString() {return "Color RLE sorted (RLE)";}
		public boolean equals(Object other) {return other.toString().equals(this.toString());}
		public int hashCode() {return this.getClass().hashCode();}
	}
	
}
