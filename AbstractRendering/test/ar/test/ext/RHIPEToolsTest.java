package ar.test.ext;

import static org.junit.Assert.*;

import java.awt.geom.Rectangle2D;

import org.junit.Test;

import ar.Aggregates;
import ar.Glyph;
import ar.Glyphset;
import ar.aggregates.FlatAggregates;
import ar.ext.rhipe.*;
import ar.glyphsets.implicitgeometry.Indexed;


public class RHIPEToolsTest {
	
	@Test
	public void traceEntries() {
		String cat = "cat";
		int x=1, y=2, size=3;
		String[] entry = new String[]{Integer.toString(x),Integer.toString(y),cat};
		Indexed item = new Indexed.ArrayWrapper(entry);
		RHIPETools.TraceEntry te = new RHIPETools.TraceEntry(0,1,2,size);
		
		Glyph<String> g = te.glyph(item);
		assertEquals("Category did not match.", cat, g.value());
		assertEquals("Shape did not match", new Rectangle2D.Double(x,y,size,size), g.shape());
	}
	
	@Test
	public void fromText() {
		String entries = "10,20,30,40\n11,21,31,41\n12,22,32,42\n13,23,33,43\n14,24,34,44";
		
		RHIPETools.TraceEntry te = new RHIPETools.TraceEntry(0,1,3,1);
		Glyphset.RandomAccess<String> glyphset = RHIPETools.fromText(entries, "\n", ",", te);
		
		for (int i=0; i<glyphset.size(); i++) {
			Glyph<String> g = glyphset.get(i);
			assertEquals("X mismatch on " + i, i+10, g.shape().getBounds().x);
			assertEquals("Y mismatch on " + i, i+20, g.shape().getBounds().y);
			assertEquals("Category mismatch on " + i, Integer.toString(i+40), g.value());
		}
	}
	
	@Test
	public void reduceKeys() {
		Aggregates<Integer> aggs = new FlatAggregates<Integer>(10,10,0);
		
		for(int x=aggs.lowX();x<aggs.highX(); x++) {
			for (int y=aggs.lowY();y<aggs.highY(); y++) {
				aggs.set(x, y, x*y);
			}
		}
		
		for(String s: RHIPETools.reduceKeys(aggs)) {
			String[] parts = s.split(",");
			int x = Integer.parseInt(parts[0]);
			int y = Integer.parseInt(parts[1]);
			int val = Integer.parseInt(parts[2]);
			assertEquals(String.format("Error at %d,%s", x,y), x*y,val);
		}
	}
}
