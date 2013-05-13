package ar.app.components;

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
import ar.renderers.ParallelSpatial;

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
		public String toString() {return "BGL MEmory: 25% Cache Hit";}		
	}
	
	public static class BoostAlpha95 implements Preset {
		public WrappedReduction<?> reduction() {return new WrappedReduction.RLEColors();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public GlyphSet glyphset() {return BOOST_MEMORY;}
		public WrappedTransfer<?> transfer() {return new WrappedTransfer.Percent95();}
		public String toString() {return "BGL MEmory: 95% Cache Hit";}		
	}
	
	public static class BoostAlphaHDAlpha implements Preset {
		public WrappedReduction<?> reduction() {return new WrappedReduction.RLEColors();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public GlyphSet glyphset() {return BOOST_MEMORY;}
		public WrappedTransfer<?> transfer() {return new WrappedTransfer.HighAlphaLin();}
		public String toString() {return "BGL MEmory: HDAlpha Cache hits";}		
	}
	
	private static final GlyphSet CIRCLE_SCATTER; 
	private static final GlyphSet BOOST_MEMORY; 
//	private static final GlyphSet DATE_STATE;
	private static final GlyphSet WIKI_MEMORY;
	
	static {
		System.out.print("Loading Scatterplot...");
		CIRCLE_SCATTER = ar.app.util.CSVtoGlyphSet.autoLoad(new File("./data/circlepoints.csv"), .1, DynamicQuadTree.make());
		
//		System.out.print("Charity Net...");
//		DATE_STATE = ar.app.util.CSVtoGlyphSet.autoLoad(new File("./data/dateStateXY.csv"), .01, DynamicQuadTree.make());		

		System.out.print("Loading BGL Memory...");
		BOOST_MEMORY = ar.app.util.CSVtoGlyphSet.autoLoad(new File("./data/MemVisScaled.csv"), .001, DynamicQuadTree.make());
		
		System.out.print("Loading Wikipedia edits...");
		WIKI_MEMORY = ar.app.util.CSVtoGlyphSet.autoLoad(new File("./data/wiki.1K.bin"), .05, new MemMapList(null, .01));
	}
}
