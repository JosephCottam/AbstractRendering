package ar.ext.avro;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
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

public class AggregateSerailizer {
	public static final Schema COUNT_SCHEMA;
	static {
		InputStream countSchema;
		Schema schema; 
		try {
			countSchema = AggregateSerailizer.class.getClassLoader().getResourceAsStream("ar/ext/avro/counts.avsc");
			schema = new Schema.Parser().parse(countSchema);
		} catch (IOException e) {
			e.printStackTrace();
			schema = null;
		}
		COUNT_SCHEMA = schema;
	}
	
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
	
	public static void serializeCounts(Aggregates<Integer> aggs, String targetName) {
		Integer[] counts = new Integer[arraySize(aggs)];

		for (int x=aggs.lowX(); x<aggs.highX(); x++) {
			for (int y=aggs.lowY(); y<aggs.highY(); y++) {
				int val = aggs.at(x, y);
				counts[getIdx(x,y,aggs)] = val; 
			}
		}
		
		GenericRecord aggregates = new GenericData.Record(COUNT_SCHEMA);
		aggregates.put("lowX", aggs.lowX());
		aggregates.put("lowY", aggs.lowY());
		aggregates.put("highX", aggs.highX());
		aggregates.put("highY", aggs.highY());
		aggregates.put("default", aggs.defaultValue());
		aggregates.put("values", Arrays.asList(counts));
		
				
		DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<GenericRecord>(COUNT_SCHEMA);
		DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<GenericRecord>(datumWriter);
		try {
			dataFileWriter.create(COUNT_SCHEMA, new File(targetName));
			dataFileWriter.append(aggregates);
			dataFileWriter.close();
		} catch (IOException e) {throw new RuntimeException("Error serializing",e);}
	}
	
	
	/**Read a set of aggregates from a disk.  Only works for primitive aggregates.**/ 
	public static <A> Aggregates<A> deserialize(String sourceName, Class<A> type) {
		File source = new File(sourceName);
		DatumReader<GenericRecord> dr = new GenericDatumReader<>();
		try {
			DataFileReader<GenericRecord> fr =new DataFileReader<>(source, dr);
			GenericRecord r = fr.next();
			
			int lowX = (Integer) r.get("lowX");
			int lowY = (Integer) r.get("lowY");
			int highX = (Integer) r.get("highX");
			int highY = (Integer) r.get("highY");
			A defVal =  type.cast(r.get("default"));
			Aggregates<A> aggs = new FlatAggregates<>(lowX, lowY, highX, highY, defVal);
			GenericData.Array<A> values = (GenericData.Array<A>) r.get("values");
			for (int i=0; i<values.size(); i++) {
				Point loc = fromIdx(i, aggs);
				Object val = values.get(i);
				aggs.set(loc.x, loc.y, type.cast(val));
			}
			
			fr.close();
			return aggs;
		} catch (IOException e) {throw new RuntimeException("Error deserializing.", e);}

	}
}
