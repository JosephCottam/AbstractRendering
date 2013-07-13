package ar.glyphsets;

import java.util.Iterator;

import ar.Glyph;
import ar.Glyphset;

/**Iterate over the elements of a glyphset.
 * This class assumes that if a subset is specified, 
 * that the range is valid for the glyphset.
 * It will visit the specified low-index, but not the high-index.  
 * 
 * */
public class GlyphsetIterator<V> implements Iterator<Glyph<V>>{
	private final Glyphset.RandomAccess<V> glyphs;
	private final long high;
	private long at = 0;
	public GlyphsetIterator(Glyphset.RandomAccess<V> glyphs) {this(glyphs, 0, glyphs.size());}
	public GlyphsetIterator(Glyphset.RandomAccess<V> glyphs, long low, long high) {
		this.glyphs = glyphs;
		this.at = low;
		this.high=high;
	}

	public boolean hasNext() {return at < high;}
	public Glyph<V> next() {
		if (!hasNext()) {return null;}
		return glyphs.get(at++);
	}
	public void remove() {throw new UnsupportedOperationException();}
}
