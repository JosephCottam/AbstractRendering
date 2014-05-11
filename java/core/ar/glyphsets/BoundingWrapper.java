package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import ar.Glyph;
import ar.Glyphset;
import ar.util.Util;

/**Wrap a glyphset, only return values that fit within a bounding box.**/
public class BoundingWrapper<G,I> implements Glyphset<G,I> {

	protected final Glyphset<G,I> base;
	protected final Rectangle2D bound;
	
	public BoundingWrapper(Glyphset<G,I> base, Rectangle2D bound) {
		this.base = base;
		this.bound = bound;
	}
	
	@Override
	public Iterator<Glyph<G, I>> iterator() {return new BoundedIterator<>(base.iterator(), bound);}

	@Override
	public boolean isEmpty() {return base.isEmpty() || !base.bounds().intersects(bound);}

	@Override
	public Rectangle2D bounds() {
		Rectangle2D r = new Rectangle2D.Double();
		Rectangle2D.intersect(base.bounds(), bound, r);
		return r;
	}

	@Override
	/** Approximate size!  Returns 0 if empty, otherwise returns the base size.*/
	public long size() {return isEmpty() ? 0 : base.size();}

	@Override
	public long segments() {return base.segments();}

	@Override
	public Glyphset<G, I> segment(long bottom, long top) throws IllegalArgumentException {
		return new BoundingWrapper<>(base.segment(bottom, top), bound);
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
