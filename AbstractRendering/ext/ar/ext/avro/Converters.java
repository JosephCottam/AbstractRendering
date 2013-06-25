package ar.ext.avro;

import java.util.ArrayList;
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

	/**Generic deserialization for RLE.
	 * Keys are kept as strings.  
	 */	
	public static class ToRLE implements Valuer<GenericRecord, RLE> {
		public RLE value(GenericRecord from) {
			RLE rle = new RLE();
			rle.keys.addAll((List<Object>) from.get("keys"));
			rle.counts.addAll((List<Integer>) from.get("counts"));
			return rle;
		}
	}
	/**Generic serialization for RLE.  
	 * 
	 * Can only safely handle categories that are isomorphic to their toString
	 * since the RLE schema uses strings as keys.
	 *
	 */
	public static class FromRLE implements Valuer<RLE, GenericRecord> {
		private final Schema schema;
		public FromRLE(Schema s) {this.schema = s;}
		public GenericRecord value(RLE from) {
			GenericRecord r = new GenericData.Record(schema);
			List<String> keys = new ArrayList<>();
			for (Object k: from.keys) {keys.add(k.toString());}
			r.put("keys", keys);
			r.put("counts", from.counts);
			return r;
		}
	}

}
