package ar.glyphsets;

import java.awt.Color;
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
import ar.util.SimpleGlyph;
import ar.util.Util;


/**Implicit geometry, sequentially arranged glyphset backed by a memory-mapped file.
 * 
 * <p>
 * This glyphset uses 'implicit geometry' in that the geometry is produced just-in-time and
 * discarded immediately.  Implicit geometry significantly reduces the required memory at the
 * cost of speed.  When using implicit geometry, the display window size is the principal 
 * memory consumer (because it determines both the image size and the aggregates set size). 
 * 
 * <p>
 * The memory mapped file must be encoded as fixed-width records for this class.
 * The files may include a header to self-describe or the header information may be supplied.
 * 
 * <p>
 *  The header, when provided, is an integer indicating how many fields are in each record,
 *  followed by a set of characters (one for each field).  
 *  
 *  <p>
 *  The characters that describe field types are:
 *  <ul>
 *   <li>s -- Short (two bytes)</li>
 *   <li>i -- Int (four bytes)</li>
 *   <li>l -- Long (eight bytes)</li>
 *   <li>f -- Float (four bytes)</li>
 *   <li>d -- Double (eight bytes)</li>
 *   <li>c -- Char (two bytes)</li>
 *   <li>b -- Byte (one byte)</li>
 * </ul>
 * 
 * <p>
 * TODO: Add skip parameter to skip a certain number of bytes at the start of the file
 * <p>
 * TODO: Add support for strings (type 'V').  Multi-segmented file or multiple files, where one file is the table, the other is a string-table.  Talbe file stores offsets into string-table for string values
 * <p>
 * TODO: Add support for non-color glyphs (generalize Painter...)
 * 
 * @author jcottam
 *
 */
public class MemMapList implements Glyphset.RandomAccess<Color> {
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
	
	private final double glyphWidth;
	private final double glyphHeight;
	private final File source;
	private final TYPE[] types;
	private final ImplicitGeometry.Valuer<Double,Color> painter;
	
	private final int recordEntries;
	private final int recordSize;
	private final int headerOffset;
	private final boolean flipY;
	private final long entryCount;
	private Rectangle2D bounds;

	
	
	public MemMapList(File source, double glyphSize, ImplicitGeometry.Valuer<Double,Color> painter) {
		this(source, glyphSize, glyphSize, false, painter, null);
	}
	public MemMapList(File source, double glyphWidth, double glyphHeight, boolean flipY, ImplicitGeometry.Valuer<Double,Color> painter, TYPE[] types) {
		this.glyphWidth = glyphWidth;
		this.glyphHeight = glyphHeight;
		this.source = source;
		this.painter = painter;
		this.flipY = flipY;
		
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
		
	@Override
	public Collection<Glyph<Color>> intersects(Rectangle2D r) {
		ArrayList<Glyph<Color>> contained = new ArrayList<Glyph<Color>>();
		for (Glyph<Color> g: this) {if (g.shape().intersects(r)) {contained.add(g);}}
		return contained;
	}
	
	@Override
	public Glyph<Color> get(long i) {
		long recordOffset = (i*recordSize)+headerOffset;
		BigFileByteBuffer buffer = this.buffer.get();
		
		buffer.position(recordOffset);
		double x = value(buffer, 0);
		double y = value(buffer, 1);
		double v = types.length > 2 ? value(buffer, 2) : 0;
		// System.out.printf("Read in %d: (%s,%s)\n", i,x,y);
		y = flipY ? -y :y;
		
		Color c = painter.value(v);
		Glyph<Color> g = new SimpleGlyph<Color>(new Rectangle2D.Double(x,y,glyphWidth,glyphHeight), c);
		return g;
	}
	
	private double value(BigFileByteBuffer buffer, int offset) {
		TYPE t = types[offset];
		switch(t) {
			case INT: return buffer.getInt();
			case SHORT: return buffer.getShort();
			case LONG: return buffer.getLong();
			case DOUBLE: return buffer.getDouble();
			case FLOAT: return buffer.getFloat();
			case BYTE: return buffer.get();
			case CHAR: return buffer.getChar();
		}
		
		throw new RuntimeException("Unknown type specified at offset " + offset);
	}

	public ImplicitGeometry.Valuer<Double,Color> painter() {return painter;}
	public TYPE[] types() {return types;}

	public boolean isEmpty() {return buffer.get() == null || buffer.get().capacity() <= 0;}
	public long size() {return entryCount;}
	public void add(Glyph<Color> g) {throw new UnsupportedOperationException();}
	public Iterator<Glyph<Color>> iterator() {return new GlyphsetIterator<Color>(this);}
	
	public Rectangle2D bounds() {
		if (bounds == null) {
			bounds = pool.invoke(new BoundsTask(this, 0, size()));
		}
		return bounds;
	}
	
	private static final class BoundsTask extends RecursiveTask<Rectangle2D> {
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
