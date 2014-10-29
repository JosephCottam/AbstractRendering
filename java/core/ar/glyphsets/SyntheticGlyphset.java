package ar.glyphsets;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import ar.Glyph;
import ar.Glyphset;
import ar.glyphsets.implicitgeometry.Shaper;

/**Glyphset derived from a Generator.
 * 
 * TODO: Add info generator as well.
 * **/
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

	@Override
	public Glyphset<G, I> segmentAt(int count, int segId) throws IllegalArgumentException {
		long stride = (size()/count)+1; //+1 for the round-down
		long low = stride*segId;
		long high = Math.min(low+stride, size);

		return new GlyphSubset.Uncached<>(this, low, high);
	}
	
	@Override
	public Glyph<G, I> get(long l) {
		return new SimpleGlyph<>(shaper.shape(l), val);
	}
		
	/**Generators create elements in the glyphset.
	 * 
	 * Generators do not need to be repeatable BUT the glyphs
	 * produced by generators all need to be contained inside
	 * of the result of the bounds function.
	 * 
	 * The bounds function is required because under normal circumstances
	 * bounds is determined by observing the contents of the glyphset.
	 * However, since the generator is not required to be repeatable,
	 * that no longer works.  The bounds function returns a bound on what will
	 * be returned. 
	 * 
	 * @param <G> Type of glyph elements returned.
	 */
	public static interface Generator<G> extends Shaper<Long, G> {public Rectangle2D bounds();}

	/**Points generated from a uniform distribution points.**/
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
