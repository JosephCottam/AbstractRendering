package ar.glyphsets.implicitgeometry;

import ar.Glyphset.Glyph;

/**Convenience interface for working with double-encoding on shape and value.**/
public interface Glypher<I,V> extends Shaper<I>, Valuer<I,V> {
	/**Create a fully-realized glyph.**/
	public Glyph<V> glyph(I from);
}