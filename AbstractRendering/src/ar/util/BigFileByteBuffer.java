package ar.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**Federates multiple byte buffers into a single byte-buffer for mem-map access to a very large file**/
//TODO: Extend beyond read-only mode 
public class BigFileByteBuffer {
	private final ByteBuffer[] buffers;
	private final long limit;
	private long position;
	
	public BigFileByteBuffer(File source) throws IOException {
		FileInputStream inputStream = new FileInputStream(source);
		FileChannel channel =  inputStream.getChannel();
		
		limit = channel.size();
		buffers = new ByteBuffer[Math.round(limit/((float)Integer.MAX_VALUE))];
		for (int i=0; i<buffers.length; i++) {
			int low = Math.max(0, (i-1*Integer.MAX_VALUE));
			int size = (int) Math.min(Integer.MAX_VALUE, limit-low);
			buffers[i]=channel.map(FileChannel.MapMode.READ_ONLY, low, size);
		}
		
		position=0;
	}
	
	public byte get() {return 0;}
	public short getShort() {return 0;}
	public int getInt() {return 0;}
	public long getLong() {return 0;}
	public char getChar() {return 'c';}
	public double getDouble() {return 0;}
	public double getFloat() {return 0;}
	
	public long limit() {return limit;}
	public void position(long at) {this.position = at;}

}
