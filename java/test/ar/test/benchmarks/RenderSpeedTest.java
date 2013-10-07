package ar.test.benchmarks;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.File;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;
import ar.Transfer.Specialized;
import ar.aggregates.FlatAggregates;
import ar.app.components.Presets;
import ar.app.util.GlyphsetUtils;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;
import ar.glyphsets.*;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Valuer.Constant;
import ar.glyphsets.implicitgeometry.Indexed.ToRect;
import ar.renderers.ParallelGlyphs;
import ar.renderers.ParallelSpatial;
import ar.renderers.SerialSpatial;
import ar.rules.Advise;
import ar.rules.Categories;
import ar.rules.CategoricalCounts.CoC;
import ar.rules.CategoricalCounts.RLE;
import ar.rules.Numbers;
import ar.util.Util;

/**Tests the amount of time to render count visualizations.
 * **/
@SuppressWarnings("unused")
public class RenderSpeedTest {
	private static String arg(String[] args, String flag, String def) {
		flag = flag.toUpperCase();
		for (int i=0; i<args.length; i++) {
			if (args[i].toUpperCase().equals(flag)) {return args[i+1];}
		}
		return def;
	}
	
	public static void main(String[] args) throws Exception {
		int iterations = Integer.parseInt(arg(args, "-iters", "10"));
		int cores = Integer.parseInt(arg(args, "-p", Integer.toString(Runtime.getRuntime().availableProcessors())));
		int task = Integer.parseInt(arg(args, "-task", "100000"));
		String config = arg(args, "-config", "USPopulation");
		String rend = arg(args, "-rend", "glyph").toUpperCase();
		int width = Integer.parseInt(arg(args, "-width", "500"));
		int height = Integer.parseInt(arg(args, "-height", "500"));
		boolean header = Boolean.valueOf(arg(args, "-header", "true"));
		
		//Aggregator<Object,Integer> aggregator = new WrappedAggregator.Count().op();
		
		//Transfer<Number,Color> transfer = new Numbers.Interpolate(new Color(255,0,0,38), Color.red);
		//Transfer<Number,Color> transfer = new RenderSpeedTest.CachelessInterpolate(new Color(255,0,0,38), Color.red);
		//Transfer<Number,Color> transfer = new CachelessDrawDark(Color.white, Color.black, 5);
		//Transfer<Number,Color> transfer = new CachelessDrawDark(Color.white, Color.black, 5);		
		//Transfer<Number,Color> transfer = new WrappedTransfer.FixedAlpha().op();

		Presets.Preset source = null;
		for (Class clss: Presets.class.getClasses()) {
			if (clss.getSimpleName().equals(config)) {
				source = (Presets.Preset) clss.getConstructor().newInstance();
			}
		}
		if (source == null) {throw new IllegalArgumentException("Could not find -config indicated: " + config);}

		
		Aggregator aggregator = source.aggregator();
		Transfer transfer = source.transfer();
		Glyphset glyphs = source.glyphset();
	
		ParallelGlyphs.THREAD_POOL_SIZE = cores;
		ParallelSpatial.THREAD_POOL_SIZE = cores;
		
		Renderer render;
		if (rend.startsWith("GLYPH")) {
			render = new ParallelGlyphs(task);
		} else if (rend.startsWith("PIXEL")) {
			render = new ParallelSpatial(task);
		} else if (rend.startsWith("SPIXEL")) {
			render = new SerialSpatial();
		} else {
			throw new IllegalArgumentException("Renderer type not known: " + rend);
		}
		glyphs.bounds(); //Force bounds calc to only happen once...hopefully
		AffineTransform ivt = Util.zoomFit(glyphs.bounds(), width, height).createInverse();
		
		if (header) {
			System.out.println("source, elapse/avg agg, elapse/avg trans, iter num, width, height, renderer, cores, task-size");
		}
		
		try {
			for (int i=0; i<iterations; i++) {
				long start = System.currentTimeMillis();
				Aggregates<Integer> aggs = render.aggregate(glyphs, aggregator, ivt, width, height);
				long end = System.currentTimeMillis();
				long aggTime = end-start;

				start = System.currentTimeMillis();
				Transfer.Specialized<Number,Color> ts = transfer.specialize(aggs);
				Aggregates<Color> colors = render.transfer(aggs, ts);
				end = System.currentTimeMillis();
				long transTime = end-start;

				aggs.get(0, 0);
				colors.get(0, 0);
				System.out.printf("%s, %d, %d, %d, %d, %d, %s, %d, %d\n", source, aggTime, transTime, i, width, height, rend, cores, task);
				System.out.flush();
			}
		} catch (Exception e) {
			System.out.println("Error testing " + source);
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	/**HD interpolation between two colors EXCEPT re-calculate the extrema at each pixel.**/
	public static final class CachelessInterpolate implements Transfer.Specialized<Number, Color> {
		private static final long serialVersionUID = 2878901447280244237L;
		private final Color low, high, empty;
		private final int logBasis;
		
		/**
		 * @param low Color to associate with lowest input value
		 * @param high Color to associate with highest input value
		 */
		public CachelessInterpolate(Color low, Color high) {this(low,high, Util.CLEAR, 0);}
		
		/**
		 * @param low Color to associate with lowest input value
		 * @param high Color to associate with highest input value
		 * @param empty Color to return when the default aggregate value is encountered
		 * @param logBasis Log basis to use; Value less than 1 signals linear interpolation
		 */
		public CachelessInterpolate(Color low, Color high, Color empty, int logBasis) {
			this.low = low;
			this.high = high;
			this.empty = empty;
			this.logBasis = logBasis;
		}
		
		public Color at(int x, int y, Aggregates<? extends Number> aggregates) {
			Number v = aggregates.get(x,y);
			if (v.equals(aggregates.defaultValue())) {
				return empty;
			}
			Util.Stats extrema = Util.stats(aggregates, false);
			
			if (logBasis <= 1) {
				return Util.interpolate(low, high, extrema.min, extrema.max, v.doubleValue());
			} else {
				return Util.logInterpolate(low,high, extrema.min, extrema.max, v.doubleValue(), logBasis);
			}
		}

		public Specialized<Number,Color> specialize(Aggregates<? extends Number> aggregates) {return this;}
		
		public Color emptyValue() {return Util.CLEAR;}
	}
	
	public static class CachelessDrawDark implements Transfer.Specialized<Number, Color> {
		private static final long serialVersionUID = 4417984252053517048L;
		
		/**How large is the neighborhood?**/
		public final int distance;
		
		/**Transfer function used to determine the colors after the ratios have been determined.**/
		public final Transfer<Number, Color> inner;
		Aggregates<Double> cached;
		
		/**
		 * @param low Color to represent average or low value in the neighborhood
		 * @param high Color to represent high value for the neighborhood
		 * @param distance Distance that defines the neighborhood.
		 */
		public CachelessDrawDark(Color low, Color high, int distance) {
			this.distance=distance;
			inner = new Numbers.Interpolate(low,high,high,-1);
		}
	
		public Color at(int x, int y, Aggregates<? extends Number> aggregates) {
			Transfer.Specialized<Number,Color> innerS = specializeTo(aggregates);
			return innerS.at(x,y,cached);
		}

		public Specialized<Number,Color> specialize(Aggregates<? extends Number> aggs) {return this;}

		public Specialized<Number,Color> specializeTo(Aggregates<? extends Number> aggs) {
			this.cached = new FlatAggregates<>(aggs.lowX(), aggs.lowY(), aggs.highX(), aggs.highY(), Double.NaN);
			for (int x=aggs.lowX(); x <aggs.highX(); x++) {
				for (int y=aggs.lowY(); y<aggs.highY(); y++) {
					if (aggs.get(x, y).doubleValue() > 0) {
						cached.set(x, y, preprocOne(x,y,aggs));
					} else {
						cached.set(x,y, Double.NaN);
					}
				}
			}
			return inner.specialize(cached);
		}
		
		private double preprocOne(int x, int y, Aggregates<? extends Number> aggregates) {
			double surroundingSum =0;
			int cellCount = 0;
			for (int dx=-distance; dx<=distance; dx++) {
				for (int dy=-distance; dy<=distance; dy++) {
					int cx=x+dx;
					int cy=y+dy;
					if (cx < aggregates.lowX() || cy < aggregates.lowY() 
							|| cx>aggregates.highX() || cy> aggregates.highY()) {continue;}
					cellCount++;
					double dv = aggregates.get(cx,cy).doubleValue();
					if (dv != 0) {surroundingSum++;}
				}
			}
			return surroundingSum/cellCount;
		}

		public Color emptyValue() {return Util.CLEAR;}
	}
	
}

