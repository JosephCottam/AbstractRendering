package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.NoSuchElementException;

import ar.Glyph;
import ar.Glyphset;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.Axis.Descriptor;

/***Reduce a glyphset by only returning values that pass a given filter.**/
public final class FilterGlyphs<G, I> implements Glyphset<G,I> {
	final Glyphset<G,I> base;
	final Valuer<Glyph<G,I>, Boolean> predicate;
	
	public FilterGlyphs(Glyphset<G,I> base, Valuer<Glyph<G,I>, Boolean> predicate) {
		this.base = base;
		this.predicate = predicate;
	}
	
	@Override public Iterator<Glyph<G, I>> iterator() {return new FilterIterator<>(base.iterator(), predicate);}
	@Override public boolean isEmpty() {return base.isEmpty();}
	@Override public Rectangle2D bounds() {return base.bounds();}
	@Override public long size() {return base.size();}

	@Override
	public Glyphset<G, I> segmentAt(int count, int segId) throws IllegalArgumentException {
		return new FilterGlyphs<>(base.segmentAt(count, segId), predicate);
	}

	@Override public Descriptor axisDescriptors() {return base.axisDescriptors();}

	public static final class FilterIterator<G, I> implements Iterator<Glyph<G,I>> {
		private final Iterator<Glyph<G,I>> base;
		private Glyph<G,I> next;
		final Valuer<Glyph<G,I>, Boolean> predicate;

		public FilterIterator(Iterator<Glyph<G,I>> base, Valuer<Glyph<G,I>, Boolean> predicate) {
			this.base = base;
			this.predicate = predicate;
		}
		
		@Override 
		public boolean hasNext() {
			if (!base.hasNext()) {return false;}
			while (base.hasNext() && next == null) {
				next = base.next();
				if (predicate.value(next)) {break;}
				next = null;
			}
			return next != null;
		}
		
		@Override
		public Glyph<G,I> next() {
			if (next == null) {throw new NoSuchElementException();}
			Glyph<G,I> v = next;
			next = null;
			return v;
		}
		
		@Override public void remove() {throw new UnsupportedOperationException();}
	}
}