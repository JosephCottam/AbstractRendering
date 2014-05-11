package ar.glyphsets;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import ar.Glyph;
import ar.Glyphset;
import ar.glyphsets.implicitgeometry.Shaper;

public class SyntheticGlyphset<G,I> implements Glyphset.RandomAccess<G,I>{
	private final I val;
	private final long size;
	private final Shaper<Long, G> shaper;
	
	public SyntheticGlyphset(long size, I val, Shaper<Long, G> shaper) {
		this.val = val;
		this.size = size;
		this.shaper = shaper;
	}
	
	public Iterator<Glyph<G, I>> iterator() {return new GlyphsetIterator<>(this);}
	public boolean isEmpty() {return size <= 0;}
	public Rectangle2D bounds() {return new Rectangle2D.Double(0,0,100,100);}
	public long size() {return size;}
	public long segments() {return size();}

	@Override
	public Glyphset<G, I> segment(long bottom, long top)
			throws IllegalArgumentException {
		return new GlyphSubset.Uncached<>(this, bottom, top);
	}
	
	@Override
	public Glyph<G, I> get(long l) {
		return new SimpleGlyph<>(shaper.shape(l), val);
	}
	
	public static final class SyntheticPoints implements Shaper<Long, Point2D> {
		public Point2D shape(Long from) {
			return new Point2D.Double(Math.random()*100, Math.random()*100);
		}
	}
	
	
}
