package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
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

	
	/**Source file.**/
	private final File source;
	
	/**Pattern used to delimit fields of the rows.**/
	private final String delimiters;
	
	/**Types of the fields.**/
	private final Converter.TYPE[] types;
	
	/**Number of lines to skip at the start of the file.**/
	private final int skip;

	protected final Shaper<Indexed,G> shaper;
	protected final Valuer<Indexed,I> valuer;

	///Cached items.
	private long size;
	private Rectangle2D bounds;

		
	public DelimitedFileList(File source, String delimiters, Converter.TYPE[] types, Shaper<Indexed,G> shaper, Valuer<Indexed, I> valuer) {this(source, delimiters, types, DEFAULT_SKIP, shaper, valuer);}
	public DelimitedFileList(File source, String delimiters, Converter.TYPE[] types, int skip, Shaper<Indexed,G> shaper, Valuer<Indexed, I> valuer) {
		this.source = source;
		this.delimiters = delimiters;
		this.types = types;
		this.skip = skip;
		this.shaper = shaper;
		this.valuer = valuer;
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
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public Glyphset<G, I> segment(long bottom, long top)
			throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
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
		String cache;
		
		@Override
		public boolean hasNext() {
			if (cache == null) {
				try {cache = base.readLine();}
				catch (IOException e) {throw new RuntimeException("Error processing file: " + source.getName());}
			}
			return cache != null;
		}

		@Override
		public Glyph<G,I> next() {
			if (cache == null && !hasNext()) {throw new NoSuchElementException();}

			StringTokenizer t = new StringTokenizer(cache, delimiters);
			ArrayList<String> parts = new ArrayList<>();
			while (t.hasMoreTokens()) {parts.add(t.nextToken());}
			Indexed base = new Indexed.ListWrapper(parts);
			cache = null;

			return new SimpleGlyph<>(shaper.shape(base), valuer.value(base));
		}

		@Override public void remove() {throw new UnsupportedOperationException();}
	}
}
