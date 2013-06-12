package ar.glyphsets;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import ar.Glyphset;
import ar.util.BigFileByteBuffer;


/**Implicit geometry, sequentially arranged glyphset backed by a memory-mapped file.
 * <p>
 * Specialization of the MemMapList to focus on just points.  
 * Primarily a set of experiments, but should be serviceable.  
 * See MemMapList for input format details, etc.
 * 
 * @author jcottam
 *
 */
public class PointMemMapList implements Glyphset.RandomAccess<Color> {
	public enum TYPE {
		INT(4), DOUBLE(8), LONG(8), SHORT(2), BYTE(1), CHAR(2), FLOAT(4);
		public final int bytes;
		private TYPE(int bytes) {this.bytes=bytes;}
	};
	
	public static int BUFFER_BYTES = 30000;//Integer.MAX_VALUE added appreciable latency to thread creation, while this smaller number didn't add appreciable latency to runtime...perhaps because multi-threading hid the latency
	
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

	public PointMemMapList(File source, double glyphSize, ImplicitGeometry.Valuer<Double,Color> painter) {
		this(source, glyphSize, glyphSize, false, painter, null);
	}
	public PointMemMapList(File source, double glyphWidth, double glyphHeight, boolean flipY, ImplicitGeometry.Valuer<Double,Color> painter, TYPE[] types) {
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
			double minX=Double.MAX_VALUE, minY=Double.MAX_VALUE, maxX=Double.MIN_VALUE, maxY=Double.MIN_VALUE;
			BigFileByteBuffer buffer = this.buffer.get();

			for (long i=0; i<size();i++) {
				long recordOffset = (i*recordSize)+headerOffset;
				buffer.position(recordOffset);
				double x = value(buffer, 0);
				double y = value(buffer, 1);
				y = flipY ? -y :y;

				minX = Math.min(x, minX);
				minY = Math.min(y, minY);
				maxX = Math.max(x, maxX);
				maxY = Math.max(y, maxY);
			}
			bounds = new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
		}
		return bounds;
	}
	
}
