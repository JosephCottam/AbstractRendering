package ar.glyphsets;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import ar.Glyph;
import ar.Glyphset;
import ar.util.axis.DescriptorPair;

/**Wrap a glyphset, all glyphs are returned transformed via the supplied affine transform.
 * 
 * Note: This class is only safe to use with glyphs containing Point2D.Double/Float, Rectangle2D.Double/Float, Line2D.Double/Float, Point, Rectangle and Shape.  
 * Other glyph types will result in an exception. (This always avoids heap pollution.)
 * **/
public class TransformWrapper<G,I> implements Glyphset<G,I> {
	protected final Glyphset<G,I> base;
	protected AffineTransform transform;

	public TransformWrapper(Glyphset<G,I> base, AffineTransform transform) {
		this.base = base;
		this.transform = transform;
	}

	@Override
	public Iterator<Glyph<G, I>> iterator() {return new TransformIterator<>(base.iterator(), transform);}

	@Override
	public boolean isEmpty() {return base.isEmpty();}

	@Override
	public Rectangle2D bounds() {return transform.createTransformedShape(base.bounds()).getBounds2D();}

	@Override
	/** Approximate size!  Returns 0 if empty, otherwise returns the base size.*/
	public long size() {return base.size();}


	/**Approximate!  Returns the descriptor for the base...**/
	@Override public DescriptorPair<?,?> axisDescriptors() {return base.axisDescriptors();}
	@Override public void axisDescriptors(DescriptorPair<?,?> descriptor) {base.axisDescriptors(descriptor);}
	
	@Override
	public List<Glyphset<G, I>> segment(int count) throws IllegalArgumentException {
		return base.segment(count).stream()
				.map((s) -> new TransformWrapper<>(s, transform))
				.collect(Collectors.toList());
	}
	
	public Glyphset<G,I> base() {return base;}
		
	public static final class TransformIterator<G,I> implements Iterator<Glyph<G,I>> {
		private final Iterator<Glyph<G,I>> base;
		private final AffineTransform transform;
		
		public TransformIterator(Iterator<Glyph<G,I>> base, AffineTransform transform) {
			super();
			this.base = base;
			this.transform=transform;
		}

		@Override
		public boolean hasNext() {return base.hasNext();}
		
		@SuppressWarnings("unchecked")
		@Override 
		public Glyph<G, I> next() {
			Glyph<G,I> g = base.next();
			G shape = g.shape();
			//Points
			if (shape.getClass() == Point2D.Double.class) {
				shape = (G) transform.transform((Point2D) shape, new Point2D.Double());
			} else if (shape.getClass() == Point2D.Float.class) {
				shape = (G) transform.transform((Point2D) shape, new Point2D.Float());
			} else if (shape.getClass() == Point.class) {
				shape = (G) transform.transform((Point2D) shape, new Point());
			
			//Rectangles
			} else if (shape.getClass() == Rectangle2D.Double.class) {
				Rectangle2D r = transform.createTransformedShape((Shape) shape).getBounds2D();
				shape = (G) new Rectangle2D.Double(r.getX(),r.getY(), r.getWidth(), r.getHeight());
			} else if (shape.getClass() == Rectangle2D.Float.class) {
				Rectangle2D r = transform.createTransformedShape((Shape) shape).getBounds2D();
				shape = (G) new Rectangle2D.Float((float) r.getX(), (float) r.getY(), (float) r.getWidth(), (float) r.getHeight());
			} else if (shape.getClass() == Rectangle.class) {
				shape = (G) transform.createTransformedShape((Rectangle2D) shape).getBounds();

			//Lines
			} else if (shape.getClass() == Line2D.Double.class) {
				Point2D a = transform.transform(((Line2D) shape).getP1(), new Point2D.Double());
				Point2D b = transform.transform(((Line2D) shape).getP2(), new Point2D.Double());
				shape = (G) new Line2D.Double(a,b);
			} else if (shape.getClass() == Line2D.Float.class) {
				Point2D a = transform.transform(((Line2D) shape).getP1(), new Point2D.Float());
				Point2D b = transform.transform(((Line2D) shape).getP2(), new Point2D.Float());
				shape = (G) new Line2D.Float(a,b);
				
			//Generic Shape	
			} else if (shape.getClass() == Shape.class) {
				shape  = (G) transform.createTransformedShape((Rectangle2D) shape);
			} else {
				throw new IllegalArgumentException("Could not safely transform instance of " + shape.getClass());
			}
							
			return new SimpleGlyph<G, I>(shape, g.info());
		}
		
		@Override public void remove() {throw new UnsupportedOperationException();}
	}
}
