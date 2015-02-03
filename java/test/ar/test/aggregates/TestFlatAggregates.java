package ar.test.aggregates;

import ar.Aggregates;
import ar.aggregates.implementations.RefFlatAggregates;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

public class TestFlatAggregates {

	@Test
	public void Store() {
		Aggregates<Integer> aggs = new RefFlatAggregates<Integer>(10,10,0);
		
		for(int x=aggs.lowX();x<aggs.highX(); x++) {
			for (int y=aggs.lowY();y<aggs.highY(); y++) {
				aggs.set(x, y, x*y);
			}
		}
		
		for(int x=aggs.lowX();x<aggs.highX(); x++) {
			for (int y=aggs.lowY();y<aggs.highY(); y++) {
				assertEquals(String.format("Error at %d,%s", x,y), new Integer(x*y), aggs.get(x,y));
			}
		}
	}

	@Test
	public void FlatBoundsCheck() {
		int defVal = -1;
		
		Aggregates<Integer> aggs = TestAggregates.simpleAggregates(10,10,20,20, defVal);
		
		assertThat(aggs.lowX(), is(10));
		assertThat(aggs.lowY(), is(10));
		assertThat(aggs.highX(), is(20));
		assertThat(aggs.highY(), is(20));

		for (int x=0; x<aggs.highX()*2; x++) {
			for (int y=0; y<aggs.highY()*2; y++) {
				if (x <aggs.lowX() || x>= aggs.highX()
						|| y < aggs.lowY() || y >= aggs.highY()) {
					assertThat(String.format("Out-of-range range mismatch at %s, %s", x, y), aggs.get(x,y), is(defVal));
				} else {
					assertThat(String.format("In-range range mismatch at %s, %s", x, y),aggs.get(x, y), is(TestAggregates.valFor(x,y)));
				}
			}
		}
		
	}
	}
