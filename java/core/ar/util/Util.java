package ar.util;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import javax.imageio.ImageIO;

import ar.Aggregates;
import ar.Glyph;
import ar.Glyphset;
import ar.glyphsets.SimpleGlyph;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.glyphsets.implicitgeometry.Indexed.Converter;
import ar.util.MemMapEncoder.TYPE;

/**Collection of various utilities that don't have other homes.**/
public final class Util {
	/**Color representing clear (fully transparent).**/
	public static final Color CLEAR = new Color(0,0,0,0);

	private Util() {}
	
	/**Convert from the types understood by the memory mappers to the types understood by this system.**/
	public static final Converter.TYPE[] transcodeTypes(TYPE... types) {
		Converter.TYPE[] newTypes = new Converter.TYPE[types.length];
		for (int i=0; i< types.length; i++) {
			switch(types[i]) {
				case X: newTypes[i] = Converter.TYPE.X; break;
				case INT: newTypes[i] = Converter.TYPE.INT; break;
				case SHORT: newTypes[i] = Converter.TYPE.SHORT; break;
				case LONG: newTypes[i] = Converter.TYPE.LONG; break;
				case DOUBLE: newTypes[i] = Converter.TYPE.DOUBLE; break;
				case FLOAT: newTypes[i] = Converter.TYPE.FLOAT; break;
				default: throw new UnsupportedOperationException("Cannot perform conversion to " + types[i]);
			}
		}
		return newTypes;
	}
	
	
	///---------- TOOLS FOR PRETENDING THAT POINTS ARE SHAPES TOO!! ----------------------------
	/**Check for intersection between a rectangle and another (presumably) geometric object.**/
	public static boolean intersects(Rectangle2D r, Object o) {
		if (o instanceof Point2D) {return intersects(r, (Point2D) o);}
		if (o instanceof Shape) {return intersects(r, (Shape) o);}
		throw new IllegalArgumentException("Object passed must be either a shape or a point.");
	}
	public static boolean intersects(Rectangle2D r, Point2D p) {return r.contains(p);}
	public static boolean intersects(Rectangle2D r, Shape s) {return s.intersects(r);}
	
	public static Rectangle2D boundOne(Object o) {
		if (o instanceof Point2D) {return boundOne((Point2D) o);}
		if (o instanceof Shape) {return boundOne((Shape) o);}
		throw new IllegalArgumentException("Object passed must be either a shape or a point.  Recieved: " + o.getClass().getName());		
	}
	public static Rectangle2D boundOne(Shape s) {return s.getBounds2D();}
	public static Rectangle2D boundOne(Point2D p) {return new Rectangle2D.Double(p.getX(), p.getY(), Double.MIN_VALUE, Double.MIN_VALUE);}
	///------------------------------------------------------------------------------------------------	


	/**What bounding box closely contains all of the glyphs in the passed collection.**/
	public static <G> Rectangle2D bounds(Iterable<? extends Glyph<G, ?>> glyphs) {return bounds(glyphs.iterator());}
	
	/**What bounding box closely contains all of the glyphs covered by the iterator.**/
	public static <G> Rectangle2D bounds(Iterator<? extends Glyph<G, ?>> glyphs) {
		Rectangle2D bounds = new Rectangle2D.Double(0,0,-1,-1);
		while (glyphs.hasNext()) {
			Glyph<G, ?> g = glyphs.next();
			if (g == null) {continue;}
			G shape = g.shape();
			if (shape instanceof Point2D) {
				bounds.add((Point2D) shape);
			} else if (shape instanceof Shape) {
				Rectangle2D bound = ((Shape) shape).getBounds2D();
				if (bound != null) {add(bounds, bound);}
			}
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


	
	/**Mean of two values.**/
	public static final int mean(int low, int high) {return low+((high-low)/2);}
	public static final long mean(long low, long high) {return low+((high-low)/2);}

	
	/**Load a set of glyphs from a delimited reader, using the provided shaper and valuer.
	 * 
	 * This method creates concrete geometry, though it uses the implicit geometry system to achieve it. 
	 * 
	 * @param glyphs Glyphset to load items into
	 * @param reader Source of the glyph data
	 * @param converter Convert read entries to indexed entries
	 * @param shaper Convert the read item into a shape
	 * @param valuer Convert the read item into a value
	 * @return The glyphset passed in as a parameter (now with more glyphs)
	 */
	public static <G,I> Glyphset<G,I> load(
			Glyphset<G,I> glyphs, 
			DelimitedReader reader, 
			Indexed.Converter converter, 
			Shaper<G, Indexed> shaper, 
			Valuer<Indexed, I> valuer) {
		int count =0;
		
		Method m;
		try {m = glyphs.getClass().getMethod("add", Glyph.class);}
		catch (NoSuchMethodException | SecurityException e1) {throw new IllegalArgumentException("Cannot access 'add' on the passed glypshet.", e1);}
		m.setAccessible(true); //Suppress java access checking.  Allows access to (for example) public methods of private classes


		while (reader.hasNext()) {
			String[] parts = reader.next();
			if (parts == null) {continue;}
			
			Converter item = converter.applyTo(parts);
			I value = valuer.value(item);
			G shape = shaper.shape(item);

			Glyph<G,I> g = new SimpleGlyph<G,I>(shape, value);
			try {m.invoke(glyphs, g);}
			catch (Exception e) {throw new RuntimeException("Error loading item number " + count, e);}
			count++;
		}
		//The check below causes an issue if memory is tight...the check has a non-trivial overhead on some glyphset types
		if (count != glyphs.size()) {throw new RuntimeException(String.format("Error loading data; Read and retained glyph counts don't match (%s read vs %s retained).", count, glyphs.size()));}
		return glyphs;
	}
	
	/**Sort a set of colors.**/
	public static final Comparator<Color> COLOR_SORTER  = new Comparator<Color>() {
		public int compare(Color o1, Color o2) {
			return o1.getRGB() - o2.getRGB();
		}
	};

	
	/**Create an indentation string of x*2 spaces.**/
	public static String indent(int x) {
		char[] chars = new char[x*2];
		Arrays.fill(chars,' ');
		return new String(chars);
	}
		
	/**Adds two rectangles together, updating the first so it is a bounds over the whole.
	 * Unlike Rectangle2D.add, this method treats NaN as if it were zero.
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

	/**Log weight average v between min and max, then do a linear interpolation between low and high.
	 * @param low The color to associate with the minimum value
	 * @param high The color to associate with the maximum value
	 * @param min The lowest value to expect
	 * @param max The highest value to expect
	 * @param v The current value under consideration
	 * @param basis The basis for the log
	 * @return A color between low and high proportional to the log-distance of v between min and max
	 * **/
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
	
	/**Weighted average between two values
	 * 
	 * @param min The lowest value to expect
	 * @param max The highest value to expect
	 * @param p The desired percentage offset between max and min 
	 * @return The resulting value
	 */
	public static double weightedAverage(double min, double max, double p) {
		return (min-max) * p + max;
	}


	/**What is the min/max/mean/stdev in the collection of aggregates (assuming its over numbers).
	 * 
	 * NaN's are skipped.
	 * 
	 * **/
	public static <N extends Number> Stats<N> stats(Aggregates<? extends N> aggregates, boolean ignoreZeros) {
		//Single-pass std. dev: http://en.wikipedia.org/wiki/Standard_deviation#Rapid_calculation_methods 
		long count=0;
		N min = null;
		N max = null;
		double sum=0;

		for (N n: aggregates) {
			double v = n.doubleValue();
			if (ignoreZeros && v == 0) {continue;}
			if (n.doubleValue() == Double.NaN) {continue;}
			if (min == null || min.doubleValue() > v) {min = n;}
			if (max == null || max.doubleValue() < v) {max = n;}
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
		
		return new Stats<>(min,max,mean,stdev);
	}


	/**Wrapper class for statistical values derived from a common source.**/
	@SuppressWarnings("javadoc")
	public static final class Stats<N extends Number> {
		public final N min;
		public final N max;
		public final double mean;
		public final double stdev;
		public Stats(N min, N max, double mean, double stdev) {
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
		double xmargin = width/scale-content.getWidth();
		double ymargin = height/scale-content.getHeight();
		double tx = content.getMinX()-(xmargin/2);
		double ty = content.getMinY()-(ymargin/2);

		AffineTransform t = AffineTransform.getScaleInstance(scale,scale);
		t.translate(-tx,-ty);
		return t;
	}
	
	/**Write a buffered image to a file.**/
	public static void writeImage(BufferedImage img, File f) {
		try {
			if (f.getParentFile() != null && !f.getParentFile().exists()) {
				f.getParentFile().mkdirs();
			}
			ImageIO.write(img, "png", f);
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**Comparator to wrap the compareTo method of comparable items.**/
	public static class ComparableComparator<T extends Comparable<T>> implements Comparator<T> {
		public int compare(T lhs, T rhs) {return lhs.compareTo(rhs);}
	}
	
	/**Insert a value into an array at the given index.**/
	public static final <T> T[] insertInto(T[] values, T value, int at) {
		@SuppressWarnings("unchecked")
		T[] newValues = (T[]) Array.newInstance(value.getClass(), values.length+1);
		System.arraycopy(values, 0, newValues, 0, at);
		newValues[at] = value;
		System.arraycopy(values, at, newValues, at+1, values.length-at);
		return newValues;
	}
	
	/**Insert a value into an array at the given index.**/
	public static final int[] insertInto(int[] values, int value, int at) {
		int[] newValues = new int[values.length+1];
		System.arraycopy(values, 0, newValues, 0, at);
		newValues[at] = value;
		System.arraycopy(values, at, newValues, at+1, values.length-at);
		return newValues;
	}
}
