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
		BasicLibrary.put(LIBRARY, "vt", "Get parts fo the view transform: sx,sy,tx,ty", args -> ARLangExtensions.viewTransform(BasicLibrary.get(args,0,"sx")));
	}
	
	private static AffineTransform viewTransform;
	private static Function<?, Double> viewTransform(Object arg) {
		switch (arg.toString()) {
			case "sx": return a -> viewTransform.getScaleX();
			case "sy": return a -> viewTransform.getScaleY();
			case "tx": return a -> viewTransform.getTranslateX();
			case "ty": return a -> viewTransform.getTranslateY();
			default: throw new IllegalArgumentException("View transform parameter not recognized: " + arg);
		}
	}
	
	
}
