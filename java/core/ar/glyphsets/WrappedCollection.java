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
import ar.util.axis.Axis;
import ar.util.axis.DescriptorPair;


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
	protected Shaper<B,G> shaper;
	protected Valuer<B,I> valuer;
	
	/**Wrap the passed collection, ready to construct glyphs with the passed shaper/valuer.**/
	public WrappedCollection(Collection<B> values, 
							Shaper<B,G> shaper, 
							Valuer<B,I> valuer) {
		this.values = values;
		this.shaper = shaper;
		this.valuer = valuer;
	}

	@Override public boolean isEmpty() {return values == null || values.isEmpty();}
	@Override public long size() {return values==null ? 0 : values.size();}
	@Override public Rectangle2D bounds() {return Util.bounds(this);}

	private DescriptorPair axisDescriptor;
	@Override public DescriptorPair axisDescriptors() {return axisDescriptor != null ? axisDescriptor : Axis.coordinantDescriptors(this);}
	@Override public void axisDescriptors(DescriptorPair descriptor) {this.axisDescriptor = descriptor;} 

	
	@Override 
	public Iterator<ar.Glyph<G,I>> iterator() {
		return new Iterator<ar.Glyph<G,I>>() {
			Iterator<B> basis = values.iterator();
			public boolean hasNext() {return basis.hasNext();}
			public ar.Glyph<G,I> next() {
				B next = basis.next();
				return next == null ? null : new SimpleGlyph<G,I>(shaper.apply(next), valuer.apply(next));
			}
			public void remove() {throw new UnsupportedOperationException();}
		};
	}
	
	/**WARNING: Relies on different iterator instance returning elements in the same order...**/
	@Override
	@SuppressWarnings("unchecked")
	public Glyphset<G,I> segmentAt(int count, int segId) throws IllegalArgumentException {
		long stride = (size()/count)+1; //+1 for the round-down
		long low = stride*segId;
		long high = Math.min(low+stride, size());

		if (stride > Integer.MAX_VALUE) {throw new IllegalArgumentException("Segment size excceeds maximum allowable.");}
		
		int size = (int) (high-low);
		final B[] vals = (B[]) new Object[size];
		Iterator<B> it = values.iterator();
		for (long i=0; i<low; i++) {it.next();}  //Walk iterator up to the start
		for (int i=0; i<size; i++) {vals[i]=it.next();} //Copy values over...
		return new WrappedCollection.List<B,G,I>(Arrays.asList(vals), shaper, valuer);
	}

	
	/**Wrap a list as a set of glyphs.**/
	public static class List<B,G,I> extends WrappedCollection<B,G,I> implements Glyphset.RandomAccess<G,I> {
		protected final java.util.List<B> values;
		
		/**List-specific wrapped collection constructor.**/
		public List(java.util.List<B> values,
				Shaper<B,G> shaper, 
				Valuer<B,I> valuer) {
			super(values, shaper, valuer);
			this.values=values;
		}
		
		@Override
		public Iterator<ar.Glyph<G,I>> iterator() {
			return new GlyphsetIterator<G,I>(this);
		}
		
		@Override
		public ar.Glyph<G,I> get(long l) {
			if (l > Integer.MAX_VALUE) {throw new IllegalArgumentException("Can only index through ints in wrapped list.");}
			if (l < 0) {throw new IllegalArgumentException("Negative index not allowed.");}
			B value = values.get((int) l);
			return new SimpleGlyph<G,I>(shaper.apply(value), valuer.apply(value));
		}

		@Override
		public Glyphset<G,I> segmentAt(int count ,int segId) {
			long stride = (size()/count)+1; //+1 for the round-down
			long low = stride*segId;
			long high = Math.min(low+stride, size());

			return GlyphSubset.make(this, low, high, true);
		}
	}
	
	
	/**Create a glyphset from a collection.  
	 * Attempts to pick the most efficient option for the given basis.**/
	public static <B,G,I> WrappedCollection<B,G,I> wrap(
				Collection<B> basis, 
				Shaper<B,G> shaper, 
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
			Shaper<B,G> shaper, 
			Valuer<B,I> valuer) {
		GlyphList<G,I> glyphs = new GlyphList<>();
		for (B val: basis) {
			Glyph<G,I> g = new SimpleGlyph<>(shaper.apply(val), valuer.apply(val));
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
			Shaper<B,G> shaper, 
			Valuer<B,I> valuer) {
		DynamicQuadTree<G,I> glyphs = DynamicQuadTree.make();
		for (B val: basis) {
			Glyph<G,I> g = new SimpleGlyph<>(shaper.apply(val), valuer.apply(val));
			glyphs.add(g);
		}
		return glyphs;		
	}
}
