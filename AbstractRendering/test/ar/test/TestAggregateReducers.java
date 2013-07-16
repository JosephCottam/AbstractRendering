package ar.test;

import static org.junit.Assert.*;

import org.junit.Test;

import ar.AggregateReducer;
import ar.Aggregates;
import ar.aggregates.FlatAggregates;
import ar.renderers.AggregationStrategies;

public class TestAggregateReducers {

	@Test
	public void rollupSimpleIntegers() {
		int width=10;
		int height=12;
		AggregateReducer<Integer,Integer,Integer> red = new AggregationStrategies.Count();
		Aggregates<Integer> start = new FlatAggregates<Integer>(0,0,width,height,1);
		Aggregates<Integer> end = AggregateReducer.Strategies.foldUp(start, red);
		
		assertEquals(0, end.lowX());
		assertEquals(0, end.lowY());
		assertEquals(width/2, end.highX());
		assertEquals(height/2, end.highY());
		
		for (int x=0;x<width/2;x++) {
			for (int y=0;y<height/2;y++) {
				assertEquals(String.format("Rollup incorrect at %d, %d",x,y), (Integer) 4, end.at(x,y));
			}
		}
		
	}

}
