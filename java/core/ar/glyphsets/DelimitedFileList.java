package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.ForkJoinPool;

import ar.Glyph;
import ar.Glyphset;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Indexed.Converter;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;

/**Given a file with line-oriented, regular-expression delimited values,
 * provides a list-like (read-only) interface.
 * 
 * @author josephcottam
 *
 *
 * NOT THREAD SAFE!!!!
 * TODO: Implement Glyphset instead of List<Indexed>...
 */
public class DelimitedFileList<G,I> implements Glyphset<G,I> {
	public static int DEFAULT_SKIP =0;

	
	/**Segmenting is derived from the number of bytes,
	 * but since this is not a fixed record-size format this can't be
	 * exact.  The SEGMENT_FACTOR is used to make segments larger,
	 * and thus divide work into larger blocks.
	 */
	private static final int SEGMENT_FACTOR = 20;   
	
	/**Source file.**/
	private final File source;
	
	/**Segment information for subsets.**/
	private final long segStart;
	private final long segEnd;
	
	/**Pattern used to delimit fields of the rows.**/
	private final String delimiters;
	
	/**Types of the fields.**/
	private final Converter.TYPE[] types;
	
	/**Number of lines to skip at the start of the file.**/
	private final int skip;

	private final Shaper<Indexed,G> shaper;
	private final Valuer<Indexed,I> valuer;

	///Cached items.
	private long size;
	private Rectangle2D bounds;

		
	public DelimitedFileList(File source, String delimiters, Converter.TYPE[] types, Shaper<Indexed,G> shaper, Valuer<Indexed, I> valuer) {this(source, delimiters, types, DEFAULT_SKIP, shaper, valuer);}
	public DelimitedFileList(File source, String delimiters, Converter.TYPE[] types, int skip, Shaper<Indexed,G> shaper, Valuer<Indexed, I> valuer) {this(source, delimiters, types, skip, shaper, valuer, 0, -1);}
	public DelimitedFileList(File source, String delimiters, Converter.TYPE[] types, int skip, Shaper<Indexed,G> shaper, Valuer<Indexed, I> valuer, long segStart, long segEnd) {
		this.source = source;
		this.delimiters = delimiters;
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
			ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
			bounds = pool.invoke(new BoundsTask<>(this, 100000));
		}
		return bounds;
	}
	
	@Override
	public long segments() {
		long size = source.length();
		long segs = size/(types.length*SEGMENT_FACTOR); 	
		return segs;
	}
	
	@Override
	public Glyphset<G, I> segment(long bottom, long top) throws IllegalArgumentException {
		return new DelimitedFileList<>(source, delimiters, types, skip, shaper, valuer, bottom, top);
	}
	
	@Override public boolean isEmpty() {return size ==0;}
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
	
	private final class Iterator implements java.util.Iterator<Glyph<G,I>> {
		private final Converter conv = new Converter(types);
		private final RandomAccessFile base;
		private final long stop = segEnd * types.length * SEGMENT_FACTOR;

		private long byteOffset;
		private String cache;
		private boolean closed = false;
		
		
		public Iterator() {
			try {
				base = new RandomAccessFile(source,"r");
				
				//Read past header if this iterator is for the first segment
				for (long i=segStart-skip; i<0; i++) {base.readLine();}
				
				//Scan forward to the first full record in the assigned segments
				long start = segStart*types.length*SEGMENT_FACTOR;
				base.seek(start);
				base.readLine();
			} catch (IOException e) {
				throw new RuntimeException("Error initializing iterator for " + source.getName(), e);
			}
		}
		
		@Override
		public boolean hasNext() {
			if (!closed && cache == null) {
				try {
					if (stop > 0 && base.getFilePointer() > stop) {
						base.close();
						closed = true;
						return false;
					}
					cache = base.readLine();
					byteOffset = base.getFilePointer();
				} catch (IOException e) {throw new RuntimeException("Error processing file: " + source.getName());}
			}
			return cache != null;
		}

		@Override
		public Glyph<G,I> next() {
			if (cache == null && !hasNext()) {throw new NoSuchElementException();}
			//System.out.printf("Processed %d%n", byteOffset);

			StringTokenizer t = new StringTokenizer(cache, delimiters);
			cache = null;

			ArrayList<String> parts = new ArrayList<>();
			while (t.hasMoreTokens()) {parts.add(t.nextToken());}
			Indexed base = conv.applyTo(new Indexed.ListWrapper(parts));

			return new SimpleGlyph<>(shaper.shape(base), valuer.value(base));
		}

		@Override public void remove() {throw new UnsupportedOperationException();}
	}
}
