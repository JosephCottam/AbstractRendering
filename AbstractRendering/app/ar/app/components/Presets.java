package ar.app.components;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.File;

import javax.swing.JComboBox;

import ar.Glyphset;
import ar.Renderer;
import ar.app.ARApp;
import ar.app.display.ARComponent;
import ar.app.display.FullDisplay;
import ar.app.util.GlyphsetUtils;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;
import static ar.glyphsets.implicitgeometry.Valuer.*;
import static ar.glyphsets.implicitgeometry.Indexed.*;
import ar.glyphsets.DynamicQuadTree;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.renderers.ParallelGlyphs;
import ar.renderers.ParallelSpatial;

public class Presets extends PanelDelegator {
	private static final long serialVersionUID = -5290930773909190497L;
	
	private final JComboBox<Preset> presets = new JComboBox<Preset>();
	
	public Presets() {
		this.add(new LabeledItem("Presets:", presets));
		presets.addActionListener(new PanelDelegator.DelegateAction(this));
		
		ARApp.loadInstances(presets, Presets.class, "");
	}
	
	public boolean doZoomWith(ARComponent.Aggregating oldPanel) {
		Preset p = (Preset) presets.getSelectedItem();

		return oldPanel == null
				|| oldPanel.dataset() != p.glyphset()
				|| !oldPanel.aggregator().equals(p.aggregator().op());
	}
	
	public ARComponent.Aggregating update(ARComponent.Aggregating oldPanel) {
		Preset p = (Preset) presets.getSelectedItem();
		ARComponent.Aggregating newPanel = new FullDisplay(p.aggregator().op(), p.transfer().op(), p.glyphset(), p.renderer());
		if (oldPanel != null 
				&& newPanel.dataset() == oldPanel.dataset()
				&& newPanel.aggregator().equals(oldPanel.aggregator())) {
			newPanel.aggregates(oldPanel.aggregates());
			try {
				newPanel.viewTransform(oldPanel.viewTransform());
			} catch (NoninvertibleTransformException e) {
				try {newPanel.viewTransform(new AffineTransform());}
				catch (NoninvertibleTransformException e1) {/**(Hopefully) Not possible, identity transform is invertible**/}
			}
		} else {
			newPanel.zoomFit();
		}
		return newPanel;
	}
	
	public static interface Preset {
		public WrappedAggregator<?,?> aggregator();
		public WrappedTransfer<?,?> transfer();
		public Renderer renderer();
		public Glyphset<?> glyphset();
		public String name();
	}
	
	/**Generate a descriptive name from the parts of the preset instance.**/
	public static String fullName(Preset preset) {
		Glyphset<?> glyphset = preset.glyphset();
		if (glyphset ==null) {
			return preset.name() + "**LOAD FAILED**";
		} else {
			String subset = glyphset.size() < 1000 ? "**Subset**" : "";
			String memMap = glyphset instanceof ar.glyphsets.MemMapList ? "(Memory Maped)" : "";
			return String.format("%s %s %s", preset.name(),  subset, memMap);
		}	
	}		


	
	public static class ScatterplotAlpha implements Preset {
		public WrappedAggregator<?,?> aggregator() {return new WrappedAggregator.Count();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset<?> glyphset() {return CIRCLE_SCATTER;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.FixedAlpha();}
		public String name() {return "Scatterplot: 10% Alpha";}
		public String toString() {return fullName(this);}
	}

	public static class ScatterplotHDALphaLin implements Preset {
		public WrappedAggregator<?,?> aggregator() {return new WrappedAggregator.Count();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset<?> glyphset() {return CIRCLE_SCATTER;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.RedWhiteLinear();}
		public String name() {return "Scatterplot: HDAlpha (Linear)";}
		public String toString() {return fullName(this);}
	}
	
	public static class ScatterplotHDALpha implements Preset {
		public WrappedAggregator<?,?> aggregator() {return new WrappedAggregator.Count();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset<?> glyphset() {return CIRCLE_SCATTER;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.RedWhiteLog();}
		public String name() {return "Scatterplot: HDAlpha (log)";}
		public String toString() {return fullName(this);}
	}
	
	public static class BoostAlpha25 implements Preset {
		public WrappedAggregator<?,?> aggregator() {return new WrappedAggregator.RLEColors();}
		public Renderer renderer() {return new ParallelSpatial(1000);}
		public Glyphset<?> glyphset() {return BOOST_MEMORY;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.Percent25();}
		public String name() {return "BGL Memory: 25% Cache Hit";}		
		public String toString() {return fullName(this);}
	}
	
	public static class BoostAlpha95 implements Preset {
		public WrappedAggregator<?,?> aggregator() {return new WrappedAggregator.RLEColors();}
		public Renderer renderer() {return new ParallelSpatial(1000);}
		public Glyphset<?> glyphset() {return BOOST_MEMORY;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.Percent95();}
		public String name() {return "BGL Memory: 95% Cache Hit";}		
		public String toString() {return fullName(this);}
	}
	
	public static class BoostAlphaHDAlpha implements Preset {
		public WrappedAggregator<?,?> aggregator() {return new WrappedAggregator.RLEColors();}
		public Renderer renderer() {return new ParallelSpatial(1000);}
		public Glyphset<?>  glyphset() {return BOOST_MEMORY;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.HighAlphaLog();}
		public String name() {return "BGL Memory: HDAlpha Cache hits (log)";}		
		public String toString() {return fullName(this);}
	}
	
	public static class BoostMMAlphaHDAlpha implements Preset {
		public WrappedAggregator<?,?> aggregator() {return new WrappedAggregator.RLEColors();}
		public Renderer renderer() {return new ParallelGlyphs(1000);}
		public Glyphset<?> glyphset() {return BOOST_MEMORY_MM;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.HighAlphaLog();}
		public String name() {return "BGL Memory: HDAlpha Cache hits (log)";}		
		public String toString() {return fullName(this);}
	}
	
	public static class BoostMMAlphaActivity implements Preset {
		public WrappedAggregator<?,?> aggregator() {return new WrappedAggregator.Count();}
		public Renderer renderer() {return new ParallelGlyphs(1000);}
		public Glyphset<?> glyphset() {return BOOST_MEMORY_MM;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.RedWhiteLog();}
		public String name() {return "BGL Memory: Activity (log)";}		
		public String toString() {return fullName(this);}
	}
	
	public static class Kiva implements Preset {
		public WrappedAggregator<?,?> aggregator() {return new WrappedAggregator.Count();}
		public Renderer renderer() {return new ParallelGlyphs(100000);}
		public Glyphset<?> glyphset() {return KIVA_ADJ;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.RedWhiteLog();}
		public String name() {return "Kiva: HDAlpha";}
		public String toString() {return fullName(this);}
	}
	
	public static class Census implements Preset {
		public WrappedAggregator<?,?> aggregator() {return new WrappedAggregator.Count();}
		public Renderer renderer() {return new ParallelGlyphs(10);}
		public Glyphset<?> glyphset() {return CENSUS_MM;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.RedWhiteLog();}
		public String name() {return "US Census";}
		public String toString() {return fullName(this);}
	}
	
	public static class Overplot implements Preset {
		public WrappedAggregator<?,?> aggregator() {return new WrappedAggregator.Count();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset<?> glyphset() {return CIRCLE_SCATTER;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.OverUnder();}
		public String name() {return "Scatterplot: clipping warning";}
		public String toString() {return fullName(this);}
	}
	
	private static final Glyphset<Color> CIRCLE_SCATTER; 
	private static final Glyphset<Color> KIVA_ADJ; 
	private static final Glyphset<Color> BOOST_MEMORY; 
	private static final Glyphset<Color> BOOST_MEMORY_MM; 
	private static final Glyphset<Color> CENSUS_MM;
	
	private static String MEM_VIS_CSV = "../data/MemVisScaled.csv";
	private static String MEM_VIS_BIN = "../data/MemVisScaledB.hbin";
	private static String CIRCLE_CSV = "../data/circlepoints.csv";
	private static String KIVA_BIN = "../data/kivaAdj.hbin";
	private static String CENSUS = "../data/census/Total_LatLong.hbin";
	
	static {
		if (!(new File(MEM_VIS_CSV)).exists()) {MEM_VIS_CSV = MEM_VIS_CSV + "_subset";}
		if (!(new File(MEM_VIS_BIN)).exists()) {MEM_VIS_BIN = MEM_VIS_BIN + "_subset";}
		if (!(new File(CIRCLE_CSV)).exists()) {CIRCLE_CSV = CIRCLE_CSV + "_subset";}
		if (!(new File(KIVA_BIN)).exists()) {KIVA_BIN = KIVA_BIN + "_subset";}

		Glyphset<Color> set;

		set = null;
		try {set = GlyphsetUtils.autoLoad(new File(MEM_VIS_CSV), .1, DynamicQuadTree.<Color>make());}
		catch (Exception e) {e.printStackTrace();}
		BOOST_MEMORY = set;


		set = null;
		try {set = GlyphsetUtils.memMap(
				"BGL Memory", MEM_VIS_BIN, 
				new Indexed.ToRect(.001, .001, true, 0, 1),
				new ToValue<>(2, new Binary<Integer,Color>(0, Color.BLUE, Color.RED)), 
				1, "ddi");
		} catch (Exception e) {e.printStackTrace();}
		BOOST_MEMORY_MM = set;
		
		set = null;
		try {set = GlyphsetUtils.memMap(
				"US Census", CENSUS, 
				new Indexed.ToRect(.1, .1, true, 3, 2),
				new Valuer.Constant<Indexed, Color>(Color.RED), 
				1, null);
		} catch (Exception e) {e.printStackTrace();}
		CENSUS_MM = set;

		
		set = null;
		try {set = GlyphsetUtils.autoLoad(new File(CIRCLE_CSV), .1, DynamicQuadTree.<Color>make());}
		catch (Exception e) {e.printStackTrace();}
		CIRCLE_SCATTER = set;
		
		set = null;
		try {set = GlyphsetUtils.autoLoad(new File(KIVA_BIN), .1, DynamicQuadTree.<Color>make());}
		catch (Exception e) {e.printStackTrace();}
		KIVA_ADJ = set;

	}
	
}
