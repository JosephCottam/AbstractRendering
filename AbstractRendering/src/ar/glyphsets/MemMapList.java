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
import ar.Util;
import ar.GlyphSet.Glyph;


public class MemMapList implements GlyphSet, GlyphSet.RandomAccess, Iterable<Glyph> {
	private static final int ENTRY_SIZE = 16;  //4-int-fields per entry, 4 bytes per int
	public enum TYPE {
		INT(4), DOUBLE(8);
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

//	public MemMapList(File source, double glyphSize) {
//		this(source, glyphSize, new Painter.Constant<Double>(Color.red), false, null);
//	}
	
	public MemMapList(File source, double glyphSize, Painter<Double> painter, TYPE[] types) {
		this.glyphSize = glyphSize;
		this.source = source;
		this.painter = painter;
		
		if (types == null) {
			int length = 0;
			while(true) {if (buffer.get().getInt() == 30) {break;} length++;} //30 is the "record separator" character...seemed appropriate!
			recordEntries = length;
			
			
			types = new TYPE[recordEntries];
			
			for (int i =0; i<recordEntries; i++) {
				int t = buffer.get().getInt();
				if (t==105) {types[i] = TYPE.INT;}  //letter 'i'
				else if (t==100) {types[i] = TYPE.DOUBLE;} //letter 'd'
				else {throw new RuntimeException("Unknown type indicator at position " + i);}
			}
			this.types = types;
			headerOffset = (1+types.length)*4;  //4 bytes per int, plus one for the length entry
		} else {
			recordEntries = 0;
			headerOffset = 0;
			this.types = types;
		}
		
		int acc=0;
		for (TYPE t:types) {acc += t.bytes;}
		this.recordSize = acc;
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
		double v = value(buffer, 2);
		
		Color c = painter.from(v);
		Glyph g = new Glyph(new Rectangle2D.Double(x,y,glyphSize,glyphSize), c, v);
		return g;
	}
	
	private double value(ByteBuffer buffer, int offset) {
		if (types[offset]==TYPE.INT) {return buffer.getInt();}
		if (types[offset]==TYPE.DOUBLE) {return buffer.getDouble();}
		throw new RuntimeException("Unknown type specified at offset " + offset);
	}

	public Painter<Double> painter() {return painter;}
	public TYPE[] types() {return types;}

	public boolean isEmpty() {return buffer == null || buffer.get().limit() <= 0;}
	public int size() {return buffer == null ? 0 : buffer.get().limit()/ENTRY_SIZE;}
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
