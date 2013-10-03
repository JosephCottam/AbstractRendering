package ar.util;

import ar.glyphsets.implicitgeometry.Indexed;
import ar.util.MemMapEncoder.TYPE;

/**Wrapper to interface items encoded using the MemMapEncoder with the implicit geometry system.
 * **/
public class IndexedEncoding implements Indexed {
	private static final long serialVersionUID = 3550855955493381035L;
	
	private final TYPE[] types;
	private final int[] offsets;
	private final BigFileByteBuffer buffer;
	private long recordOffset;

	/**Convenience for working with the BigFileByteBuffer.  
	 * Performs buffer assurance of the requested content but requires external synchronization for multi-threading.*/
	public IndexedEncoding(final TYPE[] types, long recordOffset, BigFileByteBuffer buffer) {
		this(types, recordOffset, buffer, MemMapEncoder.recordOffsets(types));
	}

	/**Convenience for working with the BigFileByteBuffer.  
	 * Performs buffer assurance of the requested content but requires external synchronization for multi-threading.*/
	public IndexedEncoding(final TYPE[] types, long recordOffset, BigFileByteBuffer buffer, int[] offsets) {
		this.types = types;
		this.offsets = offsets;
		this.buffer = buffer;
		this.recordOffset = recordOffset;
	}

	public Object get(int f) {
		TYPE t = types[f];
		long offset= offsets[f]+recordOffset;
		switch(t) {
			case INT: return buffer.getInt(offset);
			case SHORT: return buffer.getShort(offset);
			case LONG: return buffer.getLong(offset);
			case DOUBLE: return buffer.getDouble(offset);
			case FLOAT: return buffer.getFloat(offset);
			case BYTE: return buffer.get(offset);
			case CHAR: return buffer.getChar(offset);
			case X: throw new IllegalArgumentException("'Skip-type' not supported (denoted 'X'); found at index " + offset);
		}
		throw new IllegalArgumentException("'Unhandled type at offset " + offset);
	}
}
