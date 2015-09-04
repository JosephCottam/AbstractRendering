package ar.ext.server;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import ar.Aggregates;
import ar.Renderer;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.app.components.sequentialComposer.OptionDataset;
import ar.app.components.sequentialComposer.OptionTransfer;
import ar.ext.lang.BasicLibrary;
import ar.ext.lang.Parser;
import ar.rules.General;
import ar.rules.General.Spread.Spreader;
import ar.util.Util;
import static ar.ext.lang.BasicLibrary.get;
import static ar.ext.lang.BasicLibrary.put;

public class ARLangExtensions {
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
				+ Parser.functionHelp(BasicLibrary.COMMON, "<li>%s</li>", "\n");

	}
	
	public static Transfer<?,?> parseTransfer(String source, AffineTransform vt) {
		ARLangExtensions.viewTransform = vt;
		
		Parser.TreeNode<?> tree;
		try{tree = Parser.parse(source);}
		catch (Exception e) {
			System.out.println(Parser.tokens(source).stream().collect(Collectors.joining(" -- ")));
			e.printStackTrace();
			throw new RuntimeException(e.getMessage() + "\n While parsing " + source);
		}

		
		try {
			return (Transfer<?,?>) Parser.reify(tree, LIBRARY);
		} catch (Throwable e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage() + "\n While processing " + tree.toString());
		}
	}
	
	public static final  Map<String, Function<List<Object>, Object>> LIBRARY = new HashMap<>();
	static {
		LIBRARY.putAll(BasicLibrary.COMMON);
		 put(LIBRARY, "vt", "Get parts fo the view transform: sx,sy,tx,ty", args -> ARLangExtensions.viewTransform(get(args,0,"sx")));
		
		put(LIBRARY, "dynSpread", "Spreading function where the radius is determined at specialization time. The parameter is the target minimum percent of non-empty bins.", 
				args -> new DensitySpread<>(get(args, 0, 10.0)));
		
		put(LIBRARY, "dynScale", "Dyanmically resize based on current view's zoom order-of-mangitude (a modified linear interpolate based on view scale). args: base-zoom, damp",
				args -> dynScale(get(args, 0, 1), get(args, 1, 1)));

	}

	private static AffineTransform viewTransform;

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
					inner = new OptionTransfer.FlexSpread<>(spreader).specialize(aggs);
				}
				
			}

			@Override 
			public Aggregates<V> process(Aggregates<? extends V> aggregates, Renderer rend) {return inner.process(aggregates, rend);}
		}
	}
	
}
