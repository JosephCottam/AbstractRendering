package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import ar.Glyph;
import ar.Glyphset;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.IndexedEncoding;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.memoryMapping.MappedFile;
import ar.util.memoryMapping.MemMapEncoder;
import ar.util.memoryMapping.MemMapEncoder.TYPE;
import ar.util.Util;

/**Implicit geometry, sequentially arranged glyphset backed by a memory-mapped file.
 * 
 * This glyphset uses 'implicit geometry' in that the geometry is produced just-in-time and
 * discarded immediately.  Implicit geometry significantly reduces the required memory at the
 * cost of speed.  When using implicit geometry, the display window size is the principal 
 * memory consumer (because it determines both the image size and the aggregates set size). 
 * 
 * The memory mapped file must be encoded as fixed-width records for this class.
 * The files may include a header to self-describe or the header information may be supplied.
 * 
 *  The header, when provided, is an integer indicating how many fields are in each record,
 *  followed by a set of characters (one for each field).  
 *  
 *  This is class is NOT thread-safe.  However, subsets are logically independent units 
 *  so multiple subsets can be safely created and used concurrently (even if they overlap).
 *  
 *  The characters that describe field types are:
 *  
 *   +   s -- Short (two bytes)
 *   +   i -- Int (four bytes)
 *   +   l -- Long (eight bytes)
 *   +   f -- Float (four bytes)
 *   +   d -- Double (eight bytes)
 *   +   c -- Char (two bytes)
 *   +   b -- Byte (one byte)
 *   
 * @author jcottam
 *
 */
public class MemMapList<G,I> implements Glyphset.RandomAccess<G,I> {
	/**Flag field indicating the binary file encoding (hbin) version understood by the parser.**/
	public static final int VERSION_UNDERSTOOD = -1;
	
	/**How large should backing read buffer be?
	 * Reducing this number tends to result in faster thread startup times, but slower overall run-times.
	 * **/
	public static int BUFFER_BYTES = Integer.MAX_VALUE;
	
	/**Thread-pool size for parallel operations.**/
	private static final ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

	private final MappedFile buffer;

	private final TYPE[] types;
	private final Valuer<Indexed,I> valuer;
	private final Shaper<Indexed,G> shaper;

	private final File source; //TODO: Remove, make this a general "ByteBackedList" or something like that..
	private final int recordLength;
	private final int[] offsets;
	private final long dataTableOffset;
	private final long entryCount;
	private Rectangle2D bounds;

	/**Create a new memory mapped list, types are read from the source.
	 * @throws IOException **/
	public MemMapList(File source, Shaper<Indexed, G> shaper, Valuer<Indexed,I> valuer) {
		this.valuer = valuer;
		this.shaper = shaper;
		this.source = source;
		
		if (source != null) {
			try {this.buffer = MappedFile.Util.make(source, FileChannel.MapMode.READ_ONLY, BUFFER_BYTES);}
			catch (Exception e) {throw new RuntimeException("Error construction buffer for mem-mapped list.", e);}
			
			MemMapEncoder.Header header = MemMapEncoder.Header.from(buffer);
			if (header.version != VERSION_UNDERSTOOD) {
				throw new IllegalArgumentException(String.format("Unexpected version number in file %d; expected %d", header.version, VERSION_UNDERSTOOD));
			}

			dataTableOffset = header.dataTableOffset;
			types = header.types;
			this.recordLength = header.recordLength;
			this.offsets = MemMapEncoder.recordOffsets(types);
			
			if (shaper instanceof Shaper.SafeApproximate) {
				IndexedEncoding max = entryAt(header.maximaRecordOffset);				
				IndexedEncoding min = entryAt(header.minimaRecordOffset);
				Rectangle2D maxBounds = Util.boundOne(shaper.shape(max));
				Rectangle2D minBounds = Util.boundOne(shaper.shape(min));
				bounds = Util.bounds(maxBounds, minBounds);
			} 
			entryCount = (source.length()-dataTableOffset)/recordLength;
		} else {
			this.dataTableOffset = -1;
			this.buffer = null;
			this.types = null;
			this.offsets = new int[0];
			this.recordLength = -1;
			this.entryCount=0;
		}
		
	}
	
	public MemMapList(MappedFile buffer, File source, Shaper<Indexed,G> shaper, Valuer<Indexed,I> valuer, TYPE[] types, long dataTableOffset) {
		this.buffer = buffer;
		this.shaper = shaper;
		this.valuer = valuer;
		this.types = types;
		this.source = source;
		this.offsets = MemMapEncoder.recordOffsets(types);
		this.recordLength = MemMapEncoder.recordLength(types);
		this.entryCount = buffer.capacity()/recordLength;
		this.dataTableOffset=dataTableOffset;
	}

	@Override
	public Glyph<G,I> get(long i) {
		IndexedEncoding entry = entryAt(recordOffset(i));
		Glyph<G,I> g = new SimpleGlyph<G,I>(shaper.shape(entry), valuer.value(entry));
		return g;
	}

	protected long recordOffset(long i) {return (i*recordLength)+dataTableOffset;}
	
	protected IndexedEncoding entryAt(long recordOffset) {
		MappedFile buffer = this.buffer;
		return new IndexedEncoding(types, recordOffset, buffer, offsets);
	}

	/**Valuer being used to establish a value for each entry.**/
	public Valuer<Indexed,I> valuer() {return valuer;}
	
	/**Shaper being used to provide geometry for each entry.**/ 
	public Shaper<Indexed,G> shaper() {return shaper;}
	
	/**Types array used for conversions on read-out.**/
	public TYPE[] types() {return types;}

	public boolean isEmpty() {return buffer == null || buffer.capacity() <= 0;}
	public long size() {return entryCount;}
	public Iterator<Glyph<G,I>> iterator() {return new GlyphsetIterator<G,I>(this);}

	public long segments() {return size();}

	@Override
	public Glyphset<G,I> segment(long bottom, long top)
			throws IllegalArgumentException {
		
		long offset = recordOffset(bottom)+buffer.filePosition();
		long end = recordOffset(top)+buffer.filePosition();
		
		
		
		try {
			MappedFile mf = MappedFile.Util.make(source, FileChannel.MapMode.READ_ONLY, BUFFER_BYTES, offset, end);
			return new MemMapList<>(mf, source, shaper, valuer, types, 0);
		} catch (Exception e) {throw new RuntimeException("Error segmenting glyphset", e);}
	}
	
	public Rectangle2D bounds() {
		if (bounds == null) {
			bounds = pool.invoke(new BoundsTask(0, this.size()));
		}
		return bounds;
	}

	private final class BoundsTask extends RecursiveTask<Rectangle2D> {
		public static final long serialVersionUID = 1L;
		private static final int TASK_SIZE = 100000;
		private final long low, high;

		public BoundsTask(long low, long high) {
			this.low = low;
			this.high = high;
		}

		@Override
		protected Rectangle2D compute() {
			if (high-low > TASK_SIZE) {return split();}
			else {return local();}
		}

		private Rectangle2D split() {
			long mid = low+((high-low)/2);
			BoundsTask top = new BoundsTask(low, mid);
			BoundsTask bottom = new BoundsTask(mid, high);
			invokeAll(top, bottom);
			Rectangle2D bounds = Util.bounds(top.getRawResult(), bottom.getRawResult());
			return bounds;
		}

		private Rectangle2D local() {
			Rectangle2D bounds = new Rectangle2D.Double(0,0,-1,-1);

			for (long i=low; i<high; i++) {
				Rectangle2D bound = Util.boundOne(MemMapList.this.get(i).shape());
				if (bound != null) {Util.add(bounds, bound);}

			}
			return bounds;
		}

	}
}
