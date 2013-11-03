package ar.test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import org.junit.Test;

import ar.Aggregates;
import ar.Glyph;
import ar.aggregates.FlatAggregates;
import ar.glyphsets.SimpleGlyph;
import ar.util.Util;

public class Utils {

	@SuppressWarnings("unchecked")
	@Test
	public void  pointBounds() {
		Rectangle2D b = Util.boundOne(new Point2D.Double(1,1));
		assertEquals(b, new Rectangle2D.Double(1,1,Double.MIN_VALUE,Double.MIN_VALUE));
		
		Glyph<Point2D,Object>[] pts = new Glyph[] {
				new SimpleGlyph<>(new Point2D.Double(1,1)),
				new SimpleGlyph<>(new Point2D.Double(2,2)),
				new SimpleGlyph<>(new Point2D.Double(-1,-1))
		};
		
		Rectangle2D bounds = Util.bounds(Arrays.asList(pts));
		assertEquals(new Rectangle2D.Double(-1,-1,3,3), bounds);
	}
	
	@Test
	public void isEqual() {
		assertTrue("nulls", Util.isEqual(null, null));
		assertFalse("left null", Util.isEqual(null, new Object()));
		assertFalse("right null", Util.isEqual(new Object(), null));
		assertTrue("really equal", Util.isEqual(new Color(255,0,0), new Color(255,0,0)));
		assertFalse("not really equal", Util.isEqual(new Color(255,0,0), new Color(0,0,0)));
	}
	
	@Test
	public void stats() {
		Aggregates<Integer> aggs = new FlatAggregates<Integer>(10,10,-1);
		
		for (int x=aggs.lowX(); x<aggs.highX(); x++) {
			for (int y = aggs.lowY(); y<aggs.highY(); y++) {
				aggs.set(x, y, x);
			}
		}
		Util.Stats<Integer> s1 = Util.stats(aggs, false);
		Util.Stats<Integer> s2 = Util.stats(aggs, true);

		assertThat(s1.max.doubleValue(), is((double) aggs.highX()-1));
		assertThat(s2.max.doubleValue(), is((double) aggs.highX()-1));
		assertThat(s1.min.doubleValue(), is((double) 0));
		assertThat(s2.min.doubleValue(), is((double) 1));
	}
	
}

