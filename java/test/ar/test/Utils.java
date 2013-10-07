package ar.test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.awt.Color;

import org.junit.Test;

import ar.Aggregates;
import ar.aggregates.FlatAggregates;
import ar.util.Util;

public class Utils {

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
		Util.Stats s1 = Util.stats(aggs, false);
		Util.Stats s2 = Util.stats(aggs, true);

		assertThat(s1.max, is((double) aggs.highX()-1));
		assertThat(s2.max, is((double) aggs.highX()-1));
		assertThat(s1.min, is((double) 0));
		assertThat(s2.min, is((double) 1));
	}
	
}

