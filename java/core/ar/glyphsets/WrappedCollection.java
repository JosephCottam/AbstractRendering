package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import ar.Glyph;
import ar.Glyphset;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.Util;


/**Wrap an existing collection as glyphs.
 * 
 * Also includes tools for working with existing collections of object to turn them into glyphs.
 * 
 * @param <B> Value type of the base collection
 * @param <G> Glyph type of the resulting glyphs
 * @param <I> Value type of the resulting glyphs
 * **/
public class WrappedCollection<B,G,I> implements Glyphset<G,I> {
	protected Collection<B> values;
	protected Shaper<G,B> shaper;
	protected Valuer<B,I> valuer;
	
	/**Wrap the passed collection, ready to construct glyphs with the passed shaper/valuer.**/
	public WrappedCollection(Collection<B> values, 
							Shaper<G,B> shaper, 
							Valuer<B,I> valuer) {
		this.values = values;
		this.shaper = shaper;
		this.valuer = valuer;
	}

	public boolean isEmpty() {return values == null || values.isEmpty();}
	public long size() {return values==null ? 0 : values.size();}
	public Rectangle2D bounds() {return Util.bounds(this);}
	public Iterator<ar.Glyph<G,I>> iterator() {
		return new Iterator<ar.Glyph<G,I>>() {
			Iterator<B> basis = values.iterator();
			public boolean hasNext() {return basis.hasNext();}
			public ar.Glyph<G,I> next() {
				B next = basis.next();
				return next == null ? null : new SimpleGlyph<G,I>(shaper.shape(next), valuer.value(next));
			}
			public void remove() {throw new UnsupportedOperationException();}
		};
	}
	
	@Override
	public long segments() {return values.size();}

	@Override
	@SuppressWarnings("unchecked")
	//TODO: investigate reifying the glyphs at this point and using GlyphList instead of wrapped list (would also remove the suprress)
	public Glyphset<G,I> segment(long bottom, long top) throws IllegalArgumentException {
		int size = (int) (top-bottom);
		final B[] vals = (B[]) new Object[size];
		Iterator<B> it = values.iterator();
		for (long i=0; i<bottom; i++) {it.next();}
		for (int i=0; i<size; i++) {vals[i]=it.next();}
		return new WrappedCollection.List<B,G,I>(Arrays.asList(vals), shaper, valuer);
	}

	
	/**Wrap a list as a set of glyphs.**/
	public static class List<B,G,I> extends WrappedCollection<B,G,I> implements Glyphset.RandomAccess<G,I> {
		protected final java.util.List<B> values;
		
		/**List-specific wrapped collection constructor.**/
		public List(java.util.List<B> values,
				Shaper<G,B> shaper, 
				Valuer<B,I> valuer) {
			super(values, shaper, valuer);
			this.values=values;
		}
		
		public Iterator<ar.Glyph<G,I>> iterator() {
			return new GlyphsetIterator<G,I>(this);
		}
		
		public ar.Glyph<G,I> get(long l) {
			if (l > Integer.MAX_VALUE) {throw new IllegalArgumentException("Can only index through ints in wrapped list.");}
			if (l < 0) {throw new IllegalArgumentException("Negative index not allowed.");}
			B value = values.get((int) l);
			return new SimpleGlyph<G,I>(shaper.shape(value), valuer.value(value));
		}

		@Override
		public long segments() {return size();}

		@Override
		public Glyphset<G,I> segment(long bottom, long top) {
			return GlyphSubset.make(this, bottom, top, true);
		}
	}
	
	
	/**Create a glyphset from a collection.  
	 * Attempts to pick the most efficient option for the given basis.**/
	public static <B,G,I> WrappedCollection<B,G,I> wrap(
				Collection<B> basis, 
				Shaper<G,B> shaper, 
				Valuer<B,I> valuer) {
		
		if (basis instanceof java.util.List) {
			return new List<>((java.util.List<B>) basis, shaper, valuer);
		} else {
			return new WrappedCollection<>(basis, shaper, valuer);
		}
	}	
	
	/**Copies items from the basis into a list.  
	 * 
	 * Copying is advisable if the source data structure is either (1) actively being changed
	 * or (2) a glyph-parallel rendering is desired but the source data is not random access.
	 */
	public static <B,G,I> Glyphset.RandomAccess<G,I> toList(
			Collection<B> basis, 
			Shaper<G,B> shaper, 
			Valuer<B,I> valuer) {
		GlyphList<G,I> glyphs = new GlyphList<>();
		for (B val: basis) {
			Glyph<G,I> g = new SimpleGlyph<>(shaper.shape(val), valuer.value(val));
			glyphs.add(g);
		}
		return glyphs;		
	}
	
	/**Copies items from the basis into a quad-tree.
	 * 	  
	 * Copying to quad-tree is advisable if the source data structure is either (1) actively being changed
	 * or (2) a pixel-parallel rendering is desired.
	 * **/
	public static <B,G,I> Glyphset<G,I> toQuadTree(
			Collection<B> basis, 
			Shaper<G,B> shaper, 
			Valuer<B,I> valuer) {
		DynamicQuadTree<G,I> glyphs = DynamicQuadTree.make();
		for (B val: basis) {
			Glyph<G,I> g = new SimpleGlyph<>(shaper.shape(val), valuer.value(val));
			glyphs.add(g);
		}
		return glyphs;		
	}
}
