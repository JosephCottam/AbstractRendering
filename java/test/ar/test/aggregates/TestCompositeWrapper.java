package ar.test.aggregates;

import org.junit.Test;

import ar.Aggregates;
import ar.aggregates.implementations.ConstantAggregates;
import ar.aggregates.wrappers.CompositeWrapper;
import ar.aggregates.wrappers.CompositeWrapper.Pair;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;


public class TestCompositeWrapper {
	@Test
	public void PackUnpack() {
		int leftVal = 2, rightVal = -100;
		Aggregates<Integer> left = new ConstantAggregates<Integer>(10,10,20,20,leftVal);
		Aggregates<Integer> right = new ConstantAggregates<Integer>(10,10,20,20,rightVal);
		
		Aggregates<Pair<Integer,Integer>> aggs = CompositeWrapper.wrap(left, right);
		
		for (int x=aggs.lowX(); x<aggs.highX(); x++) {
			for (int y=aggs.lowY(); y<aggs.highY(); y++) {
				Pair<Integer, Integer> val = aggs.get(x, y); 
				assertThat("Left val pack mismatch.", val.left, is(leftVal));
				assertThat("Right val pack mismatch.", val.right, is(rightVal));
			}
		}
		
		CompositeWrapper<Integer,Integer, Pair<Integer,Integer>> unpacked = CompositeWrapper.convert(aggs, 0,0);
		Aggregates<Integer> laggs = unpacked.left();
		Aggregates<Integer> raggs = unpacked.right();
		
		for (int x=unpacked.lowX(); x<unpacked.highX(); x++) {
			for (int y=unpacked.lowY(); y<unpacked.highY(); y++) {
				Pair<Integer, Integer> val = unpacked.get(x, y); 
				assertThat("Left val unpacked mismatch.", val.left, is(leftVal));
				assertThat("Right val unpacked mismatch.", val.right, is(rightVal));
				assertThat("Left aggs unpacked mismatch.", laggs.get(x,y), is(leftVal));
				assertThat("Right aggs unpacked mismatch.", raggs.get(x,y), is(rightVal));
			}
		}
	}
	
}
