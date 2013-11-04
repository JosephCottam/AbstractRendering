package ar.benchmarks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;
import javax.swing.JPanel;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyph;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.app.util.GlyphsetUtils;
import ar.app.util.ZoomPanHandler;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.renderers.ParallelRenderer;
import ar.rules.ISOContours;
import ar.rules.Numbers;
import ar.rules.TransferMath;
import ar.selectors.TouchesPixel;
import ar.util.HasViewTransform;
import ar.util.Util;

public class ContourApp {
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

		
		final int width = 800;
		final int height = 800;
		AffineTransform vt = Util.zoomFit(dataset.bounds(), width, height);
		Aggregates<Integer> counts = r.aggregate(dataset, selector, aggregator, vt, width, height);
		Aggregates<Double> magnitudes = r.transfer(counts, new TransferMath.LogDouble(10, aggregator.identity()));

		//final ISOContours.Single.Specialized<Double> contour = new ISOContours.Single.Specialized<>(0, 20, aggregates);
		final ISOContours.NContours.Specialized<Double> contour = new ISOContours.NContours.Specialized<>(0d, 5, magnitudes);
		//final ISOContours.SpacedContours.Specialized<Double> contour = new ISOContours.SpacedContours.Specialized<>(0, 5, null, aggregates);


		r.transfer(magnitudes, contour);
		
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		f.setLayout(new BorderLayout());
		f.add(new Display(contour), BorderLayout.CENTER);
		f.setSize(width,height);
		f.validate();
		f.setVisible(true);
	}
	
	public static class Display extends JPanel implements HasViewTransform {
		final ISOContours<Double> contour;
		AffineTransform transform = new AffineTransform();
		ZoomPanHandler handler = new ZoomPanHandler();

		public Display(ISOContours<Double> contour) {
			this.contour = contour;
			this.addMouseListener(handler);
			this.addMouseMotionListener(handler);
		}

		public void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setColor(new Color(240,240,255));
			g2.fill(new Rectangle2D.Double(0,0,this.getWidth(), this.getHeight()));
			
			AffineTransform saved = g2.getTransform();
			g2.setTransform(transform);
			
			Number min = contour.contours().get(0).info();
			Number max = contour.contours().get(contour.contours().size()-1).info();
			
			for (Glyph<? extends Shape, ? extends Number> glyph: contour.contours()) {
				Color c = Util.interpolate(Color.black, Color.red, min.doubleValue(), max.doubleValue(), glyph.info().doubleValue());
				Shape s = glyph.shape();
				g2.setColor(c);
				g2.draw(s);
			}
			g2.setTransform(saved);
		}

		@Override
		public AffineTransform viewTransform() {return transform;}

		@Override
		public void viewTransform(AffineTransform vt)
				throws NoninvertibleTransformException {
			this.transform  =vt;
			this.repaint();
		}
	}
	
}
