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
		/**Sets the value at a single point in the aggregates.**/
		public <I,A> Aggregates<A> processSubset(
				Iterable<? extends Glyph<? extends Point2D, ? extends I>> subset,
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
		/**Bressenham interpolation on a line.**/
		public <I,A> Aggregates<A> processSubset(
				Iterable<? extends Glyph<? extends Line2D, ? extends I>> subset,
				AffineTransform view, 
				Aggregates<A> 
				target, Aggregator<I, A> op) {

			Point2D p1 = new Point2D.Double();
			Point2D p2 = new Point2D.Double();
			for (Glyph<? extends Line2D, ? extends I> g: subset) {
				Line2D p = g.shape();	//A point has no bounding box...so life is easy
				view.transform(p.getP1(), p1);
				view.transform(p.getP2(), p2);

				bressenham(target, op, p1,p2, g.info());
			}

			return target;
		}	
		

		@Override
		public boolean hitsBin(Glyph<? extends Line2D, ?> glyph, AffineTransform view, int x, int y) {
			Shape s = view.createTransformedShape(glyph.shape());
			return s.intersects(x, y, 1, 1);
		}
		
		//based on 'optimized' version at http://en.wikipedia.org/wiki/Bresenham's_line_algorithm
		private static <I,A> void bressenham(Aggregates<A> canvas, Aggregator<I,A> aggregator, Point2D start, Point2D end, I val) {
			int x0 = (int) start.getX(); 
			int y0 = (int) start.getY();
			int x1 = (int) end.getX();
			int y1 = (int) end.getY();
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
			  for (int x=x0; x <= x1; x++) {
			    if (steep) {
			      TouchesPixel.update(canvas, val, x,y, aggregator);
			    } else {
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
		/**Iterates over the projection of a rectangle (no hit-tests required).
		 * 
		 * TODO: Can this be done with Point instead of Point2D?
		 * **/
		public <I,A> Aggregates<A> processSubset(
				Iterable<? extends Glyph<? extends Rectangle2D, ? extends I>> subset,
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

				for (int x=lowx; x<highx; x++){
					for (int y=lowy; y<highy; y++) {
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
		/**Iterates the bounds, with a hit-test to only set values inside of the shape.
		 * 		 
		 * TODO: Can this be done with Point instead of Point2D?
		 **/
		public <I,A> Aggregates<A> processSubset(
				Iterable<? extends Glyph<? extends Shape, ? extends I>> subset,
				AffineTransform view, 
				Aggregates<A> target, 
				Aggregator<I, A> op) {

			Point2D lowP = new Point2D.Double();
			Point2D highP = new Point2D.Double();
			Point2D testP = new Point2D.Double();

			for (Glyph<? extends Shape, ? extends I> g: subset) {
				Shape transformedShape = view.createTransformedShape(g.shape()); 	//Full new transformed shape to support hit-testing
				Rectangle2D b = transformedShape.getBounds();
				lowP.setLocation(b.getMinX(), b.getMinY());
				highP.setLocation(b.getMaxX(), b.getMaxY());

				int lowx = (int) Math.floor(lowP.getX());
				int lowy = (int) Math.floor(lowP.getY());
				int highx = (int) Math.ceil(highP.getX());
				int highy = (int) Math.ceil(highP.getY());

				I v = g.info();
				for (int x=lowx; x<highx; x++){ 
					for (int y=lowy; y<highy; y++) { 
						testP.setLocation(x, y);
						if (transformedShape.contains(testP)) {
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
	/**Construct a selector based on the geometry type of the first item in the glyphset.**/
	public static <G> Selector<G> make(Glyphset<? extends G, ?> glyphs) {
		for (Glyph<? extends G, ?> g: glyphs) {
			Object o = g.shape();
			if (o != null) {return (Selector<G>) TouchesPixel.make(o.getClass());}
		}
		if (glyphs.size() == 0){ throw new IllegalArgumentException("Passed empty glyphset, cannot discern geometry type.");}
		throw new IllegalArgumentException("Passed glyphset with all null shapes.  Cannot discern geometry type.");
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