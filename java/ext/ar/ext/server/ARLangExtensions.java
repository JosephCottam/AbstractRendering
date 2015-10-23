package ar.ext.server;

import java.awt.Rectangle;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyph;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.app.components.sequentialComposer.OptionDataset;
import ar.app.components.sequentialComposer.OptionTransfer;
import ar.ext.lang.BasicLibrary;
import ar.ext.lang.BasicLibrary.ARConfig;
import ar.ext.lang.Parser;
import ar.ext.server.NanoHTTPD.Response.Status;
import ar.glyphsets.BoundingWrapper;
import ar.glyphsets.FilterGlyphs;
import ar.glyphsets.MemMapList;
import ar.glyphsets.implicitgeometry.Cartography;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.rules.CategoricalCounts;
import ar.rules.General;
import ar.rules.General.Spread.Spreader;
import ar.rules.Numbers;
import ar.selectors.TouchesPixel;
import ar.util.Util;
import static ar.ext.lang.BasicLibrary.get;
import static ar.ext.lang.BasicLibrary.put;

public class ARLangExtensions {
	private static AffineTransform viewTransform;
	private static Map<String, OptionDataset<?,?>> glyphsets;
	
	public static  String getArl(Field f) {
		 try {return ((OptionDataset<?,?>) f.get(null)).arl;}
		 catch (Throwable e) {return "null";}
	}
	
	private static String asList(Collection<Field> items, Function<Field, Object[]> toString, String format) {
		return "<ul>" + items.stream().map(toString).map(e -> String.format(format, e)).collect(Collectors.joining("\n")) + "</ul>\n\n";
	}

	
	public static String help(Collection<Field> datasets) {
		return "<hr>"
				+ "<h3>AR Language:</h3>"
				+ Parser.basicHelp("<br>") + "<br><br>"
				+ "A few examples (base configurations with the transfer spelled out):\n"
				+ asList(datasets, f->new String[]{f.getName(), getArl(f)},"<li><a href='%1$s?arl=%2$s'>%1$s?arl=%2$s</a></li>") + "<br><br>" 
				+ "Available functions:<br>"
				+ Parser.functionHelp(LIBRARY, "<li>%s</li>", "\n");

	}
	
	@SuppressWarnings("rawtypes")
	public static ARConfig parse(String source, AffineTransform vt, Map<String, OptionDataset<?,?>> glyphsets) {
		ARLangExtensions.viewTransform = vt;
		ARLangExtensions.glyphsets = glyphsets;
		
		Parser.TreeNode<?> tree;
		try{tree = Parser.parse(source);}
		catch (Exception e) {
			System.out.println(Parser.tokens(source).stream().collect(Collectors.joining(" -- ")));
			e.printStackTrace();
			throw new RuntimeException(e.getMessage() + "\n While parsing " + source);
		}

		try {
			Object r = Parser.reify(tree, LIBRARY);
			if (r instanceof Transfer) {
				Transfer<?,?> t = (Transfer) r;
				return new ARConfig<>(null, null, t);
			} else if (r instanceof ARConfig) {
				return (ARConfig<?,?,?,?>) r;
			} else {
				throw new IllegalArgumentException("Can only use 'AR' or transfer as as root in ARL for AR server");
			}
		} catch (Throwable e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage() + "\n While processing " + tree.toString());
		}
	}
	
	public static final  Map<String, Function<List<Object>, Object>> LIBRARY = new HashMap<>();
	static {
		LIBRARY.putAll(BasicLibrary.COMMON);
		LIBRARY.putAll(GDelt.LIBRARY);

		put(LIBRARY, "rect", "Make a rectangle (x1, y1, x2, y2)", 
				args->new Rectangle2D.Double((double) args.get(0), (double) args.get(1), 
											 (double) args.get(0) - (double) args.get(2), (double) args.get(1) - (double) args.get(3)));
		
		put(LIBRARY, "data", "Load a named dataset", args->makeGlyphset(args));
		
		put(LIBRARY, "sub", "Binary minus function", args-> new BiFunction<Number, Number, Double>(){public Double apply(Number a, Number b) {return a.doubleValue()-b.doubleValue();}});

		put(LIBRARY, "aggregate", "Basic aggregate configuration: glypshet and aggregate functions.", 
				args -> new Aggregate<>(get(args, 0, null), get(args, 1, null), get(args,2, null), get(args, 3, true)));
		
		put(LIBRARY, "local", "Compute a spatial kernel.  Args: kernel, radius, empty-value",
				args -> makeLocal(get(args, 0, null), get(args, 1, 1), get(args, 2, 0d)));
		
		put(LIBRARY, "zscore", "Replace values with their zscores.",
				args -> new ZScore<>());
		
		put(LIBRARY, "vt", "Get parts fo the view transform: sx,sy,tx,ty", args -> ARLangExtensions.viewTransform(get(args,0,"sx")));
		
		put(LIBRARY, "dynSpread", "Spreading function where the radius is determined at specialization time. The parameter is the target minimum percent of non-empty bins.", 
				args -> new DensitySpread<>(get(args, 0, 10.0)));
		
		put(LIBRARY, "dynScale", "Dyanmically resize based on current view's zoom order-of-mangitude (a modified linear interpolate based on view scale). args: base-zoom, damp",
				args -> dynScale(get(args, 0, 1), get(args, 1, 1)));
		
		put(LIBRARY, "print", "Print out a value at specialization time.  Otherwise acts as echo, returning aggregates equivalent to those passed in.  args: msg",
				args -> new Print<>(get(args,0,"here")));
		
		put(LIBRARY, "alpha", "Simulate single-color alpha composition on simple counts. args: color, alpha value (double)",
				args -> new Numbers.FixedInterpolate<Number>(Color.WHITE, get(args,0,Color.RED), 1, 1/get(args,1,1.0), Util.CLEAR));
	}
	
	public static <G,I> Glyphset<G,I> makeGlyphset(List args) {
		OptionDataset<G,I> baseConfig = (OptionDataset<G,I>) glyphsets.get(args.get(0));
		Map<String, Object> opts = get(args, 1, new HashMap<>());
		Rectangle2D bound = (Rectangle2D) opts.getOrDefault("bound", null);
		Rectangle viewport = (Rectangle) opts.getOrDefault("view", new Rectangle(0,0,400,400));
		boolean latlon = (boolean) opts.getOrDefault("latlon", false);
		boolean allowStretch = (boolean) opts.getOrDefault("stretch", false);
		
		Optional<Rectangle2D> selection = Optional.empty();
		if (latlon) {
			Rectangle2D bounds;
			if (baseConfig.flags.contains(OptionDataset.WEB_MERCATOR)) {
				bounds = Cartography.DegreesToMeters.from(bound);
			} else {
				bounds = bound;
			}
			selection = Optional.of(bounds);
		}
		
		Glyphset<G,I> glyphs;
		Rectangle2D zoomBounds;
		if (selection.isPresent()) {
			zoomBounds = selection.get();
		} else {
			zoomBounds = baseConfig.glyphset.bounds();
		}
		
		AffineTransform vt;
		Rectangle2D renderBounds;
		if (!allowStretch) {
			vt = ARServer.centerFit(zoomBounds, viewport);
			renderBounds = ARServer.expandSelection(vt, zoomBounds, viewport);
			zoomBounds = renderBounds;
		} else {
			vt = ARServer.stretchFit(zoomBounds, viewport);
			renderBounds = zoomBounds;
		}		
		
		if (selection.isPresent()) {
			glyphs = new BoundingWrapper<>(baseConfig.glyphset, zoomBounds);
		} else {
			glyphs = baseConfig.glyphset;
		}
		
		if (baseConfig.flags.contains(OptionDataset.ZERO_COUNTS)) {
			//HACK: VERY Fragile...
			glyphs = new FilterGlyphs<>(glyphs, g -> ((CategoricalCounts) ((Glyph) g).info()).fullSize() > 0);
		}
		
		return glyphs;
	}
	
	public static <V> Transfer<V,V> makeLocal(BiFunction<V,V,V> kernel, int radius, V defVal) {
		if (kernel == null) {throw new IllegalArgumentException("Must supply a kernel function");}
		General.Spread.Spreader<V> spreader = new General.Spread.UnitRectangle<>(radius);
		return new General.Spread<>(spreader, kernel, defVal);
	}
	
	public static final class Context {
		Renderer r;
		AffineTransform vt;
	}
	
	/**Tagging interface, dynamically inspectable to get context->aggregates**/
	public static interface AggregatorFunction<A> extends Function<Context, Aggregates<A>> {}
	
	/**Make a glyphset in the passed context
	 * TODO: Merge this with ar.ext.lang.BasicLibrary.ARConfig
	 */
	public static class Aggregate<G,I,A> implements AggregatorFunction<A> {
		public final Optional<Glyphset<G,I>> glyphs;
		public final Optional<Aggregator<I,A>> agg;
		public final Rectangle2D bounds;
		public Aggregate(Glyphset<G,I> glyphs, Aggregator<I,A> agg, Rectangle2D bounds, boolean latlon) {
			this.glyphs = Optional.ofNullable(glyphs);
			this.agg = Optional.ofNullable(agg);
			this.bounds = bounds != null ? bounds : (glyphs == null ? null : glyphs.bounds()); 
		}
		
		@Override public Aggregates<A> apply(Context t) {return inner(t);}
		
		private Aggregates<A> inner(Context t) {
			Selector<G> s = TouchesPixel.make(glyphs.get());
			return t.r.aggregate(glyphs.get(), s, agg.get(), viewTransform);
		}
	}
	
	public static class Merge<L,R,M> implements AggregatorFunction<M> {
		AggregatorFunction<L> left;
		AggregatorFunction<R> right;
		BiFunction<L,R, M> merge;
		M defVal;
		
		public Merge(AggregatorFunction<L> left, AggregatorFunction<R> right, BiFunction<L,R,M> merge, M defVal) {
			this.left = left;
			this.right = right;
			this.merge = merge;
			this.defVal = defVal;
		}
		
		public Aggregates<M> apply(Context context) {
			Aggregates<L> l = left.apply(context);
			Aggregates<R> r = right.apply(context);
			return AggregateUtils.alignedMerge(l, r, defVal, merge);
		}
		
	}
	public static class ZScore<N extends Number> implements Transfer<N, Double> {

		@Override public Double emptyValue() {return 0d;}
		public Specialized<N> specialize(Aggregates<? extends N> aggregates) {
			Util.Stats<N> stats = Util.stats(aggregates);
			return new Specialized<>(stats.stdev, stats.mean);
		}
		
		public static class Specialized<N extends Number> implements Transfer.ItemWise<N, Double> {
			protected final double stddev;
			protected final double mean;
			public Specialized(double stddev, double mean) {
				this.stddev = stddev;
				this.mean = mean;
			} 
			@Override public Double emptyValue() {return 0d;}

			@Override
			public Double at(int x, int y, Aggregates<? extends N> input) {
				return (input.get(x, y).doubleValue()-mean)/stddev;
			}
			
			
		}
	}
 
	
	

	private static int dynScale(Number baseScale, Number delay) {
		double currentScale = Math.min(viewTransform.getScaleX(), viewTransform.getScaleY());		
		double factor = (currentScale/baseScale.doubleValue())/delay.doubleValue();
		factor = factor < 1 ? 1 : factor;
		System.out.printf("Zoom factor: %s,%s --> %s%n", baseScale, currentScale, factor);
		return (int) factor;
	}
	
	private static double viewTransform(Object arg) {
		switch (arg.toString()) {
			case "sx": return viewTransform.getScaleX();
			case "sy": return viewTransform.getScaleY();
			case "tx": return viewTransform.getTranslateX();
			case "ty": return viewTransform.getTranslateY();
			default: throw new IllegalArgumentException("View transform parameter not recognized: " + arg);
		}
	}
	
	public static class Print<V> implements Transfer.Specialized<V,V> {
		final String msg;
		public Print(Object msg) {this.msg = msg.toString();}
		
		@Override public V emptyValue() {return null;}

		@Override
		public Aggregates<V> process(Aggregates<? extends V> aggregates,Renderer rend) {
			System.out.println(msg);
			return AggregateUtils.copy(aggregates, aggregates.defaultValue());
		}
		
	}

	public static class DensitySpread<V> implements Transfer<V,V> {
		final Double targetCoverage;

		/** 
		 * Spread points by the given radius if the targetCoverage is not met.
		 * Does not guarantee to meet targetCoverage, but will uses it as a target
		 * when calculating the final spread factor.  May exceed the targetCoverage by a small amount.
		 * 
		 * @param targetCoverage Minimum desired non-empty pixel percentage.
		 */
		public DensitySpread(double targetCoverage) {
			this.targetCoverage = targetCoverage;
		}
		
		@Override public V emptyValue() {return null;}
		
		@Override
		public Specialized<V> specialize(Aggregates<? extends V> aggregates) {
			return new Specialized<>(targetCoverage, aggregates);
		}
		
		public static final class Specialized<V> extends DensitySpread<V> implements Transfer.Specialized<V, V> {
			private final Transfer.Specialized<V,V> inner;
			
			public Specialized(Double targetCoverage, Aggregates<? extends V> aggs) {
				super(targetCoverage);
				
				int count = 0;
				V defVal = aggs.defaultValue();
				for (int x= aggs.lowX(); x<aggs.highX(); x++) {
					for (int y=aggs.lowY(); y<aggs.highY(); y++) {
						count = Util.isEqual(aggs.get(x, y), defVal) ? count : count+ 1;
					}
				}
				
				Rectangle bounds = AggregateUtils.bounds(aggs);
				double radius = Math.sqrt((targetCoverage*bounds.width*bounds.height)/(count*Math.PI));
				
				radius = 1+Math.round(radius);
				if (radius < 2) {inner = new General.Echo<>(aggs.defaultValue());}
				else {
					Spreader<V> spreader = new General.Spread.UnitCircle<V>((int) radius);
					inner = new OptionTransfer.FlexSpread<>(spreader, null, null).specialize(aggs);
				}
				
			}

			@Override 
			public Aggregates<V> process(Aggregates<? extends V> aggregates, Renderer rend) {return inner.process(aggregates, rend);}
		}
	}
}
