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
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

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
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.renderers.ParallelRenderer;
import ar.rules.General;
import ar.rules.ISOContours;
import ar.rules.Numbers;
import ar.selectors.TouchesPixel;
import ar.util.HasViewTransform;
import ar.util.Util;

public class ContourApp {
	public static void main(String[] args) throws Exception {
		//------------------------ Setup Operations -------------------
		//Get a set of geometry and associated values (aka, a glyphset) 
		Glyphset<Point2D, Character> dataset = 
				GlyphsetUtils.memMap(
						//"US Census tracts", "../data/2010Census_RaceTract.hbin",
						"US Census Synthetic People", "../data/2010Census_RacePersonPoints.hbin", 
						new Indexed.ToPoint(true, 0, 1),
						new Indexed.ToValue<Indexed,Character>(2),
						1, null);

		
		
		Renderer r = new ParallelRenderer();
		Aggregator<Object,Integer> aggregator = new Numbers.Count<Object>();
		Selector<Point2D> selector = TouchesPixel.make(dataset);

		
		final int width = 1280;
		final int height = 800;
		AffineTransform vt = Util.zoomFit(dataset.bounds(), width, height);
		Aggregates<Integer> counts = r.aggregate(dataset, selector, aggregator, vt, width, height);
		
//		final ISOContours.Single.Specialized<Integer> contour = new ISOContours.Single.Specialized<>(0, 5, counts);
//		final ISOContours.NContours.Specialized<Integer> contour = new ISOContours.NContours.Specialized<>(0, 5, counts);
//		final ISOContours.SpacedContours.Specialized<Integer> contour = new ISOContours.SpacedContours.Specialized<>(0, 100, null, counts);
//		r.transfer(counts, contour);
		
		Aggregates<Double> magnitudes = r.transfer(counts, new General.ValuerTransfer<>(new MathValuers.Log<>(10, false, true), aggregator.identity().doubleValue()));
		Util.Stats<Double> stats = Util.stats(magnitudes, false);
		System.out.println(stats);

		
		//final ISOContours.Single.Specialized<Double> contour = new ISOContours.Single.Specialized<>(0d, 2d, magnitudes);
		final ISOContours.NContours.Specialized<Double> contour = new ISOContours.NContours.Specialized<>(0d, 3, magnitudes);
		//final ISOContours.SpacedContours.Specialized<Double> contour = new ISOContours.SpacedContours.Specialized<>(0d, .5, null, magnitudes);
		r.transfer(magnitudes, contour);
		
		BufferedImage img = new BufferedImage(width,height, BufferedImage.TYPE_INT_ARGB);
		renderTo(contour.contours(), (Graphics2D) img.getGraphics(), width, height);
		Util.writeImage(img, new File("Contours.png"));
		
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		f.setLayout(new BorderLayout());
		f.add(new Display(contour), BorderLayout.CENTER);
		f.setSize(width+50,height+50);
		f.validate();
		f.setVisible(true);
	}
	
	public static <S extends Shape,N extends Number> void renderTo(List<? extends Glyph<S,N>> contours, Graphics2D g2, int width, int height) {
		//g2.setColor(new Color(240,240,255));
		g2.setColor(Color.white);
		g2.fill(new Rectangle2D.Double(0,0,width, height));
		
		AffineTransform saved = g2.getTransform();
		g2.setTransform(new AffineTransform());
		
		Number min = contours.get(0).info();
		Number max = contours.get(contours.size()-1).info();
		
		for (Glyph<S, N> glyph: contours) {
			Color c = Util.interpolate(new Color(254, 229, 217), new Color(165, 15, 21), min.doubleValue(), max.doubleValue(), glyph.info().doubleValue());
			Shape s = glyph.shape();
//			g2.setColor(c);
//			g2.draw(s);
			
			g2.setColor(c);
			g2.fill(s);
		}
		g2.setTransform(saved);
	}
	
	public static class Display extends JPanel implements HasViewTransform {
		final ISOContours<? extends Number> contour;
		AffineTransform transform = new AffineTransform();
		ZoomPanHandler handler = new ZoomPanHandler();

		public Display(ISOContours<? extends Number> contour) {
			this.contour = contour;
			this.addMouseListener(handler);
			this.addMouseMotionListener(handler);
		}

		public void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			renderTo(contour.contours(), g2, this.getWidth(), this.getHeight());
		}
		
		public static Color desaturate(Color c, int factor) {
			return new Color(
					Math.max(0, Math.min(255, c.getRed()+factor)),
					Math.max(0, Math.min(255, c.getGreen()+factor)),
					Math.max(0, Math.min(255, c.getBlue()+factor))
					);
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
