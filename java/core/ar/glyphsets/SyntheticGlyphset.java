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
	private final Generator<G> shaper;
	
	public SyntheticGlyphset(long size, I val, Generator<G> shaper) {
		this.val = val;
		this.size = size;
		this.shaper = shaper;
	}
	
	@Override public Iterator<Glyph<G, I>> iterator() {return new GlyphsetIterator<>(this);}
	@Override public boolean isEmpty() {return size <= 0;}
	@Override public Rectangle2D bounds() {return new Rectangle2D.Double(0,0,100,100);}
	@Override public long size() {return size;}
	@Override public long segments() {return size();}
	
	@Override
	public Glyph<G, I> get(long l) {
		return new SimpleGlyph<>(shaper.shape(l), val);
	}
	
	@Override
	public Glyphset<G, I> segment(long bottom, long top)
			throws IllegalArgumentException {
		return new GlyphSubset.Uncached<>(this, bottom, top);
	}
	
	public static interface Generator<G> extends Shaper<Long, G> {public Rectangle2D bounds();}

	/**Randomly generates points.**/
	public static class UniformPoints implements Generator<Point2D> {
		final int maxX, maxY;
		
		public UniformPoints() {this(100,100);}
		public UniformPoints(int maxX, int maxY) {
			this.maxX = maxX;
			this.maxY = maxY;
		}

		@Override public Rectangle2D bounds() {return new Rectangle2D.Double(0,0,100,100);}
		
		@Override
		public Point2D shape(Long from) {return new Point2D.Double(Math.random()*maxX, Math.random()*maxY);}
	}
	
	
}
