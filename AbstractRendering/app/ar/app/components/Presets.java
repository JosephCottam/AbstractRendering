package ar.app.components;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;
import ar.app.ARApp;
import ar.app.display.ARComponent;
import ar.app.display.FullDisplay;
import ar.app.display.SubsetDisplay;
import ar.app.util.GlyphsetUtils;
import ar.app.util.ActionProvider;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;
import static ar.glyphsets.implicitgeometry.Valuer.*;
import static ar.glyphsets.implicitgeometry.Indexed.*;
import ar.glyphsets.DynamicQuadTree;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.renderers.ParallelGlyphs;
import ar.renderers.ParallelSpatial;
import ar.rules.Advise;
import ar.rules.CategoricalCounts;
import ar.rules.Categories;
import ar.rules.Numbers;
import ar.rules.Advise.DrawDark;
import ar.rules.CategoricalCounts.CoC;
import ar.rules.TransferMath;
import ar.util.ChainedTransfer;
import ar.util.Util;

public class Presets extends JPanel {
	private static final long serialVersionUID = -5290930773909190497L;
	private final ActionProvider actionProvider = new ActionProvider();
	private static final Renderer CHAIN_RENDERER = new ParallelSpatial();

	private final JComboBox<Preset> presets = new JComboBox<Preset>();
	
	public Presets() {
		this.add(new LabeledItem("Presets:", presets));
		presets.addActionListener(actionProvider.delegateListener());
		
		ARApp.loadInstances(presets, Presets.class, "");
	}
	
	/**Should the display be re-zoomed?  
	 * Returns true when the new glyphset & aggregator is not the same as the old one.**/
	public boolean doZoomWith(ARComponent.Aggregating oldPanel) {
		Preset p = (Preset) presets.getSelectedItem();

		return oldPanel == null
				|| oldPanel.dataset() != p.glyphset()
				|| !oldPanel.aggregator().equals(p.aggregator());
	}
	
	public SubsetDisplay update(ARComponent.Aggregating oldPanel) {
		Preset p = (Preset) presets.getSelectedItem();
		SubsetDisplay newPanel = new SubsetDisplay(p.aggregator(), p.transfer(), p.glyphset(), p.renderer());
		if (oldPanel != null 
				&& newPanel.dataset() == oldPanel.dataset()
				&& newPanel.aggregator().equals(oldPanel.aggregator())) {
			try {
				newPanel.viewTransform(oldPanel.viewTransform());
				newPanel.aggregates(oldPanel.aggregates(), oldPanel.renderTransform());
			} catch (NoninvertibleTransformException e) {
				try {newPanel.viewTransform(new AffineTransform());}
				catch (NoninvertibleTransformException e1) {/**(Hopefully) Not possible, identity transform is invertible**/}
			}
		} else {
			newPanel.zoomFit();
		}
		return newPanel;
	}
	
	public void addActionListener(ActionListener l) {actionProvider.addActionListener(l);}
	
	public static interface Preset {
		public Aggregator<?,?> aggregator();
		public Transfer<?,?> transfer();
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
		public Aggregator<?,?> aggregator() {return new Numbers.Count<Object>();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset<?> glyphset() {return CIRCLE_SCATTER;}
		public Transfer<?,?> transfer() {return new WrappedTransfer.FixedAlpha().op();}
		public String name() {return "Scatterplot: 10% Alpha";}
		public String toString() {return fullName(this);}
	}

	public static class ScatterplotHDALphaLin implements Preset {
		public Aggregator<?,?> aggregator() {return new Numbers.Count<Object>();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset<?> glyphset() {return CIRCLE_SCATTER;}
		public Transfer<?,?> transfer() {return new WrappedTransfer.RedWhiteLinear().op();}
		public String name() {return "Scatterplot: HDAlpha (Linear)";}
		public String toString() {return fullName(this);}
	}
	
	public static class ScatterplotHDALpha implements Preset {
		public Aggregator<?,?> aggregator() {return new Numbers.Count<Object>();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset<?> glyphset() {return CIRCLE_SCATTER;}
		public Transfer<?,?> transfer() {return new WrappedTransfer.RedWhiteLog().op();}
		public String name() {return "Scatterplot: HDAlpha (log)";}
		public String toString() {return fullName(this);}
	}
	
	public static class BoostMMAlphaHDAlpha implements Preset {
		public Aggregator<?,?> aggregator() {return new WrappedAggregator.RLEColors().op();}
		public Renderer renderer() {return new ParallelGlyphs(1000);}
		public Glyphset<?> glyphset() {return BOOST_MEMORY_MM;}
		public Transfer<?,?> transfer() {return new WrappedTransfer.HighAlphaLog().op();}
		public String name() {return "BGL Memory: HDAlpha Cache hits (log)";}		
		public String toString() {return fullName(this);}
	}
	
	public static class BoostMMAlphaActivity implements Preset {
		public Aggregator<?,?> aggregator() {return new WrappedAggregator.RLEColors().op();}
		public Renderer renderer() {return new ParallelGlyphs(1000);}
		public Glyphset<?> glyphset() {return BOOST_MEMORY_MM;}
		public Transfer<?,?> transfer() {
			return new ChainedTransfer(
					CHAIN_RENDERER,
					new Categories.ToCount<>(), 
					new WrappedTransfer.RedWhiteLog().op());
		}
		public String name() {return "BGL Memory: Activity (log)";}		
		public String toString() {return fullName(this);}
	}
	
	public static class Kiva implements Preset {
		public Aggregator<?,?> aggregator() {return new Numbers.Count<Object>();}
		public Renderer renderer() {return new ParallelGlyphs(10000);}
		public Glyphset<?> glyphset() {return KIVA_ADJ;}
		public Transfer<?,?> transfer() {return new WrappedTransfer.RedWhiteLog().op();}
		public String name() {return "Kiva: HDAlpha";}
		public String toString() {return fullName(this);}
	}
	
	public static class KivaDrawDark implements Preset {
		public Aggregator<?,?> aggregator() {return new Numbers.Count<Object>();}
		public Renderer renderer() {return new ParallelGlyphs(10000);}
		public Glyphset<?> glyphset() {return KIVA_ADJ;}
		public Transfer<?,?> transfer() {
			return new DrawDark(Color.black, Color.white, 6);
		}
		public String name() {return "Kiva: DrawDark";}
		public String toString() {return fullName(this);}
	}
	
	public static class USPopMinAlpha implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.MergeCategories<>();}
		public Renderer renderer() {return new ParallelGlyphs(1000);}
		public Glyphset<?> glyphset() {return CENSUS_MM;}
		public Transfer<?,?> transfer() {
			return new ChainedTransfer<>(
				CHAIN_RENDERER,
				new Categories.ToCount<>(),
				new TransferMath.DivideInt(3000),
				new Numbers.FixedInterpolate(Color.white, Color.red, 0, 255));
		}
		public String name() {return "US Population (Min Alpha)";}
		public String toString() {return fullName(this);}
	}
	

	public static class USPop10Pct implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.MergeCategories<>();}
		public Renderer renderer() {return new ParallelGlyphs(1000);}
		public Glyphset<?> glyphset() {return CENSUS_MM;}
		public Transfer<?,?> transfer() {
			return new ChainedTransfer<>(
				CHAIN_RENDERER,
				new Categories.ToCount<>(),
				new TransferMath.DivideInt(3000),
				new Numbers.FixedInterpolate(Color.white, Color.red, 0, 25));
		}
		public String name() {return "US Population 10% alpha";}
		public String toString() {return fullName(this);}
	}
	
	public static class USPopulation implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.MergeCategories<>();}
		public Renderer renderer() {return new ParallelGlyphs(1000);}
		public Glyphset<?> glyphset() {return CENSUS_MM;}
		public Transfer<?,?> transfer() {
			return new ChainedTransfer<>(
					CHAIN_RENDERER, 
					new Categories.ToCount<>(), 
					new  Numbers.Interpolate(new Color(255,0,0,50), new Color(255,0,0,255)));
		}
		public String name() {return "US Population";}
		public String toString() {return fullName(this);}
	}
	
	public static class USPopulationClipWarn implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.MergeCategories<>();}
		public Renderer renderer() {return new ParallelGlyphs(1000);}
		public Glyphset<?> glyphset() {return CENSUS_MM;}
		public Transfer<?,?> transfer() {
			Transfer<CoC<Number>, Color> inner = 
					new ChainedTransfer<>(
							CHAIN_RENDERER,
							new Categories.ToCount<>(),
							new TransferMath.DivideInt(3000),
							new Numbers.FixedInterpolate(Color.white, Color.red, 0, 255));
			return new Advise.OverUnder<>(Color.BLACK, Color.gray, inner, new CategoricalCounts.CompareMagnitude(), 10);
		}
		public String name() {return "US Population (Clip Warn)";}
		public String toString() {return fullName(this);}
	}
	
	public static class USRaces implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.MergeCategories<>();}
		public Renderer renderer() {return new ParallelGlyphs(1000);}
		public Glyphset<?> glyphset() {return CENSUS_MM;}
		public Transfer<?,?> transfer() {
			Map<Object, Color> colors = new HashMap<>();
			colors.put(2, new Color(0,0,200));	//White
			colors.put(3, new Color(0,200,0));	//African American
			colors.put(4, new Color(220,0,0));	//Native American
			colors.put(5, Color.GRAY);	//Asian
			colors.put(6, Color.GRAY);	//Hawaiian
			colors.put(7, Color.GRAY);	//Other
			colors.put(8, Color.GRAY);	//Mixed

			Transfer<CategoricalCounts<Object>, CategoricalCounts<Color>> rekey = new Categories.ReKey<Object, Color>(new CoC<Color>(Util.COLOR_SORTER), colors, Color.BLACK);
			Transfer<CategoricalCounts<Color>, Color> stratAlpha = new Categories.HighAlpha(Color.white, .1, true);
			return new ChainedTransfer(
					CHAIN_RENDERER,
					rekey,
					stratAlpha);
		}
		public String name() {return "US Ratial Distribution";}
		public String toString() {return fullName(this);}
	}
	
//	public static class USRaces implements Preset {
//		public WrappedAggregator<?,?> aggregator() {return new WrappedAggregator.Count();}
//		public Renderer renderer() {return new ParallelGlyphs(10);}
//		public Glyphset<?> glyphset() {return CENSUS_MM;}
//		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.RedWhiteLog();}
//		public String name() {return "US Census";}
//		public String toString() {return fullName(this);}
//	}
	
	public static class Overplot implements Preset {
		public Aggregator<?,?> aggregator() {return new Numbers.Count<Object>();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset<?> glyphset() {return CIRCLE_SCATTER;}
		public Transfer<?,?> transfer() {return new WrappedTransfer.OverUnder().op();}
		public String name() {return "Scatterplot: clipping warning";}
		public String toString() {return fullName(this);}
	}
	
	private static final Glyphset<Color> CIRCLE_SCATTER; 
	private static final Glyphset<Color> KIVA_ADJ; 
	private static final Glyphset<Color> BOOST_MEMORY_MM; 
	private static final Glyphset<Color> CENSUS_MM;
	
	private static String MEM_VIS_BIN = "../data/MemVisScaled.hbin";
	private static String CIRCLE_CSV = "../data/circlepoints.csv";
	private static String KIVA_BIN = "../data/kiva-adj.hbin";
	private static String CENSUS = "../data/census/Race_TractLatLonDenorm.hbin";
	
	static {
		if (!(new File(MEM_VIS_BIN)).exists()) {MEM_VIS_BIN = MEM_VIS_BIN + "_subset";}
		if (!(new File(CIRCLE_CSV)).exists()) {CIRCLE_CSV = CIRCLE_CSV + "_subset";}
		if (!(new File(KIVA_BIN)).exists()) {KIVA_BIN = KIVA_BIN + "_subset";}

		Glyphset set;


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
				new Indexed.ToRect(.2, .2, true, 0, 1),
				new Valuer.CategoryCount(3,2),
				1, null);
		} catch (Exception e) {e.printStackTrace();}
		CENSUS_MM = set;

		
		set = null;
		try {set = GlyphsetUtils.autoLoad(new File(CIRCLE_CSV), .1, DynamicQuadTree.<Color>make());}
		catch (Exception e) {e.printStackTrace();}
		CIRCLE_SCATTER = set;
		
		set = null;
		try {set = GlyphsetUtils.memMap(
						"Kiva", KIVA_BIN, 
						new Indexed.ToRect(.1, 0, 1),
						new Valuer.Constant<Indexed, Color>(Color.RED), 
						1, null);
		} catch (Exception e) {e.printStackTrace();}
		KIVA_ADJ = set;

	}
	
}
