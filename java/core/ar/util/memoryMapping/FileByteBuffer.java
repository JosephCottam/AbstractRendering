package ar.util.memoryMapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**Wraps a byte buffer with long-based indexing (ostensibly to file positions).**/
public class FileByteBuffer implements MappedFile {
	private ByteBuffer buffer;
	private long offset;
	
	public FileByteBuffer(File source, long start, long end) throws IOException {
		this.offset = 0;
		try (FileInputStream fs = new FileInputStream(source);
			FileChannel c = fs.getChannel();) {
			buffer = c.map(FileChannel.MapMode.READ_ONLY, start, end-start);
		}
		
	}
	
	
	
	public FileByteBuffer(ByteBuffer buffer, long offset) {
		this.buffer = buffer;
		this.offset = offset;
	}

	private final int bufferPos(long pos) {return (int) (pos-offset);}
	
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
	public void get(byte[] target, long offset, int length) {buffer.get(target, bufferPos(offset), length);}
	public long position() {return buffer.position()+offset;}
	public long filePosition() {return offset;}
	
	public long capacity() {return buffer.capacity();}	
}
