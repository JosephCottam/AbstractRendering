package ar.ext.textfile;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import ar.Glyph;
import ar.Glyphset;
import ar.glyphsets.SimpleGlyph;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Indexed.Converter;
import ar.util.Util;
import ar.util.axis.Axis;
import ar.util.axis.DescriptorPair;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

/**Given a file with line-oriented, regular-expression delimited values,
 * provides a list-like (read-only) interface.
 * 
 * TODO: Replace 'Indexed' and 'Converter' to a RecordCreator of some sort.  The default one is essentially Converter, but sometimes that cost does not need to paid  
 */
public class DelimitedFile<G,I> implements Glyphset<G,I> {
	/**Number of lines to skip by default.  Captured at object creation time.**/
	public static int DEFAULT_SKIP =0;
	
	/**Source file.**/
	private final File source;
	
	/**Segment information for subsets, in terms of file bytes**/
	private final long segStart;
	private final long segEnd;
	
	/**Character used to delimit fields of the rows.**/
	private final char delimiter;
	
	/**Types of the fields.**/
	private final Converter.TYPE[] types;
	
	/**Number of lines to skip at the start of the file.**/
	private final int skip;

	private final Function<Indexed,G> shaper;
	private final Function<Indexed,I> valuer;

	///Cached items.
	private long size =-1;
	private Rectangle2D bounds;

	/**When true, only reports errors to standard error.
	 * When false (default), throws exceptions.  
	 */
	private final boolean report_only; 
	
		
	/**
	 * @param source File to pull from 
	 * @param delimiter Field delimiter
	 * @param types List of field types
	 * @param shaper 
	 * @param valuer
	 */
	public DelimitedFile(File source, char delimiter, Converter.TYPE[] types, Function<Indexed,G> shaper, Function<Indexed, I> valuer) {this(source, delimiter, types, DEFAULT_SKIP, shaper, valuer);}
	public DelimitedFile(File source, char delimiter, Converter.TYPE[] types, int skip, Function<Indexed,G> shaper, Function<Indexed, I> valuer) {this(source, delimiter, types, skip, shaper, valuer, 0, -1, false);}
	public DelimitedFile(File source, char delimiter, Converter.TYPE[] types, int skip, Function<Indexed,G> shaper, Function<Indexed, I> valuer, long segStart, long segEnd, boolean report_only) {
		this.source = source;
		this.delimiter = delimiter;
		this.types = types;
		this.skip = skip;
		this.shaper = shaper;
		this.valuer = valuer;
		this.segStart = segStart;
		this.segEnd = segEnd;
		this.report_only = report_only;
	}

	/**Get an instance with a (potentially) different error response style.**/
	public DelimitedFile<G,I> reportOnly(boolean report) {
		if (report_only == report) {return this;}
		return new DelimitedFile<>(source, delimiter, types, skip, shaper, valuer, segStart, segEnd, report);
	}

	@Override
	public Rectangle2D bounds() {
		if (segStart ==0 && segEnd == -1) {bounds = Util.bounds(this);}	//Only call this parallel version if it is the full file...
		else {bounds = Util.bounds(this.iterator());}
		return bounds;
	}
		
	@Override
	public List<Glyphset<G, I>> segment(int count) {
		long stride = (source.length()/count)+1; //+1 for the round-down
		List<Glyphset<G,I>> segments = new ArrayList<>();
		for (int segId=0; segId<count; segId++) {
			long low = stride*segId;
			long high = segId == count-1 ? size() : Math.min(low+stride, size());
			segments.add(new DelimitedFile<>(source, delimiter, types, skip, shaper, valuer, low, high, report_only));
		}
		return segments;
	}
	
	@Override public boolean isEmpty() {return source.length() == 0;}
	@Override public Iterator iterator() {
		return new Iterator();
	}

	@Override
	public long size() {
		if (size <0) {
			try (BufferedReader r = new BufferedReader(new FileReader(source))) {
				size=0;
				while(r.readLine() != null) {size++;}
			} catch (IOException e) {
				throw new RuntimeException("Error processing file: " + source.getName(), e);
			}
		}
		size = size-skip;
		return size;
	}
	
	private final class Iterator implements java.util.Iterator<Glyph<G,I>> {
		private final Converter conv = new Converter(types);
		private final CsvListReader base;
		private int charsRead;
		private Glyph<G,I> cached;
		
		public Iterator() {
			try {
				FileReader core = new FileReader(source);
				//Get to the first record-start in the segment
				core.skip(segStart);
				CsvPreference pref = new CsvPreference.Builder('"', delimiter, CsvPreference.STANDARD_PREFERENCE.getEndOfLineSymbols()).build();
				base = new CsvListReader(core, pref);

				//Get to the first full record in the segment
				if (segStart == 0) {
					for (long i=skip; i>0; i--) {base.read();}					
				} else {
					base.read();
				}
			} catch (IOException e) {
				throw new RuntimeException("Error initializing iterator for " + source.getName(), e);
			}
		}
		
		@Override
		protected void finalize() {
			try {if (base != null) {base.close();}}
			catch (IOException e) {e.printStackTrace();}
		}
		
		@Override 
		public boolean hasNext() {
			cacheNext();
			return cached != null;
		}
		
		@Override
		public Glyph<G,I> next() {
			cacheNext();
			Glyph<G,I> next = this.cached;
			cached = null;
			return next;
		}
		
		private void cacheNext() {
			if (cached != null) {return;}
			if (segEnd > 0 && charsRead > segEnd) {return;}
			
			List<String> next = null;
			try {next = base.read();}
			catch (IOException e) {
				String msg = String.format("Error reading around character %d of %s.", charsRead, source.getName());
				if (report_only) {System.err.println(msg); System.err.println(e.getMessage());}
				else {throw new RuntimeException(msg, e);}
			}
			if (next == null) {return;}
			
			try {
				for (String s: next) {charsRead += s==null ? 0 : s.length();} //TODO: Probably not the fastest way to do this...
				Indexed base = conv.apply(new Indexed.ListWrapper(next));
				cached = new SimpleGlyph<>(shaper.apply(base), valuer.apply(base));
			} catch (Exception e) {
				String msg = String.format("Error constructing glyph around character %d of %s", charsRead, source.getName());
				if (report_only) {System.err.println(msg); System.err.println(e.getMessage());}
				else {throw new RuntimeException(msg, e);}
				cacheNext();
			}
		}

		@Override public void remove() {throw new UnsupportedOperationException();}
	}
	
	private DescriptorPair<?,?> axisDescriptor;
	@Override public DescriptorPair<?,?> axisDescriptors() {return axisDescriptor != null ? axisDescriptor : Axis.coordinantDescriptors(this);}
	@Override public void axisDescriptors(DescriptorPair<?,?> descriptor) {this.axisDescriptor = descriptor;} 
}
