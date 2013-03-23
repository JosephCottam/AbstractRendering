package ar.rules;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;

import ar.*;

public class HomoAlpha {
	public static class Count implements Reduction<Integer> {
		public Integer at(int x, int y, GlyphSet glyphs, AffineTransform v) {
			Point2D p = new Point2D.Double(x,y);
			v.transform(p, p);
			return glyphs.containing(p).size();
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
}
