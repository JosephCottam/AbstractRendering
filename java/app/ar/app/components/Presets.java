package ar.app.components;

import java.awt.Color;
import java.awt.Shape;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Resources;
import ar.Transfer;
import ar.app.display.ARComponent;
import ar.app.display.SubsetDisplay;
import ar.app.util.GeoJSONTools;
import ar.app.util.GlyphsetUtils;
import ar.app.util.ActionProvider;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;
import ar.util.HasViewTransform;
import static ar.glyphsets.implicitgeometry.Valuer.*;
import static ar.glyphsets.implicitgeometry.Indexed.*;
import ar.glyphsets.DynamicQuadTree;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.rules.CategoricalCounts;
import ar.rules.Categories;
import ar.rules.General;
import ar.rules.ISOContours;
import ar.rules.Numbers;
import ar.rules.Advise.DrawDark;
import ar.rules.CategoricalCounts.CoC;
import ar.rules.combinators.Chain;
import ar.rules.combinators.If;
import ar.rules.Shapes;
import ar.util.Util;
import ar.glyphsets.SyntheticGlyphset;

public class Presets extends JPanel {
	private static final long serialVersionUID = -5290930773909190497L;
	private final ActionProvider actionProvider = new ActionProvider();  

	private final JComboBox<Preset> presets = new JComboBox<Preset>();
	
	public Presets(HasViewTransform transformSource) {
		this.add(new LabeledItem("Presets:", presets));
		presets.addActionListener(actionProvider.delegateListener());
		
		ar.app.util.AppUtil.loadInstances(presets, Presets.class, Presets.Preset.class, "");

		for (int i=0; i<presets.getItemCount(); i++) {
			Preset item = presets.getItemAt(i);
			boolean success = item.init(transformSource);
			if (!success) {
				presets.removeItem(item); 
				i--;
			}
		}
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
		SubsetDisplay newPanel = new SubsetDisplay(p.aggregator(), p.transfer(), p.glyphset(), Resources.DEFAULT_RENDERER);
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
		public Glyphset<?,?> glyphset();
		public String name();
		public boolean init(HasViewTransform panel);
	}
	
	/**Generate a descriptive name from the parts of the preset instance.**/
	public static String fullName(Preset preset) {
		Glyphset<?,?> glyphset = preset.glyphset();
		if (glyphset ==null) {
			return preset.name() + "**LOAD FAILED**";
		} else {
			String subset = glyphset.size() < 1000 ? "**Subset**" : "";
			return String.format("%s %s", preset.name(),  subset);
		}	
	}		

	public static class SyntheticPoints implements Preset {
		public Aggregator<?,?> aggregator() {return new Numbers.Count<Object>();}
		public Glyphset<?,?> glyphset() {return new SyntheticGlyphset<>(100_000_000, 0, new SyntheticGlyphset.SyntheticPoints());}
		public Transfer<?,?> transfer() {return new Numbers.Interpolate(Color.white, Color.red);}
		public String name() {return String.format("Synthetic Points (%d points)", glyphset().size());}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}

	
	public static class ScatterplotAlpha implements Preset {
		public Aggregator<?,?> aggregator() {return new Numbers.Count<Object>();}
		public Glyphset<?,?> glyphset() {return CIRCLE_SCATTER;}
		public Transfer<?,?> transfer() {return new Numbers.FixedInterpolate(Color.white, Color.red, 0, 25.5);}
		public String name() {return "Scatterplot: 10% Alpha";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}

	public static class ScatterplotHDALphaLin implements Preset {
		public Aggregator<?,?> aggregator() {return new Numbers.Count<Object>();}
		public Glyphset<?,?> glyphset() {return CIRCLE_SCATTER;}
		public Transfer<?,?> transfer() {return new Numbers.Interpolate(new Color(255,0,0,38), Color.red);}
		public String name() {return "Scatterplot: HDAlpha (Linear)";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	
	public static class ScatterplotHDALpha implements Preset {
		public Aggregator<?,?> aggregator() {return new Numbers.Count<Object>();}
		public Glyphset<?,?> glyphset() {return CIRCLE_SCATTER;}
		public Transfer<?,?> transfer() {return new Numbers.Interpolate(new Color(255,0,0,25), Color.red, Util.CLEAR, 10);}
		public String name() {return "Scatterplot: HDAlpha (log)";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	
	public static class BoostMMAlphaHDAlpha implements Preset {
		public Aggregator<?,?> aggregator() {return new WrappedAggregator.RLEColors().op();}
		public Glyphset<?,?> glyphset() {return BOOST_MEMORY_MM;}
		public Transfer<?,?> transfer() {return new WrappedTransfer.HighAlphaLog().op();}
		public String name() {return "BGL Memory: HDAlpha Cache hits (log)";}		
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	
	public static class BoostMMAlphaActivity implements Preset {
		public Aggregator<?,?> aggregator() {return new WrappedAggregator.RLEColors().op();}
		public Glyphset<?,?> glyphset() {return BOOST_MEMORY_MM;}
		public Transfer<?,?> transfer() {
			return new Chain<Object, Object>(
					Resources.DEFAULT_RENDERER,
					new Categories.ToCount<>(), 
					new Numbers.Interpolate(new Color(255,0,0,25), Color.red, Color.white, 10));
		}
		public String name() {return "BGL Memory: Activity (log)";}		
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	
	public static class Kiva implements Preset {
		public Aggregator<?,?> aggregator() {return new Numbers.Count<Object>();}
		public Glyphset<?,?> glyphset() {return KIVA_ADJ;}
		public Transfer<?,?> transfer() {return new WrappedTransfer.RedWhiteLog().op();}
		public String name() {return "Kiva: HDAlpha";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	

	public static class KivaDrawDark implements Preset {
		public Aggregator<?,?> aggregator() {return new Numbers.Count<Object>();}
		public Glyphset<?,?> glyphset() {return KIVA_ADJ;}
		public Transfer<?,?> transfer() {
			return new DrawDark(Color.black, Color.white, 6);
		}
		public String name() {return "Kiva: DrawDark";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	
	public static class WikipediaAdj implements Preset {
		public Aggregator<?,?> aggregator() {return new Numbers.Count<Object>();}
		public Glyphset<?,?> glyphset() {return WIKIPEDIA;}
		public Transfer<?,?> transfer() {return new WrappedTransfer.RedWhiteLog().op();}
		public String name() {return "Wikipedia Adjacency (BFS Error layout): HDAlpha";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	
	
	public static class USPopMinAlpha implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.MergeCategories<>();}
		public Glyphset<?,?> glyphset() {return CENSUS_MM;}
		public Transfer<?,?> transfer() {
			return new Chain<>(
					Resources.DEFAULT_RENDERER, 
					new Categories.ToCount<>(),
					new General.Spread<>(new General.Spread.UnitSquare<Integer>(1), new Numbers.Count<Integer>()),
					new General.ValuerTransfer<>(new MathValuers.DivideInt<>(4000),0),
					new Numbers.FixedInterpolate(Color.white, Color.red, 0, 255));
		}
		public String name() {return "US Population (Min Alpha)";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	

	public static class USCensusPop10Pct implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.MergeCategories<>();}
		public Glyphset<?,?> glyphset() {return CENSUS_MM;}
		public Transfer<?,?> transfer() {
			return new Chain<>(
					Resources.DEFAULT_RENDERER, 
					new Categories.ToCount<>(),
					new General.Spread<>(new General.Spread.UnitSquare<Integer>(1), new Numbers.Count<Integer>()),
					new General.ValuerTransfer<>(new MathValuers.DivideInt<>(4000),0),
					new Numbers.FixedInterpolate(Color.white, Color.red, 0, 25));
		}
		public String name() {return "US Population 10% alpha";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	
	public static class USCensusPopulationLinear implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.MergeCategories<>();}
		public Glyphset<?,?> glyphset() {return CENSUS_MM;}
		public Transfer<?,?> transfer() {
			return new Chain<>(
					Resources.DEFAULT_RENDERER, 
					new Categories.ToCount<>(),
					new General.Spread<>(new General.Spread.UnitSquare<Integer>(1), new Numbers.Count<Integer>()),
					new Numbers.Interpolate(new Color(255,0,0,30), new Color(255,0,0,255)));
		}
		public String name() {return "US Population (Linear)";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	

	public static class USCensusPopulationOpaque implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.MergeCategories<>();}
		public Glyphset<?,?> glyphset() {return CENSUS_MM;}
		public Transfer<?,?> transfer() {
			return new Chain<>(
					Resources.DEFAULT_RENDERER, 
					new Categories.ToCount<>(),
					new General.Spread<>(new General.Spread.UnitSquare<Integer>(1), new Numbers.Count<Integer>()),
					new General.Present<>(Color.RED, Color.white));
		}
		public String name() {return "US Population (Opaque)";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	
	public static class USCensusPopulationExp implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.MergeCategories<>();}
		public Glyphset<?,?> glyphset() {return CENSUS_MM;}
		public Transfer<?,?> transfer() {
			return new Chain<>(
					Resources.DEFAULT_RENDERER, 
					new Categories.ToCount<>(),
					new General.Spread<>(new General.Spread.UnitSquare<Integer>(1), new Numbers.Count<Integer>()),
					new General.ValuerTransfer<>(new MathValuers.Raise<>(.333333d), 0d),
					new  Numbers.Interpolate(new Color(255,0,0,30), new Color(255,0,0,255)));
		}
		public String name() {return "US Population (Exp)";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	
	public static class USCensusPopulationWeave implements Preset {
		HasViewTransform transformProvider = null;
		private final List<Shape> shapes;
		
		public USCensusPopulationWeave() {
			try {
				shapes = GeoJSONTools.flipY(GeoJSONTools.loadShapesJSON(new File("../data/maps/USStates"), false));
				//shapes = GeoJSONTools.flipY(GeoJSONTools.loadShapesJSON(new File("../data/maps/USCounties"), true));
			} catch (Exception e) {throw new RuntimeException(e);}
		}

		/**Provide the viewTransform-access pathway.**/
		public boolean init(HasViewTransform provider) {
			this.transformProvider = provider;
			return glyphset() != null;
		}
		public Aggregator<?,?> aggregator() {return new Categories.MergeCategories<>();}
		public Glyphset<?,?> glyphset() {return CENSUS_MM;}
		public Transfer<?,?> transfer() {
			try {
				Map<Object, Color> colors = new HashMap<>();
				colors.put(2, new Color(0,0,200));	//White
				colors.put(3, new Color(0,200,0));	//African American
				colors.put(4, new Color(220,0,0));	//Native American
				colors.put(5, Color.GRAY);	//Asian
				colors.put(6, Color.GRAY);	//Hawaiian
				colors.put(7, Color.GRAY);	//Other
				colors.put(8, Color.GRAY);	//Mixed
				Transfer<CategoricalCounts<Object>, CategoricalCounts<Color>> rekey = new Categories.ReKey<Object, Color>(new CoC<Color>(Util.COLOR_SORTER), colors, Color.BLACK);

				Transfer<?, ?> chain = new Chain<>(Resources.DEFAULT_RENDERER,
						rekey, 
						new Shapes.ShapeGather(shapes, transformProvider),
						new Categories.RandomWeave());
				return chain;
			} catch (Exception e) {throw new RuntimeException("Error creating transfer.",e);}
		}
		public String name() {return "US Population (Weave)";}
		public String toString() {return fullName(this);}
	}
	

	
	public static class USCensusRaces implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.MergeCategories<>();}
		public Glyphset<?,?> glyphset() {return CENSUS_MM;}
		public Transfer<?,?> transfer() {
			Map<Object, Color> colors = new HashMap<>();
			colors.put(2, new Color(0,0,200));	//White
			colors.put(3, new Color(0,200,0));	//African American
			colors.put(4, new Color(136,90,68));//Native American
			colors.put(5, new Color(255,69,0));	//Asian
			colors.put(6, new Color(136,90,68));//Hawaiian
			colors.put(7, new Color(136,90,68));//Other
			colors.put(8, new Color(136,90,68));//Mixed

			Transfer<CategoricalCounts<Object>, CategoricalCounts<Color>> rekey = new Categories.ReKey<Object, Color>(new CoC<Color>(Util.COLOR_SORTER), colors, Color.BLACK);
			Transfer<CategoricalCounts<Color>, Color> stratAlpha = new Categories.HighAlpha(Color.white, .1, true);
			return new Chain<Object, Object>(
					Resources.DEFAULT_RENDERER,
					rekey,
					new General.Spread<>(new General.Spread.UnitSquare<CoC<Object>>(1), new Categories.MergeCategories<>()),
					stratAlpha);
		}
		public String name() {return "US Racial Distribution";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	

	public static class USCensusRacesLift implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.MergeCategories<>();}
		public Glyphset<?,?> glyphset() {return CENSUS_MM;}
		public Transfer<?,?> transfer() {
			final Color other = new Color(136,90,68);
			Map<Object, Color> colors = new HashMap<>();
			colors.put(2, new Color(0,0,200));	//White
			colors.put(3, new Color(0,200,0));	//African American
			colors.put(4, other);//Native American
			colors.put(5, new Color(255,69,0));	//Asian
			colors.put(6, other);//Hawaiian
			colors.put(7, other);//Other
			colors.put(8, other);//Mixed
			
			Transfer<CategoricalCounts<Object>, CategoricalCounts<Color>> rekey = new Categories.ReKey<Object, Color>(new CoC<Color>(Util.COLOR_SORTER), colors, Color.BLACK);
			Transfer<CategoricalCounts<Color>, Color> stratAlpha = new Categories.HighAlpha(Color.white, .1, true);
			Transfer<CategoricalCounts<Color>, Color> black = new General.Const<>(Color.black);
			
			Valuer<CategoricalCounts<Color>, Boolean> p = new Valuer<CategoricalCounts<Color>, Boolean>() {
				public Boolean value(CategoricalCounts<Color> val) {
					if (val.hasKey(other)) {
						double ratio = val.count(other)/((double) val.fullSize());
						return ratio > .1;
					} else {
						return false;
					}
				}
			};
			
			Transfer<CategoricalCounts<Color>, Color> lift = new If<>(p, black, stratAlpha);
			
			
			return new Chain<Object, Object>(
					Resources.DEFAULT_RENDERER,
					rekey,
					new General.Spread<>(new General.Spread.UnitSquare<CoC<Object>>(1), new Categories.MergeCategories<>()),
					lift);
		}
		public String name() {return "US Racial Distribution (highlight 'other')";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	
	public static class USSynPopulationMinAlpha implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.CountCategories<>();}
		public Glyphset<?,?> glyphset() {return CENSUS_SYN_PEOPLE;}
		public Transfer<?,?> transfer() {
			return new Chain<Object,Object>(
					Resources.DEFAULT_RENDERER,
					new Categories.ToCount<>(),
					new Numbers.FixedInterpolate(Color.white, Color.red, 0, 255));
		}
		public String name() {return "US Synthetic Population (minApha)";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}

	public static class USSynPopulationLinear implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.CountCategories<>();}
		public Glyphset<?,?> glyphset() {return CENSUS_SYN_PEOPLE;}
		public Transfer<?,?> transfer() {
			return new Chain<Object,Object>(
					Resources.DEFAULT_RENDERER,
					new Categories.ToCount<>(),
	  				new General.ValuerTransfer<>(new MathValuers.DivideInt<>(10), 0),
					new Numbers.FixedInterpolate(Color.white, Color.red, 0, 255));
		}
		public String name() {return "US Synthetic Population (Fractional Linear)";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	
	public static class USSynPopulationExp implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.CountCategories<>();}
		public Glyphset<?,?> glyphset() {return CENSUS_SYN_PEOPLE;}
		public Transfer<?,?> transfer() {
			return new Chain<Object,Object>(
					Resources.DEFAULT_RENDERER,
					new Categories.ToCount<>(),
					new General.ValuerTransfer<>(new MathValuers.Raise<>(.333333d), 0d),
					new Numbers.Interpolate(new Color(255,0,0,30), new Color(255,0,0,255)));
		}
		public String name() {return "US Synthetic Population (exp)";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	
	public static class USSynPopulationLog implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.CountCategories<>();}
		public Glyphset<?,?> glyphset() {return CENSUS_SYN_PEOPLE;}
		public Transfer<?,?> transfer() {
			return new Chain<Object,Object>(
					Resources.DEFAULT_RENDERER,
					new Categories.ToCount<>(),
	  				new General.ValuerTransfer<>(new MathValuers.Log<>(10, false, true), 0d),
					new Numbers.Interpolate(new Color(254, 229, 217), new Color(165, 15, 21)));
		}
		public String name() {return "US Synthetic Population (Log 10)";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	
	public static class USSynPopulationRaces implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.CountCategories<>();}
		public Glyphset<?,?> glyphset() {return CENSUS_SYN_PEOPLE;}
		public Transfer<?,?> transfer() {
			Map<Object, Color> colors = new HashMap<>();
			colors.put('w', new Color(0,0,200));	//White
			colors.put('b', new Color(0,200,0));	//African American
			colors.put('a', new Color(255,69,0));	//Asian
			colors.put('h', new Color(255,165,0));	//Hispanic
			colors.put('o', new Color(136,90,68));	//Other

			Transfer<CategoricalCounts<Object>, CategoricalCounts<Color>> rekey = new Categories.ReKey<Object, Color>(new CoC<Color>(Util.COLOR_SORTER), colors, Color.BLACK);
			Transfer<CategoricalCounts<Color>, Color> stratAlpha = new Categories.HighAlpha(Color.white, .1, true);
			return new Chain<Object, Object>(
					Resources.DEFAULT_RENDERER,
					rekey,
					stratAlpha);
		}
		public String name() {return "US Synthetic Population (Races)";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}
	
	public static class UniqueReport<A extends CategoricalCounts<?>> implements Transfer.Specialized<A,A> {
		Collection<Object> seen = new HashSet<>();
		public A emptyValue() {return null;}
		public Specialized<A,A> specialize(Aggregates<? extends A> aggregates) {return this;}
		public A at(int x, int y, Aggregates<? extends A> aggregates) {
			A cc = aggregates.get(x, y);
			
			for (int i=0; i<cc.size(); i++) {
				Object key = cc.key(i);
				if (!seen.contains(key)) {
					System.out.println("New key: " + key);
					seen.add(key);
				}
			}
			
			return cc;
		}
		
	}
	

	public static class USSynPopulationContours implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.CountCategories<>();}
		public Glyphset<?,?> glyphset() {return CENSUS_SYN_PEOPLE;}
		public Transfer<?,?> transfer() {
			return new Chain<Object,Object>(
					Resources.DEFAULT_RENDERER,
					new Categories.ToCount<>(),
					new General.ValuerTransfer<>(new MathValuers.Log<>(10, false, true), 0d),
					new ISOContours.NContours<>(Resources.DEFAULT_RENDERER, 3, true),
					new Numbers.Interpolate(new Color(254, 229, 217), new Color(165, 15, 21))
					);
		}
		public String name() {return "US Synthetic Population (Contour)";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}

	public static class USSynPopulationContourLines implements Preset {
		public Aggregator<?,?> aggregator() {return new Categories.CountCategories<>();}
		public Glyphset<?,?> glyphset() {return CENSUS_SYN_PEOPLE;}
		public Transfer<?,?> transfer() {
			return new Chain<Object,Object>(
					Resources.DEFAULT_RENDERER,
					new Categories.ToCount<>(),
					new General.ValuerTransfer<>(new MathValuers.Log<>(10, false, true), 0d),
					new ISOContours.NContours<>(Resources.DEFAULT_RENDERER, 3, false),
					new Numbers.Interpolate(new Color(254, 229, 217), new Color(165, 15, 21))
					);
		}
		public String name() {return "US Synthetic Population (Contour Lines)";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}

	
	public static class ScatterplotContours implements Preset {
		public Aggregator<?,Integer> aggregator() {return new Numbers.Count<Object>();}
		public Glyphset<?,?> glyphset() {return CIRCLE_SCATTER;}
		public Transfer<?,?> transfer() {
			return new Chain<Object,Object>(
					Resources.DEFAULT_RENDERER,
					new ISOContours.NContours<>(Resources.DEFAULT_RENDERER, 5, true),
					new General.Simplify<>(0),
					new General.Replace<>(null, 0, 0),
					new Numbers.Interpolate(new Color(254, 229, 217), new Color(165, 15, 21))
					);
		}
		public String name() {return "Scatterplot (Contour Lines)";}
		public String toString() {return fullName(this);}
		public boolean init(HasViewTransform panel) {return glyphset() != null;}
	}

	
	
	private static final Glyphset<Rectangle2D, Color> CIRCLE_SCATTER; 
	private static final Glyphset<Point2D, Color> KIVA_ADJ; 
	private static final Glyphset<Point2D, Color> BOOST_MEMORY_MM; 
	private static final Glyphset<Point2D, CoC<String>> CENSUS_MM;
	private static final Glyphset<Point2D, Character> CENSUS_SYN_PEOPLE;
	private static final Glyphset<Point2D, Color> WIKIPEDIA;
	
	private static String MEM_VIS_BIN = "../data/MemVisScaled.hbin";
	private static String CIRCLE_CSV = "../data/circlepoints.csv";
	private static String KIVA_BIN = "../data/kiva-adj.hbin";
	private static String CENSUS_TRACTS = "../data/2010Census_RaceTract.hbin";
	private static String CENSUS_SYN_PEOPLE_BIN = "../data/2010Census_RacePersonPoints.hbin";
	private static String CENSUS = "../data/2010Census_RaceTract.hbin";
	private static String WIKIPEDIA_BFS= "../data/wiki-adj.hbin";
	
	static {
		//Glyphset<Rectangle2D, Color> boost_temp = null;
		Glyphset<Point2D, Color> boost_temp = null;
		try {boost_temp = GlyphsetUtils.memMap(
				"BGL Memory", MEM_VIS_BIN, 
				new Indexed.ToPoint(true, 0, 1),
				//new Indexed.ToRect(.001, .001, true,0,1),
				new ToValue<>(2, new Binary<Integer,Color>(0, Color.BLUE, Color.RED)), 
				1, "ddi");
		} catch (Exception e) {
			System.err.printf("## Error loading data from %s.  Related presets are unavailable.\n", MEM_VIS_BIN);
		}
		BOOST_MEMORY_MM = boost_temp;
		
		Glyphset<Point2D, CoC<String>> census_temp = null;
		try {census_temp = GlyphsetUtils.memMap(
				"US Census Tracts", CENSUS_TRACTS, 
				new Indexed.ToPoint(true, 0, 1),
				new Valuer.CategoryCount<>(new Util.ComparableComparator<String>(), 3,2),
				1, null);
		} catch (Exception e) {
			System.err.printf("## Error loading data from %s.  Related presets are unavailable.\n", CENSUS);
		}
		CENSUS_MM = census_temp;
		
		Glyphset<Point2D, Character> census_temp2 = null;
		try {census_temp2 = GlyphsetUtils.memMap(
				"US Census Synthetic People", CENSUS_SYN_PEOPLE_BIN, 
				new Indexed.ToPoint(true, 0, 1),
				new Indexed.ToValue<Indexed,Character>(2),
				1, null);
		} catch (Exception e) {
			System.err.printf("Error loading data from %s.  Related presets are unavailable.\n", CENSUS_TRACTS);
		}
		CENSUS_SYN_PEOPLE = census_temp2;

		
		Glyphset<Rectangle2D, Color> circle_temp = null;
		try {
			circle_temp = GlyphsetUtils.autoLoad(new File(CIRCLE_CSV), .1, DynamicQuadTree.<Rectangle2D, Color>make());
		} catch (Exception e) {
			System.err.printf("## Error loading data from %s.  Related presets are unavailable.\n", CIRCLE_CSV);
		}
		CIRCLE_SCATTER = circle_temp;
		
		Glyphset<Point2D, Color> kiva_temp = null;
		try {kiva_temp = GlyphsetUtils.memMap(
						"Kiva", KIVA_BIN, 
						new Indexed.ToPoint(false, 0, 1),
						new Valuer.Constant<Indexed, Color>(Color.RED), 
						1, null);
		} catch (Exception e) {
			System.err.printf("## Error loading data from %s.  Related presets are unavailable.\n", KIVA_BIN);
		}
		KIVA_ADJ = kiva_temp;		
		
		Glyphset<Point2D, Color> wiki_temp = null;
		try {wiki_temp = GlyphsetUtils.memMap(
						"Wikipedia BFS adjacnecy", WIKIPEDIA_BFS, 
						new Indexed.ToPoint(false, 0, 1),
						new Valuer.Constant<Indexed, Color>(Color.RED), 
						1, null);
		} catch (Exception e) {
			System.err.printf("## Error loading data from %s.  Related presets are unavailable.\n", KIVA_BIN);
		}
		WIKIPEDIA = wiki_temp;
	}
	
	


}
