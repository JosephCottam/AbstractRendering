package ar.ext.seamCarving;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.Transfer;
import ar.app.display.ARComponent;
import ar.app.display.TransferDisplay;
import ar.app.util.GlyphsetUtils;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.renderers.ParallelRenderer;
import ar.rules.General;
import ar.rules.Numbers;
import ar.rules.SeamCarving;
import ar.rules.SeamCarving.Direction;
import static ar.rules.combinators.Combinators.*;
import ar.selectors.TouchesPixel;
import ar.util.Util;

public class CarveApp {
	public static void main(String[] args) throws Exception {
		//------------------------ Setup Operations -------------------
		//Get a set of geometry and associated values (aka, a glyphset) 
		Glyphset<Point2D, Character> dataset = 
				GlyphsetUtils.memMap(
						"US Census tracts", "../data/2010Census_RaceTract.hbin",
						//"US Census Synthetic People", "../data/2010Census_RacePersonPoints.hbin", 
						new Indexed.ToPoint(true, 0, 1),
						new Indexed.ToValue<Indexed,Character>(2),
						1, null);

		final boolean showSeams = Boolean.parseBoolean(Util.argKey(args, "-showseams", "false"));
		final String selected = Util.argKey(args, "-carvers", "*");
		final int spread = Integer.parseInt(Util.argKey(args, "-spread", "0"));
		final int perhapsSeams = Integer.parseInt(Util.argKey(args, "-seams", "100"));
		final float seamFactor = Float.parseFloat(Util.argKey(args, "-sf", "0"));
		final int width = Integer.parseInt(Util.argKey(args, "-width", "1200"));
		final int height = Integer.parseInt(Util.argKey(args, "-height", "800"));

		final int seams;
		if (seamFactor != 0) {seams = (int) (width*seamFactor);}
		else {seams = perhapsSeams;}

		
		Renderer r = new ParallelRenderer();
		Aggregator<Object,Integer> aggregator = new Numbers.Count<Object>();
		Selector<Point2D> selector = TouchesPixel.make(dataset);


		AffineTransform vt = Util.zoomFit(dataset.bounds(), width, height);
		Aggregates<Integer> aggregates = r.aggregate(dataset, selector, aggregator, vt, width, height);

		
		Map<String, Transfer<Integer,Integer>> allCarvers = new HashMap<String, Transfer<Integer,Integer>>() {{
				put("incremental", new SeamCarving.CarveIncremental<>(new SeamCarving.LeftValue<Integer>(), Direction.V, 0,seams));
				put("sweep", new SeamCarving.CarveSweep<>(new SeamCarving.LeftValue<Integer>(), Direction.V, 0, seams));
				put("twosweeps", new SeamCarving.CarveTwoSweeps<>(new SeamCarving.LeftValue<Integer>(), Direction.V, 0, seams));
				put("sweepn", new SeamCarving.CarveSweepN<>(new SeamCarving.LeftValue<Integer>(), Direction.V, 0, seams));
		}};
		
		Map<String, Transfer<Integer,Integer>> carvers;
		if (selected == null || selected.equals("*")) { 
			carvers = allCarvers;
		} else {
			carvers = new HashMap<>();
			for (String c:selected.split(",")) {
				c = c.toLowerCase();
				if (allCarvers.containsKey(c)) {carvers.put(c, allCarvers.get(c));}
			}
		}
		
		Map<String, Transfer<Integer,Color>> carvers2 = new HashMap<>();
		if (showSeams) {
			for (Map.Entry<String, Transfer<Integer,Integer>> e: carvers.entrySet()) {
				if (e.getValue() instanceof SeamCarving.AbstractCarver) { //TODO: Remove check when/if incremental joins the AbstractCarver framework...
					Transfer<Integer, Color> nc = new SeamCarving.DrawSeams<>((SeamCarving.AbstractCarver<Integer>) e.getValue(), new Color(100,149,237), Color.blue);
					carvers2.put(e.getKey(), nc);
				} 
			}
		} else {
			final Transfer<Integer, Color> rest = 
					seq().then(new General.ValuerTransfer<>(new MathValuers.Log<Integer>(10d), 0d))
						 .then(new General.Replace<>(Double.NEGATIVE_INFINITY, 0d, 0d))
						 .then(new Numbers.Interpolate<Double>(new Color(255,0,0,25), new Color(255,0,0,255)));

			for (Map.Entry<String, Transfer<Integer,Integer>> e: carvers.entrySet()) {
				carvers2.put(e.getKey(), seq().then(e.getValue()).then(rest));
			}
		}
		
		
		for (String carver:carvers2.keySet()) {
			ARComponent.PERFORMANCE_REPORTING = true;
			final Transfer<Integer, Color> transfer = 
					seq().then(new General.Spread<>(new General.Spread.UnitRectangle<Integer>(spread), new Numbers.Count<Integer>()))
					.then(carvers2.get(carver));
					
			JFrame frame = new JFrame(String.format("Seam Carving -- Removed %d seams (%s method)", seams, carver));
			frame.setLayout(new BorderLayout());
			frame.setSize(width,height);
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.add(new TransferDisplay(aggregates, transfer), BorderLayout.CENTER);
			frame.setVisible(true);
			frame.revalidate();
			frame.validate();
		}
	}
	
}
