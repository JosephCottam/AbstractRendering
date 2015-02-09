package ar.test.rules;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.Test;

import ar.Aggregator;
import ar.rules.Numbers;

public class NumbersTests {
	
	@Test 
	public void sum() {
		Aggregator<Double, Double> d = Numbers.Sum.make(Double.class);
		Aggregator<Float, Float> f = Numbers.Sum.make(Float.class);
		Aggregator<Integer, Integer> i = Numbers.Sum.make(Integer.class);
		Aggregator<Long, Long> l = Numbers.Sum.make(Long.class);

		
		assertThat(d.rollup(3d, 4d), is(7d));
		assertThat(f.rollup(3f, 4f), is(7f));
		assertThat(l.rollup(3l, 4l), is(7l));
		assertThat(i.rollup(3, 4), is(7));

		assertThat(d.identity(), is(0d));
		assertThat(f.identity(), is(0f));
		assertThat(l.identity(), is(0l));
		assertThat(i.identity(), is(0));
	}
}
