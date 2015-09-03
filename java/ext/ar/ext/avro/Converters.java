package ar.ext.avro;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import ar.Aggregator;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.rules.CategoricalCounts;
import ar.rules.Categories;
import ar.rules.Numbers;
import ar.util.Util;

public class Converters {
	
	/**Get a deserializing valuer based on the passed aggregator.*/
	//TODO: Make more general...
	@SuppressWarnings("unchecked")
	public static <A> Valuer<GenericRecord, A> getDeserialize(Aggregator<?,A> aggregator, Function<String, A> fn) {
		if (aggregator instanceof Numbers.Count<?>) {
			return (Valuer<GenericRecord, A>) new ToCount();
		} else if (aggregator instanceof Categories.CountCategories<?>
			|| aggregator instanceof Categories.MergeCategories<?>) {
			if (fn == null) {return (Valuer<GenericRecord, A>) new ToCoCString();}
			else {return (Valuer<GenericRecord, A>) new ToCoC(fn);}
		} else {
			throw new IllegalArgumentException("No converter known for aggregator " + aggregator.toString());
		}
	}
	
	public static class ToCount implements Valuer<GenericRecord, Integer> {
		private static final long serialVersionUID = 1015273131104304754L;

		public Integer apply(GenericRecord from) {
			if (from == null) {return 0;}
			else {return (Integer) from.get(0);}
		}
	}
	
	public static class FromCount implements Valuer<Integer, GenericRecord> {
		private static final long serialVersionUID = -6907147121637279770L;
		private final Schema schema;
		
		public FromCount(Schema s) {this.schema = s;}
		
		public GenericRecord apply(Integer from) {
			GenericRecord r = new GenericData.Record(schema);
			r.put("value", from);
			return r;
		}
	}

	/**Generic deserialization for CoC (category labels are strings).*/	
	public static class ToCoCString implements Valuer<GenericRecord, CategoricalCounts<String>> {
		private static final long serialVersionUID = 2979290290172689482L;
		private static final Comparator<String> COMP = new Util.ComparableComparator<>();

		public CategoricalCounts<String> apply(GenericRecord from) {
			List<?> ks = (List<?>) from.get("keys");
			@SuppressWarnings("unchecked")
			List<Integer> vs = (List<Integer>) from.get("counts");
			List<String> keys = new ArrayList<>();
			for (int i=0; i < ks.size(); i++) {
				keys.add(ks.get(i).toString());
			}
			
			return CategoricalCounts.make(keys, vs, COMP);			
		}
	}
	
	/**CoC deserialization that converts categories by some function.*/	
	public static class ToCoC<A extends Comparable<A>> implements Valuer<GenericRecord, CategoricalCounts<A>> {
		private static final long serialVersionUID = 2979290290172689482L;
		private final Comparator<A> COMP = new Util.ComparableComparator<>();
		private final Function<String, A> fn;
		
		public ToCoC(Function<String, A> fn) {this.fn = fn;}
		
		public CategoricalCounts<A> apply(GenericRecord from) {
			List<?> ks = (List<?>) from.get("keys");
			@SuppressWarnings("unchecked")
			List<Integer> vs = (List<Integer>) from.get("counts");
			List<A> keys = new ArrayList<>();
			for (int i=0; i < ks.size(); i++) {
				keys.add(fn.apply(ks.get(i).toString()));
			}
			
			return CategoricalCounts.make(keys, vs, COMP);			
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
		
		public GenericRecord apply(CategoricalCounts<T> from) {
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

		public Color apply(GenericRecord from) {
			@SuppressWarnings("unchecked")
			List<Integer> vals = (List<Integer>) from.get("RGBA");
			return new Color(vals.get(0), vals.get(1),vals.get(2), vals.get(3));
		}
	}
	
	public static class FromColor implements Valuer<Color, GenericRecord> {
		private static final long serialVersionUID = 6171526106939487458L;
		private final Schema schema;
		
		public FromColor(Schema s) {this.schema = s;}
		
		public GenericRecord apply(Color from) {
			GenericRecord r = new GenericData.Record(schema);
			r.put("RGBA", Arrays.asList(new Integer[]{from.getRed(), from.getGreen(), from.getBlue(), from.getAlpha()}));
			return r;
		}
		
	}

}
