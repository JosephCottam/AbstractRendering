package ar.ext.seamCarving;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import javax.swing.JFrame;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.Transfer;
import ar.app.display.TransferDisplay;
import ar.app.util.GlyphsetUtils;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.renderers.ParallelRenderer;
import ar.rules.General;
import ar.rules.SeamCarving;
import ar.rules.Numbers;
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

		
		
		Renderer r = new ParallelRenderer();
		Aggregator<Object,Integer> aggregator = new Numbers.Count<Object>();
		Selector<Point2D> selector = TouchesPixel.make(dataset);

		int width = 800;
		int height = 375;
		int seams = 400;
		AffineTransform vt = Util.zoomFit(dataset.bounds(), width, height);
		Aggregates<Integer> aggregates = r.aggregate(dataset, selector, aggregator, vt, width, height);

		
		
		final Transfer<Integer, Color> transfer = 
				Seq.start(new General.Spread<>(new General.Spread.UnitSquare<Integer>(0), new Numbers.Count<Integer>()))
				//.then(new SeamCarving.OptimalCarve<>(new SeamCarving.DeltaInteger(), Direction.V, 0,seams))
				.then(new SeamCarving.LocalCarve<>(new SeamCarving.DeltaInteger(), Direction.V, 0, seams))
				.then(new General.ValuerTransfer<>(new MathValuers.Log<Integer>(10d), 0d))
				.then(new General.Replace<>(Double.NEGATIVE_INFINITY, 0d, 0d))
				.then(new Numbers.Interpolate<Double>(new Color(255,0,0,25), new Color(255,0,0,255)));
				
		JFrame frame = new JFrame("Seam Carving -- Removed " + seams + " seams");
		frame.setLayout(new BorderLayout());
		frame.setSize(width,height);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.add(new TransferDisplay(aggregates, transfer), BorderLayout.CENTER);
		frame.setVisible(true);
		frame.revalidate();
		frame.validate();
		
		
	}
	
}
