package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ar.Glyphset;

/**Single-element glyphset.**/
public final class GlyphSingleton<G> implements Glyphset.RandomAccess<G> {
	private final List<Glyph<G>> glyphs;
	private final Glyph<G> glyph;
	private final Class<G> valueType;
	private final Rectangle2D bounds;
	
	public GlyphSingleton(Glyph<G> g, Class<G> valueType) {
		glyphs = Collections.singletonList(g);
		glyph = g;
		bounds = g.shape().getBounds2D();
		this.valueType = valueType;
	}
	
	public Iterator<Glyph<G>> iterator() {return glyphs.iterator();}
	public Glyph<G> get(long i) {return glyphs.get(0);}
	public boolean isEmpty() {return glyphs.isEmpty();}
	public void add(Glyph<G> g) {throw new UnsupportedOperationException();}
	public long size() {return glyphs.size();}
	public Rectangle2D bounds() {return bounds;}
	public Class<G> valueType() {return valueType;}

	public Collection<Glyph<G>> intersects(Rectangle2D r) {
		if (glyph.shape().intersects(r)) {return glyphs;}
		else {return Collections.emptyList();}
	}

	public long limit() {return 1;}
	public ar.Glyphset.Segementable<G> segement(long bottom, long top)
			throws IllegalArgumentException {return this;}
}