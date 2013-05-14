package ar.util;

import java.nio.*;
import java.io.*;

public class MemMapEncoder {
	public static final int INT_BYTES = 4;
	public static final int DOUBLE_BYTES = 8;
	public static final int LONG_BYTES = 8;
	public static final int SHORT_BYTES = 2;
	public static final int CHAR_BYTES = 2;


	private static byte[] makeHeader(char[] types) {
		assert types != null;
		assert types.length != 0;
		for (char c: types) {
			if (c != 's' && c != 'i' && c != 'c' && c != 'd' && c != 'f' && c != 'l') {throw new IllegalArgumentException("Invalid type marker; only i,s,l,d,f,c allowed, found  '" + c + "'");}
		}

		byte[] size = intBytes(types.length);
		byte[] encoding = charBytes(types);	
		byte[] rslt = new byte[encoding.length+size.length] ;

		System.arraycopy(size, 0, rslt, 0, size.length);
		System.arraycopy(encoding, 0, rslt, size.length, encoding.length);
		return rslt;
	}

	private static byte[] intBytes(int i ){return ByteBuffer.allocate(INT_BYTES).putInt(i).array();}
	private static byte[] doubleBytes(double d) {return ByteBuffer.allocate(DOUBLE_BYTES).putDouble(d).array();}
	private static byte[] longBytes(long l) {return ByteBuffer.allocate(LONG_BYTES).putLong(l).array();}
	private static byte[] shortBytes(short s) {return ByteBuffer.allocate(SHORT_BYTES).putShort(s).array();}
	private static byte[] charBytes(char c) {return ByteBuffer.allocate(CHAR_BYTES).putChar(c).array();}
	private static byte[] charBytes(char... cs) {
		byte[][] parts = new byte[cs.length][];
		for (int i=0; i<cs.length; i++) {parts[i] = charBytes(cs[i]);}
		
		byte[] full = new byte[parts.length*CHAR_BYTES];
		for (int i=0; i<parts.length; i++) {
			System.arraycopy(parts[i], 0, full, i*CHAR_BYTES, CHAR_BYTES);
		}
		return full;
	}


	/**Get a byte array of a single data value
	 * **/
	private static byte[] asBinary(String value, char type) {
		switch (type) {
		case 'i' : return intBytes(Integer.parseInt(value));
		case 'd' : return doubleBytes(Double.parseDouble(value));
		case 'l' : return longBytes(Long.parseLong(value));
		case 's' : return shortBytes(Short.parseShort(value));
		case 'c' : return charBytes(value.charAt(0));
		default: throw new IllegalArgumentException("Unknown type: " + type);
		}			
	}

	private static byte[] asBinary(String[] entry, char[] types) {
		byte[][] entries= new byte[entry.length][];
		int total=0;
		for (int i=0;i<entry.length;i++) {
			entries[i] = asBinary(entry[i], types[i]);
			total += entries[i].length;
		}

		int offset=0;
		byte[] full = new byte[total];
		for (byte[] bentry: entries) { 
			System.arraycopy(bentry, 0, full, offset, bentry.length);
			offset += bentry.length;
		}
		return full;
	}

	public static void write(File sourceFile, int skip, File target, char[] types) throws Exception {
		CSVReader source = new CSVReader(sourceFile, skip); 
		FileOutputStream file = new FileOutputStream(target);
		
		try {
			byte[] header = makeHeader(types); 
			file.write(header);

			int entriesRead = 0;
			while(source.hasNext()) {
				String[] entry = source.next();
				if (entry == null) {continue;}
				for (int i=0;i<entry.length;i++) {
					byte[] value = asBinary(entry[i], types[i]);
					file.write(value);						
				}
				entriesRead++;
				if (entriesRead % 100000 ==0) {System.out.printf("Processed %s entries.\n", entriesRead);}
			}
			
			System.out.printf("Processed %s entries.\n", entriesRead);
			
		} finally {file.close();}
	}

	
	private static String entry(String[] args, String key, String defVal) {
		int i=0;
		key = key.toUpperCase();
		for (i=0; i< args.length; i++) {if (args[i].toUpperCase().equals(key)) {break;}}
		if (i>=0 && args[i].toUpperCase().equals(key)) {return args[i+1];}
		return defVal;
	}
	public static void main(String[] args) throws Exception {
		System.out.println("Usage: MemMapEncoder -in <file> -out <file> -skip <int> -types <string>");
		System.out.println("Type string is a string made up of s/i/l/d/f/c for short/int/long/double/float/char.");
		
		File in = new File(entry(args, "-in", null));
		File out = new File(entry(args, "-out", null));
		int skip = Integer.parseInt(entry(args, "-skip", null));
		char[] types = entry(args, "-types", "").toCharArray();
		
		write(in, skip, out, types);
	}
}

