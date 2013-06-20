package ar.ext.avro;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;

import ar.Aggregates;
import ar.aggregates.FlatAggregates;
import ar.glyphsets.implicitgeometry.Valuer;

public class AggregateSerailizer {
	public static final String AGGREGATES_SCHEMA ="ar/ext/avro/aggregates.avsc";
	public static final String COUNTS_SCHEMA="ar/ext/avro/counts.avsc";
	public static final String RLE_SCHEMA="ar/ext/avro/rle.avsc";
	
	/**Common aggregates serialization code.
	 * Note: Values must be either a collection or a reference-type array (sorry, no primitive arrays)
	 */
	private static void serializeContainer(Aggregates<?> aggs, String targetName, Schema schema, GenericRecord defaultVal, List<List<GenericRecord>> values) {
		GenericRecord aggregates = new GenericData.Record(schema);
		aggregates.put("lowX", aggs.lowX());
		aggregates.put("lowY", aggs.lowY());
		aggregates.put("highX", aggs.highX());
		aggregates.put("highY", aggs.highY());
		aggregates.put("default", defaultVal);
		aggregates.put("values", values);
		
		DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<GenericRecord>(schema);
		DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<GenericRecord>(datumWriter);
		try {
			dataFileWriter.create(schema, new File(targetName));
			dataFileWriter.append(aggregates);
			dataFileWriter.close();
		} catch (IOException e) {throw new RuntimeException("Error serializing",e);}		
	}
	
	public static <A> void serialize(Aggregates<A> aggs, String targetName, Schema itemSchema, Valuer<A, GenericRecord> converter) throws IOException {
		Schema fullSchema = new SchemaResolver().addSchema(itemSchema).loadSchema(AGGREGATES_SCHEMA).resolve();

		List<List<GenericRecord>> records = new ArrayList<>();
		A defVal = aggs.defaultValue();
		GenericRecord defrec = converter.value(defVal);
		
		for (int x=aggs.lowX(); x<aggs.highX(); x++) {
			List<GenericRecord> row = new ArrayList<>();
			records.add(row);
			for (int y=aggs.lowY(); y<aggs.highY(); y++) {
				A val = aggs.at(x,y);
				//if (defVal == val || (defVal != null && defVal.equals(val))) {continue;}  TODO: Re-instate default-value occusion by making a union type with null...(maybe)
				GenericRecord vr = converter.value(val);
				row.add(vr);
			}
		}
		
		serializeContainer(aggs, targetName, fullSchema, defrec, records);
	}
	
	/**Read a set of aggregates from a disk.  Only works for primitive aggregates.**/ 
	public static <A> Aggregates<A> deserialize(String sourceName, Valuer<GenericRecord, A> converter) {
		File source = new File(sourceName);
		DatumReader<GenericRecord> dr = new GenericDatumReader<>();
		try {
			DataFileReader<GenericRecord> fr =new DataFileReader<>(source, dr);
			GenericRecord r = fr.next();
			
			int lowX = (Integer) r.get("lowX");
			int lowY = (Integer) r.get("lowY");
			int highX = (Integer) r.get("highX");
			int highY = (Integer) r.get("highY");
			A defVal = converter.value((GenericRecord) r.get("default"));			
			GenericData.Array<GenericData.Array<GenericRecord>> rows = (GenericData.Array<GenericData.Array<GenericRecord>>) r.get("values");

			Aggregates<A> aggs = new FlatAggregates<>(lowX, lowY, highX, highY, defVal);
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
