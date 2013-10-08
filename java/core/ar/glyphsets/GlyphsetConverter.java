package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import ar.Glyph;
import ar.Glyphset;
import ar.glyphsets.implicitgeometry.Valuer;

public class GlyphsetConverter<I,V> implements Glyphset<V> {
	protected final Valuer<I,V> converter;
	protected final Glyphset<I> base;
	
	public GlyphsetConverter(Valuer<I,V> converter, Glyphset<I> base) {
		this.converter = converter;
		this.base = base;
	}
	
	protected Glyph<V> wrap(Glyph<I> g) {
		return new SimpleGlyph<>(g.shape(), converter.value(g.info()));
	}
	
	@Override
	public Iterator<Glyph<V>> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Glyph<V>> intersects(Rectangle2D r) {
		Collection<Glyph<I>> subs = base.intersects(r);
		ArrayList<Glyph<V>> a = new ArrayList<>(subs.size());
		for (Glyph<I> g: subs) {a.add(wrap(g));}
		return a;
	}

	public boolean isEmpty() {return base.isEmpty();}
	public Rectangle2D bounds() {return base.bounds();}
	public long size() {return base.size();}
	public long segments() {return base.segments();}

	@Override
	public Glyphset<V> segment(long bottom, long top)
			throws IllegalArgumentException {
		return new GlyphsetConverter<>(converter, base.segment(bottom, top));
	}

}
