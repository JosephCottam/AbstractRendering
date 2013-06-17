package ar.glyphsets.implicitgeometry;

import java.awt.Shape;

import ar.Glyphset.Glyph;
import ar.glyphsets.SimpleGlyph;

/**Convenience interface for working with double-encoding on shape and value.**/
public interface Glypher<I,V> extends Shaper<I>, Valuer<I,V> {
	/**Create a fully-realized glyph.**/
	public Glyph<V> glyph(I from);
	
	public class Composite<I,V> implements Glypher<I,V> {
		private final Shaper<I> shaper;
		private final Valuer<I,V> valuer;
		public Composite(Shaper<I> shaper, Valuer<I,V> valuer) {
			this.shaper = shaper;
			this.valuer = valuer;
		}
		public Shape shape(I from) {return shaper.shape(from);}
		public V value(I from) {return valuer.value(from);}
		public Glyph<V> glyph(I from) {return new SimpleGlyph<V>(shape(from), value(from));}		
	}
}