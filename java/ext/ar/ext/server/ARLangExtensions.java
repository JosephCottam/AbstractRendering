package ar.ext.server;

import java.awt.geom.AffineTransform;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import ar.Transfer;
import ar.app.components.sequentialComposer.OptionDataset;
import ar.ext.lang.BasicLibrary;
import ar.ext.lang.Parser;
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
		put(LIBRARY, "dynScale", "Dyanmically resize based on current view's zoom order-of-mangitude (a modified linear interpolate based on view scale). args: base-zoom, dely",
				args -> dynScale(get(args, 0, 1), get(args, 1, 1)));

	}
	
	private static int dynScale(Number baseScale, Number delay) {
		double currentScale = Math.min(viewTransform.getScaleX(), viewTransform.getScaleY());		
		double factor = (currentScale/baseScale.doubleValue())/delay.doubleValue();
		factor = factor < 1 ? 1 : factor;
		System.out.printf("Zoom factor: %s,%s --> %s%n", baseScale, currentScale, factor);
		return (int) factor;
	}
	
	private static AffineTransform viewTransform;
	private static double viewTransform(Object arg) {
		switch (arg.toString()) {
			case "sx": return viewTransform.getScaleX();
			case "sy": return viewTransform.getScaleY();
			case "tx": return viewTransform.getTranslateX();
			case "ty": return viewTransform.getTranslateY();
			default: throw new IllegalArgumentException("View transform parameter not recognized: " + arg);
		}
	}
	
	
}
