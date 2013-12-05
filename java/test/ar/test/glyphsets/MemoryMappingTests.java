package ar.test.glyphsets;

import static org.junit.Assert.*;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.Iterator;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ar.Glyph;
import ar.Glyphset;
import ar.glyphsets.GlyphList;
import ar.glyphsets.MemMapList;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.IndexedEncoding;
import ar.util.DelimitedReader;
import ar.util.memoryMapping.BigFileByteBuffer;
import ar.util.memoryMapping.MemMapEncoder;
import ar.util.memoryMapping.MemMapEncoder.TYPE;
import ar.util.Util;

public class MemoryMappingTests {
	private static String csvName = "../data/circlepoints.csv";
	private static String hbinName = "../data/circlepointsTests.hbin";

	private static Glyphset.RandomAccess<Rectangle2D, Integer> mm = null;
	private static Glyphset.RandomAccess<Rectangle2D, Integer> ref = null;
	
	@BeforeClass
	public static void makeHbin() throws Exception {
		File csv = new File(csvName);
		File hbin = new File(hbinName);
		
		assertTrue("Source file not found: " + csvName, csv.exists());
		if (hbin.exists()) {hbin.delete();}
		MemMapEncoder.write(csv, 1, hbin, "xxddi".toCharArray());
		assertTrue("hbin not found after encode.", hbin.exists());
		
		mm = new MemMapList<Rectangle2D, Integer>(hbin, new Indexed.ToRect(1, 0, 1), new Indexed.ToValue<Integer,Integer>(2));
		ref = (GlyphList<Rectangle2D, Integer>) Util.load(
				new ar.glyphsets.GlyphList<Rectangle2D, Integer>(), 
				new DelimitedReader(csv, 1, "\\s*,\\s*"),
				new Indexed.Converter(TYPE.X, TYPE.X, TYPE.DOUBLE, TYPE.DOUBLE, TYPE.INT),
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
			assertEquals("Mismatched value at " + i, ref.get(i).info(), mm.get(i).info());
		}
	}
	
	@Test
	public void bounds() throws Exception {
		assertEquals(ref.bounds(), mm.bounds());
	}


	@Test
	public void minMax() throws Exception {
		BigFileByteBuffer buffer = new BigFileByteBuffer(new File(hbinName), 1000);
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
	
	@Test
	public void segment() throws Exception {
		Glyphset<Rectangle2D, Integer> glyphs = mm.segment(0, 10);
		assertEquals("Subset segment check", glyphs.segments(), 10);
		assertEquals("Subse size check", glyphs.size(), 10);
		for (Glyph<Rectangle2D, Integer> g: glyphs) {
			g.toString();
		}
		
		Glyphset<Rectangle2D, Integer> glyphsA = mm.segment(0, mm.segments());
		Glyphset<Rectangle2D, Integer> glyphsB = glyphsA.segment(0, glyphsA.segments());
		assertEquals(mm.bounds(), glyphsA.bounds());
		assertEquals(mm.bounds(), glyphsB.bounds());
		
		Glyphset<Rectangle2D, Integer> glyphs2 = glyphs.segment(0, 10).segment(0, 5);
		assertEquals("Subset-subset segment check", 5, glyphs2.segments());
		assertEquals("Subset-subset size check", 5, glyphs2.size());
		glyphsEqual("Subset-subset", glyphs, 0, glyphs2);
		
		glyphsEqual("Subset-subset", mm, 4, glyphs.segment(1,10).segment(3,9));
		
	}
	
	/**Check that items in one glyphset equal those in another.
	 * 
	 * @param baseMessage Appended to error messages
	 * @param original Items to be compared to
	 * @param offset Where to start comparison in the original (will start comparison at the offset-th item)
	 * @param generated Items to compare to.
	 */
	private static <G,I> void glyphsEqual(String baseMessage, final Glyphset<G,I> original, int offset, final Glyphset<G,I> generated) {
		Iterator<Glyph<G,I>> refItems = original.iterator();
		Iterator<Glyph<G,I>> resItems = generated.iterator();
		
		for (int i=0; i<offset; i++) {refItems.next();}
		
		int i=0;
		while (refItems.hasNext() && resItems.hasNext()) {
			Glyph<G,I> ref = refItems.next();
			Glyph<G,I> res = resItems.next();
			assertEquals(String.format("%s info failed at %d", baseMessage, i), ref.info(), res.info());
			assertEquals(String.format("%s shape failed at %d", baseMessage, i), ref.shape(), res.shape());
			i++;
		}
	}
	
}
