package ar.glyphsets;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.nio.*;
import java.nio.channels.FileChannel;

import ar.GlyphSet;
import ar.GlyphSet.Glyph;
import ar.util.Util;


public class MemMapList implements GlyphSet, GlyphSet.RandomAccess, Iterable<Glyph> {
	public enum TYPE {
		INT(4), DOUBLE(8), LONG(8), SHORT(2), BYTE(1), CHAR(2), FLOAT(4);
		final int bytes;
		private TYPE(int bytes) {this.bytes=bytes;}
	};
	
	private final ThreadLocal<ByteBuffer> buffer = 
			new ThreadLocal<ByteBuffer>() {
				public ByteBuffer initialValue() {
					try {
						FileInputStream inputStream = new FileInputStream(source);
						FileChannel channel =  inputStream.getChannel();
						
						ByteBuffer b = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());	//TODO: must multiplex buffers if channel.size()>max-int 
						inputStream.close();
						channel.close();

						return b;
					} catch (Exception e) {throw new RuntimeException(e);}
				}
		
	};
	
	private final double glyphSize;
	private final File source;
	private final TYPE[] types;
	private final Painter<Double> painter;
	
	private final int recordEntries;
	private final int recordSize;
	private final int headerOffset;
	private final boolean flipY;

//	public MemMapList(File source, double glyphSize) {
//		this(source, glyphSize, new Painter.Constant<Double>(Color.red), false, null);
//	}
	
	public MemMapList(File source, double glyphSize, boolean flipY, Painter<Double> painter, TYPE[] types) {
		this.glyphSize = glyphSize;
		this.source = source;
		this.painter = painter;
		this.flipY = flipY;
		
		if (source != null && types == null) {
			recordEntries = buffer.get().getInt();
			
			types = new TYPE[recordEntries];
			for (int i =0; i<recordEntries; i++) {
				char t = buffer.get().getChar();
				if (t=='i') {types[i] = TYPE.INT;}  //letter 'i' is hex 69
				else if (t=='l') {types[i] = TYPE.LONG;}
				else if (t=='s') {types[i] = TYPE.SHORT;}
				else if (t=='d') {types[i] = TYPE.DOUBLE;} //letter 'd' is hex 64
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
		
	}
		
	@Override
	public Collection<Glyph> intersects(Rectangle2D r) {
		ArrayList<Glyph> contained = new ArrayList<Glyph>();
		for (Glyph g: this) {if (g.shape.intersects(r)) {contained.add(g);}}
		return contained;
	}
	
	@Override
	public Glyph get(int i) {
		int recordOffset = (i*recordSize)+headerOffset;
		ByteBuffer buffer = this.buffer.get();
		
		buffer.position(recordOffset);
		double x = value(buffer, 0);
		double y = value(buffer, 1);
		double v = types.length > 2 ? value(buffer, 2) : 0;
		
		y = flipY ? -y :y;
		
		Color c = painter.from(v);
		Glyph g = new Glyph(new Rectangle2D.Double(x,y,glyphSize,glyphSize), c, v);
		return g;
	}
	
	private double value(ByteBuffer buffer, int offset) {
		TYPE t = types[offset];
		switch(t) {
			case INT: return buffer.getInt();
			case SHORT: return buffer.getShort();
			case LONG: return buffer.getLong();
			case DOUBLE: return buffer.getDouble();
			case FLOAT: return buffer.getFloat();
			case BYTE: return buffer.get();
		}
		
		throw new RuntimeException("Unknown type specified at offset " + offset);
	}

	public Painter<Double> painter() {return painter;}
	public TYPE[] types() {return types;}

	public boolean isEmpty() {return buffer.get() == null || buffer.get().limit() <= 0;}
	public int size() {return buffer.get() == null ? 0 : (buffer.get().limit()-headerOffset)/recordSize;}
	public Rectangle2D bounds() {return Util.bounds(this);}
	public void add(Glyph g) {throw new UnsupportedOperationException();}
	public Iterator<Glyph> iterator() {return new It(this);}

	
	private static final class It implements Iterator<Glyph> {
		private final GlyphSet.RandomAccess glyphs;
		private int at = 0;
		public It(GlyphSet.RandomAccess glyphs) {this.glyphs = glyphs;}

		public boolean hasNext() {return at < glyphs.size();}
		public Glyph next() {return glyphs.get(at++);}
		public void remove() {throw new UnsupportedOperationException();}		
	}	
}
