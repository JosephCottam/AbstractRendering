package ar.ext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.avro.*;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.*;
import org.apache.avro.io.*;

import ar.Glyphset;
import ar.Glyphset.Glyph;
import ar.glyphsets.GlyphList;
import ar.glyphsets.ImplicitGeometry;
import ar.glyphsets.WrappedCollection;

/**Utilities for interacting with Avro**/
public class Avro {
	/**Wrap an GenericRecord as a ImplicitGemoetry.Indexed item for use
	 * with other implicit geometry tools using Indexed.
	 */
	public static class IndexedRecord implements ImplicitGeometry.Indexed {
		private final GenericRecord r;
		public IndexedRecord(GenericRecord r) {this.r=r;}
		public Object get(int f) {return r.get(f);}
	}
	
	/**Internal utility for seting up an avro reader.**/
	private static DataFileReader<GenericRecord> reader(String sourceFile) throws IOException {
		File source = new File(sourceFile);
		DatumReader<GenericRecord> dr = new GenericDatumReader<>();
		DataFileReader<GenericRecord> fr =new DataFileReader<>(source, dr);
		return fr;
	}
	
	/**Unpack a generic-record into a concrete class.
	 * TODO: Can this be merged with the ImplicitGeometry system?
	 * **/
	public static interface Realizer<A> {
		public A wrap(GenericRecord r);
		
		public static final class ID implements Realizer<GenericRecord> {
			public GenericRecord wrap(GenericRecord r) {return r;}
		}
	}
	
	/**Read an avro file into a glyphset.
	 * All items from the file will be read and converted into glyph objects,
	 * so the data is "fully loaded" in that all load-related computation is done.
	 * 
	 * @param sourceFile Name of the avro file
	 * @param glypher Converter from generic-record to a glyph-derived class
	 * @return
	 * @throws IOException
	 */
	public static <A extends Glyph<V>,V> Glyphset.RandomAccess<V> fullLoad(String sourceFile, Realizer<A> glypher) throws IOException {
		DataFileReader<GenericRecord> reader = reader(sourceFile); 
		GlyphList<V> l = new GlyphList<>();
		for (GenericRecord r: reader) {l.add(glypher.wrap(r));}
		return l;
	}
	
	/**Read an avro file into a set of generic-records and produce a glyphset.
	 * Conversion into a glyphs is deferred until access time (via the passed shaper and valuer).
	 * The deferred conversion is based on the implicit geometry system and follows
	 * the semantics of the wrapped-collection glyphset.
	 * 
	 * @param sourceFile Name of the avro file
	 * @param realizer Converter from generic-record to an value for use in shaper and valuer
	 * @param shaper Used to eventually convert the generic-record into geometry
	 * @param valuer Used ot eventually convert the generic-record into a value for a glyph
	 * @return
	 * @throws IOException
	 */
	public static <A extends Glyph<V>,V,INNER> Glyphset<V> wrappedLoad(String sourceFile, Realizer<INNER> realizer,
			ImplicitGeometry.Shaper<INNER> shaper, ImplicitGeometry.Valuer<INNER, V> valuer) throws IOException {
		DataFileReader<GenericRecord> reader = reader(sourceFile); 
		ArrayList<INNER> l = new ArrayList<>();
		for (GenericRecord r: reader) {l.add(realizer.wrap(r));}
		return new WrappedCollection.List<>(l, shaper, valuer);
	}
}
