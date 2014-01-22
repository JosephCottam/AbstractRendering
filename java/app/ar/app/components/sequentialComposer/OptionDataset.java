package ar.app.components.sequentialComposer;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.File;

import ar.Glyphset;
import ar.glyphsets.MemMapList;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.glyphsets.implicitgeometry.Indexed.ToValue;
import ar.glyphsets.implicitgeometry.Valuer.Binary;
import ar.rules.CategoricalCounts;
import ar.util.Util;

public final class OptionDataset<G,I> {
	
	//TODO: Add "default chain" for ready access to a decent configuration 
	
	private final String name;
	private final Glyphset<G,I> glyphs;
	private final OptionAggregator<? super I,?> defaultAggregator;
	private final OptionTransfer<?>[] defaultTransfers;
	
	public OptionDataset(
			String name, File file, 
			Shaper<G,Indexed> shaper, Valuer<Indexed,I> valuer, 
			OptionAggregator<? super I,?> defAgg,
			OptionTransfer<?>... defTrans) {
		this.name = name;
		glyphs = new MemMapList<>(file, shaper, valuer);
		this.defaultAggregator = defAgg;
		this.defaultTransfers = defTrans;
	}
	public Glyphset<G,I> dataset() {return glyphs;}
	public String toString() {return name;}
	public OptionAggregator<? super I,?> defaultAggregator() {return defaultAggregator;}
	public OptionTransfer<?>[] defaultTransfers() {return defaultTransfers;}

	public static OptionDataset<Point2D, Color> BOST_MEMORY = new OptionDataset<> (
					"BGL Memory", 
					new File("../data/MemVisScaled.hbin"), 
					new Indexed.ToPoint(true, 0, 1),
					new ToValue<>(2, new Binary<Integer,Color>(0, Color.BLUE, Color.RED)),
					OptionAggregator.COC_COLOR,
					new OptionTransfer.HighAlphaLog());
	
	public static OptionDataset<Point2D, CategoricalCounts<String>> CENSUS_TRACTS = new OptionDataset<>(
			"US Census Tracts", 
			new File("../data/2010Census_RaceTract.hbin"), 
			new Indexed.ToPoint(true, 0, 1),
			new Valuer.CategoryCount<>(new Util.ComparableComparator<String>(), 3,2),
			OptionAggregator.MERGE_CATS,
			new OptionTransfer.Spread(),
			new OptionTransfer.ToCount(),
			new OptionTransfer.OneArgMathTransfer(),
			new OptionTransfer.HDInterpolate());
	
	public static OptionDataset<Point2D, Character> CENSUS_SYN_PEOPLE = new OptionDataset<>(
			"US Census Synthetic People", 
			new File("../data/2010Census_RacePersonPoints.hbin"), 
			new Indexed.ToPoint(true, 0, 1),
			new Indexed.ToValue<Indexed,Character>(2),
			OptionAggregator.COC_COMP,
			new OptionTransfer.ColorKey(),
			new OptionTransfer.HighAlphaLog());
	
	public static OptionDataset<Point2D, Color> WIKIPEDIA = new OptionDataset<>(
			"Wikipedia BFS adjacnecy", 
			new File("../data/wiki-adj.hbin"), 
			new Indexed.ToPoint(false, 0, 1),
			new Valuer.Constant<Indexed, Color>(Color.RED),
			OptionAggregator.COUNT,
			new OptionTransfer.RefArgMathTransfer(),
			new OptionTransfer.HDInterpolate());
	
	public static OptionDataset<Point2D, Color> KIVA = new OptionDataset<>(
			"Kiva", 
			new File("../data/kiva-adj.hbin"),
			new Indexed.ToPoint(false, 0, 1),
			new Valuer.Constant<Indexed, Color>(Color.RED),
			OptionAggregator.COUNT,
			new OptionTransfer.RefArgMathTransfer(),
			new OptionTransfer.HDInterpolate());
	
}