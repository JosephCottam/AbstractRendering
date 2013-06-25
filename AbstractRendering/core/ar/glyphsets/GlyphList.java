package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.*;

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
public class GlyphList<T> implements Glyphset<T>, Glyphset.RandomAccess<T> {
	protected final List<Glyph<T>> glyphs = new ArrayList<Glyph<T>>();
	protected Rectangle2D bounds;
	protected final Class<T> valueType;
	
	public GlyphList(Class<T> valueType) {this.valueType = valueType;}
	
	public Iterator<Glyph<T>> iterator() {return glyphs.iterator();}
	public boolean isEmpty() {return glyphs.isEmpty();}
	public void add(Glyph<T> g) {glyphs.add(g); bounds=null;}
	public long size() {return glyphs.size();}
	public Glyph<T> get(long i) {
		if (i>Integer.MAX_VALUE) {throw new IllegalArgumentException("Cannot acces items beyond max int value");}
		return glyphs.get((int) i);
	}

	public Collection<Glyph<T>> intersects(Rectangle2D r) {
		ArrayList<Glyph<T>> contained = new ArrayList<Glyph<T>>();
		for (Glyph<T> g: glyphs) {if (g.shape().intersects(r)) {contained.add(g);}}
		return contained;
	}
	
	public Rectangle2D bounds() {
		if (bounds == null) {bounds = Util.bounds(glyphs);}
		return bounds;		
	}
	
	public Class<T> valueType() {return valueType;}
}
