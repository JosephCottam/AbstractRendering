package ar.ext.avro;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import ar.glyphsets.implicitgeometry.Valuer;
import ar.rules.CategoricalCounts;

public class Converters {
	public static class ToCount implements Valuer<GenericRecord, Integer> {
		public Integer value(GenericRecord from) {
			if (from == null) {return 0;}
			else {return (Integer) from.get(0);}
		}
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
	 * 
	 * TODO: Expand to be general CategoricalCounts
	 *   
	 */	
	public static class ToRLE implements Valuer<GenericRecord, CategoricalCounts.RLE> {
		public CategoricalCounts.RLE value(GenericRecord from) {
			CategoricalCounts.RLE rle = new CategoricalCounts.RLE();
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
	 * TODO: Expand to be general CategoricalCounts
	 *
	 */
	public static class FromRLE implements Valuer<CategoricalCounts.RLE, GenericRecord> {
		private final Schema schema;
		public FromRLE(Schema s) {this.schema = s;}
		public GenericRecord value(CategoricalCounts.RLE from) {
			GenericRecord r = new GenericData.Record(schema);
			List<String> keys = new ArrayList<String>();
			for (Object k: from.keys) {keys.add(k.toString());}
			r.put("keys", keys);
			r.put("counts", from.counts);
			return r;
		}
	}
	
	public static class ToColor implements Valuer<GenericRecord, Color> {
		public Color value(GenericRecord from) {
			List<Integer> vals = (List<Integer>) from.get("RGBA");
			return new Color(vals.get(0), vals.get(1),vals.get(2), vals.get(3));
		}
	}
	
	public static class FromColor implements Valuer<Color, GenericRecord> {
		private final Schema schema;
		public FromColor(Schema s) {this.schema = s;}
		public GenericRecord value(Color from) {
			GenericRecord r = new GenericData.Record(schema);
			r.put("RGBA", Arrays.asList(new Integer[]{from.getRed(), from.getGreen(), from.getBlue(), from.getAlpha()}));
			return r;
		}
		
	}

}
