package ar.test.ext;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.File;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.junit.Test;

import ar.Glyph;
import ar.Glyphset;
import ar.ext.avro.GlyphsetTools;
import ar.glyphsets.GlyphList;
import ar.glyphsets.SimpleGlyph;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.DelimitedReader;
import ar.glyphsets.implicitgeometry.Indexed.Converter.TYPE;
import ar.util.Util;

public class AvroGlyphsTest {

	public class AvroRect<V> implements Valuer<GenericRecord, Glyph<Rectangle2D, V>> {
		private static final long serialVersionUID = 1897764346439188788L;
		Shaper<Indexed, Rectangle2D> shaper;
		Valuer<Indexed, V> valuer;
		public AvroRect(double size, int xfield, int yfield, int vfield) {
			shaper = new Indexed.ToRect(size, size, false, xfield, yfield);
			valuer = new Indexed.ToValue<Object,V>(vfield);
		}
		
		public Glyph<Rectangle2D, V> apply(GenericRecord r) {
			Indexed from =new GlyphsetTools.IndexedRecord(r);
			return new SimpleGlyph<Rectangle2D, V>(shaper.apply(from), valuer.apply(from));
		}
	}
	

	@Test
	public void circlepointsRoundTrip() throws Exception {
		File csv = new File("../data/circlepoints.csv");
		File schema = new File("../data/circlepoints.avsc");
		File output = new File("./testResults/circlepoints.avro");
		
		assertTrue("Input file not found.", csv.exists());
		assertTrue("Schema file not found.", schema.exists());

		encode(csv, output, schema);
		
		GlyphList<?,?> reference = (GlyphList<?,?>) Util.load(
				new GlyphList<Rectangle2D, Object>(), 
				new DelimitedReader(csv), 
				new Indexed.Converter(TYPE.X, TYPE.X, TYPE.DOUBLE, TYPE.DOUBLE, TYPE.INT), 
				new Indexed.ToRect(.1, 2,3), 
				new Indexed.ToValue<>(4));
		
		Glyphset.RandomAccess<Rectangle2D, Color> result = GlyphsetTools.fullLoad(output, new AvroRect<Color>(.1, 2, 3, 4));
		
		assertEquals("Size did not match", reference.size(), result.size());
		for (int i=0;i<reference.size(); i++) {
			Glyph<?,?> res = result.get(i);
			Glyph<?,?> ref = reference.get(i);
			assertEquals("Shape did not match at " + i, ref.shape(), res.shape());
		}

		for (int i=0;i<reference.size(); i++) {
			Glyph<?,?> res = result.get(i);
			Glyph<?,?> ref = reference.get(i);
			assertEquals("Value did not match at " + i, ref.info(), res.info());
		}

	}
	
	/**Utility to write items to an avro file.**/
	public void encode(File sourceFile, File targetFile, File schemaFile) throws Exception {
		Schema schema = new Schema.Parser().parse(schemaFile);
		DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<GenericRecord>(schema);

		try (DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<GenericRecord>(datumWriter)) {
			dataFileWriter.create(schema, targetFile);
			
			DelimitedReader reader = new DelimitedReader(sourceFile, 1, "\\s*,\\s*");
			while (reader.hasNext()) {
				final String[] record = reader.next();
				if (record == null) {continue;}
				final GenericRecord r = new GenericData.Record(schema);
				for (int i=0; i<record.length;i++) {r.put(i, Double.valueOf(record[i]));}
				dataFileWriter.append(r);
			}
		}
	}

}
