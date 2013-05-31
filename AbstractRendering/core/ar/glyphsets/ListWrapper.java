package ar.glyphsets;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import ar.Glyphset;
import ar.Glyphset.Glyph;
import ar.util.SimpleGlyph;
import ar.util.Util;


/**Wrap an existing list of values as glyphs.**/
public class ListWrapper<I,V> implements Glyphset.RandomAccess<V>, Iterable<Glyph<V>> {
	private List<I> values;
	private ImplicitGlyph<I,V> transformer;
	
	public ListWrapper(List<I> values, ImplicitGlyph<I,V> transformer) {
		this.values = values;
		this.transformer = transformer;
	}
	@Override
	public Collection<? extends ar.Glyphset.Glyph<V>> intersects(Rectangle2D r) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isEmpty() {return values == null || values.isEmpty();}
	public long size() {return values==null ? 0 : values.size();}
	public Rectangle2D bounds() {return Util.bounds(this);}
	public Iterator<ar.Glyphset.Glyph<V>> iterator() {return new GlyphsetIterator<V>(this);}

	public void add(ar.Glyphset.Glyph<V> g) {
		throw new UnsupportedOperationException("Cannot add directly to wrapped list.  Must add to backing collection.");
	}

	public ar.Glyphset.Glyph<V> get(long l) {
		if (l > Integer.MAX_VALUE) {throw new IllegalArgumentException("Can only index through ints in wrapped list.");}
		if (l < 0) {throw new IllegalArgumentException("Negative index not allowed.");}
		I value = values.get((int) l);
		return new SimpleGlyph<V>(transformer.shape(value), transformer.value(value));
	}
	
	public static interface ImplicitGlyph<I,V> {
		public Shape shape (I from);
		public V value(I from);
	}
}
