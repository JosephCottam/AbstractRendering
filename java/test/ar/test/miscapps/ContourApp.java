package ar.test.miscapps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

import ar.Aggregates;
import ar.Aggregator;
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
		AffineTransform vt = Util.zoomFit(dataset.bounds(), width, height);
		Aggregates<Integer> aggregates = r.aggregate(dataset, selector, aggregator, vt, width, height);

		final ISOContours.Specialized<Integer> contour = new ISOContours.Specialized<Integer>(20,0, aggregates);
		r.transfer(aggregates, contour);
		
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel p = new JPanel() {
			public void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setColor(new Color(240,240,255));
				g2.fill(new Rectangle2D.Double(0,0,width,height));
				g2.setColor(Color.blue);
				GeneralPath p = contour.contours().shape();
				g2.fill(p);
				g2.setColor(Color.black);
				g2.draw(p);
			}
		};
		f.setLayout(new BorderLayout());
		f.add(p, BorderLayout.CENTER);
		f.setSize(width,height);
		f.validate();
		f.setVisible(true);
	}
}
