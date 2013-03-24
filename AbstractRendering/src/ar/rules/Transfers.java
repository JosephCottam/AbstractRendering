package ar.rules;

import java.awt.Color;

import ar.Aggregates;
import ar.Transfer;
import ar.Util;

public class Transfers {

	public static final class IDColor implements Transfer<Color> {
		public Color at(int x, int y, Aggregates<Color> aggregates) {return aggregates.at(x, y);}
	}

	public static final class ZScore implements Transfer<Integer> {
		final Color low, high;
		private Aggregates<Integer> cacheKey;	//Could be a weak-reference instead...
		private Aggregates<Double> scored;
		private Util.Stats stats;
		
		public ZScore(Color low, Color high) {
			this.low = low;
			this.high = high;
		}
		
		public Color at(int x, int y, Aggregates<Integer> aggregates) {
			if (cacheKey == null || cacheKey != aggregates) {
				stats = Util.stats(aggregates, true);
				scored = Util.score(aggregates, stats);
				stats = Util.stats(scored, false);
				cacheKey = aggregates;
			}
			
			if (aggregates.at(x, y) ==0 ) {return Util.CLEAR;}
			return Util.interpolate(low, high, stats.min, stats.max, scored.at(x, y));
		}
	}

	public static final class Direct implements Transfer<Integer> {
		final Color low, high;
		private Aggregates<Integer> cacheKey;	//Could be a weak-reference instead...
		private Util.Stats extrema;
		
		public Direct(Color low, Color high) {
			this.low = low;
			this.high = high;
		}
		
		public Color at(int x, int y, Aggregates<Integer> aggregates) {
			if (cacheKey == null || cacheKey != aggregates) {
				extrema = Util.stats(aggregates, false);
				cacheKey = aggregates;
			}
			return Util.interpolate(low, high, extrema.min, extrema.max, aggregates.at(x, y));
		}
	}
	
	/**Percent of total contributed by the first item**/
	public static final class FirstPercent implements Transfer<Reductions.RLE> {
		private final double ratio;
		private final Color background, match, noMatch;
		public FirstPercent(double ratio, Color background, Color match, Color noMatch) {
			this.ratio = ratio;
			this.background = background;
			this.match = match;
			this.noMatch = noMatch;
		}
		public Color at(int x, int y, Aggregates<Reductions.RLE> aggregates) {
			Reductions.RLE rle = aggregates.at(x,y);
			double size = rle.fullSize();
			
			if (size == 0) {return background;}
			else if (rle.key(0).equals(Color.RED)) {return noMatch;}	//HACK: The use of "RED" here derives from the BGL vis and is not general purpose 
			else if (rle.count(0)/size >= ratio) {return match;}
			else {return noMatch;}
		}
		
	}

}
