package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
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
 * **/
public class WrappedCollection<I,V> implements Glyphset<V> {
	protected Collection<I> values;
	protected Shaper<I> shaper;
	protected Valuer<I,V> valuer;
	protected Class<V> valueType;
	
	public WrappedCollection(Collection<I> values, 
							Shaper<I> shaper, 
							Valuer<I,V> valuer,
							Class<V> valueType) {
		this.values = values;
		this.shaper = shaper;
		this.valuer = valuer;
		this.valueType = valueType;
	}
	
	public Collection<ar.Glyph<V>> intersects(Rectangle2D r) {
		ArrayList<ar.Glyph<V>> hits = new ArrayList<ar.Glyph<V>>();
		for (Glyph<V> g: this) {
			if (g.shape().getBounds2D().intersects(r)) {hits.add(g);}
		}
		return hits;
	}

	public boolean isEmpty() {return values == null || values.isEmpty();}
	public long size() {return values==null ? 0 : values.size();}
	public Rectangle2D bounds() {return Util.bounds(this);}
	public Iterator<ar.Glyph<V>> iterator() {
		return new Iterator<ar.Glyph<V>>() {
			Iterator<I> basis = values.iterator();
			public boolean hasNext() {return basis.hasNext();}
			public ar.Glyph<V> next() {
				I next = basis.next();
				return next == null ? null : new SimpleGlyph<V>(shaper.shape(next), valuer.value(next));
			}
			public void remove() {throw new UnsupportedOperationException();}
		};
	}
	
	public void add(ar.Glyph<V> g) {
		throw new UnsupportedOperationException("Cannot add directly to wrapped list.  Must add to backing collection.");
	}
	
	public Class<V> valueType() {return valueType;}

	

	@Override
	public long segments() {return values.size();}

	@Override
	@SuppressWarnings("unchecked")
	//TODO: investigate reifying the glyphs at this point and using GlyphList instead of wrapped list (would also remove the suprress)
	public Glyphset<V> segment(long bottom, long top) throws IllegalArgumentException {
		int size = (int) (top-bottom);
		final I[] vals = (I[]) new Object[size];
		Iterator<I> it = values.iterator();
		for (long i=0; i<bottom; i++) {it.next();}
		for (int i=0; i<size; i++) {vals[i]=it.next();}
		return new WrappedCollection.List<I,V>(Arrays.asList(vals), shaper, valuer, valueType);
	}

	
	/**Wrap a list as a set of glyphs.**/
	public static class List<I,V> extends WrappedCollection<I,V> implements Glyphset.RandomAccess<V> {
		protected final java.util.List<I> values;
		
		public List(java.util.List<I> values,
				Shaper<I> shaper, 
				Valuer<I,V> valuer,
				Class<V> valueType) {
			super(values, shaper, valuer, valueType);
			this.values=values;
		}
		
		public Iterator<ar.Glyph<V>> iterator() {
			return new GlyphsetIterator<V>(this);
		}
		
		public ar.Glyph<V> get(long l) {
			if (l > Integer.MAX_VALUE) {throw new IllegalArgumentException("Can only index through ints in wrapped list.");}
			if (l < 0) {throw new IllegalArgumentException("Negative index not allowed.");}
			I value = values.get((int) l);
			return new SimpleGlyph<V>(shaper.shape(value), valuer.value(value));
		}

		@Override
		public long segments() {return size();}

		@Override
		public Glyphset<V> segment(long bottom, long top) {
			return GlyphSubset.make(this, bottom, top, true);
		}
	}
	
	
	/**Create a glyphset from a collection.  
	 * Attempts to pick the most efficient option for the given basis.**/
	public static <I,V> WrappedCollection<I,V> wrap(
				Collection<I> basis, 
				Shaper<I> shaper, 
				Valuer<I,V> valuer,
				Class<V> valueType) {
		
		if (basis instanceof java.util.List) {
			return new List<I,V>((java.util.List<I>) basis, shaper, valuer, valueType);
		} else {
			return new WrappedCollection<I,V>(basis, shaper, valuer, valueType);
		}
	}	
	
	/**Copies items from the basis into a list.  
	 * 
	 * Copying is advisable if the source data structure is either (1) actively being changed
	 * or (2) a glyph-parallel rendering is desired but the source data is not random access.
	 */
	public static <I,V> Glyphset<V> toList(
			Collection<I> basis, 
			Shaper<I> shaper, 
			Valuer<I,V> valuer, 
			Class<V> valueType) {
		Glyphset<V> glyphs = DynamicQuadTree.make(valueType);
		for (I val: basis) {
			Glyph<V> g = new SimpleGlyph<V>(shaper.shape(val), valuer.value(val));
			glyphs.add(g);
		}
		return glyphs;		
	}
	
	/**Copies items from the basis into a quad-tree.
	 * 	  
	 * Copying to quad-tree is advisable if the source data structure is either (1) actively being changed
	 * or (2) a pixel-parallel rendering is desired.
	 * **/
	public static <I,V> Glyphset<V> toQuadTree(
			Collection<I> basis, 
			Shaper<I> shaper, 
			Valuer<I,V> valuer, 
			Class<V> valueType) {
		Glyphset<V> glyphs = DynamicQuadTree.make(valueType);
		for (I val: basis) {
			Glyph<V> g = new SimpleGlyph<V>(shaper.shape(val), valuer.value(val));
			glyphs.add(g);
		}
		return glyphs;		
	}
}
