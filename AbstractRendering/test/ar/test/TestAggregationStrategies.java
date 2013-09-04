package ar.test;

import static org.junit.Assert.*;

import org.junit.Test;

import ar.Aggregates;
import ar.Aggregator;
import ar.aggregates.FlatAggregates;
import ar.renderers.AggregationStrategies;
import ar.rules.Numbers;

public class TestAggregationStrategies {

	private static void testUniformRollup(int size) {
		int width=100;
		int height=121;
		Aggregator<Object,Integer> red = new Numbers.Count<>();
		Aggregates<Integer> start = new FlatAggregates<Integer>(0,0,width,height,1);
		Aggregates<Integer> end = AggregationStrategies.verticalRollup(start, red,size);
		
		assertEquals(0, end.lowX());
		assertEquals(0, end.lowY());
		assertEquals(width/2, end.highX());
		assertEquals(height/2, end.highY());
		
		for (int x=0;x<width/2;x++) {
			for (int y=0;y<height/2;y++) {
				assertEquals(String.format("Rollup incorrect at %d, %d",x,y), (Integer) (size*size), end.get(x,y));
			}
		}		
	}
		
	@Test
	public void rollup_2x2_UniformIntegers() {testUniformRollup(2);}
	public void rollup_3x3_UniformIntegers() {testUniformRollup(3);}
	public void rollup_10x10_UniformIntegers() {testUniformRollup(10);}
}
