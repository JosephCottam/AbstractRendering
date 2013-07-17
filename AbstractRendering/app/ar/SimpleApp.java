package ar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import ar.app.components.ARPanel;
import ar.glyphsets.WrappedCollection;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.renderers.ParallelGlyphs;
import ar.rules.Numbers;
import ar.util.DelimitedReader;
import ar.util.Util;

public class SimpleApp {
	public static void main(String[] args) throws Exception {
		//------------------------ Setup Operations -------------------
		//The setup operations do not reflect Abstract-Rendering specific work.

		JFrame frame = new JFrame();
		frame.setLayout(new BorderLayout());
		frame.setSize(500,500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		ArrayList<Indexed> items = new ArrayList<>();
		DelimitedReader reader = new DelimitedReader(new File("../data/circlepoints.csv"), 1, ",");
		System.out.print("Loading example data...");
		while (reader.hasNext()) {
			String[] parts = reader.next();
			if (parts == null) {continue;}
			double[] vals = new double[parts.length];
			for (int i=0; i<parts.length; i++) {vals[i] = Double.parseDouble(parts[i]);}
			items.add(new Indexed.ArrayWrapper(vals));
		}
		System.out.println(items.size() + " items");
		//----------------------- End of setup operations -------------
		
		
		//Load the existing dataset into an abstract rendering glyphset.
		//We use the Wrapper glyphset because no copying is required.
		//However, in exchange we need to supply the "shaper" and the "valuer" instances.
		//These instances are defined above and depend on what exactly is
		//held in the collection being wrapped.
		Glyphset<Double> dataset = WrappedCollection.wrap(items, new Indexed.ToRect(.05,2,3), new Indexed.ToValue<Indexed,Double>(4), Double.class);
		


		//The Renderer class defines a strategy for iterating through the abstract
		//canvas space and through the glyphset.  The two big options are to divide
		//the canvas into regions or divide the glyphset into subsets.
		//The ParalleGlyph renderer is the highest performance BUT requires
		// the glyphset to be partitionable and requires an "Aggregate Reduction" function
		// to combine results from different glyph subsets.
		Renderer r = new ParallelGlyphs();

		//The Aggregator is used to combine values from multiple glyphs into a value for a single
		//aggregate bin.  The 'Count' aggregator simply counts how many glyphs fell into the bin.
		Aggregator<Object,Integer> aggregator = new Numbers.Count();
		

		//The transfer function is used to convert one set of aggregates into another.
		//In the end, an image is a set of aggreagates where the value in each bin is a color.
		Transfer<Number, Color> transfer = new  Numbers.Interpolate(new Color(255,0,0,25), new Color(255,0,0,255));
		
		
		//A panel is constructed from a specific configuration.
		final ARPanel panel = new ARPanel(aggregator, transfer, dataset, r);
		

		//----------- A few lines to display the panel in a window --------------
		frame.add(panel, BorderLayout.CENTER);
		frame.setVisible(true);
		frame.revalidate();
		frame.validate();
		SwingUtilities.invokeAndWait(new Runnable() {public void run() {panel.zoomFit();}});
		//-------------------------------------------------
		
		//If a panel is not desired, then the renderer can be invoked directly
		//(After all is said and done, the panel just coordinates when to invoke the renderer anyway).
		//The next few (commented) lines walk through directly rendering
		int width = 800;
		int height = 800;
		AffineTransform vt = Util.zoomFit(dataset.bounds(), width, height);
		vt.invert();
		Aggregates<Integer> aggregates = r.reduce(dataset, aggregator, vt, width, height);
		Aggregates<Color> colors = r.transfer(aggregates, transfer);
		Util.asImage(colors, width, height, Color.white);



	}
}
