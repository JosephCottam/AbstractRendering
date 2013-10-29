package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ar.Glyph;
import ar.Glyphset;
import ar.util.Util;

/**Single-element glyphset.**/
public final class GlyphSingleton<G,I> implements Glyphset.RandomAccess<G,I> {
	private final List<Glyph<G,I>> glyphs;
	private final Rectangle2D bounds;
	
	/**Initialize the glyphset with the item.**/
	public GlyphSingleton(Glyph<G,I> g) {
		glyphs = Collections.singletonList(g);
		bounds = Util.boundOne(g.shape());
	}
	
	public Iterator<Glyph<G,I>> iterator() {return glyphs.iterator();}
	public Glyph<G,I> get(long i) {return glyphs.get(0);}
	public boolean isEmpty() {return glyphs.isEmpty();}
	public long size() {return glyphs.size();}
	public Rectangle2D bounds() {return bounds;}

	public long segments() {return 1;}
	public Glyphset<G,I> segment(long bottom, long top)
			throws IllegalArgumentException {return this;}
}