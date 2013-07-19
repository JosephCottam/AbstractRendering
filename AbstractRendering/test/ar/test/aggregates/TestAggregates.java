package ar.test.aggregates;

import org.junit.Test;

import ar.Aggregates;
import ar.aggregates.ConstantAggregates;
import ar.aggregates.FlatAggregates;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;


public class TestAggregates {
	public static Aggregates<Integer> simpleAggregates(int lowX, int lowY, int highX, int highY, int defVal) {
		Aggregates<Integer> aggs = new FlatAggregates<Integer>(lowX,lowY,highX,highY, defVal);
		for (int x=aggs.lowX(); x<aggs.highX(); x++) {
			for (int y=aggs.highY(); y<aggs.highY(); y++) {
				aggs.set(x, y, valFor(x,y));
			}
		}
		return aggs;
	}
	
	public static int valFor(int x, int y) {return x*y+y;}
	
	@Test
	public void Constant() {
		int value = -1;
		Aggregates<Integer> aggs = new ConstantAggregates<Integer>(10,10,20,20,value);
		assertThat(aggs.lowX(), is(10));
		assertThat(aggs.lowY(), is(10));
		assertThat(aggs.highX(), is(20));
		assertThat(aggs.highY(), is(20));
		
		for (int x=0; x<aggs.highX()*2; x++) {
			for (int y=0; y<aggs.highY()*2; y++) {
				assertThat(String.format("In-range range mismatch at %s, %s", x, y),aggs.at(x, y), is(value));
			}
		}
	}
	
}
