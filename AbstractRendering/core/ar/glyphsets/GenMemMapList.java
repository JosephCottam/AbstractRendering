package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import ar.Glyphset;
import ar.util.BigFileByteBuffer;
import ar.util.ImplicitGeometry;
import ar.util.Util;
import ar.util.ImplicitGeometry.Indexed;
import ar.util.IndexedEncoding;
import ar.util.SimpleGlyph;


/**Implicit geometry, sequentially arranged glyphset backed by a memory-mapped file.
 * <p>
 * Generalization of the MemMapList to use more implicit geometry.  This is an experimental
 * class still, but probably serviceable.  See MemMapList for input format details, etc.
 * 
 * @author jcottam
 *
 */
public class GenMemMapList<V> implements Glyphset.RandomAccess<V> {
	public enum TYPE {
		INT(4), DOUBLE(8), LONG(8), SHORT(2), BYTE(1), CHAR(2), FLOAT(4);
		public final int bytes;
		private TYPE(int bytes) {this.bytes=bytes;}
	};
	
	public static int BUFFER_BYTES = 30000;//Integer.MAX_VALUE added appreciable latency to thread creation, while this smaller number didn't add appreciable latency to runtime...perhaps because multi-threading hid the latency
	
	private final ForkJoinPool pool = new ForkJoinPool();
	private final ThreadLocal<BigFileByteBuffer> buffer = 
			new ThreadLocal<BigFileByteBuffer>() {
				public BigFileByteBuffer initialValue() {
					if (source == null) {return null;}
					try {return new BigFileByteBuffer(source, recordSize, BUFFER_BYTES);}
					catch (Exception e) {throw new RuntimeException(e);}
				}
	};
	
	private final File source;
	private final TYPE[] types;
	private final ImplicitGeometry.Valuer<Indexed,V> painter;
	private final ImplicitGeometry.Shaper<Indexed> shaper;
	
	private final int recordEntries;
	private final int recordSize;
	private final int headerOffset;
	private final long entryCount;
	private Rectangle2D bounds;

	public GenMemMapList(File source, ImplicitGeometry.Shaper<Indexed> shaper, ImplicitGeometry.Valuer<Indexed,V> painter) {
		this(source, null, shaper, painter);
	}
	
	public GenMemMapList(File source, TYPE[] types, ImplicitGeometry.Shaper<Indexed> shaper, ImplicitGeometry.Valuer<Indexed,V> painter) {
		this.source = source;
		this.painter = painter;
		this.shaper = shaper;
		
		if (source != null && types == null) {
			recordEntries = buffer.get().getInt();
			
			types = new TYPE[recordEntries];
			for (int i =0; i<recordEntries; i++) {
				char t = buffer.get().getChar();
				if (t=='i') {types[i] = TYPE.INT;}  
				else if (t=='l') {types[i] = TYPE.LONG;}
				else if (t=='s') {types[i] = TYPE.SHORT;}
				else if (t=='d') {types[i] = TYPE.DOUBLE;} 
				else if (t=='f') {types[i] = TYPE.FLOAT;}
				else if (t=='b') {types[i] = TYPE.BYTE;}
 				else {throw new RuntimeException(String.format("Unknown type indicator '%s' at position %s", t,i));}
			}
			this.types = types;
			headerOffset = (TYPE.INT.bytes+(types.length*TYPE.CHAR.bytes));  //Int for the header length, one char per entry  

			int acc=0;
			for (TYPE t:this.types) {acc += t.bytes;}
			this.recordSize = acc;
		} else {
			recordEntries = 0;
			headerOffset = 0;
			this.types = null;
			this.recordSize = -1;
		}
		entryCount = buffer.get() == null ? 0 : (buffer.get().fileSize()-headerOffset)/recordSize;
		
	}
	
	protected void finalize() {pool.shutdownNow();}
		
	@Override
	public Collection<Glyph<V>> intersects(Rectangle2D r) {
		ArrayList<Glyph<V>> contained = new ArrayList<Glyph<V>>();
		for (Glyph<V> g: this) {if (g.shape().intersects(r)) {contained.add(g);}}
		return contained;
	}
	
	@Override
	public Glyph<V> get(long i) {
		long recordOffset = (i*recordSize)+headerOffset;
		BigFileByteBuffer buffer = this.buffer.get();
		IndexedEncoding entry = new IndexedEncoding(types, recordOffset,recordSize,buffer);
		Glyph<V> g = new SimpleGlyph<V>(shaper.shape(entry), painter.value(entry));
		return g;
	}

	public ImplicitGeometry.Valuer<Indexed,V> painter() {return painter;}
	public TYPE[] types() {return types;}

	public boolean isEmpty() {return buffer.get() == null || buffer.get().capacity() <= 0;}
	public long size() {return entryCount;}
	public void add(Glyph<V> g) {throw new UnsupportedOperationException();}
	public Iterator<Glyph<V>> iterator() {return new GlyphsetIterator<V>(this);}
	
	public Rectangle2D bounds() {
		if (bounds == null) {
			bounds = pool.invoke(new BoundsTask(this, 0, this.size()));
			//bounds = Util.bounds(this);
		}
		return bounds;
	}
	
	private final class BoundsTask extends RecursiveTask<Rectangle2D> {
		public static final long serialVersionUID = 1L;
		private static final int TASK_SIZE = 100000;
		private final Glyphset.RandomAccess<?> glyphs;
		private final long low, high;
		
		public BoundsTask(Glyphset.RandomAccess<?> glyphs, long low, long high) {
			this.glyphs = glyphs;
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
			BoundsTask top = new BoundsTask(glyphs, low, mid);
			BoundsTask bottom = new BoundsTask(glyphs, mid, high);
			invokeAll(top, bottom);
			Rectangle2D bounds = Util.bounds(top.getRawResult(), bottom.getRawResult());
			return bounds;
		}
		
		private Rectangle2D local() {
			GlyphsetIterator<?> it = new GlyphsetIterator(glyphs, low, high);
			return Util.bounds(it);
		}
		
	}
	
}
