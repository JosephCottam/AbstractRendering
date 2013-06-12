package ar.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import ar.AggregateReducer;
import ar.Aggregates;
import ar.Glyphset.Glyph;

/**Collection of various utilities that don't have other homes.**/
public final class Util {
	/**Color representing clear (fully transparent).**/
	public static final Color CLEAR = new Color(0,0,0,0);

	private Util() {}

	/**Create an RGB buffered image of the given size, filled with the requested color.**/
	public static BufferedImage initImage(int width, int height, Color background) {
		BufferedImage i = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics g = i.getGraphics();
		g.setColor(background);
		g.fillRect(0, 0, width, height);
		g.dispose();
		return i;
	}

	/**Create an indentation string of x*2 spaces.**/
	public static String indent(int x) {
		char[] chars = new char[x*2];
		Arrays.fill(chars,' ');
		return new String(chars);
	}

	/**What bounding box closely contains all of the glyphs in the passed collection.**/
	public static Rectangle2D bounds(Iterable<? extends Glyph<?>> glyphs) {return bounds(glyphs.iterator());}
	
	/**What bounding box closely contains all of the glyphs covered by the iterator.**/
	public static Rectangle2D bounds(Iterator<? extends Glyph<?>> glyphs) {
		Rectangle2D bounds = new Rectangle2D.Double(0,0,-1,-1);
		while (glyphs.hasNext()) {
			Glyph<?> g = glyphs.next();
			if (g == null) {continue;}
			Rectangle2D bound = g.shape().getBounds2D();
			if (bound != null) {add(bounds, bound);}
		}
		return bounds;
	}

	
	/**What bounding box closely contains all of the glyphs passed.**/
	public static Rectangle2D bounds(Rectangle2D... rs) {
		Rectangle2D bounds = new Rectangle2D.Double(0,0,-1,-1);
		for (Rectangle2D r: rs) {
			if (r != null) {add(bounds, r);}
		}
		return bounds;
	}

	
	/**Adds two rectangles together, returning a bounds box over the whole.
	 * Unlike Rectangle2D.union, this method treats NaN as if it were zero.
	 * Total bounds is placed into the first argument.
	 */
	public static void add(Rectangle2D target, Rectangle2D more) {
		double x = more.getX();
		double y = more.getY();
		double w = more.getWidth();
		double h = more.getHeight();

		x = Double.isNaN(x) ? 0 : x;
		y = Double.isNaN(y) ? 0 : y;
		w = Double.isNaN(w) ? 0 : w;
		h = Double.isNaN(h) ? 0 : h;

		if (target.isEmpty()) {
			target.setFrame(x,y,w,h);
		} else if (!more.isEmpty()) {
			target.add(new Rectangle2D.Double(x,y,w,h));
		}
	}

	
	/**Linear interpolation between two colors.
	 * 
	 * @param low Color to associate with the min value
	 * @param high Color to associate with the max value
	 * @param min Smallest value that will be passed
	 * @param max Largest value that will be passed
	 * @param v Current value 
	 * **/
	public static Color interpolate(Color low, Color high, double min, double max, double v) {
		if (v>max) {v=max;}
		if (v<min) {v=min;}
		double distance = 1-((max-v)/(max-min));
		int r = (int) weightedAverage(high.getRed(), low.getRed(), distance);
		int g = (int) weightedAverage(high.getGreen(), low.getGreen(), distance);
		int b = (int) weightedAverage(high.getBlue(), low.getBlue(), distance);
		int a = (int) weightedAverage(high.getAlpha(), low.getAlpha(), distance);
		return new java.awt.Color(r,g,b,a);
	}

	public static Color logInterpolate(Color low, Color high, double min, double max, double v, double basis) {
		if (v>max) {v=max;}
		if (v<min) {v=min;}
		int a = (int) logWeightedAvg(low.getAlpha(), high.getAlpha(), min, max, v, basis);
		Color c = interpolate(low,high, min,max,v);
		return new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue(),a);
	}

	
	/**From "inMens: Realtime visual querying of Big Data" Zhicheng Liu, Biye Jiang and Jeffrey Heer (2013)**/
	public static double expWeightedAvg(double min, double max, double weight, double exp) {
		return min+((1-min)*Math.pow(weight, exp));
	}

	/** Based on "Visual Analysis of Inter-Process Communication for Large-Scale Parallel Computing"
	 *   Chris Muelder, Francois Gygi, and Kwan-Liu Ma (2009)
	 *   
	 * @param rmin Minim range value
	 * @param vmax Value maximum
	 * @param v    Value
	 * @param basis
	 * @return
	 */
	public static double logWeightedAvg(double rmin, double rmax, double vmin, double vmax, double v, double basis) {
		if (v == 0) {return rmin;}
//		double logV= Math.log(v)/Math.log(basis);
//		double logMX = Math.log(vmax)/Math.log(basis);
		double logV = Math.log(v);
		double logMX = Math.log(vmax);
		double p = 1-(logV/logMX);
		return weightedAverage(rmin, rmax, p);
	}
	
	public static double weightedAverage(double v1, double v2, double weight) {
		return (v1 -v2) * weight + v2;
	}


	/**What is the min/max/mean/stdev in the collection of aggregates (assuming its over numbers)**/
	public static Stats stats(Aggregates<? extends Number> aggregates, boolean ignoreZeros) {
		//Single-pass std. dev: http://en.wikipedia.org/wiki/Standard_deviation#Rapid_calculation_methods 
		double count=0;
		double min = Double.POSITIVE_INFINITY, max=Double.NEGATIVE_INFINITY;
		double sum=0;

		for (Number n: aggregates) {
			double v = n.doubleValue();
			if (ignoreZeros && v == 0) {continue;}
			if (min > v) {min = v;}
			if (max < v) {max = v;}
			sum += v;
			count++;
		}

		final double mean = sum/count;
		double acc =0;

		for (Number n: aggregates) {
			final double v = n.doubleValue();
			acc = Math.pow((v-mean),2);
		}
		double stdev = Math.sqrt(acc/count);
		return new Stats(min,max,mean,stdev);
	}

	public static <T extends Number> Aggregates<Double> score(Aggregates<T> source, Stats extrema) {
		final Aggregates<Double> results = new FlatAggregates<Double>(source.highX(), source.highY(), 0d);
		final double mean = extrema.mean;
		final double stdev = extrema.stdev;
		for (int x=0;x<results.lowX();x++) {
			for (int y=0; y<results.highX(); y++) {
				final double v = source.at(x, y).doubleValue();
				final double z = Math.abs((v-mean)/stdev);
				results.set(x, y, z);
			}
		}
		return results;
	}

	public static final class Stats {
		public final double min;
		public final double max;
		public final double mean;
		public final double stdev;
		public Stats(double min, double max, double mean, double stdev) {
			this.min = min; 
			this.max=max;
			this.mean=mean;
			this.stdev = stdev;
		}
		public String toString() {return String.format("Min: %.3f; Max: %.3f; Mean: %.3f; Stdev: %.3f", min,max,mean,stdev);}
	}

	/**Combine two aggregate sets according to the passed reducer.
	 * 
	 * The resulting aggregate set will have a realized subset region sufficient to
	 * cover the realized sbuset region of both source aggregate sets (regardless of 
	 * the values found in those sources).  If one of the two aggregate sets provided
	 * is already of sufficient size, it will be used as both a source and a target.
	 * 
	 * 
	 * @param left Aggregate set to use for left-hand arguments
	 * @param right Aggregate set to use for right-hand arguments
	 * @param red Reduction operation
	 * @return Resulting aggregate set (may be new or a destructively updated left or right parameter) 
	 */
	public static <T> Aggregates<T> reduceAggregates(Aggregates<T> left, Aggregates<T> right, AggregateReducer<T,T,T> red) {
		List<Aggregates<T> >sources = new ArrayList<Aggregates<T>>();
		Aggregates<T> target;
		Rectangle rb = new Rectangle(right.lowX(), right.lowY(), right.highX()-right.lowX(), right.highY()-right.lowY());
		Rectangle lb = new Rectangle(left.lowX(), left.lowY(), left.highX()-left.lowX(), left.highY()-left.lowY());
		Rectangle bounds = rb.union(lb);

		if (lb.contains(bounds)) {
			sources.add(right);
			target = left;
		} else if (rb.contains(bounds)) {
			sources.add(left);
			target = right;
		} else {
			sources.add(right);
			sources.add(left);
			target = new FlatAggregates<T>(bounds.x, bounds.y, bounds.x+bounds.width, bounds.y+bounds.height, red.identity());
		}

		for (Aggregates<T> source: sources) {
			for (int x=Math.max(0, source.lowX()); x<source.highX(); x++) {
				for (int y=Math.max(0, source.lowY()); y<source.highY(); y++) {
					target.set(x,y, red.combine(target.at(x,y), source.at(x,y)));
				}
			}
		}
		return target;
	}

}
