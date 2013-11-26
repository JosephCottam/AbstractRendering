package ar.selectors;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyph;
import ar.Glyphset;
import ar.Selector;

/**Collection of selectors that modify bins that a shape touches.
 * **/
public abstract class TouchesPixel {
	/**DESTRUCTIVELY updates the target at x/y with the value passed and the target operation.**/
	protected static final <A,I> void update(Aggregates<A> target, I v, int x, int y, Aggregator<I,A> op) {
		A existing = target.get(x,y);
		A update = op.combine(existing,v);
		target.set(x, y, update);
	}
		
	public static final class Points implements Selector<Point2D> {
		public <I,A> Aggregates<A> processSubset(
				Glyphset<? extends Point2D, ? extends I> subset,
				AffineTransform view, 
				Aggregates<A> target, 
				Aggregator<I, A> op) {
			
			Point2D scratch = new Point2D.Double();
			for (Glyph<? extends Point2D, ? extends I> g: subset) {
				Point2D p = g.shape();	//A point has no bounding box...so life is easy
				view.transform(p, scratch);
				int x = (int) scratch.getX();
				int y = (int) scratch.getY();
				I v = g.info();
				
				TouchesPixel.update(target, v, x, y, op);
			}

			return target;		
		}

		@Override
		public boolean hitsBin(Glyph<? extends Point2D, ?> glyph, AffineTransform view, int x, int y) {
			Point2D p = view.transform(glyph.shape(), null);
			int px = (int) p.getX();
			int py = (int) p.getY();
			return px == x && py == y;
		}
	}

	public static final class Lines implements Selector<Line2D> {
		public <I,A> Aggregates<A> processSubset(
				Glyphset<? extends Line2D, ? extends I> subset,
				AffineTransform view, 
				Aggregates<A> 
				target, Aggregator<I, A> op) {

			for (Glyph<? extends Line2D, ? extends I> g: subset) {
				bressenham(target, op, g.shape(), g.info());
			}

			return target;
		}	
		

		@Override
		public boolean hitsBin(Glyph<? extends Line2D, ?> glyph, AffineTransform view, int x, int y) {
			Shape s = view.createTransformedShape(glyph.shape());
			return s.intersects(x, y, 1, 1);
		}
		
		//based on 'optimized' version at http://en.wikipedia.org/wiki/Bresenham's_line_algorithm
		private static <I,A> void bressenham(Aggregates<A> canvas, Aggregator<I,A> aggregator, Line2D line, I val) {
			int x0 = (int) line.getX1(); //TODO: Not sure if the rounding should happen here or later....
			int y0 = (int) line.getY1();
			int x1 = (int) line.getX2();
			int y1 = (int) line.getY2();
			boolean steep = Math.abs(y1 - y0) > Math.abs(x1 - x0);
			  if (steep) {
				  int temp = x0;
				  x0 = y0;
				  y0 = temp;

				  temp = x1;
				  x1 = y1;
				  y1 = temp;
			  }
			  
			  if (x0 > x1) {
				  int temp = x0;
				  x0 = x1;
				  x1 = temp;
				  
				  temp = y0;
				  y0 = y1;
				  y1 = temp;
			  }

			  double deltax = x1 - x0;
			  double deltay = Math.abs(y1 - y0);
			  double error = 0;
			  int ystep = y0 < y1 ? 1 : -1;
			  
			  int y = y0;
			  for (int x=x0; x> x1; x++) { //line only includes first endpoint
			    if (steep) {
			      if (x >= canvas.highY()) {break;}
			      if (y >= canvas.highX()) {break;}
			      TouchesPixel.update(canvas, val, x,y, aggregator);
			    } else {
			      if (x >= canvas.highX()) {break;}
			      if (y >= canvas.highY()) {break;}
			      TouchesPixel.update(canvas, val, x,y, aggregator);
			    }

			    error = error - deltay;
			    if (error < 0) {
			      y = y + ystep;
			      error = error + deltax;
			    }
			  }
		}
	}
	
	public static final class Rectangles implements Selector<Rectangle2D> {
		public <I,A> Aggregates<A> processSubset(
				Glyphset<? extends Rectangle2D, ? extends I> subset,
				AffineTransform view, 
				Aggregates<A> target, 
				Aggregator<I, A> op) {

			Point2D lowP = new Point2D.Double();
			Point2D highP = new Point2D.Double();

			for (Glyph<? extends Rectangle2D, ? extends I> g: subset) {
				Rectangle2D b = g.shape();	//A rectangle is its own bounding box!
				lowP.setLocation(b.getMinX(), b.getMinY());
				highP.setLocation(b.getMaxX(), b.getMaxY());

				view.transform(lowP, lowP);
				view.transform(highP, highP);

				int lowx = (int) Math.floor(lowP.getX());
				int lowy = (int) Math.floor(lowP.getY());
				int highx = (int) Math.ceil(highP.getX());
				int highy = (int) Math.ceil(highP.getY());

				I v = g.info();

				for (int x=Math.max(0,lowx); x<highx; x++){//TODO: What is this Max(0, lowX) doing here?
					for (int y=Math.max(0, lowy); y<highy; y++) {//TODO: What is this Max(0, lowY) doing here?
						update(target, v, x,y, op);
					}
				}
			}
			return target;
		}
		
		@Override
		public boolean hitsBin(Glyph<? extends Rectangle2D, ?> glyph, AffineTransform view, int x, int y) {
			Shape s = view.createTransformedShape(glyph.shape());
			return s.intersects(x, y, 1, 1);
		}
	}

	public static final class Shapes implements Selector<Shape> {
		public <I,A> Aggregates<A> processSubset(
				Glyphset<? extends Shape, ? extends I> subset,
				AffineTransform view, 
				Aggregates<A> target, 
				Aggregator<I, A> op) {

			Point2D lowP = new Point2D.Double();
			Point2D highP = new Point2D.Double();
			Point2D testP = new Point2D.Double();

			for (Glyph<? extends Shape, ? extends I> g: subset) {
				Rectangle2D b = g.shape().getBounds2D();
				lowP.setLocation(b.getMinX(), b.getMinY());
				highP.setLocation(b.getMaxX(), b.getMaxY());

				view.transform(lowP, lowP);
				view.transform(highP, highP);

				int lowx = (int) Math.floor(lowP.getX());
				int lowy = (int) Math.floor(lowP.getY());
				int highx = (int) Math.ceil(highP.getX());
				int highy = (int) Math.ceil(highP.getY());

				I v = g.info();
				for (int x=Math.max(0,lowx); x<highx; x++){  //TODO: What is this Max(0, lowX) doing here?
					for (int y=Math.max(0, lowy); y<highy; y++) { //TODO: What is this Max(0, lowY) doing here?
						testP.setLocation(x, y);
						if (g.shape().contains(testP)) {
							update(target, v, x,y, op);
						}
					}
				}
			}

			return target;
		}
		
		@Override
		public boolean hitsBin(Glyph<? extends Shape, ?> glyph, AffineTransform view, int x, int y) {
			Shape s = view.createTransformedShape(glyph.shape());
			return s.intersects(x, y, 1, 1);
		}
	}
	

	@SuppressWarnings("unchecked")
	public static <G> Selector<G> make(Glyphset<? extends G, ?> glyphs) {
		for (Glyph<? extends G, ?> g: glyphs) {
			Object o = g.shape();
			if (o != null) {return (Selector<G>) TouchesPixel.make(o.getClass());}
		}
		if (glyphs.size() == 0){ throw new IllegalArgumentException("Passed empty glyphset, cannot discern geometry type.");}
		throw new IllegalArgumentException("Passed glyphset with no non-null shapes.  Cannot discern geometry type.");
	}

	@SuppressWarnings("unchecked")
	/**Construct a selector based on the expected geometry type.**/
	public static <G> Selector<G> make(Class<? extends G> geometryType) {
		if (Point2D.class.isAssignableFrom(geometryType)) {
			return (Selector<G>) new Points();			
		} else if (Rectangle2D.class.isAssignableFrom(geometryType)) {
			return (Selector<G>) new Rectangles();
		} else if (Line2D.class.isAssignableFrom(geometryType)) {
			return (Selector<G>) new Lines();
		} else if (Shape.class.isAssignableFrom(geometryType)){
			return (Selector<G>) new Shapes();
		} else {
			throw new IllegalArgumentException("Could not construct aggregator for geometry type: " + geometryType.getName());
		}

	}
}