package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ar.Glyph;
import ar.Glyphset;
import ar.util.axis.Axis;
import ar.util.axis.DescriptorPair;

/**Single-element glyphset.**/
public final class EmptyGlyphset<G,I> implements Glyphset.RandomAccess<G,I> {
	public Iterator<Glyph<G,I>> iterator() {return Collections.<Glyph<G,I>>emptyList().iterator(); }
	@Override public Glyph<G,I> get(long i) {throw new IllegalArgumentException("No items in glyphset.");}
	@Override public boolean isEmpty() {return true;}
	@Override public long size() {return 0;}
	@Override public Rectangle2D bounds() {return new Rectangle2D.Double(Double.NaN, Double.NaN, Double.NaN, Double.NaN);}
	@Override public List<Glyphset<G,I>> segment(int count) {return Collections.singletonList(this);}
	@Override public DescriptorPair<?,?> axisDescriptors() {return new DescriptorPair<>(Axis.empty(), Axis.empty());}
	@Override public void axisDescriptors(DescriptorPair<?,?> descriptor) {return;}

}