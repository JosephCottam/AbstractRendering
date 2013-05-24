package ar.rules;

import java.awt.Color;

import ar.Aggregates;
import ar.Transfer;
import ar.util.Util;


/**Implementation of common transfer functions.**/
public class Transfers {

	/**Return the color stored in the aggregate set;
	 * essentially a pass-through for aggregators that produce colors.**/
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
	
	/**Switch between two colors depending on the percent contribution of
	 * a specified category.
	 * 
	 * TODO: Convert from RLE to CoC based
	 * 
	 * **/
	public static final class FirstPercent implements Transfer<Aggregators.RLE> {
		private final double ratio;
		private final Color background, match, noMatch;
		private final Object firstKey;
		
		public FirstPercent(double ratio, Object firstKey,  Color background, Color match, Color noMatch) {
			this.ratio = ratio;
			this.background = background;
			this.match = match;
			this.noMatch = noMatch;
			this.firstKey = firstKey;
		}
		
		public Color at(int x, int y, Aggregates<Aggregators.RLE> aggregates) {
			Aggregators.RLE rle = aggregates.at(x,y);
			double size = rle.fullSize();
			
			if (size == 0) {return background;}
			else if (!rle.key(0).equals(firstKey)) {return noMatch;} 
			else if (rle.count(0)/size >= ratio) {return match;}
			else {return noMatch;}
		}
		
	}
	
	
	/**Performs high-definition alpha composition on a run-length encoding.
	 * High-definition alpha composition computes color compositions in double space
	 * with knowledge of the full range of compositions that will be required.
	 * (See "Visual Analysis of Inter-Process Communication for Large-Scale Parallel Computing"
	 *  by Chris Muelder, Francois Gygi, and Kwan-Liu Ma).
	 *  
	 * @author jcottam
	 *
	 */
	public static final class HighAlpha implements Transfer<Aggregators.RLE> {
		private final Color background;
		private boolean log;
		private double omin;
		private Aggregates<Color> colors;
		private Aggregates<Aggregators.RLE> cacheKey;
		
		public HighAlpha(Color background, double omin, boolean log) {
			this.background = background;
			this.log = log;
			this.omin = omin;
		}
		
		private Color fullInterpolate(Aggregators.RLE rle) {
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
		
		public Color at(int x, int y, Aggregates<Aggregators.RLE> aggregates) {
			if (aggregates!=cacheKey) {
				double max =0;
				colors = new Aggregates<Color>(aggregates.highX(), aggregates.highY(), Color.WHITE);
				for (Aggregators.RLE rle:aggregates) {max = Math.max(max,rle.fullSize());}
				for (int xi=0; xi<aggregates.highX(); xi++) {
					for (int yi =0; yi<aggregates.highY(); yi++) {
						Aggregators.RLE rle = aggregates.at(xi, yi);
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
	
	/**Pull the first item from a run-lenght encoding.**/
	public static final class FirstItem implements Transfer<Aggregators.RLE> {
		private final Color background;
		public FirstItem(Color background) {
			this.background = background;
		}
		public Color at(int x, int y, Aggregates<Aggregators.RLE> aggregates) {
			Aggregators.RLE rle = aggregates.at(x,y);
			double size = rle.fullSize();			
			if (size == 0) {return background;}
			else {return (Color) rle.key(0);}
		}
	}
	


}
