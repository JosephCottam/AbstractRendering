package ar.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**Sliding buffer across a large file to get around the int-limit of mem-maps**/
public class BigFileByteBuffer {
	private final FileChannel channel;
	private final long fileSize;
	private final int margin;
	private ByteBuffer buffer;
	private long filePos=0;
	
	
	public BigFileByteBuffer(File source, int margin) throws IOException {
		FileInputStream inputStream = new FileInputStream(source);
		channel =  inputStream.getChannel();
		
		fileSize = channel.size();
		filePos = 0;
		buffer = channel.map(FileChannel.MapMode.READ_ONLY, filePos, Math.min(Integer.MAX_VALUE, (fileSize-filePos)));
		inputStream.close();
		
		this.margin=margin;
	}
	
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
		if(buffer.limit()-buffer.position()<bytes) {
			//Shift the mapping so it contains the requested number of bytes (if possible)
			filePos = buffer.position()+filePos;	
			try {buffer = channel.map(FileChannel.MapMode.READ_ONLY, filePos, Math.min(Integer.MAX_VALUE, (fileSize-filePos)));}
			catch (IOException e) {throw new RuntimeException("Error shifting buffer position.", e);}
		}
		return buffer;
	}
}
