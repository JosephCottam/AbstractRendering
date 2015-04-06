package ar.test.rules;

import static org.junit.Assert.*;

import org.junit.Test;

import java.awt.Color;

import ar.Aggregates;
import ar.Renderer;
import ar.aggregates.AggregateUtils;
import ar.aggregates.implementations.RefFlatAggregates;
import ar.renderers.ForkJoinRenderer;
import ar.rules.CategoricalCounts;
import ar.rules.Categories.*;
import ar.test.AllEqual;
import ar.util.Util;
import static java.lang.String.format;

public class CategoriesTests {
	
	@Test
	public void countCategories() {
		CountCategories<Color> counter = new CountCategories<>(Util.COLOR_SORTER);
		CategoricalCounts<Color> coc = new CategoricalCounts<>(Util.COLOR_SORTER);
		coc = coc.extend(Color.BLUE, 1);
		coc = coc.extend(Color.RED, 2);
		Aggregates<CategoricalCounts<Color>> aggs = new RefFlatAggregates<>(10,10,coc);
		
		aggs = AggregateUtils.alignedMerge(aggs, aggs, aggs.defaultValue(), counter::rollup);
		
		for (int x=aggs.lowX(); x<aggs.highX(); x++) {
			for (int y=aggs.lowY(); y<aggs.highY(); y++) {
				assertEquals(format("Incorrect number of categories found at (%d, %d)", x,y), 2, aggs.get(x,y).size());
				assertEquals(format("Unexpected aggregate value 0 at (%d, %d)", x,y), 2, aggs.get(x, y).count(0));
				assertEquals(format("Unexpected aggregate value 1 at (%d, %d)", x,y), 4, aggs.get(x, y).count(1));
			}
		}
	}

	private Aggregates<CategoricalCounts<String>> testAggregates() {
		CategoricalCounts<String> coc = new CategoricalCounts<>();
		coc = coc.extend("Zero", 0);
		coc = coc.extend("One", 1);
		coc = coc.extend("Two", 2);
		coc = coc.extend("Three", 3);
		Aggregates<CategoricalCounts<String>> aggs = new RefFlatAggregates<>(10,10, coc);
		return aggs;
	}
	
	

	@Test
	public void keyPercent() {
		Aggregates<CategoricalCounts<String>> aggs = testAggregates();
		
		Renderer r = new ForkJoinRenderer();
		Aggregates<Color> rslt = r.transfer(aggs, new KeyPercent<String>(1/3d, "Two", Util.CLEAR, Color.BLACK, Color.WHITE));
		assertThat(rslt, new AllEqual<>(AggregateUtils.make(10,10, Color.BLACK)));

		rslt = r.transfer(aggs, new KeyPercent<String>(.25, "Two", Util.CLEAR, Color.BLACK, Color.WHITE));
		assertThat(rslt, new AllEqual<>(AggregateUtils.make(10,10, Color.WHITE)));

	}
	

	@Test
	public void toCounts() {
		Aggregates<CategoricalCounts<String>> aggs = testAggregates();
		
		Renderer r = new ForkJoinRenderer();
		Aggregates<Integer> rslt = r.transfer(aggs, new ToCount<String>());
		assertThat(rslt, new AllEqual<>(AggregateUtils.make(10,10, 6)));
	}
	
	@Test
	public void NthEntry() {
		Aggregates<CategoricalCounts<String>> aggs = testAggregates();
		
		Renderer r = new ForkJoinRenderer();
		Aggregates<CategoricalCounts<String>> ones = r.transfer(aggs, new NthEntry<String>(0));
		assertThat(ones, new AllEqual<>(new RefFlatAggregates<>(10,10, new CategoricalCounts<>("One", 1))));
		
		Aggregates<CategoricalCounts<String>> threes = r.transfer(aggs, new NthEntry<String>(1));
		assertThat(threes, new AllEqual<>(new RefFlatAggregates<>(10,10, new CategoricalCounts<>("Thrres", 3))));

		Aggregates<CategoricalCounts<String>> twos = r.transfer(aggs, new NthEntry<String>(3));
		assertThat(twos, new AllEqual<>(new RefFlatAggregates<>(10,10, new CategoricalCounts<>("Twos", 2))));

		Aggregates<CategoricalCounts<String>> zeros = r.transfer(aggs, new NthEntry<String>(3));
		assertThat(zeros, new AllEqual<>(new RefFlatAggregates<>(10,10, new CategoricalCounts<>("Zeros", 0))));
	}
	
	@Test
	public void NthKey() {
		Aggregates<CategoricalCounts<String>> aggs = testAggregates();
		
		Renderer r = new ForkJoinRenderer();
		Aggregates<String> ones = r.transfer(aggs, new NthKey<String>(0, "Empty"));
		assertThat(ones, new AllEqual<>(new RefFlatAggregates<>(10,10, "One")));
		
		Aggregates<String> threes = r.transfer(aggs, new NthKey<String>(1, "Empty"));
		assertThat(threes, new AllEqual<>(new RefFlatAggregates<>(10,10, "Three")));

		Aggregates<String> empty = r.transfer(aggs, new NthKey<String>(5, "Empty"));
		assertThat(empty, new AllEqual<>(new RefFlatAggregates<>(10,10, "Empty")));
	}
	
	@Test
	public void NthCount() {
		Aggregates<CategoricalCounts<String>> aggs = testAggregates();
		
		Renderer r = new ForkJoinRenderer();
		assertThat(r.transfer(aggs, new NthCount<String>(0, -1)), 
					new AllEqual<>(AggregateUtils.make(10,10, 1)));

		assertThat(r.transfer(aggs, new NthCount<String>(2, -1)), 
				new AllEqual<>(AggregateUtils.make(10,10, 2)));

		assertThat(r.transfer(aggs, new NthCount<String>(10, -1)), 
				new AllEqual<>(AggregateUtils.make(10,10, -1)));
	}

	@Test
	public void NumCategories() {
		Aggregates<CategoricalCounts<String>> aggs = testAggregates();
		
		Renderer r = new ForkJoinRenderer();
		assertThat(r.transfer(aggs, new NumCategories<String>()), 
					new AllEqual<>(AggregateUtils.make(10, 10, 4)));

		CountCategories<String> counter = new CountCategories<>();

		aggs = AggregateUtils.alignedMerge(aggs, aggs, aggs.defaultValue(), counter::rollup);	
		assertThat(r.transfer(aggs, new NumCategories<String>()), 
				new AllEqual<>(AggregateUtils.make(10, 10, 8)));
	}
	

}
