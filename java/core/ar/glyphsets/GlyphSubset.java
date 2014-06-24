package ar.glyphsets;

import java.awt.geom.Rectangle2D;

import ar.Glyph;
import ar.Glyphset;
import ar.util.Axis;
import ar.util.Util;
import ar.util.Axis.Descriptor;

/**Subset of a random-access dataset. **/
public abstract class GlyphSubset<G,I> implements Glyphset.RandomAccess<G,I> {
	protected final Glyphset.RandomAccess<G,I> glyphs;
	protected final long low, high;
	protected GlyphSubset(Glyphset.RandomAccess<G,I> glyphs, long low, long high) {
		this.glyphs=glyphs;
		this.low =low;
		this.high=high;
		if (high - low > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(
					"Must subset smaller number of items (this class uses int-based caching)");
		}
	}

	@Override public GlyphsetIterator<G,I> iterator() {return new GlyphsetIterator<G,I>(this, 0, size());}
	@Override public boolean isEmpty() {return low >= high;}
	@Override public long size() {return high - low;}
	@Override public Rectangle2D bounds() {return Util.bounds(this);}

	@Override 
	public Glyphset<G,I> segmentAt(int count, int segId) throws IllegalArgumentException {
		long stride = (size()/count)+1; //+1 for the round-down
		long bottom = stride*segId;
		long top = Math.min(low+stride, high);

		return new Cached<>(glyphs, bottom + this.low, top + this.low);
	}

	@Override public Descriptor axisDescriptors() {return Axis.coordinantDescriptors(this);}

	
	/**Subset where glyphs are cached in the subset.
	 *   
	 * A reference to any glyph returned from the backing glyphset will be
	 * stored in this subset.  This is useful if the backing glyphset
	 * is slow, but it adds a memory cost.  For example, memory
	 * mapped lists are relatively slow to produce any give glyph. 
	 */
	public static final class Cached<G,I> extends GlyphSubset<G,I> {
		private final Glyph<G,I>[] cache;
		private final Rectangle2D bounds;

		@SuppressWarnings({"unchecked","javadoc"})
		public Cached(Glyphset.RandomAccess<G,I> glyphs, long low, long high) {
			super(glyphs,low, high);
			this.cache = new Glyph[(int) (high - low)];
			Rectangle2D temp = new Rectangle2D.Double();
			for (int i=0; i<cache.length;i++) {
				Glyph<G,I> glyph = glyphs.get(i+low);
				Util.add(temp, Util.boundOne(glyph.shape()));
				cache[i] = glyph;
			}
			this.bounds = temp;
		}
		
		@Override
		public Glyph<G,I> get(long l) {
			if (l > Integer.MAX_VALUE) {throw new ArrayIndexOutOfBoundsException();}
			return cache[(int) l];
		}
		
		@Override
		public Rectangle2D bounds() {return bounds;}
	}

	/**Subset that defers to the backing dataset. 
	 * This is essentially a re-framing of indices to form a subset.**/
	public static final class Uncached<G,I> extends GlyphSubset<G,I> {
		@SuppressWarnings({"javadoc"})
		public Uncached(Glyphset.RandomAccess<G,I> glyphs, long low, long high) {super(glyphs, low,high);}
		public Glyph<G,I> get(long l) {return glyphs.get(low+l);}
	}

	/**Subset a random-access glyphset; caching optional.*/
	public static <G,I> GlyphSubset<G,I> make(Glyphset.RandomAccess<G,I> source, long low, long  high, boolean cache) {
		if (cache) {
			return new GlyphSubset.Cached<G,I>(source, low, high);
		} else {
			return new GlyphSubset.Uncached<G,I>(source, low, high);
		}
	}

}