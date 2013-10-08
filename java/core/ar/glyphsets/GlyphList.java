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
public class GlyphList<I> implements Glyphset<I>, Glyphset.RandomAccess<I> {
	protected final List<Glyph<I>> glyphs = new ArrayList<Glyph<I>>();
	protected Rectangle2D bounds;
	
	public Iterator<Glyph<I>> iterator() {return glyphs.iterator();}
	public boolean isEmpty() {return glyphs.isEmpty();}
	public void add(Glyph<I> g) {glyphs.add(g); bounds=null;}
	public long size() {return glyphs.size();}
	public Glyph<I> get(long i) {
		if (i>Integer.MAX_VALUE) {throw new IllegalArgumentException("Cannot acces items beyond max int value");}
		return glyphs.get((int) i);
	}

	public Collection<Glyph<I>> intersects(Rectangle2D r) {
		ArrayList<Glyph<I>> contained = new ArrayList<Glyph<I>>();
		for (Glyph<I> g: glyphs) {if (g.shape().intersects(r)) {contained.add(g);}}
		return contained;
	}
	
	public Rectangle2D bounds() {
		if (bounds == null) {bounds = Util.bounds(glyphs);}
		return bounds;		
	}
	
	public long segments() {return size();}

	public Glyphset<I> segment(long bottom, long top)
			throws IllegalArgumentException {
		return new GlyphSubset.Uncached<I>(this, bottom, top);
	}
}
