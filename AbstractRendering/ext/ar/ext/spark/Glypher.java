package ar.ext.spark;

import ar.Glyph;
import ar.glyphsets.SimpleGlyph;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import spark.api.java.function.Function;


public class Glypher<V> extends Function<IndexedProduct, Glyph<V>> {
	private static final long serialVersionUID = -2010087917940244951L;
	
	final Shaper<Indexed> shaper;
	final Valuer<Indexed, V> valuer;
	public Glypher(Shaper<Indexed> shaper, Valuer<Indexed, V> valuer) {
		this.shaper=shaper;
		this.valuer=valuer;
	}

	public Glyph<V> call(IndexedProduct item) throws Exception {
		return new SimpleGlyph<V>(shaper.shape(item), valuer.value(item));
	}

}
