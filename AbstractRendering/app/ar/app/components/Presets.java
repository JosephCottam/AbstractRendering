package ar.app.components;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.File;

import javax.swing.JComboBox;

import ar.Glyphset;
import ar.Renderer;
import ar.app.ARApp;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;
import ar.glyphsets.DynamicQuadTree;
import ar.glyphsets.MemMapList;
import ar.renderers.ParallelGlyphs;
import ar.renderers.ParallelSpatial;
import ar.rules.AggregateReducers;
import ar.util.ImplicitGeometry;
import ar.util.MemMapEncoder;

public class Presets extends CompoundPanel {
	private static final long serialVersionUID = -5290930773909190497L;
	
	private final JComboBox<Preset> presets = new JComboBox<Preset>();
	
	public Presets() {
		this.add(new LabeledItem("Presets:", presets));
		presets.addActionListener(new CompoundPanel.DelegateAction(this));
		
		ARApp.loadInstances(presets, Presets.class, "");
	}
	
	public boolean doZoomWith(ARPanel oldPanel) {
		Preset p = (Preset) presets.getSelectedItem();

		return oldPanel == null
				|| oldPanel.dataset() != p.glyphset()
				|| !oldPanel.reduction().equals(p.reduction());
	}
	
	public ARPanel update(ARPanel oldPanel) {
		Preset p = (Preset) presets.getSelectedItem();
		ARPanel newPanel = new ARPanel(p.reduction(), p.transfer(), p.glyphset(), p.renderer());
		if (oldPanel != null 
				&& newPanel.dataset() == oldPanel.dataset()
				&& newPanel.reduction().equals(oldPanel.reduction())) {
			newPanel.aggregates(oldPanel.aggregates());
			try {newPanel.transferViewTransform(oldPanel.viewTransform());}
			catch (NoninvertibleTransformException e) {
				try {newPanel.setViewTransform(new AffineTransform());}
				catch (NoninvertibleTransformException e1) {/**(Hopefully) Not possible, identity transform is invertible**/}
			}
		} else {
			newPanel.zoomFit();
		}
		return newPanel;
	}
	
	public static interface Preset {
		public WrappedAggregator<?,?> reduction();
		public Renderer renderer();
		public Glyphset glyphset();
		public WrappedTransfer<?> transfer();
	}
	
	public static class ScatterplotAlpha implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.Count();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset glyphset() {return CIRCLE_SCATTER;}
		public WrappedTransfer<?> transfer() {return new WrappedTransfer.FixedAlpha();}
		public String toString() {return "Scatterplot: 10% Alpha";}
	}

	public static class ScatterplotHDALpha implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.Count();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset glyphset() {return CIRCLE_SCATTER;}
		public WrappedTransfer<?> transfer() {return new WrappedTransfer.RedWhiteInterpolate();}
		public String toString() {return "Scatterplot: HiDef Alpha";}
	}
	
	public static class BoostAlpha25 implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.RLEColors();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset glyphset() {return BOOST_MEMORY;}
		public WrappedTransfer<?> transfer() {return new WrappedTransfer.Percent25();}
		public String toString() {return "BGL Memory: 25% Cache Hit";}		
	}
	
	public static class BoostAlpha95 implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.RLEColors();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset glyphset() {return BOOST_MEMORY;}
		public WrappedTransfer<?> transfer() {return new WrappedTransfer.Percent95();}
		public String toString() {return "BGL Memory: 95% Cache Hit";}		
	}
	
	public static class BoostAlphaHDAlpha implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.RLEColors();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset glyphset() {return BOOST_MEMORY;}
		public WrappedTransfer<?> transfer() {return new WrappedTransfer.HighAlphaLin();}
		public String toString() {return "BGL Memory: HDAlpha Cache hits";}		
	}
	
	public static class BoostMMAlphaHDAlpha implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.RLEColors();}
		public Renderer renderer() {return new ParallelGlyphs(100000, new AggregateReducers.MergeCOC());}
		public Glyphset glyphset() {return BOOST_MEMORY_MM;}
		public WrappedTransfer<?> transfer() {return new WrappedTransfer.HighAlphaLin();}
		public String toString() {return "BGL Memory (Memory Mapped): Cache hits";}		
	}
	
	public static class BoostMMAlphaActivity implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.Count();}
		public Renderer renderer() {return new ParallelGlyphs(100000, new AggregateReducers.Count());}
		public Glyphset glyphset() {return BOOST_MEMORY_MM;}
		public WrappedTransfer<?> transfer() {return new WrappedTransfer.RedWhiteInterpolate();}
		public String toString() {return "BGL Memory (Memory Mapped): MemActivity hits";}		
	}
	
	public static class CharityNet implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.Count();}
		public Renderer renderer() {return new ParallelGlyphs(100000, new AggregateReducers.Count());}
		public Glyphset glyphset() {return CHARITY_NET_MM;}
		public WrappedTransfer<?> transfer() {return new WrappedTransfer.RedWhiteInterpolate();}
		public String toString() {return "Charity Net Donations (Memory Mapped): HDAlpha";}		
	}
	
	private static final Glyphset CIRCLE_SCATTER = load("Scatterplot", "./data/circlepoints.csv", .1);
	private static final Glyphset BOOST_MEMORY = load("BGL Memory", "./data/MemVisScaled.csv", .001);
	private static final Glyphset BOOST_MEMORY_MM = memMap("BGL Memory", "./data/MemVisScaledB.hbin", .001, .001, true, new ImplicitGeometry.AB<Double>(0d, Color.BLUE, Color.RED), 1, "ddi"); 
	private static final Glyphset CHARITY_NET_MM = memMap("Charity Net", "./data/dateStateXY.hbin", .5, .1, false, new ImplicitGeometry.Constant<>(Color.BLUE), 1, "ii");
//	private static final GlyphSet WIKIPEDIA_MM = memMap("Wikipedia Edits", "./data/dateStateXY.hbin", .01, false, new Painter.Constant<>(Color.BLUE));
//	private static final GlyphSet DATE_STATE = load("Charity Net", "./data/dateStateXY.csv", .01);

	public static final Glyphset load(String label, String file, double size) {
		System.out.printf("Loading %s...", label);
		try {
			final long start = System.currentTimeMillis();
			Glyphset g = ar.app.util.CSVtoGlyphSet.autoLoad(new File(file), size, DynamicQuadTree.make());
			final long end = System.currentTimeMillis();
			System.out.printf("\tLoad time (%s ms)\n ", (end-start));
			return g;
		} catch (Exception e) {
			System.out.println("Faield to load data.");
			return null;
		}
	}
	
	public static final Glyphset memMap(String label, String file, double width, double height, boolean flipY, ImplicitGeometry.Valuer valuer, int skip, String types) {
		System.out.printf("Memory mapping %s...", label);
		File f = new File(file);
		ImplicitGeometry.Shaper shaper = new ImplicitGeometry.IndexedToRect(width, height, flipY, 0, 1);

		try {
			long start = System.currentTimeMillis();
			Glyphset g = new MemMapList(f, shaper, valuer);
			long end = System.currentTimeMillis();
			System.out.printf("prepared %s entries (%s ms).\n", g.size(), end-start);
			return g;
		} catch (Exception e) {
			try {
				if (types != null) {
					System.out.println("Error loading.  Attempting re-encode...");
					File source = new File(file.replace(".hbin", ".csv"));
					MemMapEncoder.write(source, skip, f, types.toCharArray());
					return new MemMapList(f, shaper, valuer);	  //change types to null so it only tries to encode once
				} else {throw e;}
			} catch (Exception ex) {
				System.out.println("Faield to load data.");
				return null;
			}
		}
	}
	
}
