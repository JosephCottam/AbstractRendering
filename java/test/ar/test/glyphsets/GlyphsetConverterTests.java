package ar.test.glyphsets;

import static org.junit.Assert.*;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import ar.Glyphset;
import ar.glyphsets.GlyphsetConverter;
import ar.glyphsets.WrappedCollection;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;

public class GlyphsetConverterTests {
	
	@Test
	public void convert() {
		Valuer<Integer, Double> converter = (from) -> from/2.5;
		Collection<Indexed> data = data();
		Glyphset.RandomAccess<Rectangle2D, Integer> base = WrappedCollection.toList(data, shaper(), valuer());
		Glyphset.RandomAccess<Rectangle2D, Double> glyphs = new GlyphsetConverter<>(base, converter);
		
		assertNotNull(base);
		assertNotNull(glyphs);
		assertEquals("Size mismatch.", base.size(), glyphs.size());
		for (int i=0; i<base.size(); i++) {
			assertEquals(converter.apply(base.get(i).info()), glyphs.get(i).info());
		}
		
	}


	public Shaper<Indexed, Rectangle2D> shaper() {return new Indexed.ToRect(1, 0, 1);}
	public Valuer<Indexed,Integer> valuer() {return new Indexed.ToValue<Object,Integer>(2);}
	
	public ArrayList<Indexed> data() {
		ArrayList<Indexed> values = new ArrayList<Indexed>();
		for (int i=0; i< 100; i++) {
			Integer[] array = new Integer[]{i,i,i};
			values.add(new Indexed.ArrayWrapper(array));
		}
		return values;
	}

}
