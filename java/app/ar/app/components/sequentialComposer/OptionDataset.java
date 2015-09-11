package ar.app.components.sequentialComposer;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ar.Glyphset;
import ar.glyphsets.MemMapList;
import ar.glyphsets.SyntheticGlyphset;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.glyphsets.implicitgeometry.Indexed.ToValue;
import ar.glyphsets.implicitgeometry.Valuer.Binary;
import ar.rules.CategoricalCounts;
import ar.util.Util;

//TODO: Make the ARL definition generate the other defaults so they don't have to be kept in sync
public final class OptionDataset<G,I> {	
	public final String name;				//Name to appear in convenient locations
	public final Glyphset<G,I> glyphset;	//Actual glyphset to load
	public final File sourceFile;			//Where it came from
	public final Shaper<Indexed, G> shaper; //How shapes were determined
	public final Valuer<Indexed, I> valuer; //How values were determined
	public final OptionAggregator<? super I,?> defaultAggregator;
	public final String arl;		//AR Language description 
	public final List<OptionTransfer<?>> defaultTransfers;
	public final Set<String> flags;	//Hints, allegations and other information about the data set
	
	
	public OptionDataset(
			String name, 
			File file, 
			Shaper<Indexed, G> shaper, 
			Valuer<Indexed,I> valuer,
			OptionAggregator<? super I,?> defAgg,
			String arl,
			OptionTransfer<?>... defTrans) {
		this(name, new MemMapList<>(file, shaper, valuer), file, shaper, valuer, defAgg, arl, Arrays.asList(defTrans), new HashSet<>());
	}
	
	public OptionDataset(
			String name, 
			Glyphset<G,I> glyphset,
			OptionAggregator<? super I,?> defAgg,
			String arl,
			OptionTransfer<?>... defTrans) {
		this(name, glyphset, null, null, null, defAgg, arl, Arrays.asList(defTrans), new HashSet<>());
	}
		
	public OptionDataset(
			String name, 
			Glyphset<G,I> glyphset,
			File file, Shaper<Indexed,G> shaper, Valuer<Indexed,I> valuer,
			OptionAggregator<? super I,?> defAgg,
			String arl,
			List<OptionTransfer<?>> defTrans,
			Set<String> flags) {
		this.name = name;
		this.sourceFile = file;
		this.shaper = shaper;
		this.valuer = valuer;
		this.glyphset = glyphset;
		this.defaultAggregator = defAgg;
		this.arl = arl;
		this.defaultTransfers = defTrans;
		this.flags = flags;
	}
	
	public String toString() {return name;}
	public OptionAggregator<? super I,?> defaultAggregator() {return defaultAggregator;}
	public List<OptionTransfer<?>> defaultTransfers() {return defaultTransfers;}


//	public static final OptionDataset<Point2D, Integer> WIKIPEDIA_TXT;
//	static {
//		OptionDataset<Point2D, Integer> temp;
//		try {
//			temp = new OptionDataset<>(
//				"Wikipedia BFS adjacnecy (Commons txt)",
//				new DelimitedFile<>(
//						new File("../data/wiki.full.txt"), ',', new Converter.TYPE[]{Converter.TYPE.LONG,Converter.TYPE.LONG, Converter.TYPE.COLOR}, 
//						new Indexed.ToPoint(false, 0,1), new Valuer.Constant<Indexed,Integer>(1)),
//				OptionAggregator.COUNT,
//				new OptionTransfer.MathTransfer(),
//				new OptionTransfer.Interpolate());
//		} catch (Exception e) {temp = null;}
//		WIKIPEDIA_TXT = temp;
//	}
	
	public static final OptionDataset<Point2D, String> BOOST_MEMORY;
	static {
		OptionDataset<Point2D, String> temp;
		try {
			temp = new OptionDataset<> (
					"BGL Memory", 
					new File("../data/MemVisScaled.hbin"), 
					new Indexed.ToPoint(true, 0, 1),
					new ToValue<>(2, new Binary<Integer,String>(0, "Hit", "Miss")),
					OptionAggregator.COC_COMP,
					"(seq(colorkey(cableColors))(catInterpolate(color,clear),.1))",
					new OptionTransfer.ColorKey(),
					new OptionTransfer.ColorCatInterpolate());
		} catch (Exception e) {temp = null;}
		BOOST_MEMORY = temp;
	}
	
	public static final OptionDataset<Point2D, CategoricalCounts<String>> CENSUS_TRACTS;
	static {
		OptionDataset<Point2D, CategoricalCounts<String>>  temp;
		try {
			temp = new OptionDataset<>(
				"US Census Tracts", 
				new File("../data/2010Census_RaceTract.hbin"), 
				new Indexed.ToPoint(false, 0, 1),
				new Valuer.CategoryCount<>(new Util.ComparableComparator<String>(), 3,2),
				OptionAggregator.MERGE_CATS,
				"(seq(toCount)(spread)(fn(log,10.0))(interpolate(color,pink)(color,red)(color,clear)))",
				new OptionTransfer.Spread(),
				new OptionTransfer.ToCount(),
				new OptionTransfer.MathTransfer(),
				new OptionTransfer.Interpolate());
			
			temp.flags.add("NegativeDown");
			temp.flags.add("LatLon");
		} catch (Exception e) {temp = null;}
		CENSUS_TRACTS = temp;
	}

	
	public static final OptionDataset<Point2D, Character> CENSUS_SYN_PEOPLE;
	static {
		OptionDataset<Point2D, Character> temp;
		try {
			temp = new OptionDataset<>(
				"US Census Synthetic People", 
				new File("../data/2010Census_RacePersonPoints.hbin"),
				new Indexed.ToPoint(false, 0, 1),
				new Indexed.ToValue<Indexed,Character>(2),
				OptionAggregator.COC_COMP,
				"(seq(colorkey(cableColors))(catInterpolate(color,clear),.1))",
				new OptionTransfer.ColorKey(),
				new OptionTransfer.ColorCatInterpolate());
			
			temp.flags.add("NegativeDown");
			temp.flags.add("EPSG:900913");
		} catch (Exception e) {temp = null;}
		CENSUS_SYN_PEOPLE = temp;
	}
	

	public static final OptionDataset<Point2D, Integer> GDELT_YEAR;
	static {
		OptionDataset<Point2D, Integer> temp;
		try {
			temp = new OptionDataset<>( 
					"GDELT (Year)",
					new File("../data/gdelt.hbin"),
					new Indexed.ToPoint(false, 4,3),
					new Indexed.ToValue<>(0),
					OptionAggregator.COC_COMP,
					"(seq(toCount)(spread)(fn(cbrt))(interpolate))",
					new OptionTransfer.ToCount(),
					new OptionTransfer.Spread(),
					new OptionTransfer.MathTransfer(),
					new OptionTransfer.Interpolate()
					);
			temp.flags.add("NegativeDown");
		} catch (Exception e) {
			e.printStackTrace();
			temp = null;}
		GDELT_YEAR = temp;
	}

	public static final OptionDataset<Point2D, Character> CENSUS_NY_SYN_PEOPLE;
	static {
		OptionDataset<Point2D, Character> temp;
		try {
			temp = new OptionDataset<>(
				"US Census Synthetic People (NY)", 
				new File("../data/2010Census_RacePersonPoints_NY.hbin"), 
				new Indexed.ToPoint(false, 0, 1),
				new Indexed.ToValue<Indexed,Character>(2),
				OptionAggregator.COC_COMP,
				"(seq(colorkey(cableColors))(catInterpolate(color,clear),.1))",
				new OptionTransfer.ColorKey(),
				new OptionTransfer.ColorCatInterpolate());
			
			temp.flags.add("NegativeDown");
			temp.flags.add("EPSG:900913");
		} catch (Exception e) {temp = null;}
		CENSUS_NY_SYN_PEOPLE = temp;
	}
	
	public static final OptionDataset<Point2D, Color> WIKIPEDIA;
	static {
		OptionDataset<Point2D, Color> temp;
		try {
			temp = new OptionDataset<>(
				"Wikipedia BFS adjacnecy", 
				new File("../data/wiki-adj.hbin"), 
				new Indexed.ToPoint(false, 0, 1),
				new Valuer.Constant<Indexed, Color>(Color.RED),
				OptionAggregator.COUNT,
				"(seq(fn(log,10.0))(interpolate(color,pink)(color,red)(color,clear)))",
				new OptionTransfer.MathTransfer(),
			new OptionTransfer.Interpolate());
		} catch (Exception e) {temp = null;}
		WIKIPEDIA = temp;
	}
	
	
	public static final OptionDataset<Point2D, Color> KIVA;
	static {
		OptionDataset<Point2D, Color> temp;
		try {
			temp = new OptionDataset<>(
				"Kiva", 
				new File("../data/kiva-adj.hbin"),
				new Indexed.ToPoint(false, 0, 1),
				new Valuer.Constant<Indexed, Color>(Color.RED),
				OptionAggregator.COUNT,
				"(seq(fn(log,10.0))(interpolate(color,pink)(color,red)(color,clear)))",
				new OptionTransfer.MathTransfer(),
				new OptionTransfer.Interpolate());
		} catch (Exception e) {temp = null;}
		KIVA = temp;
	}
	
////	public static final OptionDataset<Rectangle2D, Color> CIRCLE_SCATTER;
////	static {
////		OptionDataset<Rectangle2D, Color> temp;
////		try {
////			temp = new OptionDataset<>(
////			"Circle Scatter",
////			GlyphsetUtils.autoLoad(new File("../data/circlepoints.csv"), .1, DynamicQuadTree.<Rectangle2D, Color>make()),
////			OptionAggregator.COUNT,
////			new OptionTransfer.Interpolate());
////		} catch (Exception e) {temp = null;}
////		CIRCLE_SCATTER = temp;
////	}
//	
//	
	public static final OptionDataset<Rectangle2D, Integer> CIRCLE_SCATTER;
	static {
		OptionDataset<Rectangle2D, Integer> temp;
		try {
			temp = new OptionDataset<>(
			"Circle Scatter (HBIN)",
			new File("../data/circlepoints.hbin"),
			new Indexed.ToRect(.1,0,1),
			new Valuer.Constant<Indexed, Integer>(1),
			OptionAggregator.COUNT,
			"(interpolate(color,pink)(color,red)(color,clear))",
			new OptionTransfer.Interpolate());
		} catch (Exception e) {temp = null;}
		CIRCLE_SCATTER = temp;
	}

	
	private static int SYNTHETIC_POINT_COUNT = 1_000_000;
	public static  OptionDataset<Point2D, Integer> SYNTHETIC = syntheticPoints(SYNTHETIC_POINT_COUNT);
	public static OptionDataset<Point2D, Integer> syntheticPoints(int size) {
		return new OptionDataset<>(
				String.format("Synthetic Points (%,d points)", size),
				new SyntheticGlyphset<>(size, new SyntheticGlyphset.UniformPoints(), c->0),
				OptionAggregator.COUNT,
				"(interpolate(color,pink)(color,red)(color,clear))",
				new OptionTransfer.Interpolate());
	}
}