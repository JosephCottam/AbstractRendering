package ar.ext.seamCarving;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ar.Aggregates;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.aggregates.wrappers.CompositeWrapper;
import ar.aggregates.wrappers.CompositeWrapper.Pair;
import ar.app.util.GeoJSONTools;
import ar.glyphsets.WrappedCollection;
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.renderers.ForkJoinRenderer;
import ar.rules.CategoricalCounts;
import ar.rules.General;
import ar.rules.Numbers;
import ar.rules.SeamCarving;
import ar.rules.SeamCarving.Direction;
import static ar.rules.combinators.Combinators.*;
import ar.selectors.TouchesPixel;
import ar.util.Util;

public class Cartogram {

	public static void main(String[] args) throws Exception {
		Rectangle viewBounds = new Rectangle(0, 0, 1200,800);
		Renderer renderer = new ForkJoinRenderer();
		
		
		//Glyphset<Point2D, Character> populationSource = ar.app.components.sequentialComposer.OptionDataset.CENSUS_SYN_PEOPLE.dataset();
		final Glyphset<Point2D, CategoricalCounts<String>> populationSource = ar.app.components.sequentialComposer.OptionDataset.CENSUS_TRACTS.glyphset;
		System.out.println("Population glyphset loaded.");
		
		File statesSource = new File("../data/maps/USStates/");
		final Map<String, Shape> rawShapes = simplifyKeys(GeoJSONTools.flipY(GeoJSONTools.loadShapesJSON(statesSource, false)));
		rawShapes.remove("AK");
		rawShapes.remove("HI");
		final Glyphset<Shape, String> states = WrappedCollection.wrap(rawShapes.entrySet(), new Shaper.MapValue<String, Shape>(), new Valuer.MapKey<String, Shape>());
		System.out.println("State shapes loaded.");

		final AffineTransform viewTransform = Util.zoomFit(populationSource.bounds().createUnion(states.bounds()), viewBounds.width, viewBounds.height);

		
		final Aggregates<Integer> population = renderer.aggregate(populationSource, TouchesPixel.make(populationSource), new Numbers.Count<>(), viewTransform, viewBounds.width, viewBounds.height);
		final Aggregates<String> labels = renderer.aggregate(states, TouchesPixel.make(states), new General.Last<>(""), viewTransform, viewBounds.width, viewBounds.height);
		Aggregates<Pair<String,Integer>> pairs = CompositeWrapper.wrap(labels, population);
		System.out.println("Base aggregates created.\n");
		
		
		int step=100;
		//final Transfer.Specialized<Pair<String,Integer>,Pair<String,Integer>> smear = new General.Smear<>(EMPTY);
		//Aggregates<Pair<String,Integer>> smeared = renderer.transfer(pairs, smear);

		final Transfer<Integer, Color> colorPopulation = 
				seq().then(new General.ValuerTransfer<>(new MathValuers.Log<Integer>(10d), 0d))
					 .then(new General.Replace<>(Double.NEGATIVE_INFINITY, 0d, 0d))
					 .then(new Numbers.Interpolate<Double>(new Color(255,0,0,25), new Color(255,0,0,255)));

		final General.MapWrapper<String, Color> color2012= new General.MapWrapper<>(results2012, Color.gray);  
		final General.MapWrapper<String, Color> color2008= new General.MapWrapper<>(results2008, Color.gray);  
		
		for (int seams=0; seams<viewBounds.width; seams+=step) {
			System.out.println("Starting removing " + seams + " seams");

			//Transfer.Specialized<Pair<String,Integer>, Pair<String,Integer>> carver = new SeamCarving.CarveIncremental<>(new DeltaPair(), Direction.V, EMPTY, seams);
			Transfer.Specialized<Pair<String,Integer>, Pair<String,Integer>> carver = new SeamCarving.CarveSweep<>(new DeltaPair(), Direction.V, EMPTY, seams);
			Aggregates<Pair<String,Integer>> carved = renderer.transfer(pairs, carver);
			//carved = renderer.transfer(carved, new Borders(EMPTY));
			
			CompositeWrapper<String,Integer, ?> composite = CompositeWrapper.convert(carved, "", 0);
			
			Aggregates<Integer> carvedPop = composite.right();
			Aggregates<String> carvedStates = composite.left();
			
			Aggregates<Color> popImg = renderer.transfer(carvedPop, colorPopulation.specialize(carvedPop));
			Aggregates<Color> states2012 = renderer.transfer(carvedStates, color2012);
			Aggregates<Color> states2008 = renderer.transfer(carvedStates, color2008);
			
			Util.writeImage(AggregateUtils.asImage(popImg), new File(String.format("./testResults/seams/%d-seams-population.png",seams)));
			Util.writeImage(AggregateUtils.asImage(states2008), new File(String.format("./testResults/seams/%d-2008-seams-election.png",seams)));
			Util.writeImage(AggregateUtils.asImage(states2012), new File(String.format("./testResults/seams/%d-2012-seams-election.png",seams)));
			System.out.println("Completed export on " + seams + " seams\n");
		}
	}
	
	public static final Color PARTY1 = Color.green;
	public static final Color PARTY2 = Color.pink;
	public static final String BOARDER = "BOARDER";
	public static Map<String, Color> results2008 = new HashMap<>();
	public static Map<String, Color> results2012 = new HashMap<>();
	static {
		File input = new File("../data/elections.csv");
		try (BufferedReader r = new BufferedReader(new FileReader(input))) {
			String line = r.readLine();
			while (line!=null) {
				String[] parts = line.split(",");
				results2012.put(parts[0], parts[1].equals("1") ? PARTY1 : PARTY2);
				results2008.put(parts[0], parts[2].equals("1") ? PARTY1 : PARTY2);
				line = r.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		results2012.put(BOARDER, Color.black);
		results2008.put(BOARDER, Color.black);
		System.out.println("Loaded results data.");
	}

	public static final Pair<String, Integer> EMPTY = new Pair<>("",0);
	
	public static final class DeltaPair implements SeamCarving.Delta<Pair<String, Integer>> {
		@Override
		public double delta(Pair<String, Integer> left, Pair<String, Integer> right) {
			return left.right - right.right;
		}
	}
	
	/**Strip off file extensions from keys (if present).**/
	public static final Map<String,Shape> simplifyKeys(Map<String,Shape> input) {
		Map<String, Shape> out =new HashMap<>();
		for (Map.Entry<String, Shape> e:input.entrySet()) {
			String simpleKey = e.getKey().split("\\.")[0];
			out.put(simpleKey, e.getValue());
		}
		return out;
	}
	
	public static final class Borders implements Transfer.ItemWise<Pair<String,Integer>, Pair<String,Integer>> {
		private final Pair<String,Integer> empty;
		public Borders(Pair<String,Integer> empty) {this.empty = empty;}
		
		@Override
		public Aggregates<Pair<String, Integer>> process(Aggregates<? extends Pair<String, Integer>> aggregates, Renderer rend) {
			return rend.transfer(aggregates, this);
		}

		@Override public Pair<String, Integer> emptyValue() {return empty;}

		@Override
		public ar.Transfer.Specialized<Pair<String, Integer>, Pair<String, Integer>> specialize(
				Aggregates<? extends Pair<String, Integer>> aggregates) {
			return this;
		}

		@Override
		public Pair<String, Integer> at(int x, int y, Aggregates<? extends Pair<String, Integer>> aggs) {
			String ref = state(aggs, x,y);
			
			String up = state(aggs,x,y+1);
			String down = state(aggs,x,y-1);
			String left = state(aggs,x-1,y);
			String right = state(aggs,x+1,y);
			
			Pair<String,Integer> v = aggs.get(x, y);
			if (ref.equals(up) && ref.equals(down) && ref.equals(left) && ref.equals(right)) {
				return v;
			} else {
				return new Pair<>(BOARDER,v.right);
			}
			
		}
		
		private static final String state(Aggregates<? extends Pair<String, Integer>> aggs, int x, int y) {return aggs.get(x,y).left;}
		
	}
}
