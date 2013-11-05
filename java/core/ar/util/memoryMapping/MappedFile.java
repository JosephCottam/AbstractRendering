package ar.util.memoryMapping;

public interface MappedFile {

	/**Number of bytes in the file.**/
	public abstract long fileSize();

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

	/**Where in the file is the cursor current?**/
	public abstract long position();

	/**What byte of the backing file does the zero-buffer position correspond to?*/
	public abstract long filePosition();

}