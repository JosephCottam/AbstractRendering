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
		buffers = new ByteBuffer[(int) limit/Integer.MAX_VALUE];
		for (int i=0; i<buffers.length; i++) {
			int low = Math.max(0, (i-1*Integer.MAX_VALUE));
			int size = (int) Math.min(Integer.MAX_VALUE, limit-low);
			buffers[i]=channel.map(FileChannel.MapMode.READ_ONLY, low, size);
		}
		
		position=0;
		
		inputStream.close();
		channel.close();
	}
	
	public byte get() {return getAt(position, 1).get();}
	public short getShort() {return getAt(position, 2).getShort();}
	public int getInt() {return getAt(position, 4).getInt();}
	public long getLong() {return getAt(position, 8).getLong();}
	public char getChar() {return getAt(position, 2).getChar();}
	public double getFloat() {return getAt(position, 4).getFloat();}
	public double getDouble() {return getAt(position, 8).getDouble();}
	
	public long limit() {return limit;}
	public void position(long at) {this.position = at;}
	
	
	private ByteBuffer getAt(long p, int count) {
		int bufferID = (int) p/Integer.MAX_VALUE;
		long bottom = bufferID*Integer.MAX_VALUE;
		long start = p-bottom;
		long end = start+count;
		
		if (start < 0 || start > Integer.MAX_VALUE) {throw new IllegalArgumentException();}

		int s = (int) start;
		int e = (int) end;
		
		if (end < Integer.MAX_VALUE) {
			//Everything is in one buffer...
			buffers[bufferID].position(s);
			return buffers[bufferID];
		} else {
			//Split between buffers, must make a temp buffer...
			e = (int) end-Integer.MAX_VALUE;
			byte[] bytes = new byte[count];
			buffers[bufferID].get(bytes, s, (int) (buffers[bufferID].limit()-start));
			buffers[bufferID+1].get(bytes, 0, e);
			return ByteBuffer.wrap(bytes);
		}
	}

}
