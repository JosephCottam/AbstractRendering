package ar.ext.avro;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;

import ar.Aggregates;
import ar.aggregates.AggregateUtils;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.rules.CategoricalCounts;

public class AggregateSerializer {
	public static final String AGGREGATES_SCHEMA ="ar/ext/avro/tile.avsc";
	public static final String COUNTS_SCHEMA="ar/ext/avro/count.avsc";
	public static final String COC_SCHEMA="ar/ext/avro/coc.avsc";
	public static final String COLOR_SCHEMA="ar/ext/avro/color.avsc";

	public static enum FORMAT{BINARY,JSON}

	public static final Map<String,String> META;
	static {
		Map<String,String> map = new HashMap<String,String>();
		map.put("AbstractRendering", "Summer 2012");
		META = Collections.unmodifiableMap(map);
	}

	/**Common aggregates serialization code.
	 * Note: Values must be either a collection or a reference-type array (sorry, no primitive arrays)
	 */
	private static void serializeContainer(
			Aggregates<?> aggs, 
			OutputStream out,
			Schema schema, 
			FORMAT format,
			GenericRecord defaultVal, 
			List<GenericRecord> values) {

		GenericRecord aggregates = new GenericData.Record(schema);
		aggregates.put("xOffset", aggs.lowX());
		aggregates.put("yOffset", aggs.lowY());
		aggregates.put("xBinCount", aggs.highX()-aggs.lowX());
		aggregates.put("yBinCount", aggs.highY()-aggs.lowX());
		aggregates.put("values", values);
		aggregates.put("default", defaultVal);
		aggregates.put("level", 0);
		aggregates.put("meta", META);

		try {
			switch (format) {
				case BINARY : emitBinary(aggregates, schema, out); break;
				case JSON: emitJSON(aggregates, schema, out); break;
			}
		} catch (IOException e) {throw new RuntimeException("Error serializing",e);}		
	}

	/**Emit to JSON.  Does not include the schema.**/
	public static void emitJSON(GenericRecord aggregates, Schema schema, OutputStream out) throws IOException {
		DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<GenericRecord>(schema);
		Encoder e = EncoderFactory.get().jsonEncoder(schema, out);
		datumWriter.write(aggregates, e);
		e.flush();
		out.close();
	}

	/**Emit to binary format.  Does a schema-included output.**/
	public static void emitBinary(GenericRecord aggregates, Schema schema, OutputStream out) throws IOException {
		DatumWriter<GenericRecord> writer=new GenericDatumWriter<GenericRecord>(schema);
		
		try (DataFileWriter<GenericRecord> dataFileWriter=new DataFileWriter<GenericRecord>(writer)) {
			dataFileWriter.create(schema, out);
			dataFileWriter.append(aggregates);
			dataFileWriter.close();
		}
	}


	public static <A> void serialize(Aggregates<A> aggs, OutputStream out, Schema itemSchema, Valuer<A, GenericRecord> converter) throws IOException {
		serialize(aggs,out,itemSchema,FORMAT.BINARY, converter);
	}
	public static <A> void serialize(Aggregates<A> aggs, OutputStream out, Schema itemSchema, FORMAT format, Valuer<A, GenericRecord> converter) throws IOException {
		Schema fullSchema = new SchemaComposer().add(itemSchema).addResource(AGGREGATES_SCHEMA).resolved();

		List<GenericRecord> records = new ArrayList<GenericRecord>();
		A defVal = aggs.defaultValue();
		GenericRecord defrec = converter.value(defVal);

		for (int y=aggs.lowY(); y<aggs.highY(); y++) {
			for (int x=aggs.lowX(); x<aggs.highX(); x++) {
				A val = aggs.get(x,y);
				//if (defVal == val || (defVal != null && defVal.equals(val))) {continue;}  TODO: Investigate reinstating default-value omission by making a union type with null...(maybe)
				GenericRecord vr = converter.value(val);
				records.add(vr);
			}
		}

		serializeContainer(aggs, out, fullSchema, format, defrec, records);
	}



	/**Output a set of aggregates, attempt to automatically detect the aggregate type.
	 * Write to the passed output stream.
	 * 
	 * @param aggs
	 * @param outputStream
	 * @throws IOException
	 */
	public static <A> void serialize(Aggregates<A> aggs, OutputStream outputStream) throws IOException {serialize(aggs, outputStream, FORMAT.BINARY);}

	@SuppressWarnings("unchecked")
	public static <A> void serialize(Aggregates<A> aggs, OutputStream outputStream, FORMAT format) throws IOException {
		Object v = aggs.defaultValue();
		Schema schema;
		Valuer<A, GenericRecord> conv;
		if (v == null) {throw new IllegalArgumentException("Could not auto-detect aggregate type, defualt value was null.");} 
		else if (v instanceof Integer) {
			schema = new SchemaComposer().addResource(COUNTS_SCHEMA).resolved();
			conv = (Valuer<A, GenericRecord>) new Converters.FromCount(schema);

		} else if (v instanceof CategoricalCounts) {
			schema = new SchemaComposer().addResource(COC_SCHEMA).resolved();
			conv = (Valuer<A, GenericRecord>) new Converters.FromCoC<A>(schema);
		} else if (v instanceof Color) {
			schema = new SchemaComposer().addResource(COLOR_SCHEMA).resolved();
			conv = (Valuer<A, GenericRecord>) new Converters.FromColor(schema);
		} else {
			throw new IllegalArgumentException("Aggreagte type not supported in auto-detection: " + v.getClass().getName());
		}

		serialize(aggs, outputStream, schema, format, conv);
	}


	public static <A> Aggregates<A> deserialize(File file, Valuer<GenericRecord, A> converter) throws IOException {
		try (FileInputStream fs = new FileInputStream(file)) {
			return deserialize(fs, converter);
		}
	}
	
	public static <A> Aggregates<A> deserialize(InputStream stream, Valuer<GenericRecord, A> converter) {
		DatumReader<GenericRecord> dr = new GenericDatumReader<GenericRecord>();
		try (DataFileStream<GenericRecord> fr =new DataFileStream<GenericRecord>(stream, dr)) {
			
			GenericRecord r = fr.next();

			int lowX = (Integer) r.get("xOffset");
			int lowY = (Integer) r.get("yOffset");
			int xCount = (Integer) r.get("xBinCount");
			int yCount = (Integer) r.get("yBinCount");
			int highX = lowX+xCount;
			int highY = lowY+yCount;
			A defVal = converter.value((GenericRecord) r.get("default"));			
			
			@SuppressWarnings("unchecked")
			GenericData.Array<GenericRecord> entries = 
					(GenericData.Array<GenericRecord>) r.get("values");

			Aggregates<A> aggs = AggregateUtils.make(lowX, lowY, highX, highY, defVal);
			for (int i=0; i<entries.size(); i++) {
				int x = i % xCount;
				int y = i / xCount;
				A val = converter.value(entries.get(i));
				aggs.set(x+lowX, y+lowY, val);
			}

			fr.close();
			return aggs;
		} catch (IOException e) {throw new RuntimeException("Error deserializing.", e);}
	}
	
	/**Deserialize a tile that is encoded as an set of aggregates.
	 * 
	 * TODO: Remove when tiles include bounds metadata  (also remove dependency fetcher from download)
	 * **/
	@SuppressWarnings("unchecked")
	public static <A> Aggregates<A> deserializeTile(
			String filename, Valuer<GenericRecord, A> converter, 
			int lowX, int lowY, int highX, int highY) {
		DatumReader<GenericRecord> dr = new GenericDatumReader<GenericRecord>();
		try (InputStream stream = new FileInputStream(filename);
			 DataFileStream<GenericRecord> fr =new DataFileStream<GenericRecord>(stream, dr)) {
			
			GenericRecord r = fr.next();
			A defVal = converter.value((GenericRecord) r.get("default"));			
			GenericData.Array<GenericData.Array<GenericRecord>> rows = 
					(GenericData.Array<GenericData.Array<GenericRecord>>) r.get("values");

			Aggregates<A> aggs = AggregateUtils.make(lowX, lowY, highX, highY, defVal);
			for (int row=0; row<rows.size(); row++) {
				int x = row+aggs.lowX();
				GenericData.Array<GenericRecord> cols = rows.get(row);
				for (int col=0; col<cols.size(); col++){
					int y=col+aggs.lowY();
					GenericRecord val = cols.get(col);
					aggs.set(x, y, converter.value(val));
				}
			}
			fr.close();
			return aggs;
		} catch (IOException e) {throw new RuntimeException("Error deserializing.", e);}
	}

	
}