package ar.util;

import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.io.*;


import static ar.glyphsets.PointMemMapList.TYPE;

/**Utility for encoding delimited files into a binary format that
 * can be read by the included mem-mapped list.  
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

 * Additionally 'x' can be used to indicate that a source-file field should not
 * be included in the output file.
 * 
 * The files that result from this encoder carry a header that describes
 * the fields of the file.  The header is an integer that indicates how many
 * fields the entries have followed by one character (two bytes) for each 
 * entry type.  The entry type characters match those passed to the encoder 
 * (except that 'x' is not valid in the header).
 * 
 * TODO: Add support for strings (type 'V').  Multi-segmented file or multiple files, where one file is the table, the other is a string-table.  Talbe file stores offsets into string-table for string values
 * 
 * @author jcottam
 */
public class MemMapEncoder {
	
	

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
	
	private static byte[] makeHeader(char[] types) {
		assert types != null;
		assert types.length != 0;

		char[] keepTypes = keepTypes(types);
		byte[] size = intBytes(keepTypes.length);
		byte[] encoding = charBytes(keepTypes);	
		byte[] rslt = new byte[encoding.length+size.length] ;

		System.arraycopy(size, 0, rslt, 0, size.length);
		System.arraycopy(encoding, 0, rslt, size.length, encoding.length);
		return rslt;
	}

	private static byte[] shortBytes(short s) {return ByteBuffer.allocate(TYPE.SHORT.bytes).putShort(s).array();}
	private static byte[] intBytes(int i ){return ByteBuffer.allocate(TYPE.INT.bytes).putInt(i).array();}
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


	public static void write(File sourceFile, int skip, File target, char[] types) throws Exception {
		DelimitedReader source = new DelimitedReader(sourceFile, skip, DelimitedReader.CSV); 
		FileOutputStream file = new FileOutputStream(target);
		
		try {
			byte[] header = makeHeader(types); 
			file.write(header);

			int entriesRead = 0;
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
	        while((count += out.transferFrom(in, count, size-count))<size);
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
	
	public static void main(String[] args) throws Exception {
		System.out.println("Usage: MemMapEncoder -in <file> -out <file> -skip <int> -types <string>");
		System.out.println("Type string is a string made up of s/i/l/f/d/c for short/int/long/float/double/char.");
		
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

