package ar.glyphsets;

import java.awt.Color;
import java.awt.geom.Point2D;
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
	private final double glyphSize;
	private final File source;
	
	private final ThreadLocal<ByteBuffer> buffer = 
			new ThreadLocal<ByteBuffer>() {
				public ByteBuffer initialValue() {
					try {
						FileInputStream inputStream = new FileInputStream(source);
						FileChannel channel =  inputStream.getChannel();
						
						ByteBuffer b = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
						inputStream.close();
						channel.close();

						return b;
					} catch (Exception e) {throw new RuntimeException(e);}
				}
		
	};

	
	
	public MemMapList(File source, double glyphSize) {
		this.glyphSize = glyphSize;
		this.source = source;
	}
		
	@Override
	public Collection<Glyph> containing(Point2D p) {
		ArrayList<Glyph> contained = new ArrayList<Glyph>();
		for (Glyph g: this) {if (g.shape.contains(p)) {contained.add(g);}}
		return contained;
	}
	
	@Override
	public Glyph get(int i) {
		int offset = i*ENTRY_SIZE;
		ByteBuffer buffer = this.buffer.get();
		
		buffer.position(offset);
		int x = buffer.getInt();
		int y = buffer.getInt();
		int v = buffer.getInt();
		int reg = buffer.getInt();
		
		if (reg != -1) {
			throw new RuntimeException(String.format("File-format error at (purported) entry %s.  Registration value %s when -1 was expected.", i, reg));}
		
		Glyph g = new Glyph(new Rectangle2D.Double(x,y,glyphSize,glyphSize), Color.RED, v);
		return g;
	}


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
