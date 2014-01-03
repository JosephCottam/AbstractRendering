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
		private static final long serialVersionUID = 1015273131104304754L;

		public Integer value(GenericRecord from) {
			if (from == null) {return 0;}
			else {return (Integer) from.get(0);}
		}
	}
	
	public static class FromCount implements Valuer<Integer, GenericRecord> {
		private static final long serialVersionUID = -6907147121637279770L;
		private final Schema schema;
		public FromCount(Schema s) {this.schema = s;}
		public GenericRecord value(Integer from) {
			GenericRecord r = new GenericData.Record(schema);
			r.put("value", from);
			return r;
		}
	}

	/**Generic deserialization for CoC.
	 * Keys are kept as strings.
	 */	
	public static class ToCoC implements Valuer<GenericRecord, CategoricalCounts<String>> {
		private static final long serialVersionUID = 2979290290172689482L;

		public CategoricalCounts<String> value(GenericRecord from) {
			List<?> ks = (List<?>) from.get("keys");
			@SuppressWarnings("unchecked")
			List<Integer> vs = (List<Integer>) from.get("counts");
			List<String> keys = new ArrayList<String>();
			for (int i=0; i < ks.size(); i++) {
				keys.add(ks.get(i).toString());
			}
			
			return CategoricalCounts.make(keys, vs);			
		}
	}
	
	/**Generic serialization for CoC.  
	 * 
	 * Can only safely handle categories that are isomorphic to their toString
	 * since the CoC schema uses strings as keys.
	 */
	public static class FromCoC<T> implements Valuer<CategoricalCounts<T>, GenericRecord> {
		private static final long serialVersionUID = 6201382979970104470L;
		private final Schema schema;
		public FromCoC(Schema s) {this.schema = s;}
		public GenericRecord value(CategoricalCounts<T> from) {
			GenericRecord r = new GenericData.Record(schema);
			List<String> keys = new ArrayList<String>();
			List<Integer> counts = new ArrayList<Integer>();
			for (int i=0; i<from.size();i++) {
				keys.add(from.key(i).toString());
				counts.add(from.count(i));
			}
			r.put("keys", keys);
			r.put("counts", counts);
			return r;
		}
	}
	
	public static class ToColor implements Valuer<GenericRecord, Color> {
		private static final long serialVersionUID = -5984313789299890307L;

		public Color value(GenericRecord from) {
			@SuppressWarnings("unchecked")
			List<Integer> vals = (List<Integer>) from.get("RGBA");
			return new Color(vals.get(0), vals.get(1),vals.get(2), vals.get(3));
		}
	}
	
	public static class FromColor implements Valuer<Color, GenericRecord> {
		private static final long serialVersionUID = 6171526106939487458L;
		private final Schema schema;
		public FromColor(Schema s) {this.schema = s;}
		public GenericRecord value(Color from) {
			GenericRecord r = new GenericData.Record(schema);
			r.put("RGBA", Arrays.asList(new Integer[]{from.getRed(), from.getGreen(), from.getBlue(), from.getAlpha()}));
			return r;
		}
		
	}

}
