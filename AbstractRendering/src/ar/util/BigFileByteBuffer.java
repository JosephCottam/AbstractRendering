package ar.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**Sliding buffer across a large file to get around the int-limit of mem-maps**/
public class BigFileByteBuffer {
	private final FileInputStream inputStream;
	private final long fileSize;
	private final int margin;
	private final int bufferSize;
	private ByteBuffer buffer;
	private long filePos=0;
	
	
	public BigFileByteBuffer(File source, int margin, int bufferSize) throws IOException {
		inputStream = new FileInputStream(source);
		FileChannel channel =  inputStream.getChannel();
		
		fileSize = channel.size();
		filePos = 0;
		buffer = channel.map(FileChannel.MapMode.READ_ONLY, filePos, Math.min(bufferSize, (fileSize-filePos)));
		
		this.margin=margin;
		this.bufferSize = bufferSize;
	}
	
	protected void finalize() {
		try {inputStream.close();}
		catch (IOException e) {}
	}
	
	public long fileSize() {return fileSize;}
	
	public byte get() {return ensure(1).get();}
	public short getShort() {return ensure(2).getShort();}
	public int getInt() {return ensure(4).getInt();}
	public long getLong() {return ensure(8).getLong();}
	public char getChar() {return ensure(2).getChar();}
	public double getFloat() {return ensure(4).getFloat();}
	public double getDouble() {return ensure(8).getDouble();}
	
	public long limit() {return fileSize;}
	public void position(long at) {ensure(at, margin);}
	
	private ByteBuffer ensure(int bytes) {return ensure(filePos+buffer.position(), bytes);}
	private ByteBuffer ensure(long position, int bytes) {
		if((buffer.limit()-buffer.position())<bytes) {
			//Shift the mapping so it contains the requested number of bytes (if possible)
			filePos = buffer.position()+filePos;	
			try {buffer = inputStream.getChannel().map(FileChannel.MapMode.READ_ONLY, filePos, Math.min(bufferSize, (fileSize-filePos)));}
			catch (IOException e) {throw new RuntimeException("Error shifting buffer position.", e);}
		} else if (filePos > position) {
			filePos = position; 
			try {buffer = inputStream.getChannel().map(FileChannel.MapMode.READ_ONLY, filePos, Math.min(bufferSize, (fileSize-filePos)));}
			catch (IOException e) {throw new RuntimeException("Error shifting buffer position backwards.", e);}
		} 

		return buffer;
	}
}
