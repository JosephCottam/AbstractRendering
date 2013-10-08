package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import ar.Glyph;
import ar.Glyphset;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.BigFileByteBuffer;
import ar.util.MemMapEncoder.TYPE;
import ar.util.MemMapEncoder;
import ar.util.Util;
import ar.util.IndexedEncoding;

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
public class MemMapList<I> implements Glyphset.RandomAccess<I> {
	/**Flag field indicating the binary file encoding (hbin) version understood by the parser.**/
	public static final int VERSION_UNDERSTOOD = -1;
	
	/**How large should backing read buffer be?**/
	public static int BUFFER_BYTES = Integer.MAX_VALUE; //Integer.MAX_VALUE added appreciable latency to thread creation, while this smaller number didn't add appreciable latency to runtime...perhaps because multi-threading hid the latency
	
	/**Thread-pool size for parallel operations.**/
	public static int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
	private final ForkJoinPool pool = new ForkJoinPool(THREAD_POOL_SIZE);

	private final ThreadLocal<BigFileByteBuffer> buffer = 
			new ThreadLocal<BigFileByteBuffer>() {
		public BigFileByteBuffer initialValue() {
			if (source == null) {return null;}
			try {return new BigFileByteBuffer(source, recordLength, BUFFER_BYTES);}
			catch (Exception e) {throw new RuntimeException(e);}
		}
	};

	private final File source;
	private final TYPE[] types;
	private final Valuer<Indexed,I> valuer;
	private final Shaper<Indexed> shaper;

	private final int recordLength;
	private final int[] offsets;
	private final long dataTableOffset;
	private final long stringTableOffset;
	private final long entryCount;
	private Rectangle2D bounds;

	/**Create a new memory mapped list, types are read from the source.**/
	public MemMapList(File source, Shaper<Indexed> shaper, Valuer<Indexed,I> valuer) {
		this.source = source;
		this.valuer = valuer;
		this.shaper = shaper;

		if (source != null) {
			MemMapEncoder.Header header = MemMapEncoder.Header.from(buffer.get());
			if (header.version != VERSION_UNDERSTOOD) {
				throw new IllegalArgumentException(String.format("Unexpected version number in file %d; expected %d", header.version, VERSION_UNDERSTOOD));
			}

			dataTableOffset = header.dataTableOffset;
			stringTableOffset = header.stringTableOffset;
			types = header.types;
			this.recordLength = header.recordLength;
			this.offsets = MemMapEncoder.recordOffsets(types);
			
			if (shaper instanceof Shaper.SafeApproximate) {
				IndexedEncoding max = entryAt(header.maximaRecordOffset);				
				IndexedEncoding min = entryAt(header.minimaRecordOffset);
				Rectangle2D maxBounds = shaper.shape(max).getBounds2D();
				Rectangle2D minBounds = shaper.shape(min).getBounds2D();
				bounds = Util.bounds(maxBounds, minBounds);
			} 
		} else {
			dataTableOffset = -1;
			stringTableOffset = -1;
			this.types = null;
			this.offsets = new int[0];
			this.recordLength = -1;
		}
		if (stringTableOffset >=0) {throw new IllegalArgumentException("Can't handle strings (yet).");}
		entryCount = buffer.get() == null ? 0 : (buffer.get().fileSize()-dataTableOffset)/recordLength;

	}

	protected void finalize() {pool.shutdownNow();}

	@Override
	public Collection<Glyph<I>> intersects(Rectangle2D r) {
		ArrayList<Glyph<I>> contained = new ArrayList<Glyph<I>>();
		for (Glyph<I> g: this) {if (g.shape().intersects(r)) {contained.add(g);}}
		return contained;
	}

	@Override
	public Glyph<I> get(long i) {
		long recordOffset = (i*recordLength)+dataTableOffset;
		IndexedEncoding entry = entryAt(recordOffset);
		Glyph<I> g = new SimpleGlyph<I>(shaper.shape(entry), valuer.value(entry));
		return g;
	}

	protected IndexedEncoding entryAt(long recordOffset) {
		BigFileByteBuffer buffer = this.buffer.get();
		return new IndexedEncoding(types, recordOffset, buffer, offsets);
	}

	/**Valuer being used to establish a value for each entry.**/
	public Valuer<Indexed,I> valuer() {return valuer;}
	
	/**Shaper being used to provide geometry for each entry.**/ 
	public Shaper<Indexed> shaper() {return shaper;}
	
	/**Types array used for conversions on read-out.**/
	public TYPE[] types() {return types;}

	public boolean isEmpty() {return buffer.get() == null || buffer.get().capacity() <= 0;}
	public long size() {return entryCount;}
	public Iterator<Glyph<I>> iterator() {return new GlyphsetIterator<I>(this);}

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
				Rectangle2D bound = MemMapList.this.get(i).shape().getBounds2D();
				if (bound != null) {Util.add(bounds, bound);}

			}
			return bounds;
		}

	}

	public long segments() {return size();}

	@Override
	public Glyphset<I> segment(long bottom, long top)
			throws IllegalArgumentException {
		Glyphset<I> subset = GlyphSubset.make(this, bottom, top, true);
		return subset;
	}

}
