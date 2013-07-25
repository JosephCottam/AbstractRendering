package ar.ext.spark;

import ar.Glyph;
import ar.glyphsets.SimpleGlyph;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import spark.api.java.function.Function;


/**Wrap a shaper and a valuer up into a single object that produces glyphs.**/
public class Glypher<V> extends Function<Indexed, Glyph<V>> {
	private static final long serialVersionUID = -2010087917940244951L;
	
	final Shaper<Indexed> shaper;
	final Valuer<Indexed, V> valuer;
	public Glypher(Shaper<Indexed> shaper, Valuer<Indexed, V> valuer) {
		this.shaper=shaper;
		this.valuer=valuer;
	}

	public Glyph<V> call(Indexed item) throws Exception {
		return new SimpleGlyph<V>(shaper.shape(item), valuer.value(item));
	}

}
