package ar.test.glyphsets;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import ar.glyphsets.GlyphList;
import ar.glyphsets.MemMapList;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.util.DelimitedReader;
import ar.util.GlyphsetLoader;
import ar.util.MemMapEncoder;
import ar.util.MemMapEncoder.TYPE;

public class MemoryMappingTests {
	
	@Test
	public void EncodeDecode() throws Exception {
		String csvName = "../data/circlepoints.csv";
		String hbinName = "../data/circlepoints.hbin";
		
		File csv = new File(csvName);
		File hbin = new File(hbinName);
		
		assertTrue("Source file not found: " + csvName, csv.exists());
		if (hbin.exists()) {hbin.delete();}
		MemMapEncoder.write(csv, 1, hbin, "xxddi".toCharArray());
		assertTrue("hbin not found after encode.", hbin.exists());
		
		MemMapList<Integer> mm = new MemMapList<Integer>(hbin, new Indexed.ToRect(1, 0, 1), new Indexed.ToValue<Integer,Integer>(2));
		GlyphList<Integer> ref = (GlyphList<Integer>) GlyphsetLoader.load(
				new ar.glyphsets.GlyphList<Integer>(), 
				new DelimitedReader(csv, 1, "\\s*,\\s*"),
				new Indexed.Converter(null, TYPE.X, TYPE.X, TYPE.DOUBLE, TYPE.DOUBLE, TYPE.INT),
				new Indexed.ToRect(1, 2, 3), new Indexed.ToValue<Integer,Integer>(4));
		
		assertEquals("Sizes don't match.", ref.size(), mm.size());
		for (int i=0; i< ref.size(); i++) {
			assertEquals("Mismatched shape at " + i, ref.get(i).shape(), mm.get(i).shape());
			assertEquals("Mismatched value at " + i, ref.get(i).value(), mm.get(i).value());
		}
		
	}

}
