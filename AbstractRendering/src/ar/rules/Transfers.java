package ar.rules;

import java.awt.Color;

import ar.Aggregates;
import ar.Transfer;
import ar.Util;
import ar.rules.Reductions.CountPair;

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
	
	public static final class CountPercent implements Transfer<Reductions.CountPair> {
		private final double ratio;
		private final Color background, match, noMatch;
		public CountPercent(double ratio, Color background, Color match, Color noMatch) {
			this.ratio = ratio;
			this.background = background;
			this.match = match;
			this.noMatch = noMatch;
		}
		public Color at(int x, int y, Aggregates<CountPair> aggregates) {
			Reductions.CountPair pair = aggregates.at(x,y);
			double sum = pair.two + pair.one;
			if (sum == 0) {return background;}
			if (pair.one/sum >= ratio) {return match;}
			else {return noMatch;}
		}
		
	}

}
