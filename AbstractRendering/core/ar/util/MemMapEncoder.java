package ar.util;

import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.io.*;

/**Utility for encoding delimited files into a binary format that
 * can be read by the included memory mapped list.  
 * 
 * To properly encode values, a field-type descriptor must be supplied.
 * This is a string where each character indicates the type of the associated
 * field in the source file.  The valid characters are :
 * 
 *   + s -- Short (two bytes)
 *   + i -- Int (four bytes)
 *   + l -- Long (eight bytes)
 *   + f -- Float (four bytes)
 *   + d -- Double (eight bytes)
 *   + c -- Char (two bytes)
 *   + b -- Byte (one byte)
 *   + v -- VarChar (eight bytes) 
 *
 * Additionally 'x' can be used to indicate that a source-file field should not
 * be included in the output file. VarChar ('v') entries are actually pointers to the string table.
 * 
 * NOTE: 'v' does not work yet. 
 * 
 * File format: header + info + strings + data
 * 
 * Header:
 * 
 * + Version Number (Int): Decoders should verify that they are ready for files encoded with the given version
 * + Data Offset (Long): Where is the first data record
 * + String Offset (Long): Where is the string table? (negative if there is no string table)
 * + Record Size (Int): How many fields are in each record
 * + Record Types ([Char]): Type characters (described above), one for each field.  Cannot include 'x'
 * + Info Records: Metadata not be required to interpret the file.  Currently two data records to provide max/min values for columns.  
 */
public class MemMapEncoder {
	/**(Magic) Number as the first value in the file to indicate what version of the format was used.*/
	public static final int VERSION_ID = -1;
	
	/**Types the encoder understands.
	 * The "X" type is used to indicate that the field is being skipped.
	 */
	@SuppressWarnings("javadoc")
	public enum TYPE {
		INT(4), DOUBLE(8), LONG(8), SHORT(2), BYTE(1), CHAR(2), FLOAT(4), X(0);
		
		/**How many bytes is this type encoded with?**/
		public final int bytes;
		private TYPE(int bytes) {this.bytes=bytes;}
		
		public static TYPE typeFor(char t) {
			if (t=='i') {return TYPE.INT;}  
			else if (t=='l') {return TYPE.LONG;}
			else if (t=='s') {return TYPE.SHORT;}
			else if (t=='d') {return TYPE.DOUBLE;} 
			else if (t=='f') {return TYPE.FLOAT;}
			else if (t=='b') {return TYPE.BYTE;}
			else if (t=='x') {return TYPE.X;}
			else {throw new RuntimeException(String.format("Unknown type indicator '%s'", t));}
		}
	}
	
	/**Container for information found in the header.**/
	@SuppressWarnings("javadoc")
	public static final class Header {
		public final int version;
		public final long dataTableOffset;
		public final long stringTableOffset;
		public final TYPE[] types;
		public final int recordLength;
		public final long maximaRecordOffset;
		public final long minimaRecordOffset;
		
		public Header(int version, TYPE[] types, long dataTableOffset, long stringTableOffset, long infoRecordOffset) {
			this.version = version;
			this.dataTableOffset = dataTableOffset;
			this.stringTableOffset = stringTableOffset;
			this.types = types;
			this.recordLength = recordLength(types);
			this.maximaRecordOffset = infoRecordOffset;
			this.minimaRecordOffset = infoRecordOffset+recordLength;
		}
		
		/**Parse a given file, return a Header object.**/
		public static Header from(BigFileByteBuffer buffer) {
			int version = buffer.getInt();
			if (version != VERSION_ID) {
				throw new IllegalArgumentException(String.format("Unexpected version number in file %d; expected %d", version, VERSION_ID));
			}

			long dataTableOffset = buffer.getLong();
			long stringTableOffset = buffer.getLong();
			
			int recordEntries = buffer.getInt();

			TYPE[] types = new TYPE[recordEntries];
			for (int i =0; i<recordEntries; i++) {
				char t = buffer.getChar();
				types[i] = TYPE.typeFor(t);
			}
			
			long infoRecordOffset = buffer.position();
			
			
			return new Header(version, types, dataTableOffset, stringTableOffset, infoRecordOffset);
		}
		
	}
	
	
	/**Utility for append byte arrays together.**/
	private static byte[] append(byte[]... allBytes) {
		int len = 0;
		for (byte[] bt: allBytes) {len +=bt.length;}
		
		int offset =0;
		byte[] combined = new byte[len];
		for (byte[] bt: allBytes) {
			System.arraycopy(bt, 0, combined, offset, bt.length);
			offset += bt.length;
		}
		return combined;
	}

	

	/**Which are the types of the fields kept (e.g. are not 'x')**/
	private static char[] keepTypes(char[] types) {
		ArrayList<Character> keeping = new ArrayList<Character>();
		for (char c: types) {
			if (c != 's' && c != 'i' && c != 'c' && c != 'd' && c != 'f' && c != 'l' && c != 'x') {
				throw new IllegalArgumentException("Invalid type marker; only i,s,l,d,f,c,x allowed, found  '" + c + "'");
			} else if(c!='x') {keeping.add(c);}
		}
		char[] keep = new char[keeping.size()];
		for (int i=0; i< keep.length;i ++) {keep[i]=keeping.get(i);}
		return keep;
	}
	
	private static int recordLength(char[] types) {
		int acc = 0;
		for (char c: types) {acc += TYPE.typeFor(c).bytes;}
		return acc;
	}
	
	/**How many bytes in a record?*/
	public static int recordLength(TYPE[] types) {
		int acc = 0;
		for (TYPE t: types) {acc += t.bytes;}
		return acc;
	}
	
	/**Calculate the field offsets for records.**/
	public static int[] recordOffsets(final MemMapEncoder.TYPE[] types) {
		int acc=0;
		int[] offsets = new int[types.length];
		for (int i=0; i<types.length; i++) {
			offsets[i]=acc;
			acc+=types[i].bytes;
		}
		return offsets;
	}
	

	/**Construct a header with spaces for string offset, data offset and info records to be filled in later.**/
	private static byte[] makeHeader(char[] types) {
		byte[] version = intBytes(VERSION_ID);
		byte[] recordHeader= recordHeader(types);
		byte[] stringOffset = longBytes(-1);
		byte[] minRecord = new byte[recordLength(types)];
		byte[] maxRecord = new byte[recordLength(types)];
		int headerSize = version.length+recordHeader.length+stringOffset.length+minRecord.length+maxRecord.length+TYPE.LONG.bytes;
		byte[] dataOffset = longBytes(headerSize);
		
		return append(version, dataOffset, stringOffset, recordHeader, minRecord, maxRecord);
	}
	
	/**Type header for the individual records.**/
	private static byte[] recordHeader(char[] types) {
		assert types != null;
		assert types.length != 0;

		char[] keepTypes = keepTypes(types);
		byte[] size = intBytes(keepTypes.length);
		byte[] encoding = charBytes(keepTypes);	

		return append(size, encoding);
	}

	private static byte[] shortBytes(short s) {return ByteBuffer.allocate(TYPE.SHORT.bytes).putShort(s).array();}
	private static byte[] intBytes(int i){return ByteBuffer.allocate(TYPE.INT.bytes).putInt(i).array();}
	private static byte[] longBytes(long l) {return ByteBuffer.allocate(TYPE.LONG.bytes).putLong(l).array();}
	private static byte[] floatBytes(float d) {return ByteBuffer.allocate(TYPE.FLOAT.bytes).putFloat(d).array();}
	private static byte[] doubleBytes(double d) {return ByteBuffer.allocate(TYPE.DOUBLE.bytes).putDouble(d).array();}
	private static byte[] charBytes(char c) {return ByteBuffer.allocate(TYPE.CHAR.bytes).putChar(c).array();}
	private static byte[] charBytes(char... cs) {
		byte[][] parts = new byte[cs.length][];
		for (int i=0; i<cs.length; i++) {parts[i] = charBytes(cs[i]);}
		
		byte[] full = new byte[parts.length*TYPE.CHAR.bytes];
		for (int i=0; i<parts.length; i++) {
			System.arraycopy(parts[i], 0, full, i*TYPE.CHAR.bytes, TYPE.CHAR.bytes);
		}
		return full;
	}


	/**Get a byte array of a single data value**/
	private static byte[] asBinary(String value, char type) {
		switch (type) {
		case 's' : return shortBytes(Short.parseShort(value));
		case 'i' : return intBytes(Integer.parseInt(value));
		case 'l' : return longBytes(Long.parseLong(value));
		case 'f' : return floatBytes(Float.parseFloat(value));
		case 'd' : return doubleBytes(Double.parseDouble(value));
		case 'c' : return charBytes(value.charAt(0));
		default: throw new IllegalArgumentException("Unknown type: " + type);
		}			
	}

	/**Write from source text to indicated binary files.**/ 
	public static void write(File sourceFile, int skip, File target, char[] types) throws Exception {
		DelimitedReader source = new DelimitedReader(sourceFile, skip, DelimitedReader.CSV); 
		FileOutputStream file = new FileOutputStream(target);
		
		int entriesRead = 0;
		try {
			byte[] header = makeHeader(types); 
			file.write(header);

			while(source.hasNext()) {
				String[] entry = source.next();
				if (entry == null) {continue;}
				for (int i=0;i<types.length;i++) {
					if (types[i]=='x') {continue;}
					byte[] value = asBinary(entry[i], types[i]);
					file.write(value);						
				}
				entriesRead++;
				if (entriesRead % 100000 ==0) {System.out.printf("Processed %,d entries.\n", entriesRead);}
			}
			System.out.printf("Processed %,d entries.\n", entriesRead);
			updateMinMax(target);
			
		}catch (Exception e) {
			throw new RuntimeException(String.format("Error on or near entry %,d", entriesRead), e);
		} finally {file.close();}
	}


	@SuppressWarnings("resource")
	private static void copy(File source, File target) throws Exception {
		if (!target.exists()) {target.createNewFile();}
		
		FileChannel in = null, out=null;
		try {
			in = new FileInputStream(source).getChannel();
			out = new FileOutputStream(target).getChannel();

			long count = 0;
	        long size = in.size();              
	        while((count += out.transferFrom(in, count, size-count))<size) {}
		} finally {
			if (in != null) {in.close();}
			if (out != null) {out.close();}
		}
	}
	
	private static String entry(String[] args, String key, String defVal) {
		int i=0;
		key = key.toUpperCase();
		for (i=0; i< args.length; i++) {if (args[i].toUpperCase().equals(key)) {break;}}
		if (i<args.length && i>=0 && args[i].toUpperCase().equals(key)) {return args[i+1];}
		return defVal;
	}


	/**Get a byte array of a single data value**/
	private static byte[] encode(Object value, TYPE type) {
		switch (type) {
		case SHORT : return shortBytes((Short) value);
		case INT : return intBytes((Integer) value);
		case LONG : return longBytes((Long) value);
		case FLOAT : return floatBytes((Float) value);
		case DOUBLE : return doubleBytes((Double) value);
		case CHAR : return charBytes((Character) value);
		default: throw new IllegalArgumentException("Unknown type: " + type);
		}			
	}

	
	private static byte[] encodeArray(Number[] nums, TYPE[] types) {
		int recordLength = recordLength(types);
		byte[] rslt = new byte[recordLength];
		int offset=0;
		for (int i=0; i<nums.length;i++) {
			byte[] nb = encode(nums[i], types[i]);
			System.arraycopy(nb, 0, rslt, offset, nb.length);
			offset+=nb.length;
		}
		return rslt;
	}
	
	private static void updateMinMax(File out) throws IOException {
		final BigFileByteBuffer buffer = new BigFileByteBuffer(out, 100, 1000, FileChannel.MapMode.READ_WRITE);
		Header header = Header.from(buffer);
		
		final long entries = (buffer.fileSize()-header.dataTableOffset)/header.recordLength;
		
		final Number[] maxima = new Number[header.types.length];
		final Number[] minima = new Number[header.types.length];
		
		for (long i=0;i<entries; i++) {
			final long recordOffset = (i*header.recordLength)+header.dataTableOffset;
			buffer.ensure(recordOffset, header.recordLength);
			final IndexedEncoding enc = new IndexedEncoding(header.types, buffer.rawOffset(recordOffset), buffer.rawBuffer());
			for (int f=0; f<header.types.length; f++) {
				Object v = enc.get(f);
				if (v instanceof Number) {
					Number n = (Number) v;
					maxima[f] = gt(maxima[f], n) ? maxima[f] : n;
					minima[f] = lt(minima[f], n) ? minima[f] : n;
				}
			}
		}
		
		byte[] maxs = encodeArray(maxima, header.types);
		byte[] mins = encodeArray(minima, header.types);
		buffer.put(maxs, header.maximaRecordOffset);
		buffer.put(mins, header.minimaRecordOffset);
	}
	
	private static boolean gt(Number a,  Number b) {
		if (a == null) {return false;}
		if (b == null) {return false;}
		if (a == b) {return false;}
		if (a.doubleValue() > b.doubleValue()) {return true;}
		return false;
	}
	
	private static boolean lt(Number a,  Number b) {
		if (a == null) {return false;}
		if (b == null) {return false;}
		if (a == b) {return false;}
		if (a.doubleValue() < b.doubleValue()) {return true;}
		return false;
	}

		
	/**Utility for converting CSVs to header-carrying binary encodings.**/
	public static void main(String[] args) throws Exception {
		System.out.println("Usage: MemMapEncoder -in <file> -out <file> -skip <int> -types <string>");
		System.out.println("Type string is a string made up of s/i/l/f/d/c for short/int/long/float/double/char.");
		System.out.println();
		
		File temp;
		File in = new File(entry(args, "-in", null));
		File out = new File(entry(args, "-out", null));
		boolean direct = !entry(args, "-direct", "FALSE").toUpperCase().equals("FALSE");
		
		if (direct) {temp =out;}
		
		else {
			temp = File.createTempFile("hbinEncoder", "hbin");
			temp.deleteOnExit();
		}
		
		
		int skip = Integer.parseInt(entry(args, "-skip", null));
		char[] types = entry(args, "-types", "").toCharArray();
		
		write(in, skip, temp, types);
		
		if (!direct) {
			try {
				out.delete();
				boolean moved = temp.renameTo(out);
				if (!moved) {copy(temp, out);} //Needed because rename doesn't work across file systems
			} catch (Exception e) {throw new RuntimeException("Error moving temporaries to final destination file.",e);}
			if (!out.exists()) {throw new RuntimeException("File could not be moved from temporary location to permanent location for unknown reason.");}
		}
	}
}

