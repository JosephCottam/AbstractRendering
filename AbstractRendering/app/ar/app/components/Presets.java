package ar.app.components;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.Comparator;

import javax.swing.JComboBox;

import ar.Aggregates;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;
import ar.app.ARApp;
import ar.app.util.GlyphsetUtils;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;
import static ar.glyphsets.implicitgeometry.Valuer.*;
import static ar.glyphsets.implicitgeometry.Indexed.*;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.renderers.ParallelGlyphs;
import ar.renderers.ParallelSpatial;
import ar.rules.Advise;
import ar.rules.AggregateReductions;
import ar.rules.Transfers;

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
				|| !oldPanel.reduction().equals(p.reduction().op());
	}
	
	public ARPanel update(ARPanel oldPanel) {
		Preset p = (Preset) presets.getSelectedItem();
		ARPanel newPanel = new ARPanel(p.reduction().op(), p.transfer().op(), p.glyphset(), p.renderer());
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
		public WrappedTransfer<?,?> transfer();
	}
	
	public static class ScatterplotAlpha implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.Count();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset glyphset() {return CIRCLE_SCATTER;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.FixedAlpha();}
		public String toString() {return "Scatterplot: 10% Alpha" + ((glyphset() == null) ? "(FAILED)" : "");}
	}

	public static class ScatterplotHDALphaLin implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.Count();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset glyphset() {return CIRCLE_SCATTER;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.RedWhiteLinear();}
		public String toString() {return "Scatterplot: HiDef Alpha (Linear)" + ((glyphset() == null) ? "(FAILED)" : "");}
	}
	
	public static class ScatterplotHDALpha implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.Count();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset glyphset() {return CIRCLE_SCATTER;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.RedWhiteLog();}
		public String toString() {return "Scatterplot: HiDef Alpha (log)" + ((glyphset() == null) ? "(FAILED)" : "");}
	}
	
	public static class BoostAlpha25 implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.RLEColors();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset glyphset() {return BOOST_MEMORY;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.Percent25();}
		public String toString() {return "BGL Memory: 25% Cache Hit" + ((glyphset() == null) ? "(FAILED)" : "");}		
	}
	
	public static class BoostAlpha95 implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.RLEColors();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset glyphset() {return BOOST_MEMORY;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.Percent95();}
		public String toString() {return "BGL Memory: 95% Cache Hit" + ((glyphset() == null) ? "(FAILED)" : "");}		
	}
	
	public static class BoostAlphaHDAlpha implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.RLEColors();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset glyphset() {return BOOST_MEMORY;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.HighAlphaLog();}
		public String toString() {return "BGL Memory: HDAlpha Cache hits (log)" + ((glyphset() == null) ? "(FAILED)" : "");}		
	}
	
	public static class BoostMMAlphaHDAlpha implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.RLEColors();}
		public Renderer renderer() {return new ParallelGlyphs(100000, new AggregateReductions.MergeCOC());}
		public Glyphset glyphset() {return BOOST_MEMORY_MM;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.HighAlphaLog();}
		public String toString() {return "BGL Memory (Memory Mapped): Cache hits (log)" + ((glyphset() == null) ? "(FAILED)" : "");}		
	}
	
	public static class BoostMMAlphaActivity implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.Count();}
		public Renderer renderer() {return new ParallelGlyphs(100000, new AggregateReductions.Count());}
		public Glyphset glyphset() {return BOOST_MEMORY_MM;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.RedWhiteLog();}
		public String toString() {return "BGL Memory (Memory Mapped): MemActivity hits (log)" + ((glyphset() == null) ? "(FAILED)" : "");}		
	}
	
	public static class CharityNet implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.Count();}
		public Renderer renderer() {return new ParallelGlyphs(100000, new AggregateReductions.Count());}
		public Glyphset glyphset() {return CHARITY_NET_MM;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.RedWhiteLog();}
		public String toString() {return "Charity Net Donations (Memory Mapped): HDAlpha (Log)" + ((glyphset() == null) ? "(FAILED)" : "");}		
	}
	
	public static class Overplot implements Preset {
		public WrappedAggregator<?,?> reduction() {return new WrappedAggregator.Count();}
		public Renderer renderer() {return new ParallelSpatial(100);}
		public Glyphset glyphset() {return CIRCLE_SCATTER;}
		public WrappedTransfer<?,?> transfer() {return new WrappedTransfer.OverUnder();}
		public String toString() {return "Scatterplot: clipping warning (int)" + ((glyphset() == null) ? "(FAILED)" : "");}

	}
	
	
	
	
	private static final Glyphset<Color> CIRCLE_SCATTER = GlyphsetUtils.load("Scatterplot", "../data/circlepoints.csv", .1);
	private static final Glyphset<Color> BOOST_MEMORY = GlyphsetUtils.load("BGL Memory", "../data/MemVisScaled.csv", .001);
	private static final Glyphset<Color> BOOST_MEMORY_MM = GlyphsetUtils.memMap("BGL Memory", "../data/MemVisScaledB.hbin", .001, .001, true, new ToValue<>(2, new Binary<Integer,Color>(0, Color.BLUE, Color.RED)), 1, "ddi"); 
	private static final Glyphset<Color> CHARITY_NET_MM =GlyphsetUtils.memMap("Charity Net", "../data/dateStateXY.hbin", .5, .1, false, new Constant<Indexed>(Color.BLUE), 1, "ii");
//	private static final GlyphSet WIKIPEDIA_MM = memMap("Wikipedia Edits", "./data/dateStateXY.hbin", .01, false, new Painter.Constant<>(Color.BLUE));
//	private static final GlyphSet DATE_STATE = load("Charity Net", "./data/dateStateXY.csv", .01);

	
}
