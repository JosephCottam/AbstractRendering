package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ar.Glyph;
import ar.Glyphset;

/**Single-element glyphset.**/
public final class GlyphSingleton<I> implements Glyphset.RandomAccess<I> {
	private final List<Glyph<I>> glyphs;
	private final Glyph<I> glyph;
	private final Rectangle2D bounds;
	
	/**Initialize the glyphset with the item.**/
	public GlyphSingleton(Glyph<I> g) {
		glyphs = Collections.singletonList(g);
		glyph = g;
		bounds = g.shape().getBounds2D();
	}
	
	public Iterator<Glyph<I>> iterator() {return glyphs.iterator();}
	public Glyph<I> get(long i) {return glyphs.get(0);}
	public boolean isEmpty() {return glyphs.isEmpty();}
	public long size() {return glyphs.size();}
	public Rectangle2D bounds() {return bounds;}

	public Collection<Glyph<I>> intersects(Rectangle2D r) {
		if (glyph.shape().intersects(r)) {return glyphs;}
		else {return Collections.emptyList();}
	}

	public long segments() {return 1;}
	public Glyphset<I> segment(long bottom, long top)
			throws IllegalArgumentException {return this;}
}