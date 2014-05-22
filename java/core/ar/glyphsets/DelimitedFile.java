package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.ForkJoinPool;

import ar.Glyph;
import ar.Glyphset;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Indexed.Converter;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;

/**Given a file with line-oriented, regular-expression delimited values,
 * provides a list-like (read-only) interface.
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

	private final Shaper<Indexed,G> shaper;
	private final Valuer<Indexed,I> valuer;

	///Cached items.
	private long size =-1;
	private Rectangle2D bounds;

		
	public DelimitedFile(File source, char delimiter, Converter.TYPE[] types, Shaper<Indexed,G> shaper, Valuer<Indexed, I> valuer) {this(source, delimiter, types, DEFAULT_SKIP, shaper, valuer);}
	public DelimitedFile(File source, char delimiter, Converter.TYPE[] types, int skip, Shaper<Indexed,G> shaper, Valuer<Indexed, I> valuer) {this(source, delimiter, types, skip, shaper, valuer, 0, -1);}
	public DelimitedFile(File source, char delimiter, Converter.TYPE[] types, int skip, Shaper<Indexed,G> shaper, Valuer<Indexed, I> valuer, long segStart, long segEnd) {
		this.source = source;
		this.delimiter = delimiter;
		this.types = types;
		this.skip = skip;
		this.shaper = shaper;
		this.valuer = valuer;
		this.segStart = segStart;
		this.segEnd = segEnd;
	}

	

	
	@Override
	public Rectangle2D bounds() {
		if (bounds == null) {
			bounds = bounds(2);
		}
		return bounds;
	}
	
	
	public Rectangle2D bounds(int m) {
		int procs = Runtime.getRuntime().availableProcessors();
		ForkJoinPool pool = new ForkJoinPool(procs);
		bounds = pool.invoke(new BoundsTask<>(this, m*procs));
		return bounds;
	}
		
	@Override
	public Glyphset<G, I> segmentAt(int count, int segId) throws IllegalArgumentException {
		long stride = (source.length()/count)+1; //+1 for the round-down
		long low = stride*segId;
		long high = Math.min(low+stride, source.length());

		return new DelimitedFile<>(source, delimiter, types, skip, shaper, valuer, low, high);
	}
	
	@Override public boolean isEmpty() {return size() == 0;}
	@Override public Iterator iterator() {return new Iterator();}

	@Override
	public long size() {
		if (size <0) {
			try (BufferedReader r = new BufferedReader(new FileReader(source))) {
				size=0;
				while(r.readLine() != null) {size++;}
			} catch (IOException e) {
				throw new RuntimeException("Error processing file: " + source.getName());
			}
		}
		size = size-skip;
		return size;
	}
	
	/**Utility for converting a given string (i.e., line of a file) into an 'Indexed' instance.**/
	public static Indexed asIndexed(String line, char split, Converter conv) {
		String[] parts = new String[conv.size()];
		int low = 0;
		int high = line.indexOf(split);
		int i=0;
		while (high > 0) {
			parts[i++] = line.substring(low, high);
			low = high+1;
			high = line.indexOf(split, low);
		}
		return conv.applyTo(new Indexed.ArrayWrapper(parts));
	}
	
	private final class Iterator implements java.util.Iterator<Glyph<G,I>> {
		private final Converter conv = new Converter(types);
		private final BufferedReader reader;

		private long charsRead;
		private String cache;
		private boolean closed = false;
		
		
		public Iterator() {
			try {
				reader = new BufferedReader(new FileReader(source));
				
				//Get to the first record-start in the segment
				reader.skip(segStart);

				if (segStart == 0) {
					for (long i=skip; i>0; i--) {reader.readLine();}					
				} else {
					reader.readLine();
				}
			} catch (IOException e) {
				throw new RuntimeException("Error initializing iterator for " + source.getName(), e);
			}
		}
		
		@Override
		protected void finalize() {
			try {if (!closed) {reader.close();}}
			catch (IOException e) {e.printStackTrace();}
		}
		
		@Override
		public boolean hasNext() {
			if (!closed && cache == null) {
				try {
					if (segEnd > 0 && charsRead > segEnd) {
						reader.close();
						closed = true;
						return false;
					}
					cache = reader.readLine();
					if (cache == null) {return false;}
					
					charsRead += cache.length();
				} catch (IOException e) {throw new RuntimeException("Error processing file: " + source.getName());}
			}
			return cache != null;
		}

		
		@Override
		public Glyph<G,I> next() {
			if (cache == null && !hasNext()) {throw new NoSuchElementException();}
			//System.out.printf("Processed %d%n", byteOffset);

			String line = cache;
			cache = null;
			Indexed base = asIndexed(line, delimiter, conv);
			return new SimpleGlyph<>(shaper.shape(base), valuer.value(base));
		}

		@Override public void remove() {throw new UnsupportedOperationException();}
	}
}
