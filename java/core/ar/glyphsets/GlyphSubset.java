package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;

import ar.Glyph;
import ar.Glyphset;
import ar.util.Util;

/**Subset of a random-access dataset. **/
public abstract class GlyphSubset<I> implements Glyphset.RandomAccess<I> {
	protected final Glyphset.RandomAccess<I> glyphs;
	protected final long low, high;
	protected GlyphSubset(Glyphset.RandomAccess<I> glyphs, long low, long high) {
		this.glyphs=glyphs;
		this.low =low;
		this.high=high;
		if (high - low > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(
					"Must subset smaller number of items (this class uses int-based caching)");
		}
	}

	public GlyphsetIterator<I> iterator() {return new GlyphsetIterator<I>(this, 0, size());}
	public boolean isEmpty() {return low >= high;}
	public long size() {return high - low;}
	public Rectangle2D bounds() {return Util.bounds(this);}
	public long segments() {return high - low;}
	public Glyphset<I> segment(long bottom, long top)
			throws IllegalArgumentException {
		return new Cached<I>(glyphs, bottom + this.low, top + this.low);
	}

	public Collection<Glyph<I>> intersects(Rectangle2D r) {
		ArrayList<Glyph<I>> contained = new ArrayList<Glyph<I>>();
		for (Glyph<I> g : this) {
			if (g.shape().intersects(r)) {
				contained.add(g);
			}
		}
		return contained;
	}
	
	
	/**Subset where glyphs are cached in the subset.
	 *   
	 * A reference to any glyph returned from the backing glyphset will be
	 * stored in this subset.  This is useful if the backing glyphset
	 * is slow, but it adds a memory cost.  For example, memory
	 * mapped lists are relatively slow to produce any give glyph. 
	 */
	public static final class Cached<G> extends GlyphSubset<G> {
		private final Glyph<G>[] cache;
		private final Rectangle2D bounds;

		@SuppressWarnings({"unchecked","javadoc"})
		public Cached(Glyphset.RandomAccess<G> glyphs, long low, long high) {
			super(glyphs,low, high);
			this.cache = new Glyph[(int) (high - low)];
			Rectangle2D temp = new Rectangle2D.Double();
			for (int i=0; i<cache.length;i++) {
				Glyph<G> glyph = glyphs.get(i+low);
				Util.add(temp, glyph.shape().getBounds2D());
				cache[i] = glyph;
			}
			this.bounds = temp;
		}
		
		@Override
		public Glyph<G> get(long l) {
			if (l > Integer.MAX_VALUE) {throw new ArrayIndexOutOfBoundsException();}
			return cache[(int) l];
		}
		
		@Override
		public Rectangle2D bounds() {return bounds;}
	}

	/**Subset that defers to the backing dataset. 
	 * This is essentially a re-framing of indices to form a subset.**/
	public static final class Uncached<G> extends GlyphSubset<G> {
		@SuppressWarnings({"javadoc"})
		public Uncached(Glyphset.RandomAccess<G> glyphs, long low, long high) {super(glyphs, low,high);}
		public Glyph<G> get(long l) {return glyphs.get(low+l);}
	}

	/**Subset a random-access glyphset; caching optional.*/
	public static <G> GlyphSubset<G> make(Glyphset.RandomAccess<G> source, long low, long  high, boolean cache) {
		if (cache) {
			return new GlyphSubset.Cached<G>(source, low, high);
		} else {
			return new GlyphSubset.Uncached<G>(source, low, high);
		}
	}

}