package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ar.Glyph;
import ar.Glyphset;
import ar.util.axis.Axis;
import ar.util.axis.DescriptorPair;
import ar.util.Util;

/**Single-element glyphset.**/
public final class SingletonGlyphset<G,I> implements Glyphset.RandomAccess<G,I> {
	private final List<Glyph<G,I>> glyphs;
	private final Rectangle2D bounds;
	private DescriptorPair<?,?> axisDescriptor;

	/**Initialize the glyphset with the item.**/
	public SingletonGlyphset(Glyph<G,I> g) {
		glyphs = Collections.singletonList(g);
		bounds = Util.boundOne(g.shape());
	}
	
	@Override public Iterator<Glyph<G,I>> iterator() {return glyphs.iterator();}
	@Override public Glyph<G,I> get(long i) {return glyphs.get(0);}
	@Override public boolean isEmpty() {return glyphs.isEmpty();}
	@Override public long size() {return glyphs.size();}
	@Override public Rectangle2D bounds() {return bounds;}

	@Override public List<Glyphset<G,I>> segment(int count) {return Collections.singletonList(this);}
	@Override public DescriptorPair<?,?> axisDescriptors() {return axisDescriptor != null ? axisDescriptor : Axis.coordinantDescriptors(this);}
	@Override public void axisDescriptors(DescriptorPair<?,?> descriptor) {this.axisDescriptor = descriptor;} 	
}