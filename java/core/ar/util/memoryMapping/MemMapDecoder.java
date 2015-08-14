package ar.util.memoryMapping;

import java.nio.channels.FileChannel;
import java.io.*;

import ar.glyphsets.implicitgeometry.IndexedEncoding;
import ar.util.memoryMapping.MemMapEncoder.Header;

/**Utility reading hbin encodings**/
public class MemMapDecoder {

	
	public static void main(String[] args) throws IOException {
		File source = new File(args[0]);

		final BigFileByteBuffer buffer = new BigFileByteBuffer(source , 1000, FileChannel.MapMode.READ_WRITE);
		Header header = Header.from(buffer);
		
		final long entries = (buffer.fileSize()-header.dataTableOffset)/header.recordLength;
		
		for (long i=0;i<entries; i++) {
			final long recordOffset = (i*header.recordLength)+header.dataTableOffset;
			final IndexedEncoding enc = new IndexedEncoding(header.types, recordOffset, buffer);
			for (int f=0; f<header.types.length; f++) {
				Object v = enc.get(f);
				System.out.print(v);
				System.out.print(", ");
			}
			System.out.println();
		}		
	}
}

