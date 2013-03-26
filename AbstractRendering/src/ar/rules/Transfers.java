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
		final boolean zeros;
		private Aggregates<Integer> cacheKey;	//Could be a weak-reference instead...
		private Aggregates<Double> scored;
		private Util.Stats stats;
		
		public ZScore(Color low, Color high, boolean zeros) {
			this.low = low;
			this.high = high;
			this.zeros = zeros;
		}
		
		public Color at(int x, int y, Aggregates<Integer> aggregates) {
			if (cacheKey == null || cacheKey != aggregates) {
				stats = Util.stats(aggregates, zeros);
				scored = Util.score(aggregates, stats);
				stats = Util.stats(scored, false);
				cacheKey = aggregates;
			}
			
			if (aggregates.at(x, y) ==0 ) {return Util.CLEAR;}
			return Util.interpolate(low, high, stats.min, stats.max, scored.at(x, y));
		}
	}
	
	public static final class FixedAlpha implements Transfer<Integer> {
		final Color low, high;
		final double lowv, highv;
		
		public FixedAlpha(Color low, Color high, double lowV, double highV) {
			this.low = low;
			this.high = high;
			this.lowv = lowV;
			this.highv = highV;
		}
		
		public Color at(int x, int y, Aggregates<Integer> aggregates) {
			return Util.interpolate(low, high, lowv, highv, aggregates.at(x, y));
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
	
	public static final class HighAlpha implements Transfer<Reductions.RLE> {
		private final Color background;
		private boolean log;
		private double omin;
		private Aggregates<Color> colors;
		private Aggregates<Reductions.RLE> cacheKey;
		
		public HighAlpha(Color background, double omin, boolean log) {
			this.background = background;
			this.log = log;
			this.omin = omin;
		}
		
		private Color fullInterpolate(Reductions.RLE rle) {
			double total = rle.fullSize();
			double r = 0;
			double g = 0;
			double b = 0;
			
			for (int i=0; i< rle.size(); i++) {
				Color c = (Color) rle.key(i);
				double a2 = rle.count(i)/total;
				double r2 = (c.getRed()/255.0) * a2;
				double g2 = (c.getGreen()/255.0) * a2;
				double b2 = (c.getBlue()/255.0) * a2;

				r += r2;
				g += g2;
				b += b2;
			}
			return new Color((int) (r*255), (int) (g * 255), (int) (b*255));
		}
		
		public Color at(int x, int y, Aggregates<Reductions.RLE> aggregates) {
			if (aggregates!=cacheKey) {
				System.out.println("----------------------");
				double max =0;
				colors = new Aggregates<Color>(aggregates.width(), aggregates.height());
				for (Reductions.RLE rle:aggregates) {max = Math.max(max,rle.fullSize());}
				for (int xi=0; xi<aggregates.width(); xi++) {
					for (int yi =0; yi<aggregates.height(); yi++) {
						Reductions.RLE rle = aggregates.at(xi, yi);
						Color c;
						if (rle.fullSize() == 0) {c = background;}
						else {
							c = fullInterpolate(rle);
							double alpha;
							if (log) {
								alpha = omin + ((1-omin) * (rle.fullSize()/max)); 
							} else {
								alpha = omin + ((1-omin) * (Math.log(rle.fullSize())/Math.log(max)));
							}
							c = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (alpha*255));
						}
						colors.set(xi, yi, c);
					}
				}
				cacheKey = aggregates;
			}
			return colors.at(x, y);			
		}
	}
	
	public static final class FirstItem implements Transfer<Reductions.RLE> {
		private final Color background;
		public FirstItem(Color background) {
			this.background = background;
		}
		public Color at(int x, int y, Aggregates<Reductions.RLE> aggregates) {
			Reductions.RLE rle = aggregates.at(x,y);
			double size = rle.fullSize();			
			if (size == 0) {return background;}
			else {return (Color) rle.key(0);}
		}
	}
	


}
