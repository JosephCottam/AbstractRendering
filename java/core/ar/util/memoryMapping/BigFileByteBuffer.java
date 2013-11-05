package ar.util.memoryMapping;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**Sliding buffer across a large file to get around the int-limit of memory maps.
 * 
 * nio memory mapped files are backed by byte arrays.  Since java limits the 
 * size of an array to int indices, the largest file that can be completely mapped
 * is also limited.  This class subverts that limit by mapping smaller buffer and
 * moving the buffer when getting close to the end of the buffer (unless it's already 
 * touches the end of the file).  
 * 
 * This class does not extend nio.buffer because 
 * it is not type-compatible at position-related operations.  This class uses
 * long-valued indices while nio.buffer uses int-valued indices.
 * 
 * To ensure complete values can be read in one operation, a "margin" can be registered.
 * When calling "position", it attempts to ensure that there are margin-number-of-bytes
 * left in mapped region.  It is suggested that margin be set to at the length of a
 * record in the file (if the file is structured as records).
 * 
 * This class is designed for linear scans of the mapped file.  It supports moving backwards,
 * but is less efficient if many backwards moves are requested in a row.
 * This class only supports reading operations (though the principles should work for 
 * writing as well, there has been no need).  
 * 
 * Where this class shares method names with java.nio.ByteBuffer, the operations 
 * performed are comparable EXCEPT items are indexed by long's instead of ints.
 * 
 * 
 * THIS CLASS IS NOT THREAD SAFE. It uses a stateful cursor and no synchronization...
 * 
 * THIS CLASS ASSUMES THE FILE SIZE DOES NOT CHANGE.  To compensate for files
 * that change size, the checkCapacity method should be called periodically
 * (which updates the internal measure of the file size).
 * 
 * TODO: Investigating working without 'margin'
 * TODO: Investigate ensureTo...it seems to overlap a lot with ensure and position...
 *   
 * **/
public class BigFileByteBuffer implements MappedFile {
	private final RandomAccessFile inputFile;
	private final FileChannel.MapMode mode;
	private final int bufferSize;
	private long fileSize;
	
	private ByteBuffer buffer;
	private long filePos=0;

	public BigFileByteBuffer(File source, int bufferSize) throws IOException {
		this(source, bufferSize, FileChannel.MapMode.READ_ONLY);
	}
	
	/**
	 * @param source File to read
	 * @param margin Proximity to the end of the buffer that will trigger a window slide
	 *               (essentially a guess at how many bytes will be needed from a reposition forward) 
	 * @param bufferSize Maximum size of memory map buffer to create 
	 * @throws IOException Thrown when file stream creation or memory mapping fails.
	 */
	public BigFileByteBuffer(File source, int bufferSize, FileChannel.MapMode mode) throws IOException {
		String fileMode = mode == FileChannel.MapMode.READ_ONLY ? "r" : "rw";
		inputFile = new RandomAccessFile(source, fileMode);
		FileChannel channel =  inputFile.getChannel();
		fileSize = checkCapacity();
		
		filePos = 0;
		buffer = channel.map(mode, filePos, Math.min(bufferSize, fileSize));
		
		this.mode = mode;
		this.bufferSize = bufferSize;
	}
	
	protected void finalize() {
		try {inputFile.close();}
		catch (IOException e) {}
	}
	
	/**Number of bytes in the file.**/
	public long fileSize() {return fileSize;}
	
	public byte get(long pos) {return ensure(pos, 1).get(rawOffset(pos));}
	public short getShort(long pos) {return ensure(pos, 2).getShort(rawOffset(pos));}	
	public int getInt(long pos) {return ensure(pos, 4).getInt(rawOffset(pos));}
	public long getLong(long pos) {return ensure(pos, 8).getLong(rawOffset(pos));}
	public char getChar(long pos) {return ensure(pos, 2).getChar(rawOffset(pos));}
	public float getFloat(long pos) {return ensure(pos, 4).getFloat(rawOffset(pos));}
	public double getDouble(long pos) {return ensure(pos, 8).getDouble(rawOffset(pos));}
	public byte get() {return ensure(1).get();}
	public short getShort() {return ensure(2).getShort();}	
	public int getInt() {return ensure(4).getInt();}
	public long getLong() {return ensure(8).getLong();}
	public char getChar() {return ensure(2).getChar();}
	public float getFloat() {return ensure(4).getFloat();}
	public double getDouble() {return ensure(8).getDouble();}
	public void get(byte[] target, long offset, int length) {
		ensure(offset, length).position(rawOffset(offset));
		buffer.get(target);
	}

	public void put(byte[] values) {put(values, position());}
	
	/**Write the given byte array at the given file offset.**/
	public void put(byte[] values, long offset) {
		ensure(offset, values.length).position(rawOffset(offset));
		buffer.put(values);
	}
	
	
	/**How large is the backing file?
	 * 
	 * Capacity is measured at the time that the BigFileBytBuffer is created.  
	 * To cause capacity to be calculated, invoke the checkCapacity method.
	 * @return Capacity in bytes
	 */
	public long capacity() {return fileSize;}

	
	/**Re-examine the backing file and update the stored capacity measure
	 * to reflect the file size at this time.  This update is not invoked 
	 * internally except at construction, so dynamically changing files
	 * need to have this method periodically invoked externally. 
	 *   
	 * @return The new file size (e.g., buffer "capacity").
	 * @throws IOException
	 */
	public long checkCapacity() throws IOException {
		fileSize = inputFile.getChannel().size();
		return fileSize;
	}
		
	@Override
	public long position() {return filePos+buffer.position();}
	public void position(long offset) {buffer.position(rawOffset(offset));}
	
	public ByteBuffer ensure(int bytes) {return ensure(filePos+buffer.position(), bytes);}
	public ByteBuffer ensure(long position, int bytes) {
		if ((position < filePos) || (position+bytes) > (buffer.limit()+filePos)) {
			filePos = position; 
			try {buffer = inputFile.getChannel().map(mode, filePos, Math.min(bufferSize, (fileSize-filePos)));}
			catch (IOException e) {throw new RuntimeException(String.format("Error shifting buffer position to %d for reading %d bytes.", position, bytes), e);}			
		}
		return buffer;
	}
	
	/**What does a given offset correspond to in the raw buffer.**/
	private int rawOffset(long offset) {return (int) (offset-filePos);}
	
	/* (non-Javadoc)
	 * @see ar.util.MappedFile#filePosition()
	 */ 
	@Override
	public long filePosition() {return filePos;}
}
