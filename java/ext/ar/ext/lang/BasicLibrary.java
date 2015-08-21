package ar.ext.lang;

import static java.util.stream.Collectors.joining;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
	public static final Map<String, Function<List, ?>> COMMON;
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
	
	public static final Map<String, Function<List<Object>, Object>> PALETTES = new HashMap<>();
	static {
		PALETTES.put("CableColors", args -> CABLE_COLORS);
		PALETTES.put("Brewer12", args -> BREWER12);
		PALETTES.put("RedBlue", args -> RED_BLUE);
	}

	
	static {
		COMMON = new HashMap<>();
		COMMON.put("interpolate", args -> 
										args.size() > 3
											? new Numbers.FixedInterpolate<>(
													get(args, 0, Color.WHITE), 
													get(args, 1, Color.RED), 
													get(args, 3, 0d), 
													get(args, 4, 1d), 
													get(args, 2, Util.CLEAR))
											: new Numbers.Interpolate<>(get(args, 0, PINK), get(args, 1, Color.RED), get(args, 2, Util.CLEAR)));
		COMMON.put("catInterpolate", args -> new Categories.HighDefAlpha(get(args, 0, Util.CLEAR), get(args, 1, .1), get(args, 2, false)));

		
		COMMON.put("present", args -> new General.Present<>(get(args, 0, Color.RED), get(args, 0, Color.WHITE)));
		COMMON.put("toCount", args -> new Categories.ToCount<>());
		
		COMMON.put("seq", args -> seqFromList((List<Transfer>) (List) args));
		
		COMMON.put("colorKey", args -> new Categories.DynamicRekey<>(
												new CategoricalCounts<>(Util.COLOR_SORTER), 
												get(args, 0, CABLE_COLORS), 
												get(args, args.size()-1, Color.BLACK)));
		
		COMMON.put("percent",  args ->  new Categories.KeyPercent<Color>(
												get(args, 0, 50)/100d, 
												get(args, 1, Color.blue), 
												get(args, 2, Color.white), 
												get(args, 3, Color.blue),
												get(args, 4, Color.red)));
		COMMON.put("const", args -> new General.Const<>(get(args, 0, 1)));
		COMMON.put("fn", args -> new General.TransferFn<>(
												get(args, 0, (Object a) -> (Number) a), 
												get(args, 1, (Number) 0d)));
		
	}
	
	public static final Map<String, Function<List, ?>> PRIMITIVE_FUNCTIONS = new HashMap<>();
	static {
		PRIMITIVE_FUNCTIONS.put("rgb", args -> new Color((int) args.get(0), (int) args.get(1), (int) args.get(2)));
		PRIMITIVE_FUNCTIONS.put("color", args -> Color.getColor(get(args, 0, "black"))); 
		PRIMITIVE_FUNCTIONS.put("string", args -> args.stream().map(e -> e.toString()).collect(joining(" ")));
	}
	
	public static final Map<String, Function<List<Object>, Object>> ADVISE = new HashMap<>();
	static {
		ADVISE.put("neighborhood", args -> new Advise.NeighborhoodDistribution(get(args, 0, 2)));
		ADVISE.put("clipwarn", args -> new Advise.Clipwarn<>(
												get(args, 0, Color.black), 
												get(args, 1, Color.gray), 
												get(args, 2, new Numbers.Interpolate<>(Color.pink, Color.red)), 
												get(args, 4, 5d)));
	}

	
	public static final Map<String, Function<List<Object>, Object>> SPREAD = new HashMap<>();
	static {
		SPREAD.put("spread", args -> new FlexSpread(get(args, 0, new General.Spread.UnitCircle(2))));
		SPREAD.put("square", args -> new General.Spread.UnitRectangle<>(get(args,0,1)));
		SPREAD.put("rect", args -> new General.Spread.UnitRectangle<>(get(args,0,1), get(args,1,1), get(args,2,1), get(args,3,1)));
		SPREAD.put("circle", args -> new General.Spread.UnitCircle<>(get(args,0,1)));
		SPREAD.put("valueCircle", args -> new General.Spread.ValueCircle<>());
	}
	
	public static final Map<String, Function<List<Object>, Function<? extends Number, ? extends Number>>> MATH = new HashMap<>();
	static {
		MATH.put("log", args -> new MathValuers.Log(get(args, 0, 10d)));

		MATH.put("cbrt", args -> v -> Math.cbrt(v.doubleValue()));
		MATH.put("sqrt", args -> v -> Math.sqrt(v.doubleValue()));
		MATH.put("sin", args -> v -> Math.sin(v.doubleValue()));
		MATH.put("cos", args -> v -> Math.cos(v.doubleValue()));
		MATH.put("tan", args -> v -> Math.tan(v.doubleValue()));
		MATH.put("exp", args -> v -> Math.exp(v.doubleValue()));
		MATH.put("ceiling", args -> v -> Math.ceil(v.doubleValue()));
		MATH.put("floor", args -> v -> Math.floor(v.doubleValue()));
		MATH.put("round", args -> v -> Math.round(v.doubleValue()));
		MATH.put("sign", args -> v -> Math.signum(v.doubleValue()));
		MATH.put("abs", args -> v -> Math.abs(v.doubleValue()));
		MATH.put("rad->deg", args -> v -> Math.toDegrees(v.doubleValue()));
		MATH.put("deg->rad", args -> v -> Math.toRadians(v.doubleValue()));
		
		MATH.put("x+c", args -> (Number v) -> ((Number) get(args, 0, 1)).doubleValue() + v.doubleValue());
		MATH.put("x-c", args -> (Number v) -> v.doubleValue() - ((Number) get(args, 0, 1)).doubleValue());
		MATH.put("x*c", args -> (Number v) -> ((Number) get(args, 0, 1)).doubleValue() * v.doubleValue());
		MATH.put("x/c", args -> (Number v) -> ((Number) get(args, 0, 1)).doubleValue() / v.doubleValue());
		MATH.put("c/x", args -> (Number v) -> v.doubleValue()/((Number) get(args, 0, 1)).doubleValue());
		
		MATH.put("=", args -> new MathValuers.EQ(get(args, 0, 0d)));
		MATH.put(">", args -> new MathValuers.GT(get(args, 0, 0d)));
		MATH.put("<", args -> new MathValuers.GT(get(args, 0, 0d)));
		MATH.put(">=", args -> new MathValuers.GTE(get(args, 0, 0d)));
		MATH.put("<=", args -> new MathValuers.LTE(get(args, 0, 0d)));

		MATH.put("id", args -> v -> v);
	}
	
	public static final  Map<String, Function<List, ?>> ALL = new HashMap<>();
	static {
		ALL.putAll(COMMON);
		ALL.putAll((Map<String, ? extends Function<List, ?>>) MATH);
		ALL.putAll((Map<String, ? extends Function<List, ?>>) PALETTES);
		ALL.putAll((Map<String, ? extends Function<List, ?>>) SPREAD);
		ALL.putAll((Map<String, ? extends Function<List, ?>>) ADVISE);
		ALL.putAll(PRIMITIVE_FUNCTIONS);
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
