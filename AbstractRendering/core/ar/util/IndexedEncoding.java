package ar.util;

import java.nio.ByteBuffer;

import ar.glyphsets.implicitgeometry.Indexed;
import ar.util.MemMapEncoder.TYPE;

public class IndexedEncoding implements Indexed {
	private final TYPE[] types;
	private final ByteBuffer buffer;
	
	public IndexedEncoding(TYPE[] types, long offset, int recordSize, BigFileByteBuffer buffer) {
		this.types = types;
		
		byte[] bytes = new byte[recordSize];
		buffer.get(bytes, offset, recordSize);
		this.buffer = ByteBuffer.wrap(bytes);
	}

	private Object value(int offset) {
		TYPE t = types[offset];
		switch(t) {
			case INT: return buffer.getInt();
			case SHORT: return buffer.getShort();
			case LONG: return buffer.getLong();
			case DOUBLE: return buffer.getDouble();
			case FLOAT: return buffer.getFloat();
			case BYTE: return buffer.get();
			case CHAR: return buffer.getChar();
			case X: throw new IllegalArgumentException("'Skip-type' not supported (denoted 'X'); found at index " + offset);
		}
		
		throw new RuntimeException("Unknown type specified at offset " + offset);
	}

	public Object get(int f) {
		int pos=0;
		for (int i=0; i<f; i++) {pos+=types[i].bytes;}
		buffer.position(pos);
		return value(f);
	}
}
