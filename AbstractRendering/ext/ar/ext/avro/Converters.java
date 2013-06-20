package ar.ext.avro;

import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import ar.glyphsets.implicitgeometry.Valuer;
import ar.rules.Aggregators.RLE;

public class Converters {
	public static class ToCount implements Valuer<GenericRecord, Integer> {
		public Integer value(GenericRecord from) {return (Integer) from.get(0);}
	}
	
	public static class FromCount implements Valuer<Integer, GenericRecord> {
		private final Schema schema;
		public FromCount(Schema s) {this.schema = s;}
		public GenericRecord value(Integer from) {
			GenericRecord r = new GenericData.Record(schema);
			r.put("value", from);
			return r;
		}
	}

	
	public static class ToRLE implements Valuer<GenericRecord, RLE> {
		public RLE value(GenericRecord from) {
			RLE rle = new RLE();
			rle.keys.addAll((List<Object>) from.get("keys"));
			rle.counts.addAll((List<Integer>) from.get("counts"));
			return rle;
		}
	}
	
	public static class FromRLE implements Valuer<RLE, GenericRecord> {
		private final Schema schema;
		public FromRLE(Schema s) {this.schema = s;}
		public GenericRecord value(RLE from) {
			GenericRecord r = new GenericData.Record(schema);
			r.put("keys", from.keys);
			r.put("counts", from.counts);
			return r;
		}
	}

}
