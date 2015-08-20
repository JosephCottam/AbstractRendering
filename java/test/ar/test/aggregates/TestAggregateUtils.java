package ar.test.aggregates;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import ar.Aggregates;
import ar.Aggregator;
import ar.aggregates.AggregateUtils;
import ar.aggregates.implementations.ConstantAggregates;
import ar.aggregates.implementations.RefFlatAggregates;
import ar.rules.Numbers;
import ar.test.AllEqual;

public class TestAggregateUtils {
	@Test
	public void alignedMerge() {
		int width = 10;
		int height = 10;
		Aggregates<Integer> ten = new ConstantAggregates<Integer>(10, 0,0,width,height);
		Aggregates<Integer> two = new ConstantAggregates<Integer>(2, 0,0,width,height);
		
		assertThat("Left greather than right",
				   AggregateUtils.alignedMerge(ten, two, false, (l, r) -> (l > r)),
				   new AllEqual<>(new ConstantAggregates<>(true, 0,0, width, height)));
		
		assertThat("Right less than left",
				   AggregateUtils.alignedMerge(two, ten, true, (l, r) -> (l > r)),
				   new AllEqual<>(new ConstantAggregates<>(false, 0,0, width, height)));
		
		assertThat("Sum in merge",
					AggregateUtils.alignedMerge(two, ten, 0d, (l, r) -> ((double) (l + r))),
					new AllEqual<>(new ConstantAggregates<>(12d, 0,0, width, height)));
		
	}
	
	private static void testUniformRollup(int size) {
		int width=100;
		int height=121;
		Aggregator<Object,Integer> red = new Numbers.Count<>();
		Aggregates<Integer> start = new RefFlatAggregates<Integer>(0,0,width,height,1);
		Aggregates<Integer> end = AggregateUtils.coarsen(start, red,size);
		
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
		Aggregates<Integer> id = new ConstantAggregates<Integer>(red.identity(), 0,0,width,height);

		Aggregates<Integer> c1 = AggregateUtils.__unsafeMerge(ten, id, red.identity(), red::rollup);
		assertThat("Error with right-side id", c1, is(ten));
		
		Aggregates<Integer> c2 = AggregateUtils.__unsafeMerge(id, ten, red.identity(), red::rollup);
		assertThat("Error with left-side id", c2, is(ten));
	}
}
