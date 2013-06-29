package ar.test.ext;

import static org.junit.Assert.*;

import java.awt.Color;
import java.io.File;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.junit.Test;

import ar.Glyphset;
import ar.Glyphset.Glyph;
import ar.app.util.GlyphsetUtils;
import ar.ext.avro.GlyphsetTools;
import ar.glyphsets.GlyphList;
import ar.glyphsets.SimpleGlyph;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.DelimitedReader;

public class AvroGlyphs {

	public class AvroRect<V> implements Valuer<GenericRecord, Glyph<V>> {
		Shaper<Indexed> shaper;
		Valuer<Indexed, V> valuer;
		public AvroRect(double size, int xfield, int yfield, int vfield) {
			shaper = new Indexed.ToRect(size, size, false, xfield, yfield);
			valuer = new Indexed.ToValue<Object,V>(vfield);
		}
		
		public Glyph<V> value(GenericRecord r) {
			Indexed from =new GlyphsetTools.IndexedRecord(r);
			return new SimpleGlyph<V>(shaper.shape(from), valuer.value(from));
		}
	}
	

	@Test
	public void circlepointsRoundTrip() throws Exception {
		String csv = "../data/circlepoints.csv";
		String avro = "../data/circlepoints.avro";
		String schema = "../data/circlepoints.avsc";
		encode(csv, avro, schema);
		
		GlyphList<?> reference =(GlyphList<?>) GlyphsetUtils.load(new GlyphList(Color.class), new File(csv), 1, .1, false, 2, 3, -1, 4); 
		Glyphset.RandomAccess<?> result = GlyphsetTools.fullLoad(avro, new AvroRect(.1, 2, 3, 4), Color.class);
		
		assertEquals("Size did not match", reference.size(), result.size());
		for (int i=0;i<reference.size(); i++) {
			Glyph<?> res = result.get(i);
			Glyph<?> ref = reference.get(i);
			assertEquals("Shape did not match at " + i, ref.shape(), res.shape());
		}

		for (int i=0;i<reference.size(); i++) {
			Glyph<?> res = result.get(i);
			Glyph<?> ref = reference.get(i);
			assertEquals("Value did not match at " + i, ref.value(), res.value().toString());
		}

	}
	
	public void encode(String sourceFile, String targetFile, String schemaFile) throws Exception {
		Schema schema = new Schema.Parser().parse(new File(schemaFile));
		DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<GenericRecord>(schema);
		DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<GenericRecord>(datumWriter);
		dataFileWriter.create(schema, new File(targetFile));
		
		DelimitedReader reader = new DelimitedReader(new File(sourceFile), 1, "\\s*,\\s*");
		while (reader.hasNext()) {
			final String[] record = reader.next();
			if (record == null) {continue;}
			final GenericRecord r = new GenericData.Record(schema);
			for (int i=0; i<record.length;i++) {r.put(i, Double.parseDouble(record[i]));}
			dataFileWriter.append(r);
		}
		
		dataFileWriter.close();
	}

}
