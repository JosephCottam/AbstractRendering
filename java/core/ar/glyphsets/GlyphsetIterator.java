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
public class GlyphsetIterator<G,I> implements Iterator<Glyph<G,I>>{
	private final Glyphset.RandomAccess<G,I> glyphs;
	private final long high;
	private long at = 0;
	
	/**Iterator over the full range of a glyphset.**/
	public GlyphsetIterator(Glyphset.RandomAccess<G,I> glyphs) {this(glyphs, 0, glyphs.size());}
	
	/**Iterator over a sub-range of a glyphset.**/
	public GlyphsetIterator(Glyphset.RandomAccess<G,I> glyphs, long low, long high) {
		this.glyphs = glyphs;
		this.at = low;
		this.high=high;
	}

	public boolean hasNext() {return at < high;}
	public Glyph<G,I> next() {
		if (!hasNext()) {return null;}
		return glyphs.get(at++);
	}
	public void remove() {throw new UnsupportedOperationException();}
}
