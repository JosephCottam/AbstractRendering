package ar.benchmarks;

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
import ar.renderers.ParallelRenderer;
import ar.rules.SeamCarving;
import ar.rules.Numbers;
import ar.rules.combinators.NTimes;
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

		
		Transfer<Integer, Color> color = new  Numbers.Interpolate<>(new Color(255,0,0,25), new Color(255,0,0,255));
		final Transfer<Integer,Integer> carve = new SeamCarving.Carve<>(new SeamCarving.DeltaInteger(),true, 0);
		final Transfer<Integer, Color> transfer = new Seq<>(new NTimes<>(300, carve), color);
		
		int width = 800;
		int height = 375;
		AffineTransform vt = Util.zoomFit(dataset.bounds(), width, height);
		Aggregates<Integer> aggregates = r.aggregate(dataset, selector, aggregator, vt, width, height);
		
				
		JFrame frame = new JFrame("Simple Display");
		frame.setLayout(new BorderLayout());
		frame.setSize(width,height);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.add(new TransferDisplay(aggregates, transfer), BorderLayout.CENTER);
		frame.setVisible(true);
		frame.revalidate();
		frame.validate();
		
		
	}
	
}
