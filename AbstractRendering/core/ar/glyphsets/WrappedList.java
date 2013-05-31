package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import ar.Glyphset;
import ar.Glyphset.Glyph;
import ar.util.SimpleGlyph;
import ar.util.Util;


/**Wrap an existing list of values as glyphs.**/
public class WrappedList<I,V> implements Glyphset.RandomAccess<V>, Iterable<Glyph<V>> {
	private List<I> values;
	private ImplicitGlyph<I,V> transformer;
	
	public WrappedList(List<I> values, ImplicitGlyph<I,V> transformer) {
		this.values = values;
		this.transformer = transformer;
	}
	@Override
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
}
