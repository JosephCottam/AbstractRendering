package ar.test.glyphsets;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ar.Glyphset;
import ar.glyphsets.GlyphList;
import ar.glyphsets.MemMapList;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.util.BigFileByteBuffer;
import ar.util.DelimitedReader;
import ar.util.GlyphsetLoader;
import ar.util.IndexedEncoding;
import ar.util.MemMapEncoder;
import ar.util.MemMapEncoder.TYPE;

public class MemoryMappingTests {
	private static String csvName = "../data/circlepoints.csv";
	private static String hbinName = "../data/circlepointsTests.hbin";

	private static Glyphset.RandomAccess<Integer> mm = null;
	private static Glyphset.RandomAccess<Integer> ref = null;
	
	@BeforeClass
	public static void makeHbin() throws Exception {
		File csv = new File(csvName);
		File hbin = new File(hbinName);
		
		assertTrue("Source file not found: " + csvName, csv.exists());
		if (hbin.exists()) {hbin.delete();}
		MemMapEncoder.write(csv, 1, hbin, "xxddi".toCharArray());
		assertTrue("hbin not found after encode.", hbin.exists());
		
		mm = new MemMapList<Integer>(hbin, new Indexed.ToRect(1, 0, 1), new Indexed.ToValue<Integer,Integer>(2));
		ref = (GlyphList<Integer>) GlyphsetLoader.load(
				new ar.glyphsets.GlyphList<Integer>(), 
				new DelimitedReader(csv, 1, "\\s*,\\s*"),
				new Indexed.Converter(null, TYPE.X, TYPE.X, TYPE.DOUBLE, TYPE.DOUBLE, TYPE.INT),
				new Indexed.ToRect(1, 2, 3), new Indexed.ToValue<Integer,Integer>(4));
	}
	
	@AfterClass
	public static void removeTemps() throws Exception {
		File hbin = new File(hbinName);
		if (hbin.exists()) {hbin.delete();}
	}
	
	@Test
	public void EncodeDecode() throws Exception {
		assertEquals("Sizes don't match.", ref.size(), mm.size());
		for (int i=0; i< ref.size(); i++) {
			assertEquals("Mismatched shape at " + i, ref.get(i).shape(), mm.get(i).shape());
			assertEquals("Mismatched value at " + i, ref.get(i).value(), mm.get(i).value());
		}
	}
	
	@Test
	public void bounds() throws Exception {
		assertEquals(ref.bounds(), mm.bounds());
	}


	@Test
	public void minMax() throws Exception {
		BigFileByteBuffer buffer = new BigFileByteBuffer(new File(hbinName), 100,1000);
		MemMapEncoder.Header header = MemMapEncoder.Header.from(buffer);
		
		IndexedEncoding maxEntry = new IndexedEncoding(header.types, header.maximaRecordOffset, buffer);
		IndexedEncoding minEntry = new IndexedEncoding(header.types, header.minimaRecordOffset, buffer);
		
		double max = Double.MIN_VALUE, min=Double.MAX_VALUE;
		
		long cursor = header.dataTableOffset;
		while(cursor < buffer.fileSize()) {
			IndexedEncoding entry = new IndexedEncoding(header.types, cursor, buffer);
			cursor += header.recordLength;
			max = Math.max(max, (Double) entry.get(0));
			min = Math.min(min, (Double) entry.get(0));
		}
		
		assertEquals("Max mismatch", max, maxEntry.get(0));
		assertEquals("Min mismatch", min, minEntry.get(0));
		
	}
	
}
