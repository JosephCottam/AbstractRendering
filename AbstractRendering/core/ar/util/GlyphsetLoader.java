package ar.util;

import java.awt.Shape;

import ar.Glyph;
import ar.Glyphset;
import ar.glyphsets.*;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.glyphsets.implicitgeometry.Indexed.Converter;

/**Tools for loading glyphs from various locations into glyphsets.**/
public class GlyphsetLoader {
	/**Load a set of glyphs from a delimited reader, using the provided shaper and valuer.
	 * 
	 * This method creates concrete geometry, though it uses the implicit geometry system to achieve it. 
	 * 
	 * @param glyphs Glyphset to load items into
	 * @param reader Source of the glyph data
	 * @param converer Convert read entries to indexed entries
	 * @param shaper Convert the read item into a shape
	 * @param valuer Convert the read item into a value
	 * @return The glyphset passed in as a parameter (now with more glyphs)
	 */
	public static <V> Glyphset<V> load(Glyphset<V> glyphs, DelimitedReader reader, Indexed.Converter c, Shaper<Indexed> shaper, Valuer<Indexed, V> valuer) {
		int count =0;

		while (reader.hasNext()) {
			String[] parts = reader.next();
			if (parts == null) {continue;}
			
			Converter item = new Converter(parts, c.types());
			V value = valuer.value(item);
			Shape shape = shaper.shape(item);

			Glyph<V> g = new SimpleGlyph<V>(shape, value);
			try {glyphs.add(g);}
			catch (Exception e) {throw new RuntimeException("Error loading item number " + count, e);}
			count++;
		}
		//The check below causes an issue if memory is tight...the check has a non-trivial overhead on some glyphset types
		if (count != glyphs.size()) {throw new RuntimeException(String.format("Error loading data; Read and retained glyph counts don't match (%s read vs %s retained).", count, glyphs.size()));}
		return glyphs;
	}
}
