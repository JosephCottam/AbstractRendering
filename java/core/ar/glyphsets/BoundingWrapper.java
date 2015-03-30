package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import ar.Glyph;
import ar.Glyphset;
import ar.util.Util;
import ar.util.axis.DescriptorPair;

/**Wrap a glyphset, only return values that are contained within the given bounding box.**/
public class BoundingWrapper<G,I> implements Glyphset<G,I> {

	protected final Glyphset<G,I> base;
	protected final Rectangle2D limitBound;
	protected final boolean lazy;
	protected Rectangle2D bounds;
	

	/**
	 * Return only values from base that are within bound.
	 * If lazy is set to false, the return from bounds() will be the maximum bound
	 *   of data within the bounds of the the limitbound. Otherwise, bounds()
	 *   is computed as the intersection of the limitbound and base.bounds(). 
	 */
	public BoundingWrapper(Glyphset<G,I> base, Rectangle2D limitBound, boolean lazy) {
		this.base = base;
		this.limitBound = limitBound;
		this.lazy = lazy;
	}

	public BoundingWrapper(Glyphset<G,I> base, Rectangle2D bound) {this(base, bound, true);}
	
	@Override
	public Iterator<Glyph<G, I>> iterator() {return new BoundedIterator<>(base.iterator(), limitBound);}

	@Override
	public boolean isEmpty() {return base.isEmpty() || !base.bounds().intersects(limitBound);}

	@Override
	public Rectangle2D bounds() {
		if (bounds != null) {return bounds;}
		Rectangle2D r;
		if (lazy) { 
			r = new Rectangle2D.Double();
			Rectangle2D.intersect(base.bounds(), limitBound, r);
		} else {
			r = Util.bounds(this); 
		}
		this.bounds = r;
		return bounds;
	}

	@Override
	/** Approximate size!  Returns 0 if empty, otherwise returns the base size.*/
	public long size() {return isEmpty() ? 0 : base.size();}


	/**Approximate!  Returns the descriptor for the base...**/
	@Override public DescriptorPair<?,?> axisDescriptors() {return base.axisDescriptors();}
	@Override public void axisDescriptors(DescriptorPair<?,?> descriptor) {base.axisDescriptors(descriptor);}

	
	@Override
	public Glyphset<G, I> segmentAt(int count, int segId) throws IllegalArgumentException {
		return new BoundingWrapper<>(base.segmentAt(count, segId), limitBound);
	}
	
	public Glyphset<G,I> base() {return base;}
	
	
	public static final class BoundedIterator<G,I> implements Iterator<Glyph<G,I>> {
		private final Iterator<Glyph<G,I>> base;
		private final Rectangle2D bound;
		private Glyph<G,I> next;
		
		public BoundedIterator(Iterator<Glyph<G,I>> base, Rectangle2D bound) {
			super();
			this.base = base;
			this.bound = bound;
		}

		@Override
		public boolean hasNext() {
			while (next == null && base.hasNext()) {
				Glyph<G,I> maybeNext = base.next();
				if (maybeNext == null) {continue;}
				G shape = maybeNext.shape();
				Rectangle2D b = Util.boundOne(shape);
				if (bound.intersects(b)) {next = maybeNext;}
			}
			
			return next != null;
		}

		@Override 
		public Glyph<G, I> next() {
			Glyph<G,I> g = next;
			next = null;
			return g;
		}
		
		@Override public void remove() {throw new UnsupportedOperationException();}
	}
}
