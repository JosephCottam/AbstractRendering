package ar.util.memoryMapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**Wraps a byte buffer with long-based indexing (ostensibly to file positions).
 * 
 * TODO: Investigate just taking in a ByteBuffer...and rename some related stuff
 * **/
public class FileByteBuffer implements MappedFile {
	private final ByteBuffer buffer;
	private final long fileOffset;
	private final int size;
	
	public FileByteBuffer(File source, long start, long end) throws IOException {
		long len = end-start;
		if (len > Integer.MAX_VALUE) {throw new IllegalArgumentException("Requested too many bytes.");}
		
		this.fileOffset = start;
		this.size = (int) len; 
		try (FileInputStream fs = new FileInputStream(source);
			FileChannel c = fs.getChannel();) {
			buffer = c.map(FileChannel.MapMode.READ_ONLY, start, len);
		}
	}

	private static final int bufferPos(long pos) {return (int) pos;}
	
	public byte get(long pos) {return buffer.get(bufferPos(pos));}
	public short getShort(long pos) {return buffer.getShort(bufferPos(pos));}
	public int getInt(long pos) {return buffer.getInt(bufferPos(pos));}
	public long getLong(long pos) {return buffer.getLong(bufferPos(pos));}
	public char getChar(long pos) {return buffer.getChar(bufferPos(pos));}
	public float getFloat(long pos) {return buffer.getFloat(bufferPos(pos));}
	public double getDouble(long pos) {return buffer.getDouble(bufferPos(pos));}
	public byte get() {return buffer.get();}
	public short getShort() {return buffer.getShort();}
	public int getInt() {return buffer.getInt();}
	public long getLong() {return buffer.getLong();}
	public char getChar() {return buffer.getChar();}
	public float getFloat() {return buffer.getFloat();}
	public double getDouble() {return buffer.getDouble();}
	public void get(byte[] target, long offset, int length) {
		int savePos = buffer.position();
		int bufferPos = bufferPos(offset);
		buffer.position(bufferPos);
		buffer.get(target, 0, length);
		buffer.position(savePos);
	}
	
	public long position() {return buffer.position();}
	public long capacity() {return size;}
	
	public ByteOrder order() {return buffer.order();}
	public void order(ByteOrder order) {buffer.order(order);}
	
	public long filePosition() {return fileOffset;}
}
