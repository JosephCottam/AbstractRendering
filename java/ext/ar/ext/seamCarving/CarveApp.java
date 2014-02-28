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
import ar.rules.combinators.Seq;
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

		final String selected = Util.argKey(args, "-carvers", null);
		final int spread = Integer.parseInt(Util.argKey(args, "-spread", "0"));
		final int seams = Integer.parseInt(Util.argKey(args, "-seams", "400"));
		
		Renderer r = new ParallelRenderer();
		Aggregator<Object,Integer> aggregator = new Numbers.Count<Object>();
		Selector<Point2D> selector = TouchesPixel.make(dataset);

		int width = 1200;
		int height = 800;
		AffineTransform vt = Util.zoomFit(dataset.bounds(), width, height);
		Aggregates<Integer> aggregates = r.aggregate(dataset, selector, aggregator, vt, width, height);

		
		Map<String, Transfer<Integer,Integer>> allCarvers = new HashMap<String, Transfer<Integer,Integer>>() {{
				put("incremental", new SeamCarving.CarveIncremental<>(new SeamCarving.LeftValue<Integer>(), Direction.V, 0,seams));
				put("sweep", new SeamCarving.CarveSweep<>(new SeamCarving.LeftValue<Integer>(), Direction.V, 0, seams));
				put("twosweep", new SeamCarving.CarveTwoSweeps<>(new SeamCarving.LeftValue<Integer>(), Direction.V, 0, seams));
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
		
		for (String carver:carvers.keySet()) {
			ARComponent.PERFORMANCE_REPORTING = true;
			final Transfer<Integer, Color> transfer = 
					Seq.start(new General.Spread<>(new General.Spread.UnitSquare<Integer>(spread), new Numbers.Count<Integer>()))
					.then(carvers.get(carver))
					.then(new General.ValuerTransfer<>(new MathValuers.Log<Integer>(10d), 0d))
					.then(new General.Replace<>(Double.NEGATIVE_INFINITY, 0d, 0d))
					.then(new Numbers.Interpolate<Double>(new Color(255,0,0,25), new Color(255,0,0,255)));
					
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
