package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.*;

import ar.Glyph;
import ar.Glyphset;
import ar.util.Util;

/**Explicit geometry, sequentially arranged glyphset.
 *   
 * The dynamic quad tree is functionally equivalent to this class 
 * but has a larger memory footprint.  If fully-realized geometry is desired
 * this class can accomodate larger data.  However, efficient rendering
 * requires using the glyph-parallel renderer instead of the pixel-parallel renderer.
 * 
 * 
 * @author jcottam
 *
 */
public class GlyphList<G,I> implements Glyphset.RandomAccess<G,I> {
	protected final List<Glyph<G,I>> glyphs = new ArrayList<Glyph<G,I>>();
	protected Rectangle2D bounds;
	
	public Iterator<Glyph<G,I>> iterator() {return glyphs.iterator();}
	public boolean isEmpty() {return glyphs.isEmpty();}
	public void add(Glyph<G,I> g) {glyphs.add(g); bounds=null;}
	public void addAll(Glyphset<G,I> newGlyphs) {
		for (Glyph<G,I> g: newGlyphs) {glyphs.add(g);}
		bounds = null;
	}
	
	public long size() {return glyphs.size();}
	public Glyph<G,I> get(long i) {
		if (i>Integer.MAX_VALUE) {throw new IllegalArgumentException("Cannot acces items beyond max int value");}
		return glyphs.get((int) i);
	}
	
	public Rectangle2D bounds() {
		if (bounds == null) {bounds = Util.bounds(glyphs);}
		return bounds;		
	}
	
	public long segments() {return size();}

	public Glyphset<G,I> segment(long bottom, long top)
			throws IllegalArgumentException {
		return new GlyphSubset.Uncached<G,I>(this, bottom, top);
	}
}
