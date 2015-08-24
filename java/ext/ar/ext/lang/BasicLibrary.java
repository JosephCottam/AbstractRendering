package ar.ext.lang;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import ar.Transfer;
import ar.app.components.sequentialComposer.OptionTransfer.Spread.FlexSpread;
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.rules.Advise;
import ar.rules.CategoricalCounts;
import ar.rules.Categories;
import ar.rules.General;
import ar.rules.Numbers;
import ar.rules.combinators.Combinators;
import ar.rules.combinators.Seq;
import ar.util.Util;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class BasicLibrary {	
	private static final <T> void put(Map<String, Function<List<Object>, T>> map, String name, String help, Function<List, T> fn) {
		map.put(name, new Parser.FunctionRecord(name, help, fn));
	}

	
	private static final Color PINK = new Color(255,204,204);

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
		put(COLOR, "pink", "A useful pink", args->PINK);

		put(COLOR, "rgb", "Color from 0-255 RGB values. Fourth alpha value is also acceptible.",
				args -> new Color(get(args, 0, 0), get(args, 1, 0), get(args, 2, 0), get(args, 3, 255)));

		put(COLOR, "color", "Named color from the java Color class",
				args -> Color.getColor(get(args, 0, "black")));

	}


	public static final Map<String, Function<List<Object>, Object>> COMMON = new HashMap<>();	
	static {
		put(COMMON, "interpolate", "Number to colors interpolation (use catInterpolate for multi-category  interpolation).", 
				args -> args.size() > 3
							? new Numbers.FixedInterpolate<>(
									get(args, 0, Color.WHITE), 
									get(args, 1, Color.RED), 
									get(args, 3, 0d), 
									get(args, 4, 1d), 
									get(args, 2, Util.CLEAR))
							: new Numbers.Interpolate<>(get(args, 0, PINK), get(args, 1, Color.RED), get(args, 2, Util.CLEAR)));
		
		put(COMMON, "catInterpolate", "Interpolate across multiple cateogories (category labels must be colors).",
				args -> new Categories.HighDefAlpha(get(args, 0, Util.CLEAR), get(args, 1, .1), get(args, 2, false)));

		put(COMMON, "present", "Fill areas with non-default value one color, and default value another.", 
				args -> new General.Present<>(get(args, 0, Color.RED), get(args, 0, Color.WHITE)));
		
		put(COMMON, "toCount", "Take mulit-category counts and combine them to a single set of counts.", 
				args -> new Categories.ToCount<>());
		
		put(COMMON, "seq", "Execute a sequence of transfers (like the thrush combinator).",
				args -> seqFromList((List<Transfer>) (List) args));
		
		put(COMMON, "colorKey", "Replace existing category labels with colors.  Often used before catInterpolate.",
				args -> new Categories.DynamicRekey<>(
								new CategoricalCounts<>(Util.COLOR_SORTER), 
								get(args, 0, CABLE_COLORS), 
								get(args, args.size()-1, Color.BLACK)));

		put(COMMON, "keyPercent",  "Color one way if a key category is over the threshold.", 
				args ->  new Categories.KeyPercent<Color>(
								get(args, 0, 50)/100d, 
								get(args, 1, Color.blue), 
								get(args, 2, Color.white), 
								get(args, 3, Color.blue),
								get(args, 4, Color.red)));
		
		put(COMMON, "const", "Return a specific value everywhere.", 
				args -> new General.Const<>(get(args, 0, 1)));
		
		put(COMMON, "fn", "Apply the passed function everywhere.  Useful for mathematical transformations.",
				args -> new General.TransferFn<>(
								get(args, 0, (Object a) -> (Number) a), 
								get(args, 1, (Number) 0d)));
		put(COMMON, "string", "Make a list of symbols into a string, separated by item in the first argument",
				args -> args.subList(1, args.size()).stream().map(s -> s.toString()).collect(Collectors.joining(args.get(0).toString())));
		
		put(COMMON, "space", "Returns a single space...needed because there are no string literals.", args -> " ");
	}
		
	public static final Map<String, Function<List<Object>, Object>> ADVISE = new HashMap<>();
	static {
		put(ADVISE, "neighborhood", "Highlight neighborhoods where bins have signficanlty different values.  Can be used for sub-pixel distribution analysis.",
				args -> new Advise.NeighborhoodDistribution(get(args, 0, 2)));
		
		put(ADVISE, "clipwarn", "Highlight areas of under and over saturation",
				args -> new Advise.Clipwarn<>(
								get(args, 0, Color.black), 
								get(args, 1, Color.gray), 
								get(args, 2, new Numbers.Interpolate<>(Color.pink, Color.red)), 
								get(args, 4, 5d)));
	}

	
	public static final Map<String, Function<List<Object>, Object>> SPREAD = new HashMap<>();
	static {
		put(SPREAD, "spread", "Spread values into adjacent bins.", args -> new FlexSpread(get(args, 0, new General.Spread.UnitCircle(2))));
		put(SPREAD, "square", "Spreader.  Square shape (up/down/left/right all the same).", args -> new General.Spread.UnitRectangle<>(get(args,0,1)));
		put(SPREAD, "rect", "Spreader. Separately secified up/down/left/right amount. All default to 1", args -> new General.Spread.UnitRectangle<>(get(args,0,1), get(args,1,1), get(args,2,1), get(args,3,1)));
		put(SPREAD, "circle", "Spreader.  Fixed size circle (default radius 1).", args -> new General.Spread.UnitCircle<>(get(args,0,1)));
		put(SPREAD, "valueCircle", "Spreader.  Spread in a circle based on the value in the source bin.", args -> new General.Spread.ValueCircle<>());
	}
	
	public static final Map<String, Function<List<Object>, Function<Number,?>>> MATH = new HashMap<>();
	static {
		put(MATH, "log", "log-base-n function.  Argument determines base, default is base-10", args -> new MathValuers.Log(get(args, 0, 10d)));
		put(MATH, "cbrt", "cube root function ", args -> v -> Math.cbrt(v.doubleValue()));
		put(MATH, "id", "Identity function.", args -> v -> v.doubleValue());
		
		put(MATH, "sqrt", "Square root function", args -> v -> Math.sqrt(v.doubleValue()));
		put(MATH, "sin", "Sin function", args -> v -> Math.sin(v.doubleValue()));
		put(MATH, "cos", "Cos function", args -> v -> Math.cos(v.doubleValue()));
		put(MATH, "tan", "Tan function", args -> v -> Math.tan(v.doubleValue()));
		put(MATH, "exp", "e^n function", args -> v -> Math.exp(v.doubleValue()));
		put(MATH, "ceiling", "Ceiling function",  args -> v -> Math.ceil(v.doubleValue()));
		put(MATH, "floor", "Floor function", args -> v -> Math.floor(v.doubleValue()));
		put(MATH, "round", "Round function", args -> v -> Math.round(v.doubleValue()));
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
		put(MATH, ">", "Greather-than function.", args -> new MathValuers.GT(get(args, 0, 0d)));
		put(MATH, "<", "Less-than function.", args -> new MathValuers.GT(get(args, 0, 0d)));
		put(MATH, ">=", "Greater-than-or-equal-to  function.", args -> new MathValuers.GTE(get(args, 0, 0d)));
		put(MATH, "<=", "Less-than-or-equal-to function.", args -> new MathValuers.LTE(get(args, 0, 0d)));

	}
	
	public static final  Map<String, Function<List<Object>, Object>> ALL = new HashMap<>();
	static {
		ALL.putAll(COMMON);
		ALL.putAll((Map<? extends String, ? extends Function<List<Object>, Object>>) MATH);
		ALL.putAll(COLOR);
		ALL.putAll(SPREAD);
		ALL.putAll(ADVISE);
	}
	
	private static final <A> A get(List<Object> list, int n, A def) {
		return n < list.size() ? (A) list.get(n) : def;
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

}
