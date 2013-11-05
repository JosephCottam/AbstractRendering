package ar.util.memoryMapping;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;

public interface MappedFile {
	public abstract byte get(long pos);
	public abstract short getShort(long pos);
	public abstract int getInt(long pos);
	public abstract long getLong(long pos);
	public abstract char getChar(long pos);
	public abstract float getFloat(long pos);
	public abstract double getDouble(long pos);
	public abstract byte get();
	public abstract short getShort();
	public abstract int getInt();
	public abstract long getLong();
	public abstract char getChar();
	public abstract float getFloat();
	public abstract double getDouble();
	public abstract void get(byte[] target, long offset, int length);
	
	/**How many bytes ended up in this buffer?**/
	public abstract long capacity();

	/**Where in the file is the cursor current?**/
	public abstract long position();

	/**What byte of the backing file does the zero-buffer position correspond to?*/
	public abstract long filePosition();

	public static final class Util {
		public static final MappedFile make(
				File f, 
				FileChannel.MapMode mode, 
				int bufferSize, 
				long offset, long end) throws IOException {
			
			if (mode == FileChannel.MapMode.READ_ONLY 
					&& (end > 0 && (end-offset) > Integer.MAX_VALUE)) {
				return new FileByteBuffer(f, offset, end);
			} else {
				BigFileByteBuffer bf = new BigFileByteBuffer(f, bufferSize);
				if (offset >= 0) {bf.position(offset);}
				return bf;
			}
			
		}
	}
	
}