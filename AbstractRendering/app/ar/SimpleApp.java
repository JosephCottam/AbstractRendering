package ar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import ar.app.components.ARDisplay;
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
		//Get a dataset loaded into a data structure.  This is not technically
		//Abstract Rendering, but it is necessary to get anything other than 
		//a white box out of this demo
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
		Aggregator<Object,Integer> aggregator = new Numbers.Count<Object>();
		

		//The transfer function is used to convert one set of aggregates into another.
		//In the end, an image is a set of aggreagates where the value in each bin is a color.
		Transfer<Number, Color> transfer = new  Numbers.Interpolate(new Color(255,0,0,25), new Color(255,0,0,255));
		
		//Drive the rendering "by-hand"  (ie, not using any of the swing tools)
		//We must first define a display surface's size, shape and zoom characteristics
		//At the end, a simple buffered image is produced.
		int width = 800;
		int height = 800;
		AffineTransform vt = Util.zoomFit(dataset.bounds(), width, height);
		vt.invert();
		Aggregates<Integer> aggregates = r.aggregate(dataset, aggregator, vt, width, height);
		Transfer.Specialized<Number,Color> specializedTransfer = transfer.specialize(aggregates);
		Aggregates<Color> colors = r.transfer(aggregates, specializedTransfer);
		
		//Make a final image (if the aggregates are colors)
		@SuppressWarnings("unused")  //Unused because its just ademo of how to do it
		BufferedImage image = Util.asImage(colors, width, height, Color.white);
		
		//A simple display panel can be found in ARDisplay.
		//It takes aggregates and a transfer function to make colors.
		//This is largely a static display though, since all display decisions have been made.
		//The ARDisplay includes a renderer (or it can be passed in), but it only
		//performs the "transfer" step.  As such, we reuse the aggregates from above.
		//Only the last line of this section is Abstract rendering specific.  The rest
		//is swing frame boilerplate.
		JFrame frame = new JFrame("ARDisplay");
		frame.setLayout(new BorderLayout());
		frame.setSize(width,height);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.add(new ARDisplay(aggregates, transfer), BorderLayout.CENTER);
		frame.setVisible(true);
		frame.revalidate();
		frame.validate();
		
		//ARPanel is more fully-featured than ARDisplay. It will run the whole render loop
		//and includes zoom/pan listeners.  It is harder to use though, because it takes control
		//over the process in a more robust but somewhat opaque way.
		//Since ARPanel drives the whole rendering process, it takes the dataset, rendering strategy
		//and related definitions in as parameters
		JFrame frame2 = new JFrame("ARPanel");
		frame2.setLayout(new BorderLayout());
		frame2.setSize(width,height);
		frame2.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		final ARPanel panel = new ARPanel(aggregator, transfer, dataset, r);
		frame2.add(panel, BorderLayout.CENTER);
		frame2.setVisible(true);
		frame2.revalidate();
		frame2.validate();
		SwingUtilities.invokeAndWait(new Runnable() {public void run() {panel.zoomFit();}});
	}
}
