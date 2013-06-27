package ar.test;

import ar.Aggregates;
import ar.aggregates.FlatAggregates;
import static org.junit.Assert.*;

import org.junit.Test;

public class TestFlatAggregates {

	@Test
	public void Store() {
		Aggregates<Integer> aggs = new FlatAggregates<>(10,10,0);
		
		for(int x=aggs.lowX();x<aggs.highX(); x++) {
			for (int y=aggs.lowY();y<aggs.lowY(); y++) {
				aggs.set(x, y, x*y);
			}
		}
		
		for(int x=aggs.lowX();x<aggs.highX(); x++) {
			for (int y=aggs.lowY();y<aggs.lowY(); y++) {
				assertEquals(String.format("Error at %d,%s", x,y), new Integer(x*y), aggs.at(x,y));
			}
		}
		
		
	}
}
