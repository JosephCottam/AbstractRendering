package ar.ext.lang;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ar.Aggregates;
import ar.Aggregator;
import ar.Transfer;
import ar.app.components.sequentialComposer.OptionTransfer.FlexSpread;
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.rules.Advise;
import ar.rules.CategoricalCounts;
import ar.rules.Categories;
import ar.rules.Debug;
import ar.rules.General;
import ar.rules.Numbers;
import ar.rules.combinators.Combinators;
import ar.rules.combinators.Seq;
import ar.util.Util;
import static java.util.stream.Collectors.*;


/**Collections of transfer functions and related support functions.**/
@SuppressWarnings({ "rawtypes", "unchecked" })
public class BasicLibrary {	
	/**Utility for storing functions with some documentation.**/
	public static final class FunctionRecord<T> implements Function<List<Object>, T> {
		public final String name;
		public final String help;
		public final Function<List<Object>, T> fn;
		public FunctionRecord(String name, String help, Function<List<Object>, T> fn) {
			this.name = name;
			this.help = help;
			this.fn = fn;
		}
		
		@Override public int hashCode() {return name.hashCode();}
		@Override public boolean equals(Object other) {return other instanceof FunctionRecord && this.name.equals(((FunctionRecord<?>) other).name);}
		@Override public T apply(List<Object> args) {return fn.apply(args);}
		@Override public String toString() {return name + ": " + help;}
	}
	
	public static final <T> void put(Map<String, Function<List<Object>, T>> map, String name, String help, Function<List, T> fn) {
		map.put(name, new FunctionRecord(name, help, fn));
	}

	
	public static final Map<String, Color> CSS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	private static List<Color> CABLE_COLORS = Arrays.asList(
			new Color(255,69,0),new Color(0,200,0),
			new Color(255,165,0),new Color(136,90,68),
			new Color(0,0,200));
	
	private static List<Color> BREWER12 = Arrays.asList(new Color(166,206,227), new Color(31,120,180),
				new Color(178,223,138), new Color(51,160,44),
				new Color(251,154,153), new Color(227,26,28),					
				new Color(253,191,111), new Color(255,127,0),
				new Color(202,178,214), new Color(106,61,154), 
				new Color(255,255,153), new Color(177,89,40));
	private static List<Color> RED_BLUE = Arrays.asList(new Color[]{Color.blue, Color.red});
	
	
	public static final Map<String, Function<List<Object>, Object>> COLOR = new HashMap<>();
	static {
		put(COLOR, "cableColors", "Colors based on the racial dot map.", args->CABLE_COLORS);
		put(COLOR, "brewer12", "Palette based on ColorBrewer 12 item categorical.", args->BREWER12);
		put(COLOR, "redBlue", "A useful red and blue.", args->RED_BLUE);
		
		put(COLOR, "rgb", "Color from 0-255 RGB values. Fourth alpha value is also acceptible.",
				args -> new Color(get(args, 0, 0), get(args, 1, 0), get(args, 2, 0), get(args, 3, 255)));

		put(COLOR, "color", "Color by name",
				args -> CSS.getOrDefault(get(args, 0, "black"), get(args,1,Color.black)));
		put(COLOR, "palette", "Build a palette from list of colors",
				args -> args.stream().filter(s -> (s instanceof Color)).collect(toList()));
	}

	/**Object to cary various parts of an AR configuration
	 * @param <E> Encoding type
	 * @param <I> Info type
	 * @param <A> Aggregate type
	 * @param <O> Output type
	 */
	public static class ARConfig<E,I,A,O> {
		public final Optional<Function<E,I>> info;
		public final Optional<Aggregator<I,A>> agg;
		public final Optional<Transfer<A,O>> transfer;
		public ARConfig(Function<E,I> info, Aggregator<I,A> agg, Transfer<A,O> t) {
			this.info = Optional.ofNullable(info);
			this.agg = Optional.ofNullable(agg);
			this.transfer = Optional.ofNullable(t);
		}
	}
	

	public static final Collector<Object, ArrayList, Stream<ArrayList>> PAIR =
			Collector.of(ArrayList::new, 
						(ArrayList acc,  Object v) -> 
							{if (acc.size() == 0 || (acc.size() > 0 && ((ArrayList) acc.get(acc.size()-1)).size() == 2)) {
								List l = new ArrayList();
								l.add(v);
								acc.add(l);
							} else {
								((ArrayList) acc.get(acc.size()-1)).add(v);
							}},
						(ArrayList acc1, ArrayList acc2) -> {acc1.addAll(acc2); return acc1;},
						(ArrayList acc) -> acc.stream());
	
	
	public static final class SafeToCount implements Transfer<Object, Integer> {
		@Override
		public Integer emptyValue() {return 0;}
		
		@Override
		public Specialized<Object,Integer> specialize(Aggregates<? extends Object> aggregates) {
			if (aggregates.defaultValue() instanceof Integer) {return new General.Echo(emptyValue(), false);}
			else {return new Categories.ToCount();}
		}
	}
	

	
	public static final Map<String, Function<List<Object>, Object>> MISC = new HashMap<>();	
	static {
		put(MISC, "AR", "AR configuration (info, aggregator, transfer)", args -> new ARConfig(get(args,0,null), get(args,1,null), get(args,2,null)));
		put(MISC, "null", "returns null", args -> null);
		put(MISC, "count", "Count aggregator", args -> new Numbers.Count<>());
		put(MISC, "char", "First character from string as a 'char'", args -> args.size() == 0 ? '\0' : args.get(0).toString().charAt(0));
		put(MISC, "dict", "Crate a dictionary. (map <key> <value> ...)", 
				args -> ((Stream<ArrayList>) args.stream().collect(PAIR))
								.collect(Collectors.toMap((ArrayList e)->e.get(0), (ArrayList e) -> e.get(1))));
		
		put(MISC, "interpolate", "Number to colors interpolation (use catInterpolate for multi-category  interpolation).", 
				args -> args.size() > 3
							? new Numbers.FixedInterpolate<>(
									get(args, 0, Color.WHITE), 
									get(args, 1, Color.RED), 
									get(args, 3, 0d), 
									get(args, 4, 1d), 
									get(args, 2, Util.CLEAR))
							: new Numbers.Interpolate<>(
									get(args, 0, CSS.get("pink")), 
									get(args, 1, Color.RED), 
									get(args, 2, Util.CLEAR)));
		
		put(MISC, "catInterpolate", "Interpolate across multiple cateogories (category labels must be colors).",
				args -> new Categories.HighDefAlpha(get(args, 0, Util.CLEAR), get(args, 1, .1), get(args, 2, true)));

		put(MISC, "present", "Fill areas with non-default value one color, and default value another.", 
				args -> new General.Present<>(get(args, 0, Color.RED), get(args, 1, Util.CLEAR)));
		
		put(MISC, "toCount", "Take mulit-category counts and combine them to a single set of counts.", 
				args -> new SafeToCount());
		
		put(MISC, "seq", "Execute a sequence of transfers (like the thrush combinator).",
				args -> seqFromList((List<Transfer>) (List) args));
		
		put(MISC, "colorkey", "Replace existing category labels with colors.  Often used before catInterpolate.",
				args -> new Categories.DynamicRekey<>(
								new CategoricalCounts<>(Util.COLOR_SORTER), 
								get(args, 0, CABLE_COLORS), 
								get(args, args.size() > 1? args.size()-1 : -1, Color.GRAY)));

		put(MISC, "rekey", "Replace existing category labels with other labels.",
				args -> new Categories.Rekey<>(
								makeExampleForRekey(get(args, 0, new HashMap<>())),
								get(args, 0, new HashMap<>()),
								get(args, args.size() > 1? args.size()-1 : -1, Color.GRAY)));
										
		put(MISC, "keyPercent",  "Color one way if a key category is over the threshold.", 
				args ->  new Categories.KeyPercent<Color>(
								get(args, 0, 50)/100d, 
								get(args, 1, Color.blue), 
								get(args, 2, Color.white), 
								get(args, 3, Color.blue),
								get(args, 4, Color.red)));
		
		put(MISC, "const", "Return a specific value everywhere.", 
				args -> new General.Const<>(get(args, 0, 1)));
		
		put(MISC, "fn", "Apply the passed function everywhere.  Unlike most things, you MUST supply a first argument and for non-double return functions, you must also supply the second argument..",
				args -> new General.TransferFn((Function) args.get(0), get(args, 1, 0d)));
		
		put(MISC, "string", "Make a list of symbols into a string, separated by item in the first argument",
				args ->
					args.size() == 0 
							? ""
							: args.subList(1, args.size()).stream().map(s -> s.toString()).collect(Collectors.joining(args.get(0).toString())));
		
		put(MISC, "space", "Returns a single space...needed because there are no string literals.", args -> " ");
	}
	
	
	/**Assumes all output mappings are of the same type, so it only inspects the first.
	 * Only works for color and comparable...
	 */
	public static final <IN,OUT> CategoricalCounts<OUT> makeExampleForRekey(Map<IN,OUT> mapping) {
		if (mapping.size() == 0) {return new CategoricalCounts();}
		OUT item = mapping.values().iterator().next();
		if (item instanceof Color) {return new CategoricalCounts<OUT>(((Comparator<OUT>) Util.COLOR_SORTER));}
		if (item instanceof Comparable) {return new CategoricalCounts<OUT>(((Comparator<OUT>) new Util.ComparableComparator<>()));}
		throw new IllegalArgumentException("Cannot rekey to items of type " + item.getClass() + " because no default comparator is known.");
	}
	
	public static final Map<String, Function<List<Object>, Object>> ADVISE = new HashMap<>();
	static {
		put(ADVISE, "neighborhood", "Highlight neighborhoods where bins have signficanlty different values.  Can be used for sub-pixel distribution analysis.",
				args -> new Advise.NeighborhoodDistribution(get(args, 0, 2)));
		
		put(ADVISE, "clipwarn", "Highlight areas of under and over saturation. args: transfer, over-color, under-color, delta-under, background-color",
				args -> new Advise.Clipwarn<>(
								get(args, 1, Color.black), 
								get(args, 2, Color.gray), 
								get(args, 4, Color.white),
								get(args, 0, new Numbers.Interpolate<>(Color.pink, Color.red)),
								get(args, 3, 5d)));
	}

	
	public static final Map<String, Function<List<Object>, Object>> SPREAD = new HashMap<>();
	static {
		put(SPREAD, "spread", "Spread values into adjacent bins.", 
				args -> new FlexSpread(
							get(args, 0, new General.Spread.UnitCircle(2)),
							get(args, 1, null),
							get(args, 2, null)));
		put(SPREAD, "square", "Spreader.  Square shape (up/down/left/right all the same).", args -> new General.Spread.UnitRectangle<>(get(args,0,1)));
		put(SPREAD, "rect", "Spreader. Separately secified up/down/left/right amount. All default to 1", args -> new General.Spread.UnitRectangle<>(get(args,0,1), get(args,1,1), get(args,2,1), get(args,3,1)));
		put(SPREAD, "circle", "Spreader.  Fixed size circle (default radius 1).", args -> new General.Spread.UnitCircle<>(get(args,0,1)));
		put(SPREAD, "valueCircle", "Spreader.  Spread in a circle based on the value in the source bin.", args -> new General.Spread.ValueCircle<>(get(args,0,a->a.doubleValue())));
	}
	
	public static final Map<String, Function<List<Object>, Function<Number,?>>> MATH = new HashMap<>();
	static {
		put(MATH, "log", "log-base-n function.  Argument determines base, default is base-10", args -> new MathValuers.Log(get(args, 0, 10d)));
		put(MATH, "cbrt", "cube root function", args -> v -> Math.cbrt(v.doubleValue()));
		put(MATH, "id", "Identity function (well...id as double so all math fns return doubles).", args -> v -> v.doubleValue());
		
		put(MATH, "sqrt", "Square root function", args -> v -> Math.sqrt(v.doubleValue()));
		put(MATH, "sin", "Sin function", args -> v -> Math.sin(v.doubleValue()));
		put(MATH, "cos", "Cos function", args -> v -> Math.cos(v.doubleValue()));
		put(MATH, "tan", "Tan function", args -> v -> Math.tan(v.doubleValue()));
		put(MATH, "exp", "e^n function", args -> v -> Math.exp(v.doubleValue()));
		put(MATH, "ceiling", "Ceiling function",  args -> v -> Math.ceil(v.doubleValue()));
		put(MATH, "floor", "Floor function", args -> v -> Math.floor(v.doubleValue()));
		put(MATH, "round", "Round function", args -> v -> Math.round(v.doubleValue()));
		put(MATH, "int", "Convert to an integer", args -> v -> v.intValue());
		put(MATH, "sign", "Sign function; returns 0 for zero, 1 for positive, -1 for negative.", args -> v -> Math.signum(v.doubleValue()));
		put(MATH, "abs", "Absolute value function.", args -> v -> Math.abs(v.doubleValue()));
		put(MATH, "rad->deg", "Function to convert radians to degrees.", args -> v -> Math.toDegrees(v.doubleValue()));
		put(MATH, "deg->rad", "Function to convert degrees to radians.",args -> v -> Math.toRadians(v.doubleValue()));
		
		put(MATH, "addc", "Make a function that adds a constant (not x+n because '+' is white-space in a URL).", args -> (Number v) -> ((Number) get(args, 0, 1)).doubleValue() + v.doubleValue());
		put(MATH, "x-c", "Make a function that substracts a constant.", args -> (Number v) -> v.doubleValue() - ((Number) get(args, 0, 1)).doubleValue());
		put(MATH, "x*c", "Make a function that multiplies a constant.", args -> (Number v) -> ((Number) get(args, 0, 1)).doubleValue() * v.doubleValue());
		put(MATH, "x/c", "Make a function that divides by a constant.", args -> (Number v) -> ((Number) get(args, 0, 1)).doubleValue() / v.doubleValue());
		put(MATH, "c/x", "Make a function that divides a constant.", args -> (Number v) -> v.doubleValue()/((Number) get(args, 0, 1)).doubleValue());
		
		put(MATH, "=", "Equal-to function.", args -> new MathValuers.EQ(get(args, 0, 0d)));
		put(MATH, ">", "Greater-than function.", args -> new MathValuers.GT(get(args, 0, 0d)));
		put(MATH, "<", "Less-than function.", args -> new MathValuers.GT(get(args, 0, 0d)));
		put(MATH, ">=", "Greater-than-or-equal-to  function.", args -> new MathValuers.GTE(get(args, 0, 0d)));
		put(MATH, "<=", "Less-than-or-equal-to function.", args -> new MathValuers.LTE(get(args, 0, 0d)));

	}
	
	
	public static final Map<String, Function<List<Object>, Object>> DEBUG = new HashMap<>();
	static {
		put(DEBUG, "stats" ,"Print out numeric statistics", args -> new Debug.Stats<Number>(get(args,0,0d)));
	}

	
	public static final  Map<String, Function<List<Object>, Object>> COMMON = new HashMap<>();
	static {
		COMMON.putAll(DEBUG);
		COMMON.putAll(MISC);
		COMMON.putAll((Map<? extends String, ? extends Function<List<Object>, Object>>) (Map) MATH);
		COMMON.putAll(COLOR);
		COMMON.putAll(SPREAD);
		COMMON.putAll(ADVISE);
	}
	
	public static final <A> A get(List<?> list, int n, A def) {
		return (n < list.size() && n>=0) ? (A) list.get(n) : def;
	}
	
	
	public static Transfer<?,?> seqFromList(List<Transfer> transfers) {
		if (transfers.size() == 0) {return new General.Echo(null);}
		if (transfers.size() == 1) {return transfers.get(0);}
		
		Seq s = Combinators.seq(transfers.get(0)).then(transfers.get(1));		//TODO: Look at unifying Seq/SeqStub/SeqEmpty so this can be done with a simple loop; or look at a collector seq type
		for (int i=2; i<transfers.size(); i++) {
			s = s.then(transfers.get(i));
		}
		return s;
	}
	
	static {
		CSS.put("AliceBlue", Color.decode("#F0F8FF"));
		CSS.put("AntiqueWhite", Color.decode("#FAEBD7"));
		CSS.put("Aqua", Color.decode("#00FFFF"));
		CSS.put("Aquamarine", Color.decode("#7FFFD4"));
		CSS.put("Azure", Color.decode("#F0FFFF"));
		CSS.put("Beige", Color.decode("#F5F5DC"));
		CSS.put("Bisque", Color.decode("#FFE4C4"));
		CSS.put("Black", Color.decode("#000000"));
		CSS.put("BlanchedAlmond", Color.decode("#FFEBCD"));
		CSS.put("Blue", Color.decode("#0000FF"));
		CSS.put("BlueViolet", Color.decode("#8A2BE2"));
		CSS.put("Brown", Color.decode("#A52A2A"));
		CSS.put("BurlyWood", Color.decode("#DEB887"));
		CSS.put("CadetBlue", Color.decode("#5F9EA0"));
		CSS.put("Chartreuse", Color.decode("#7FFF00"));
		CSS.put("Chocolate", Color.decode("#D2691E"));
		CSS.put("Coral", Color.decode("#FF7F50"));
		CSS.put("CornflowerBlue", Color.decode("#6495ED"));
		CSS.put("Cornsilk", Color.decode("#FFF8DC"));
		CSS.put("Crimson", Color.decode("#DC143C"));
		CSS.put("Cyan", Color.decode("#00FFFF"));
		CSS.put("DarkBlue", Color.decode("#00008B"));
		CSS.put("DarkCyan", Color.decode("#008B8B"));
		CSS.put("DarkGoldenRod", Color.decode("#B8860B"));
		CSS.put("DarkGray", Color.decode("#A9A9A9"));
		CSS.put("DarkGreen", Color.decode("#006400"));
		CSS.put("DarkKhaki", Color.decode("#BDB76B"));
		CSS.put("DarkMagenta", Color.decode("#8B008B"));
		CSS.put("DarkOliveGreen", Color.decode("#556B2F"));
		CSS.put("DarkOrange", Color.decode("#FF8C00"));
		CSS.put("DarkOrchid", Color.decode("#9932CC"));
		CSS.put("DarkRed", Color.decode("#8B0000"));
		CSS.put("DarkSalmon", Color.decode("#E9967A"));
		CSS.put("DarkSeaGreen", Color.decode("#8FBC8F"));
		CSS.put("DarkSlateBlue", Color.decode("#483D8B"));
		CSS.put("DarkSlateGray", Color.decode("#2F4F4F"));
		CSS.put("DarkTurquoise", Color.decode("#00CED1"));
		CSS.put("DarkViolet", Color.decode("#9400D3"));
		CSS.put("DeepPink", Color.decode("#FF1493"));
		CSS.put("DeepSkyBlue", Color.decode("#00BFFF"));
		CSS.put("DimGray", Color.decode("#696969"));
		CSS.put("DodgerBlue", Color.decode("#1E90FF"));
		CSS.put("FireBrick", Color.decode("#B22222"));
		CSS.put("FloralWhite", Color.decode("#FFFAF0"));
		CSS.put("ForestGreen", Color.decode("#228B22"));
		CSS.put("Fuchsia", Color.decode("#FF00FF"));
		CSS.put("Gainsboro", Color.decode("#DCDCDC"));
		CSS.put("GhostWhite", Color.decode("#F8F8FF"));
		CSS.put("Gold", Color.decode("#FFD700"));
		CSS.put("GoldenRod", Color.decode("#DAA520"));
		CSS.put("Gray", Color.decode("#808080"));
		CSS.put("Green", Color.decode("#008000"));
		CSS.put("GreenYellow", Color.decode("#ADFF2F"));
		CSS.put("HoneyDew", Color.decode("#F0FFF0"));
		CSS.put("HotPink", Color.decode("#FF69B4"));
		CSS.put("IndianRed ", Color.decode("#CD5C5C"));
		CSS.put("Indigo ", Color.decode("#4B0082"));
		CSS.put("Ivory", Color.decode("#FFFFF0"));
		CSS.put("Khaki", Color.decode("#F0E68C"));
		CSS.put("Lavender", Color.decode("#E6E6FA"));
		CSS.put("LavenderBlush", Color.decode("#FFF0F5"));
		CSS.put("LawnGreen", Color.decode("#7CFC00"));
		CSS.put("LemonChiffon", Color.decode("#FFFACD"));
		CSS.put("LightBlue", Color.decode("#ADD8E6"));
		CSS.put("LightCoral", Color.decode("#F08080"));
		CSS.put("LightCyan", Color.decode("#E0FFFF"));
		CSS.put("LightGoldenRodYellow", Color.decode("#FAFAD2"));
		CSS.put("LightGray", Color.decode("#D3D3D3"));
		CSS.put("LightGreen", Color.decode("#90EE90"));
		CSS.put("LightPink", Color.decode("#FFB6C1"));
		CSS.put("LightSalmon", Color.decode("#FFA07A"));
		CSS.put("LightSeaGreen", Color.decode("#20B2AA"));
		CSS.put("LightSkyBlue", Color.decode("#87CEFA"));
		CSS.put("LightSlateGray", Color.decode("#778899"));
		CSS.put("LightSteelBlue", Color.decode("#B0C4DE"));
		CSS.put("LightYellow", Color.decode("#FFFFE0"));
		CSS.put("Lime", Color.decode("#00FF00"));
		CSS.put("LimeGreen", Color.decode("#32CD32"));
		CSS.put("Linen", Color.decode("#FAF0E6"));
		CSS.put("Magenta", Color.decode("#FF00FF"));
		CSS.put("Maroon", Color.decode("#800000"));
		CSS.put("MediumAquaMarine", Color.decode("#66CDAA"));
		CSS.put("MediumBlue", Color.decode("#0000CD"));
		CSS.put("MediumOrchid", Color.decode("#BA55D3"));
		CSS.put("MediumPurple", Color.decode("#9370DB"));
		CSS.put("MediumSeaGreen", Color.decode("#3CB371"));
		CSS.put("MediumSlateBlue", Color.decode("#7B68EE"));
		CSS.put("MediumSpringGreen", Color.decode("#00FA9A"));
		CSS.put("MediumTurquoise", Color.decode("#48D1CC"));
		CSS.put("MediumVioletRed", Color.decode("#C71585"));
		CSS.put("MidnightBlue", Color.decode("#191970"));
		CSS.put("MintCream", Color.decode("#F5FFFA"));
		CSS.put("MistyRose", Color.decode("#FFE4E1"));
		CSS.put("Moccasin", Color.decode("#FFE4B5"));
		CSS.put("NavajoWhite", Color.decode("#FFDEAD"));
		CSS.put("Navy", Color.decode("#000080"));
		CSS.put("OldLace", Color.decode("#FDF5E6"));
		CSS.put("Olive", Color.decode("#808000"));
		CSS.put("OliveDrab", Color.decode("#6B8E23"));
		CSS.put("Orange", Color.decode("#FFA500"));
		CSS.put("OrangeRed", Color.decode("#FF4500"));
		CSS.put("Orchid", Color.decode("#DA70D6"));
		CSS.put("PaleGoldenRod", Color.decode("#EEE8AA"));
		CSS.put("PaleGreen", Color.decode("#98FB98"));
		CSS.put("PaleTurquoise", Color.decode("#AFEEEE"));
		CSS.put("PaleVioletRed", Color.decode("#DB7093"));
		CSS.put("PapayaWhip", Color.decode("#FFEFD5"));
		CSS.put("PeachPuff", Color.decode("#FFDAB9"));
		CSS.put("Peru", Color.decode("#CD853F"));
		CSS.put("Pink", Color.decode("#FFC0CB"));
		CSS.put("Plum", Color.decode("#DDA0DD"));
		CSS.put("PowderBlue", Color.decode("#B0E0E6"));
		CSS.put("Purple", Color.decode("#800080"));
		CSS.put("RebeccaPurple", Color.decode("#663399"));
		CSS.put("Red", Color.decode("#FF0000"));
		CSS.put("RosyBrown", Color.decode("#BC8F8F"));
		CSS.put("RoyalBlue", Color.decode("#4169E1"));
		CSS.put("SaddleBrown", Color.decode("#8B4513"));
		CSS.put("Salmon", Color.decode("#FA8072"));
		CSS.put("SandyBrown", Color.decode("#F4A460"));
		CSS.put("SeaGreen", Color.decode("#2E8B57"));
		CSS.put("SeaShell", Color.decode("#FFF5EE"));
		CSS.put("Sienna", Color.decode("#A0522D"));
		CSS.put("Silver", Color.decode("#C0C0C0"));
		CSS.put("SkyBlue", Color.decode("#87CEEB"));
		CSS.put("SlateBlue", Color.decode("#6A5ACD"));
		CSS.put("SlateGray", Color.decode("#708090"));
		CSS.put("Snow", Color.decode("#FFFAFA"));
		CSS.put("SpringGreen", Color.decode("#00FF7F"));
		CSS.put("SteelBlue", Color.decode("#4682B4"));
		CSS.put("Tan", Color.decode("#D2B48C"));
		CSS.put("Teal", Color.decode("#008080"));
		CSS.put("Thistle", Color.decode("#D8BFD8"));
		CSS.put("Tomato", Color.decode("#FF6347"));
		CSS.put("Turquoise", Color.decode("#40E0D0"));
		CSS.put("Violet", Color.decode("#EE82EE"));
		CSS.put("Wheat", Color.decode("#F5DEB3"));
		CSS.put("White", Color.decode("#FFFFFF"));
		CSS.put("WhiteSmoke", Color.decode("#F5F5F5"));
		CSS.put("Yellow", Color.decode("#FFFF00"));
		CSS.put("Clear", Util.CLEAR);
	}
}
