package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Iterator;

import ar.Glyph;
import ar.Glyphset;
import ar.util.Axis;
import ar.util.Axis.Descriptor;

/**Single-element glyphset.**/
public final class EmptyGlyphset<G,I> implements Glyphset.RandomAccess<G,I> {
	public Iterator<Glyph<G,I>> iterator() {return Collections.<Glyph<G,I>>emptyList().iterator(); }
	@Override public Glyph<G,I> get(long i) {throw new IllegalArgumentException("No items in glyphset.");}
	@Override public boolean isEmpty() {return true;}
	@Override public long size() {return 0;}
	@Override public Rectangle2D bounds() {return new Rectangle2D.Double(Double.NaN, Double.NaN, Double.NaN, Double.NaN);}
	@Override public Glyphset<G,I> segmentAt(int count, int segId) {return this;}
	@Override public Descriptor axisDescriptors() {return new Axis.Descriptor<>(Axis.empty(), Axis.empty());}
	@Override public void axisDescriptors(Axis.Descriptor descriptor) {return;}

}