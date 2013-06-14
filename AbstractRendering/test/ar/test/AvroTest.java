package ar.test;

import static org.junit.Assert.*;

import java.awt.geom.Rectangle2D;
import java.io.File;

import org.apache.avro.generic.GenericRecord;
import org.junit.Test;

import ar.Glyphset;
import ar.Glyphset.Glyph;
import ar.ext.Avro;
import ar.ext.Avro.Realizer;
import ar.glyphsets.GlyphList;
import ar.glyphsets.ImplicitGeometry;
import ar.glyphsets.SimpleGlyph;
import ar.util.CSVtoGlyphSet;

public class AvroTest {

	public class AvroRect<V> implements Avro.Realizer<Glyph<V>> {
		ImplicitGeometry.Shaper<ImplicitGeometry.Indexed> shaper;
		ImplicitGeometry.Valuer<ImplicitGeometry.Indexed, V> valuer;
		ImplicitGeometry.Glypher<ImplicitGeometry.Indexed, V> glypher;
		public AvroRect(double size, int xfield, int yfield, int vfield) {
			shaper = new ImplicitGeometry.IndexedToRect(size, size, false, xfield, yfield);
			valuer = new ImplicitGeometry.IndexedToValue<>(vfield);
			glypher = new ImplicitGeometry.WrappingGlypher<>(shaper, valuer);
		}
		
		public Glyph<V> wrap(GenericRecord r) {
			return glypher.glyph(new Avro.IndexedRecord(r));
		}
	}
	

	@Test
	public void circlepointsRoundTrip() throws Exception {
		String csv = "../data/circlepoints.csv";
		String avro = "../data/circlepoints.avro";
		String schema = "../data/circlepoints.json";
		encode(csv, avro, schema);
		
		GlyphList reference = (GlyphList) CSVtoGlyphSet.autoLoad(new File(csv), .1, new GlyphList());
		Glyphset.RandomAccess result = Avro.fullLoad(avro, new AvroRect(.1, 0, 1, 2));
		
		assertEquals("Size did not match", reference.size(), result.size());
		for (int i=0;i<reference.size(); i++) {
			assertEquals(reference.get(i), result.get(i));
		}
		
		
		fail("Not yet implemented");
	}
	
	public void encode(String source, String target, String schema) {
		
	}

}
