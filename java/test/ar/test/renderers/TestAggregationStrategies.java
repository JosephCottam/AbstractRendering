package ar.test.renderers;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.Test;

import ar.Aggregates;
import ar.Aggregator;
import ar.aggregates.implementations.ConstantAggregates;
import ar.aggregates.implementations.RefFlatAggregates;
import ar.renderers.AggregationStrategies;
import ar.rules.Numbers;
import ar.rules.combinators.Split;
import ar.test.AllEqual;

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
	public void horizontalMerge() {
		int width = 10;
		int height = 10;
		Aggregates<Integer> ten = new ConstantAggregates<Integer>(0,0,width,height, 10);
		Aggregates<Integer> two = new ConstantAggregates<Integer>(0,0,width,height, 2);
		
		Split.Merge<Integer, Integer, Boolean> gt = new Split.Merge<Integer, Integer, Boolean>() {
			@Override public Boolean merge(Integer left, Integer right) {return left > right;}
			@Override public Boolean identity() {return false;}
		};
		
		Split.Merge<Integer, Integer, Double> sum = new Split.Merge<Integer, Integer, Double>() {
			@Override public Double merge(Integer left, Integer right) {return (double) (left + right);}
			@Override public Double identity() {return 0d;}
		};
		
		assertThat("Left greather than right",
				   AggregationStrategies.horizontalMerge(ten, two, gt),
				   new AllEqual<>(new ConstantAggregates<>(0,0, width, height, true)));
		
		assertThat("Right less than left",
				   AggregationStrategies.horizontalMerge(two, ten, gt),
				   new AllEqual<>(new ConstantAggregates<>(0,0, width, height, false)));
		
		assertThat("Sum in merge",
					AggregationStrategies.horizontalMerge(two, ten, sum),
					new AllEqual<>(new ConstantAggregates<>(0,0, width, height, 12d)));
		
	}
	
	@Test
	public void horizontalConstDefaultOptimizaton() {
		int width = 10;
		int height = 10;
		Aggregator<Object,Integer> red= new Numbers.Count<>();
		Aggregates<Integer> ten = new RefFlatAggregates<Integer>(0,0,width,height, 10);
		Aggregates<Integer> id = new ConstantAggregates<Integer>(0,0,width,height,red.identity());

		Aggregates<Integer> c1 = AggregationStrategies.horizontalRollup(ten, id, red);
		assertThat("Error with right-side id", c1, is(ten));
		
		Aggregates<Integer> c2 = AggregationStrategies.horizontalRollup(id, ten, red);
		assertThat("Error with left-side id", c2, is(ten));
	}
}
