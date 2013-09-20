package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;

import ar.Glyph;
import ar.Glyphset;
import ar.util.Util;

/**Subset of a random-access dataset. **/
public abstract class GlyphSubset<G> implements Glyphset.RandomAccess<G> {
	protected final Glyphset.RandomAccess<G> glyphs;
	protected final long low, high;
	protected GlyphSubset(Glyphset.RandomAccess<G> glyphs, long low, long high) {
		this.glyphs=glyphs;
		this.low =low;
		this.high=high;
		if (high - low > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(
					"Must subset smaller number of items (this class uses int-based caching)");
		}
	}

	public GlyphsetIterator<G> iterator() {return new GlyphsetIterator<G>(this, low, high);}
	public boolean isEmpty() {return low >= high;}
	public long size() {return high - low;}
	public Rectangle2D bounds() {return Util.bounds(this);}
	public long segments() {return high - low;}
	public Glyphset<G> segment(long bottom, long top)
			throws IllegalArgumentException {
		return new Cached<G>(glyphs, bottom + this.low, top + this.low);
	}

	public void add(Glyph<G> g) {
		throw new UnsupportedOperationException(
				"Cannot add items to subset view.");
	}

	public Collection<Glyph<G>> intersects(Rectangle2D r) {
		ArrayList<Glyph<G>> contained = new ArrayList<Glyph<G>>();
		for (Glyph<G> g : this) {
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

		@SuppressWarnings({"unchecked","javadoc"})
		public Cached(Glyphset.RandomAccess<G> glyphs, long low, long high) {
			super(glyphs,low, high);
			this.cache = new Glyph[(int) (high - low)];
		}
		
		@Override
		public Glyph<G> get(long l) {
			if (l > Integer.MAX_VALUE) {throw new IllegalArgumentException();}
			int i = (int) (l-low);
			if (cache[i] == null) {cache[i] = glyphs.get(l);}
			return cache[i];
		}
		
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