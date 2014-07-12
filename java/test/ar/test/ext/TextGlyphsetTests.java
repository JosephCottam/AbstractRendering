package ar.test.ext;

import static org.junit.Assert.*;

import java.awt.geom.Point2D;
import java.io.File;

import org.junit.Test;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import ar.Glyph;
import ar.Glyphset;
import ar.ext.textfile.DelimitedFile;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Indexed.Converter.TYPE;;


public class TextGlyphsetTests {
	private static String csvName = "../data/circlepoints.csv";

	public static Glyphset<Point2D, Integer> glyphset() {
		return new DelimitedFile<>(
				new File(csvName), 
				',', 
				new TYPE[]{TYPE.DOUBLE,TYPE.DOUBLE,TYPE.DOUBLE,TYPE.DOUBLE,TYPE.INT},
				1,
				new Indexed.ToPoint(false, 2,3), 
				new Indexed.ToValue<Indexed, Integer>(4));
	}
	
	@Test
	public void recordCount() {
		long size = glyphset().size();
		assertEquals("Record count mismatch.", 1499, size);
	}
	
	@Test
	public void firstRecord() {
		Glyph<Point2D, Integer> g = glyphset().iterator().next();
		assertThat("X incorrect", g.shape().getX(), is(0.297706106));
		assertThat("Y incorrect", g.shape().getY(), is(-0.799676579));
		assertThat("Value incorrect", g.info(), is(0));
	}
}
