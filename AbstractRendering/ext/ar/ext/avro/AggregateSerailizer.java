package ar.ext.avro;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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
import ar.rules.Aggregators.RLE;

public class AggregateSerailizer {
	public static final String AGGREGATES_SCHEMA ="ar/ext/avro/aggregates.avsc";
	public static final String COUNTS_SCHEMA="ar/ext/avro/counts.avsc";
	public static final String RLE_SCHEMA="ar/ext/avro/rle.avsc";
	
	/**Project an absolute x and y into a flat idx that is dense in the sub-region described by aggs.
	 * No bounds checking is done, so x and y are assumed to lie inside of aggs' area of concern.
	 * **/
	private static int getIdx(int x, int y, Aggregates<?> aggs) {
		return ((aggs.highY()-aggs.lowY())*(x-aggs.lowX()))+(y-aggs.lowY());
	}

	private static Point fromIdx(int idx, Aggregates<?> aggs) {
		int xspan = aggs.highX()-aggs.lowX();
		int yspan = aggs.highY()-aggs.lowY();
		int row = idx/xspan;
		int col = idx%yspan;
		return new Point(row+aggs.lowX(), col+aggs.lowY());
	}

	
	private static int arraySize(Aggregates<?> aggs) {
		int xspan = aggs.highX()-aggs.lowX();
		int yspan = aggs.highY()-aggs.lowY();
		return xspan*yspan;
	}
	
	
	/**Common aggregates serialization code.
	 * Note: Values must be either a collection or a reference-type array (sorry, no primitive arrays)
	 */
	private static void serializeContainer(Aggregates<?> aggs, String targetName, Schema schema, Object defVal, Object values) {
		if (values.getClass().isArray()) {values = Arrays.asList((Object[]) values);}
		GenericRecord aggregates = new GenericData.Record(schema);
		aggregates.put("lowX", aggs.lowX());
		aggregates.put("lowY", aggs.lowY());
		aggregates.put("highX", aggs.highX());
		aggregates.put("highY", aggs.highY());
		aggregates.put("default", defVal);
		aggregates.put("values", values);
		
				
		DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<GenericRecord>(schema);
		DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<GenericRecord>(datumWriter);
		try {
			dataFileWriter.create(schema, new File(targetName));
			dataFileWriter.append(aggregates);
			dataFileWriter.close();
		} catch (IOException e) {throw new RuntimeException("Error serializing",e);}		
	}
	
	public static void serializeCounts(Aggregates<Integer> aggs, String targetName) {
		Schema fullSchema, itemSchema;
		try {
			SchemaResolver resolver =new SchemaResolver().loadSchema(COUNTS_SCHEMA);
			itemSchema = resolver.resolve();
			fullSchema = resolver.loadSchema(AGGREGATES_SCHEMA).resolve();
		} catch (IOException e) {throw new RuntimeException("Error serailziing", e);}

		GenericRecord[] counts = new GenericRecord[arraySize(aggs)];

		for (int x=aggs.lowX(); x<aggs.highX(); x++) {
			for (int y=aggs.lowY(); y<aggs.highY(); y++) {
				GenericRecord val = new GenericData.Record(itemSchema);
				val.put(0, aggs.at(x,y));
				counts[getIdx(x,y,aggs)] = val;
			}
		}
		GenericRecord defVal = new GenericData.Record(itemSchema);
		defVal.put(0, aggs.defaultValue());
		
		serializeContainer(aggs, targetName, fullSchema, defVal, counts);
	}
	
	public static void serializeRLE(Aggregates<RLE> aggs, String targetName) {
		Schema schema;
		try {
			schema = new SchemaResolver().loadSchema(RLE_SCHEMA).loadSchema(AGGREGATES_SCHEMA).resolve();
		} catch (IOException e) {throw new RuntimeException("Error serailziing", e);}
		serializeContainer(aggs, targetName, schema, null, new Object[0]);
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
			A defVal =  converter.value((GenericRecord) r.get("default"));
			Aggregates<A> aggs = new FlatAggregates<>(lowX, lowY, highX, highY, defVal);
			GenericData.Array<A> values = (GenericData.Array<A>) r.get("values");
			for (int i=0; i<values.size(); i++) {
				Point loc = fromIdx(i, aggs);
				Object val = values.get(i);
				aggs.set(loc.x, loc.y, converter.value((GenericRecord) val));
			}
			
			fr.close();
			return aggs;
		} catch (IOException e) {throw new RuntimeException("Error deserializing.", e);}

	}
}
