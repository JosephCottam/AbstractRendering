package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.*;

import ar.Glyph;
import ar.Glyphset;
import ar.util.Util;
import ar.util.axis.Axis;
import ar.util.axis.DescriptorPair;

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
	protected final List<Glyph<G,I>> glyphs;
	protected Rectangle2D bounds;
	private DescriptorPair<?,?> axisDescriptor;

	public GlyphList() {this(new ArrayList<Glyph<G,I>>());}
	public GlyphList(List<Glyph<G,I>> glyphs) {this.glyphs = glyphs;}
	
	public void add(Glyph<G,I> g) {glyphs.add(g); bounds=null;}
	public void addAll(Glyphset<G,I> newGlyphs) {
		for (Glyph<G,I> g: newGlyphs) {glyphs.add(g);}
		bounds = null;
	}
	
	@Override public Iterator<Glyph<G,I>> iterator() {return glyphs.iterator();}
	@Override public boolean isEmpty() {return glyphs.isEmpty();}
	@Override public long size() {return glyphs.size();}
	
	@Override
	public Glyph<G,I> get(long i) {
		if (i>Integer.MAX_VALUE) {throw new IllegalArgumentException("Cannot acces items beyond max int value");}
		return glyphs.get((int) i);
	}
	
	@Override
	public Rectangle2D bounds() {
		if (bounds == null) {bounds = Util.bounds(glyphs);}
		return bounds;		
	}

	@Override
	public List<Glyphset<G,I>> segment(int count) throws IllegalArgumentException {
		long stride = (size()/count)+1; //+1 for the round-down
		List<Glyphset<G,I>> segments = new ArrayList<>();
		for (long offset=0; offset<size(); offset+=stride) {
			segments.add(new GlyphSubset.Uncached<>(this, offset, Math.min(offset+stride, size())));
		}
		return segments;
	}
	
	@Override public DescriptorPair<?,?> axisDescriptors() {return axisDescriptor != null ? axisDescriptor : Axis.coordinantDescriptors(this);}
	@Override public void axisDescriptors(DescriptorPair<?,?> descriptor) {this.axisDescriptor = descriptor;} 
}
