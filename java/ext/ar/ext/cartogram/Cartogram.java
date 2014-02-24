package ar.ext.cartogram;

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
import ar.renderers.ParallelRenderer;
import ar.rules.CategoricalCounts;
import ar.rules.General;
import ar.rules.Numbers;
import ar.rules.SeamCarving;
import ar.rules.Shapes;
import ar.rules.SeamCarving.Carve.Direction;
import ar.rules.combinators.NTimes;
import ar.rules.combinators.Seq;
import ar.selectors.TouchesPixel;
import ar.util.Util;

public class Cartogram {

	public static void main(String[] args) throws Exception {
		Rectangle viewBounds = new Rectangle(0, 0, 500,500);
		Renderer renderer = new ParallelRenderer();
		
		
		//Glyphset<Point2D, Character> populationSource = ar.app.components.sequentialComposer.OptionDataset.CENSUS_SYN_PEOPLE.dataset();
		Glyphset<Point2D, CategoricalCounts<String>> populationSource = ar.app.components.sequentialComposer.OptionDataset.CENSUS_TRACTS.dataset();
		AffineTransform viewTransform = Util.zoomFit(populationSource.bounds(), viewBounds.width, viewBounds.height);
		Aggregates<Integer> population = renderer.aggregate(populationSource, TouchesPixel.make(populationSource), new Numbers.Count<>(), viewTransform, viewBounds.width, viewBounds.height);
		System.out.println("Population glyphset loaded.");


		File statesSource = new File("../data/maps/USStates/");
		final Map<String, Shape> rawShapes = GeoJSONTools.flipY(GeoJSONTools.loadShapesJSON(statesSource, false));
		Glyphset<Shape, String> states = WrappedCollection.wrap(rawShapes.entrySet(), new Shaper.MapValue<String, Shape>(), new Valuer.MapKey<String, Shape>());
		System.out.println("State shapes loaded.");
		
		Aggregates<String> labels = renderer.aggregate(states, TouchesPixel.make(states), new General.Last<>(""), viewTransform, viewBounds.width, viewBounds.height);
		Aggregates<Pair<String,Integer>> pairs = CompositeWrapper.wrap(labels, population);
		System.out.println("Base aggregates created.");


		
//		final Transfer.Specialized<Pair<String,Integer>,Pair<String,Integer>> smear = new General.Smear<>(EMPTY);
//		Aggregates<Pair<String,Integer>> smeared = renderer.transfer(pairs, smear);

		Transfer.Specialized<Pair<String,Integer>, Pair<String,Integer>> carver = new SeamCarving.Carve<>(new DeltaPair(), Direction.V, EMPTY);
		final Transfer<Integer, Color> colorPopulation = 
				Seq.start(new General.ValuerTransfer<>(new MathValuers.Log<Integer>(10d), 0d))
				.then(new General.Replace<>(Double.NEGATIVE_INFINITY, 0d, 0d))
				.then(new Numbers.Interpolate<Double>(new Color(255,0,0,25), new Color(255,0,0,255)));

		final Transfer<String, Color> color2012= new General.MapWrapper<>(results2012, Color.gray);  
		final Transfer<String, Color> color2008= new General.MapWrapper<>(results2008, Color.gray);  
		
		int step=100;
		for (int seams=0; seams<viewBounds.width; seams+=step) {
			System.out.println("Starting export on " + seams + " seams");

			final Transfer.Specialized<Pair<String,Integer>,Pair<String,Integer>> carve = new NTimes.Specialized<>(seams, carver);
			Aggregates<Pair<String,Integer>> carved = renderer.transfer(pairs, carve);
			CompositeWrapper<String,Integer, ?> composite = CompositeWrapper.convert(carved, "", 0);
			
			Aggregates<Integer> carvedPop = composite.right();
			Aggregates<String> carvedStates = composite.left();
			
			Aggregates<Color> popImg = renderer.transfer(carvedPop, colorPopulation.specialize(carvedPop));
			Aggregates<Color> states2012 = renderer.transfer(carvedStates, color2012.specialize(carvedStates));
			Aggregates<Color> states2008 = renderer.transfer(carvedStates, color2008.specialize(carvedStates));
			
			Util.writeImage(AggregateUtils.asImage(states2008), new File(String.format("./testResults/seams/2008-%d-seams-election.png",seams)));
			Util.writeImage(AggregateUtils.asImage(states2012), new File(String.format("./testResults/seams/2012-%d-seams-election.png",seams)));
			Util.writeImage(AggregateUtils.asImage(popImg), new File(String.format("./testResults/seams/%d-seams-population.png",seams)));
			System.out.println("Completed export on " + seams + " seams\n");
		}
	}
	
	public static final Color PARTY1 = Color.green;
	public static final Color PARTY2 = Color.orange;
	
	public static Map<String, Color> results2008 = new HashMap<>();
	public static Map<String, Color> results2012 = new HashMap<>();
	static {
		File input = new File("../data/elections.csv");
		try (BufferedReader r = new BufferedReader(new FileReader(input))) {
			while (r.ready()) {
				String line = r.readLine();
				String[] parts = line.split(",");
				results2012.put(parts[0], parts[1]=="1" ? PARTY1 : PARTY2);
				results2008.put(parts[0], parts[2]=="1" ? PARTY1 : PARTY2);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Loaded results data.");
	}

	public static final Pair<String, Integer> EMPTY = new Pair<>("",0);
	
	public static final class DeltaPair implements SeamCarving.Delta<Pair<String, Integer>> {
		@Override
		public double delta(Pair<String, Integer> left,
				Pair<String, Integer> right) {
			return left.right - right.right;
		}
	}
	
}
