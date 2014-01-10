package ar.ext.avro;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.*;
import org.apache.avro.io.*;

import ar.Glyph;
import ar.Glyphset;
import ar.glyphsets.GlyphList;
import ar.glyphsets.WrappedCollection;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;

/**Utilities for interacting with Avro**/
public class GlyphsetTools {
	/**Wrap an GenericRecord as a ImplicitGemoetry.Indexed item for use
	 * with other implicit geometry tools using Indexed.
	 */
	public static class IndexedRecord implements Indexed {
		private static final long serialVersionUID = -3579436222005581302L;
		private final GenericRecord r;
		public IndexedRecord(GenericRecord r) {this.r=r;}
		public Object get(int f) {return r.get(f);}
	}
	
	/**Internal utility for seting up an avro reader.**/
	private static DataFileReader<GenericRecord> reader(File source) throws IOException {
		DatumReader<GenericRecord> dr = new GenericDatumReader<GenericRecord>();
		DataFileReader<GenericRecord> fr =new DataFileReader<GenericRecord>(source, dr);
		return fr;
	}
	
	/**Read an avro file into a glyphset.
	 * All items from the file will be read and converted into glyph objects,
	 * so the data is "fully loaded" in that all load-related computation is done.
	 * 
	 * @param source Avro file
	 * @param glypher Converter from generic-record to a glyph-derived class
	 * @throws IOException
	 */
	public static <G,V> Glyphset.RandomAccess<G,V> fullLoad(File source, Valuer<GenericRecord,Glyph<G,V>> glypher) throws IOException {
		DataFileReader<GenericRecord> reader = reader(source); 
		GlyphList<G,V> l = new GlyphList<>();
		for (GenericRecord r: reader) {l.add(glypher.value(r));}
		return l;
	}
	
	/**Read an avro file into a set of generic-records and produce a glyphset.
	 * Conversion into a glyphs is deferred until access time (via the passed shaper and valuer).
	 * The deferred conversion is based on the implicit geometry system and follows
	 * the semantics of the wrapped-collection glyphset.
	 * 
	 * @param source Avro file
	 * @param realizer Converter from generic-record to an value for use in shaper and valuer
	 * @param shaper Used to eventually convert the generic-record into geometry
	 * @param valuer Used ot eventually convert the generic-record into a value for a glyph
	 * @throws IOException
	 */
	public static <G,V,INNER> Glyphset<G,V> wrappedLoad(
			File source, 
			Valuer<GenericRecord,INNER> realizer,
			Shaper<G,INNER> shaper, 
			Valuer<INNER, V> valuer) throws IOException {
		
		DataFileReader<GenericRecord> reader = reader(source); 
		ArrayList<INNER> l = new ArrayList<INNER>();
		for (GenericRecord r: reader) {l.add(realizer.value(r));}
		return new WrappedCollection.List<INNER, G, V>(l, shaper, valuer);
	}
}
