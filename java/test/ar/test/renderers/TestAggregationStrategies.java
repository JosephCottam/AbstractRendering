package ar.test.renderers;

import static org.junit.Assert.*;

import org.junit.Test;

import ar.Aggregates;
import ar.Aggregator;
import ar.aggregates.implementations.ConstantAggregates;
import ar.aggregates.implementations.RefFlatAggregates;
import ar.renderers.AggregationStrategies;
import ar.rules.Numbers;

public class TestAggregationStrategies {

	private static void testUniformRollup(int size) {
		int width=100;
		int height=121;
		Aggregator<Object,Integer> red = new Numbers.Count<>();
		Aggregates<Integer> start = new RefFlatAggregates<Integer>(0,0,width,height,1);
		Aggregates<Integer> end = AggregationStrategies.verticalRollup(start, red,size);
		
		assertEquals(0, end.lowX());
		assertEquals(0, end.lowY());
		assertEquals("Unexpeced width", width/size, end.highX());
		assertEquals("Unexpeced height", height/size, end.highY());
		
		for (int x=0;x<width/size;x++) {
			for (int y=0;y<height/size;y++) {
				assertEquals(String.format("Rollup incorrect at %d, %d",x,y), (Integer) (size*size), end.get(x,y));
			}
		}		
	}
	
	@Test
	public void rollup_2x2_UniformIntegers() {testUniformRollup(2);}
	@Test
	public void rollup_3x3_UniformIntegers() {testUniformRollup(3);}
	@Test
	public void rollup_10x10_UniformIntegers() {testUniformRollup(10);}
	
	@Test
	public void horizontalConstDefaultOptimizaton() {
		int width = 10;
		int height = 10;
		Aggregator<Object,Integer> red= new Numbers.Count<>();
		Aggregates<Integer> ten = new RefFlatAggregates<Integer>(0,0,width,height, 10);
		Aggregates<Integer> id = new ConstantAggregates<Integer>(0,0,width,height,red.identity());

		Aggregates<Integer> c1 = AggregationStrategies.horizontalRollup(ten, id, red);
		assertEquals("Error with right-side id", c1,ten);
		
		Aggregates<Integer> c2 = AggregationStrategies.horizontalRollup(id, ten, red);
		assertEquals("Error with left-side id", c2,ten);
	}
}
