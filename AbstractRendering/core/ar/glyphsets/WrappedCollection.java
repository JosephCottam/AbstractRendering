package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import ar.Glyphset;
import ar.Glyphset.Glyph;
import ar.util.ImplicitGeometry;
import ar.util.SimpleGlyph;
import ar.util.Util;


/**Wrap an existing list of values as glyphs.**/
public class WrappedCollection<I,V> implements Glyphset<V>, Iterable<Glyph<V>> {
	protected Collection<I> values;
	protected ImplicitGeometry.Shaper<I> shaper;
	protected ImplicitGeometry.Valuer<I,V> valuer;
	
	public WrappedCollection(Collection<I> values, 
							ImplicitGeometry.Shaper<I> shaper, 
							ImplicitGeometry.Valuer<I,V> valuer) {
		this.values = values;
		this.shaper = shaper;
		this.valuer = valuer;
	}
	
	public Collection<ar.Glyphset.Glyph<V>> intersects(Rectangle2D r) {
		ArrayList<ar.Glyphset.Glyph<V>> hits = new ArrayList<ar.Glyphset.Glyph<V>>();
		for (Glyph<V> g: this) {
			if (g.shape().getBounds2D().intersects(r)) {hits.add(g);}
		}
		return hits;
	}

	public boolean isEmpty() {return values == null || values.isEmpty();}
	public long size() {return values==null ? 0 : values.size();}
	public Rectangle2D bounds() {return Util.bounds(this);}
	public Iterator<ar.Glyphset.Glyph<V>> iterator() {
		return new Iterator<ar.Glyphset.Glyph<V>>() {
			Iterator<I> basis = values.iterator();
			public boolean hasNext() {return basis.hasNext();}
			public ar.Glyphset.Glyph<V> next() {
				I next = basis.next();
				return next == null ? null : new SimpleGlyph<V>(shaper.shape(next), valuer.value(next));
			}
			public void remove() {throw new UnsupportedOperationException();}
		};
	}
	
	public void add(ar.Glyphset.Glyph<V> g) {
		throw new UnsupportedOperationException("Cannot add directly to wrapped list.  Must add to backing collection.");
	}

	public static class List<I,V> extends WrappedCollection<I,V> implements Glyphset.RandomAccess<V> {
		protected final java.util.List<I> values;
		
		public List(java.util.List<I> values,
				ImplicitGeometry.Shaper<I> shaper, 
				ImplicitGeometry.Valuer<I,V> valuer) {
			super(values, shaper, valuer);
			this.values=values;
		}
		
		public Iterator<ar.Glyphset.Glyph<V>> iterator() {
			return new GlyphsetIterator<V>(this);
		}
		
		public ar.Glyphset.Glyph<V> get(long l) {
			if (l > Integer.MAX_VALUE) {throw new IllegalArgumentException("Can only index through ints in wrapped list.");}
			if (l < 0) {throw new IllegalArgumentException("Negative index not allowed.");}
			I value = values.get((int) l);
			return new SimpleGlyph<V>(shaper.shape(value), valuer.value(value));
		}
	}
}
