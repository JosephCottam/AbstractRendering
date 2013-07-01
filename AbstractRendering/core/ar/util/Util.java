package ar.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import ar.AggregateReducer;
import ar.Aggregates;
import ar.Glyphset.Glyph;
import ar.aggregates.ConstantAggregates;
import ar.aggregates.FlatAggregates;

/**Collection of various utilities that don't have other homes.**/
public final class Util {
	/**Color representing clear (fully transparent).**/
	public static final Color CLEAR = new Color(0,0,0,0);

	private Util() {}

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
		int a = (int) logWeightedAvg(low.getAlpha(), high.getAlpha(), max, v, basis);
		Color c = interpolate(low,high, min,max,v);
		return new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue(),a);
	}

	
	/**From "inMens: Realtime visual querying of Big Data" Zhicheng Liu, Biye Jiang and Jeffrey Heer (2013)**/
	public static double expWeightedAvg(double min, double max, double weight, double exp) {
		return weightedAverage(min, max, Math.pow(weight, exp));
	}

	/** Based on "Visual Analysis of Inter-Process Communication for Large-Scale Parallel Computing"
	 *   Chris Muelder, Francois Gygi, and Kwan-Liu Ma (2009)
	 *   
	 * @param rmin Minim range value
	 * @param vmax Value maximum
	 * @param v    Value
	 * @param basis
	 */
	public static double logWeightedAvg(double rmin, double rmax,  double vmax, double v, double basis) {
		if (v == 0) {return rmin;}
		double logV= Math.log(v)/Math.log(basis);
		double logMX = Math.log(vmax)/Math.log(basis);
		double p = 1-(logV/logMX);
		return weightedAverage(rmin, rmax, p);
	}
	
	public static double weightedAverage(double v1, double v2, double weight) {
		return (v1 -v2) * weight + v2;
	}


	/**What is the min/max/mean/stdev in the collection of aggregates (assuming its over numbers).
	 * 
	 * NaN's are skipped.
	 * 
	 * **/
	public static Stats stats(Aggregates<? extends Number> aggregates, boolean ignoreZeros) {
		//Single-pass std. dev: http://en.wikipedia.org/wiki/Standard_deviation#Rapid_calculation_methods 
		double count=0;
		double min = Double.POSITIVE_INFINITY, max=Double.NEGATIVE_INFINITY;
		double sum=0;

		for (Number n: aggregates) {
			double v = n.doubleValue();
			if (ignoreZeros && v == 0) {continue;}
			if (n.doubleValue() == Double.NaN) {continue;}
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
	
	/**Null-safe .equals caller.**/
	public static <T> boolean isEqual(T one, T two) {
		return one == two || (one != null && one.equals(two));
	}
	
	/**Calculate the affine transform to fit a box of the given size/location onto a 0,0,width,height space.**/
	public static AffineTransform zoomFit(Rectangle2D content, int width, int height) {
		if (content == null) {return new AffineTransform();}

		double ws = width/content.getWidth();
		double hs = height/content.getHeight();
		double scale = Math.min(ws, hs);
		double tx = content.getCenterX();
		double ty = content.getCenterY();

		AffineTransform t = AffineTransform.getScaleInstance(scale,scale);
		t.translate(tx,ty);
		return t;
	}
	
	/**From a set of color aggregates, make a new image.**/
	public static BufferedImage asImage(Aggregates<Color> aggs, int width, int height, Color background) {
		BufferedImage i = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics g = i.getGraphics();
		g.setColor(background);
		g.fillRect(0, 0, width, height);
		g.dispose();
		for (int x=Math.max(0, aggs.lowX()); x<Math.min(width, aggs.highX()); x++) {
			for (int y=Math.max(0, aggs.lowY()); y<Math.min(width, aggs.highY()); y++) {
				Color c = aggs.at(x, y);
				if (c != null) {i.setRGB(x, y, c.getRGB());}			
			}			
		}
		return i;
	}

}
