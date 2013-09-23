package ar;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.GeometryAttributeImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.awt.PolygonShape;

import ar.app.display.SimpleDisplay;
import ar.app.util.GlyphsetUtils;
import ar.glyphsets.MemMapList;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.renderers.ParallelGlyphs;
import ar.renderers.ParallelSpatial;
import ar.renderers.RenderUtils;
import ar.rules.CategoricalCounts.CoC;
import ar.rules.CategoricalCounts;
import ar.rules.Categories;
import ar.rules.Numbers;
import ar.util.Util;

public class Census {
	static class Pair {
		public final Object cat;
		public final int val;
		public Pair(Object cat, int val) {
			this.cat = cat;
			this.val = val;
		}
		public Pair(Object cat, Object val) {this(cat, Integer.parseInt(val.toString()));}
	}
	
	static class Pairer implements Valuer<Indexed,Pair> {
		private static final long serialVersionUID = 1L;
		final int catIdx, valIdx;
		public Pairer(int catIdx, int valIdx) {
			this.catIdx = catIdx;
			this.valIdx = valIdx;
		}
		
		public Pair value(Indexed from) {return new Pair(from.get(catIdx), from.get(valIdx));}
	}
	
	static class AggregatePairs implements Aggregator<Pair, CoC<Object>> {
		private static final long serialVersionUID = 1L;

		public CoC<Object> combine(long x, long y, CoC<Object> current, Pair update) {
			return current.extend(update.cat, update.val);
		}

		public CoC<Object> rollup(List<CoC<Object>> sources) {
			return CategoricalCounts.CoC.rollup(null, sources);
		}

		public CoC<Object> identity() {return new CoC<Object>();}
	}
	
	static class LiftIf implements Transfer<CategoricalCounts<Color>, Color> {
		private static final long serialVersionUID = 9066005967376232334L;

		final Transfer<CategoricalCounts<Color>, Color> baseline;
		
		public LiftIf(Transfer<CategoricalCounts<Color>, Color> baseline) {this.baseline = baseline;}

		@Override
		public Color emptyValue() {return baseline.emptyValue();}

		@Override
		public Transfer.Specialized<CategoricalCounts<Color>, Color> specialize(Aggregates<? extends CategoricalCounts<Color>> aggregates) {
			Transfer.Specialized ts = baseline.specialize(aggregates);
			return new Specialized(ts);
		}
		
		protected static class Specialized extends LiftIf implements Transfer.Specialized<CategoricalCounts<Color>, Color> {
			final Transfer.Specialized<CategoricalCounts<Color>, Color> baseline;
			public Specialized(Transfer.Specialized<CategoricalCounts<Color>, Color> baseline) {
				super(baseline);
				this.baseline = baseline;
			}

			@Override
			public Color at(int x, int y,
					Aggregates<? extends CategoricalCounts<Color>> aggregates) {
				
				CategoricalCounts<Color> val = aggregates.get(x, y);
				int keyIdx=-1;
				for (int i=0; i< val.size(); i++) {
					if (val.key(i) == Color.GRAY) {keyIdx = i; break;}
				}
				if (keyIdx >=0 && val.count(keyIdx)/((double) val.fullSize()) > .1) {
					return Color.BLACK;
				} else {
					return baseline.at(x, y, aggregates);
				}
			}
		}
 	}

	
	static class Weave implements Transfer.Specialized<CoC<Color>, Color> {
		private static final long serialVersionUID = -6006747974949256518L;

		public Color at(int x, int y, Aggregates<? extends CoC<Color>> aggregates) {
			CoC<Color> counts = aggregates.get(x, y);
			if (counts.size()>0) {return counts.key(1);}
			return Color.cyan;
		}

		public Color at2(int x, int y,
				Aggregates<? extends CoC<Color>> aggregates) {
			CoC<Color> counts = aggregates.get(x, y);
			int top = counts.fullSize();
			int r = (int) Math.random()*top;
			for (int i = 0; i<counts.size();i++) {
				int w = counts.count(i);
				r -= w;
				if (r <= 0) {return counts.key(i);}
			}
			if (counts.size() >0) {return counts.key(counts.size()-1);}
			else {return emptyValue();}
		}

		@Override
		public Color emptyValue() {return Util.CLEAR;}

		@Override
		public Weave specialize(Aggregates<? extends CoC<Color>> aggregates) {return this;}		
	}
	
	static class RegionSpread implements Transfer.Specialized<CoC<Color>, CoC<Color>> {
		private static final long serialVersionUID = 4664592034128237981L;
		final List<Shape> regions;
		final AffineTransform ivt;
		RegionSpread(List<Shape> reg, AffineTransform ivt) throws Exception {
			this.ivt = ivt;
			AffineTransform vt = ivt.createInverse();
			this.regions = new ArrayList<>();
			for (Shape s: reg) {
				regions.add(vt.createTransformedShape(s));
			}
		}

		
		@Override
		public CoC<Color> at(int x, int y, Aggregates<? extends CoC<Color>> aggregates) {
			Shape region = touches(x,y);
			if (region == null) {return emptyValue();}
			CoC<Color> v = gather(region, aggregates);
			return v;
		}

		@Override
		public CoC<Color> emptyValue() {return new CoC<Color>(Util.COLOR_SORTER);}

		@Override
		public RegionSpread specialize(Aggregates<? extends CoC<Color>> aggregates) {return this;}
		
		public Shape touches(int x, int y) {
			Rectangle2D r = new Rectangle2D.Double(x,y,1,1);
			for (Shape s: regions) {
				if (s.intersects(r)) {
					System.out.println(s);
					return s;
				}
			}
			return null;
		}

		static CoC<Color> ref=new CoC(); 
		public CoC<Color> gather(Shape region, Aggregates<? extends CoC<Color>> aggs) {
			Rectangle2D r = new Rectangle2D.Double(0,0,1,1);
			CoC<Color> acc = emptyValue();
			for (int x=aggs.lowX(); x<aggs.highX(); x++) {
				for (int y=aggs.lowY(); y < aggs.highY(); y++) {
					r.setRect(x, y, 1,1);
					if (region.contains(r)) {
						acc = CoC.rollup(Util.COLOR_SORTER, Arrays.asList(acc, aggs.get(x, y)));
					}
				}
			}
			if (!acc.equals(ref)) {
				ref =acc;
				System.out.println("Switched: " + ref.toString());
			}
			
//			System.out.println(acc);
			return acc;
		}
		
	}

	
	static class FakeMapProject implements Shaper<Indexed> {
		private static final long serialVersionUID = -8465499888784916302L;
		final Shaper<Indexed> basis;
		
		public FakeMapProject(Shaper<Indexed> basis) {
			this.basis=basis;
		}
		@Override
		public Shape shape(Indexed from) {
			Rectangle2D b = basis.shape(from).getBounds2D();
			return new Rectangle2D.Double(b.getX()*.85, b.getY(), b.getWidth(), b.getHeight());
		}
		
	}
	
	//From :http://stackoverflow.com/questions/2044876/does-anyone-know-of-a-library-in-java-that-can-parse-esri-shapefiles
	@SuppressWarnings("all")
	public static List<Shape> loadShapes(String filename) throws Exception {
		File file = new File(filename);
		List<Shape> polys = new ArrayList<>();
		
		  Map connect = new HashMap();
		  connect.put("url", file.toURL());
		
		  DataStore dataStore = DataStoreFinder.getDataStore(connect);
		  String[] typeNames = dataStore.getTypeNames();
		  String typeName = typeNames[0];
		
		  FeatureSource featureSource = dataStore.getFeatureSource(typeName);
		  FeatureCollection collection = featureSource.getFeatures();
		  FeatureIterator iterator = collection.features();
		
		  try {
		    while (iterator.hasNext()) {
		      Feature feature = iterator.next();
		      GeometryAttribute geoProp = feature.getDefaultGeometryProperty();
		      GeometryAttributeImpl impl = (GeometryAttributeImpl) geoProp;
		      Geometry hull = impl.getValue().convexHull();
		      PolygonShape shape = new PolygonShape(hull.getCoordinates(), Collections.EMPTY_LIST);
		      polys.add(shape);
		    }
		  } finally {
		    iterator.close();
		  }
		 return polys;

	}
	

	@SuppressWarnings("all")
	public static void show(String label, int width, int height, Aggregates<?> aggs, Transfer<?,?> t) {

		SimpleDisplay.show(label, width, height, (Aggregates) aggs, (Transfer) t);
		
		Renderer r=new ParallelSpatial();
		Transfer.Specialized ts = t.specialize((Aggregates) aggs);
		aggs = r.transfer((Aggregates) aggs, ts);
		BufferedImage img = Util.asImage((Aggregates<Color>) aggs, width, height, Util.CLEAR);
		Util.writeImage(img, new File("../data/" + label + ".png"));
		
	}
	
	
	public static void main(String[] args) throws Exception {
		Glyphset<Pair> race = 
				new MemMapList<>(
						new File("../data/census/Race_TractLatLonDenorm.hbin"),
						new FakeMapProject(new Indexed.ToRect(.3, .3, true, 0,1)),
						//new Indexed.ToRect(10, 10, true, 1,0),
						new Pairer(3,2));

		Renderer r = new ParallelGlyphs();
		
		double ratio = 1.853;  //With x height of the US...
		int width = 800;
		int height = (int) (width / ratio);
		AffineTransform ivt = Util.zoomFit(race.bounds(), width, height);
		ivt.invert();
		System.out.println("Aggregating");
		Aggregator<Pair,CoC<Object>> raceAggregator = new AggregatePairs();
		Aggregates<CoC<Object>> raceAggs = r.aggregate(race, raceAggregator, ivt, width, height);

		
		//Homo alpha
		System.out.println("Homo Alpha");
		Aggregates<Integer> counts = r.transfer(raceAggs, new Categories.ToCount<>());
		//Transfer<Number, Color> homoAlpha = new  Numbers.Interpolate(new Color(255,0,0,25), new Color(255,0,0,255), Util.CLEAR, 10);
		Transfer<Number, Color> homoAlpha = new  Numbers.Interpolate(new Color(255,0,0,100), new Color(255,0,0,255));
		show("Total_Homo_Alpha", width, height, counts, homoAlpha);
				
		//Stratified Alpha  		
		Map<Object, Color> colors = new HashMap<>();
		colors.put(2, new Color(0,0,200));	//White
		colors.put(3, new Color(0,200,0));	//African American
		colors.put(4, new Color(220,0,0));	//Native American
		colors.put(5, Color.GRAY);	//Asian
		colors.put(6, Color.GRAY);	//Hawaiian
		colors.put(7, Color.GRAY);	//Other
		colors.put(8, Color.GRAY);	//Mixed

		System.out.println("Strat Alpha");
		Transfer<CategoricalCounts<Object>, CategoricalCounts<Color>> t1 = new Categories.ReKey<Object, Color>(new CoC<Color>(Util.COLOR_SORTER), colors, Color.BLACK);
		Transfer.Specialized<CategoricalCounts<Object>, CategoricalCounts<Color>> ts = t1.specialize(raceAggs);
		Aggregates<CategoricalCounts<Color>> colorAggs = r.transfer(raceAggs, ts);
		Transfer<CategoricalCounts<Color>, Color> stratAlpha = new Categories.HighAlpha(Color.white, .1, true);
		show("Race_Strat_Alpha", width, height, colorAggs, stratAlpha);
		
		//Selection-Set
		System.out.println("Sel Set");
		Transfer<CategoricalCounts<Color>, Color> lift = new LiftIf(stratAlpha);
		lift.specialize(colorAggs);
		show("Race_Sel_quarter_native", width, height, colorAggs, lift);
//
//		//Color Weave
//		List<Shape> shapes = loadShapes("../data/tl_2010_us_state10.shp");
//		Transfer<CoC<Color>, CoC<Color>> spread = new RegionSpread(shapes, ivt);
//		Transfer<CoC<Color>, Color> weave = new Weave();
//		
//		Aggregates<CoC<Color>> spreadAggs = r.transfer((Aggregates) colorAggs, spread);
//		show("Weave", width, height, spreadAggs, weave);
//		System.out.println("Done");
	}
}
