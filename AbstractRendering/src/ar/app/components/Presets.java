package ar.app.components;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.File;

import javax.swing.JComboBox;

import ar.GlyphSet;
import ar.Renderer;
import ar.app.ARApp;
import ar.app.WrappedReduction;
import ar.app.WrappedTransfer;
import ar.glyphsets.DynamicQuadTree;
import ar.glyphsets.MemMapList;
import ar.glyphsets.Painter;
import ar.renderers.ParallelGlyphs;
import ar.renderers.ParallelSpatial;
import ar.rules.AggregateReducers;

public class Presets extends CompoundPanel {
	private static final long serialVersionUID = -5290930773909190497L;
	
	private final JComboBox<Preset> presets = new JComboBox<Preset>();
	
	public Presets() {
		this.add(new LabeledItem("Presets:", presets));
		presets.addActionListener(new CompoundPanel.DelegateAction(this));
		
		ARApp.loadInstances(presets, Presets.class);
	}
	
	public boolean doZoomWith(ARPanel<?,?> oldPanel) {
		Preset p = (Preset) presets.getSelectedItem();

		return oldPanel == null
				|| oldPanel.dataset() != p.glyphset()
				|| !oldPanel.reduction().equals(p.reduction());
	}
	
	public ARPanel update(ARPanel<?,?> oldPanel) {
		Preset p = (Preset) presets.getSelectedItem();
		ARPanel<?,?> newPanel = new ARPanel(p.reduction(), p.transfer(), p.glyphset(), p.renderer());
		if (oldPanel != null 
				&& newPanel.dataset() == oldPanel.dataset()
				&& newPanel.reduction().equals(oldPanel.reduction())) {
			newPanel.aggregates(oldPanel.aggregates());
			try {newPanel.transferViewTransform(oldPanel.viewTransform());}
			catch (NoninvertibleTransformException e) {
				try {newPanel.setViewTransform(new AffineTransform());}
				catch (NoninvertibleTransformException e1) {/**(Hopefully) Not possible, identity transform is invertible**/}
			}
		}	
		return newPanel;
	}
	
	public static interface Preset {
		public WrappedReduction<?> reduction();
		public Renderer renderer();
		public GlyphSet glyphset();
		public WrappedTransfer<?> transfer();
	}
	
	public static class ScatterplotAlpha implements Preset {
		public WrappedReduction<?> reduction() {return new WrappedReduction.Count();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public GlyphSet glyphset() {return CIRCLE_SCATTER;}
		public WrappedTransfer<?> transfer() {return new WrappedTransfer.FixedAlpha();}
		public String toString() {return "Scatterplot: 10% Alpha";}
	}

	public static class ScatterplotHDALpha implements Preset {
		public WrappedReduction<?> reduction() {return new WrappedReduction.Count();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public GlyphSet glyphset() {return CIRCLE_SCATTER;}
		public WrappedTransfer<?> transfer() {return new WrappedTransfer.RedWhiteInterpolate();}
		public String toString() {return "Scatterplot: HiDef Alpha";}
	}
	
	public static class BoostAlpha25 implements Preset {
		public WrappedReduction<?> reduction() {return new WrappedReduction.RLEColors();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public GlyphSet glyphset() {return BOOST_MEMORY;}
		public WrappedTransfer<?> transfer() {return new WrappedTransfer.Percent25();}
		public String toString() {return "BGL Memory: 25% Cache Hit";}		
	}
	
	public static class BoostAlpha95 implements Preset {
		public WrappedReduction<?> reduction() {return new WrappedReduction.RLEColors();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public GlyphSet glyphset() {return BOOST_MEMORY;}
		public WrappedTransfer<?> transfer() {return new WrappedTransfer.Percent95();}
		public String toString() {return "BGL Memory: 95% Cache Hit";}		
	}
	
	public static class BoostAlphaHDAlpha implements Preset {
		public WrappedReduction<?> reduction() {return new WrappedReduction.RLEColors();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public GlyphSet glyphset() {return BOOST_MEMORY;}
		public WrappedTransfer<?> transfer() {return new WrappedTransfer.HighAlphaLin();}
		public String toString() {return "BGL Memory: HDAlpha Cache hits";}		
	}
	
	public static class BoostMMAlphaHDAlpha implements Preset {
		public WrappedReduction<?> reduction() {return new WrappedReduction.RLEColors();}
		public Renderer renderer() {return new ParallelGlyphs(100, new AggregateReducers.MergeCOC());}
		public GlyphSet glyphset() {return BOOST_MEMORY_MM;}
		public WrappedTransfer<?> transfer() {return new WrappedTransfer.HighAlphaLin();}
		public String toString() {return "BGL Memory (Memory Mapped): HDAlpha Cache hits";}		
	}
	
	private static final GlyphSet CIRCLE_SCATTER = load("Scatterplot", "./data/circlepoints.csv", .1);
	private static final GlyphSet BOOST_MEMORY = load("BGL Memory", "./data/MemVisScaled.csv", .001);
	private static final GlyphSet BOOST_MEMORY_MM = memMap("BGL Memory", "./data/MemVisScaledB.hbin", .001, true, new Painter.AB<Double>(0d, Color.BLUE, Color.RED)); 
//	private static final GlyphSet DATE_STATE = load("Charity Net", "./data/dateStateXY.csv", .01);
//	private static final GlyphSet DATE_STATE_MM = load("Charity Net", "./data/dateStateXY.hbin", .01, new Painter.Constant<>(Color.BLUE));

	public static final GlyphSet load(String label, String file, double size) {
		System.out.printf("Loading %s...", label);
		try {
			return ar.app.util.CSVtoGlyphSet.autoLoad(new File(file), size, DynamicQuadTree.make());
		} catch (Exception e) {
			System.out.println("Faield to load data.");
			return null;
		}
	}
	
	public static final GlyphSet memMap(String label, String file, double size, boolean flipY, Painter p) {
		System.out.printf("Memory mapping %s...", label);
		try {
			GlyphSet g = new MemMapList(new File(file), size, flipY, p, null);
			System.out.printf("prepared %s entries.\n", g.size());
			return g;
		} catch (Exception e) {
			System.out.println("Faield to load data.");
			return null;
		}
	}
	
}
