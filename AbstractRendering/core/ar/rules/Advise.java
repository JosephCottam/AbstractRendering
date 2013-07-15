package ar.rules;

import java.awt.Color;
import java.awt.Point;
import java.util.Comparator;

import ar.Aggregates;
import ar.Transfer;
import ar.util.Util;

public class Advise {
	//TODO: Extend to reporting the magnitude of the under-saturation
	//TODO: Should this look at mins instead-of/in-addition-to empty?
	//TODO: What if there are multiple "smallest" values?
	//TODO: What about "perceptual differences" vs just absolute differences
	public static class UnderSaturate<A,B> implements Transfer<A, Boolean> {
		final Transfer<A,B> ref;
		public UnderSaturate(Transfer<A,B> reference) {this.ref = reference;}
		public Class<A> input() {return ref.input();} 
		public Class<Boolean> output() {return Boolean.class;}
		public Boolean emptyValue() {return false;}
		public Boolean at(int x, int y, Aggregates<? extends A> aggregates) {
			A def = aggregates.defaultValue();
			A val = aggregates.at(x, y);
			B empty = ref.emptyValue();
			B out = ref.at(x, y, aggregates);
			return !Util.isEqual(val, def) && Util.isEqual(empty, out); 
		}
	}
	
	//TODO: Extend to reporting the magnitude of the over-saturation
	//TODO: What if there are multiple "largest" values?
	//TODO: What about "perceptual differences" vs just absolute differences
	public static class OverSaturate<A,B> implements Transfer<A, Boolean> {
		final Transfer<A,B> ref;
		private A max;
		private B top;
		private Aggregates<? extends A> cacheKey;
		private Comparator<A> comp;
		public OverSaturate(Transfer<A,B> reference, Comparator<A> comp) {
			this.ref = reference;
			this.comp = comp;
		}
		public Class<A> input() {return ref.input();} 
		public Class<Boolean> output() {return Boolean.class;}
		public Boolean emptyValue() {return false;}
		public Boolean at(int x, int y, Aggregates<? extends A> aggregates) {
			if (!Util.isEqual(cacheKey, aggregates)) {
				Point p = max(aggregates, comp);
				max = aggregates.at(p.x, p.y);
				top = ref.at(p.x,p.y, aggregates);
				cacheKey = aggregates;
			}
			A val = aggregates.at(x, y);
			B out = ref.at(x, y, aggregates);
			return !Util.isEqual(val, max) && Util.isEqual(top, out); 
		}
	}
	
	
	public static class OverUnder implements Transfer<Number, Color> {
		private final Transfer<Number, Color> base;
		private final Transfer<Number, Boolean> under;
		private final Transfer<Number, Boolean> over;
		private final Color overColor, underColor;
		
		public OverUnder(Color overColor, Color underColor, Transfer<Number, Color> base) {
			this.overColor = overColor;
			this.underColor = underColor;
			this.base = base;
			this.under = new Advise.UnderSaturate<Number, Color>(base);
			this.over = new Advise.OverSaturate<Number, Color>(base, new NumberComp());
		}
		
		public Color at(int x, int y, Aggregates<? extends Number> aggregates) {
			boolean below = under.at(x, y, aggregates);
			boolean above = over.at(x, y, aggregates);
			if (above) {
				return overColor;
			} else if (below) {
				return underColor;
			} else {
				return base.at(x, y, aggregates);
			}
		}

		public Class<Number> input() {return Number.class;}
		public Class<Color> output() {return Color.class;}
		public Color emptyValue() {return base.emptyValue();}
	}
	
	
	private static class NumberComp implements Comparator<Number> {
		public int compare(Number o1, Number o2) {return (int) (o1.doubleValue()-o2.doubleValue());}
	}

	/**Find the smallest value.  
	 * 
	 * @param aggs Set of aggregates to search
	 * @param comp Comparator used in the search
	 * @return The location of the first "smallest" value 
	 */
	public static <A> Point min(Aggregates<? extends A> aggs, Comparator<A> comp) {
		A min = aggs.iterator().next();
		Point p = new Point(-1,-1);
		for (int x=aggs.lowX(); x<aggs.highX(); x++) {
			for (int y=aggs.lowY(); y<aggs.highY(); y++) {
				A val = aggs.at(x,y);
				int v = comp.compare(val, min);
				if (v < 0) {
					min = val;
					p.setLocation(x, y);
				}
				
			}
		}
		return p;
	}
	
	/**Find the largest value.  
	 * 
	 * @param aggs Set of aggregates to search
	 * @param comp Comparator used in the search
	 * @return The location of the first "largest" value 
	 */
	public static <A> Point max(Aggregates<? extends A> aggs, Comparator<A> comp) {
		A max = aggs.iterator().next();
		Point p = new Point(-1,-1);
		for (int x=aggs.lowX(); x<aggs.highX(); x++) {
			for (int y=aggs.lowY(); y<aggs.highY(); y++) {
				A val = aggs.at(x,y);
				int v = comp.compare(val, max);
				if (v > 0) {
					max = val;
					p.setLocation(x, y);
				}
			}
		}
		return p;
	}
}
