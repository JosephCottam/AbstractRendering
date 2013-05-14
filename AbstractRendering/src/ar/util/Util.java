package ar.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

import ar.Aggregates;
import ar.GlyphSet;
import ar.GlyphSet.Glyph;

public final class Util {
	public static final Color CLEAR = new Color(0,0,0,0);

	private Util() {}

	public static BufferedImage initImage(int width, int height, Color background) {
		BufferedImage i = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics g = i.getGraphics();
		g.setColor(background);
		g.fillRect(0, 0, width, height);
		g.dispose();
		return i;
	}

	public static String indent(int x) {
		char[] chars = new char[x*2];
		Arrays.fill(chars,' ');
		return new String(chars);
	}
	
	public static Rectangle2D bounds(Iterable<Glyph> glyphs) {
		Rectangle2D bounds = new Rectangle2D.Double(0,0,-1,-1);
		for (Glyph g: glyphs) {
			Rectangle2D bound = g.shape.getBounds2D();
			if (bound != null) {add(bounds, bound);}
		}
		return bounds;
	}

	public static Rectangle2D fullBounds(Rectangle2D... rs) {
		Rectangle2D bounds = new Rectangle2D.Double(0,0,-1,-1);
		for (Rectangle2D r: rs) {
			if (r != null) {add(bounds, r);}
		}
		return bounds;
	}

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
		final Aggregates<Double> results = new Aggregates<Double>(source.highX(), source.highY(), 0d);
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

}
