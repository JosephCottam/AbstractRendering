package ar.benchmarks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyph;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.glyphsets.WrappedCollection;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.renderers.ParallelRenderer;
import ar.rules.ISOContours;
import ar.rules.Numbers;
import ar.selectors.TouchesPixel;
import ar.util.DelimitedReader;
import ar.util.Util;

public class ContourApp {
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
		
		
		Glyphset<Rectangle2D, Double> dataset = WrappedCollection.wrap(items, new Indexed.ToRect(.05,2,3), new Indexed.ToValue<Indexed,Double>(4), Double.class);
		Renderer r = new ParallelRenderer();
		Aggregator<Object,Integer> aggregator = new Numbers.Count<Object>();
		Selector<Rectangle2D> selector = TouchesPixel.make(dataset);

		final int width = 800;
		final int height = 800;
		final int threshold = 20;
		AffineTransform vt = Util.zoomFit(dataset.bounds(), width, height);
		Aggregates<Integer> aggregates = r.aggregate(dataset, selector, aggregator, vt, width, height);

		final ISOContours.Single.Specialized<Integer> contour = new ISOContours.Single.Specialized<Integer>(0, threshold, aggregates);
		r.transfer(aggregates, contour);
		
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel p = new JPanel() {
			public void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setColor(new Color(240,240,255));
				g2.fill(new Rectangle2D.Double(0,0,width,height));
				
				for (Glyph<? extends Shape,?> glyph: contour.contours()) {
					Shape s = glyph.shape();
					g2.setColor(Color.blue);
					g2.fill(s);
					g2.setColor(Color.black);
					g2.draw(s);
				}
			}
		};
		f.setLayout(new BorderLayout());
		f.add(p, BorderLayout.CENTER);
		f.setSize(width,height);
		f.validate();
		f.setVisible(true);
	}
}
