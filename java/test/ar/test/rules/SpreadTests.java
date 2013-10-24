package ar.test.rules;



import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.junit.Test;

import ar.Aggregates;
import ar.Aggregator;
import ar.Renderer;
import ar.Transfer.Specialized;
import ar.aggregates.AggregateUtils;
import ar.aggregates.FlatAggregates;
import ar.renderers.ParallelGlyphs;
import ar.rules.General;
import ar.rules.General.Spread.Spreader;
import ar.rules.Numbers;

public class SpreadTests {

	@Test
	public void testFullSquare() {
		Renderer r = new ParallelGlyphs();
		Aggregator<Integer,Integer> combiner = new Numbers.Count<>();
		Aggregates<Integer> aggs = new FlatAggregates<Integer>(9,9,0);
		aggs.set(4, 4, 1);
		
		Spreader<Integer> spread4 = new General.Spread.UnitSquare<>(4);
		General.Spread<Integer> transfer4 = new General.Spread<Integer>(0, spread4 , combiner);
		Specialized<Integer,Integer> s4 = transfer4.specialize(aggs);
		
		Aggregates<Integer> rslt = r.transfer(aggs, s4);
		for (int x=rslt.lowX(); x<rslt.highX(); x++){
			for (int y=rslt.lowY(); y<rslt.highY(); y++){
				assertThat(String.format("Failed at (%d, %d)",x,y), rslt.get(x,y), is(1));
			}
		}
	}
		
	@Test
	public void testCenterSquare() {
		Renderer r = new ParallelGlyphs();
		Aggregator<Integer,Integer> combiner = new Numbers.Count<>();
		Aggregates<Integer> aggs = new FlatAggregates<Integer>(9,9,0);
		aggs.set(4, 4, 1);
		
		Spreader<Integer> spread2 = new General.Spread.UnitSquare<>(2);
		General.Spread<Integer> transfer2 = new General.Spread<Integer>(0, spread2, combiner);
		Specialized<Integer,Integer> s2 = transfer2.specialize(aggs);
		Aggregates<Integer> rslt = r.transfer(aggs, s2);
		
		assertThat(String.format("Failed at (%d, %d)", 0,0), rslt.get(0,0), is(0));
		assertThat(String.format("Failed at (%d, %d)", 3,3), rslt.get(3,3), is(1));
		assertThat(String.format("Failed at (%d, %d)", 3,5), rslt.get(3,5), is(1));
		assertThat(String.format("Failed at (%d, %d)", 3,6), rslt.get(3,6), is(1));
		assertThat(String.format("Failed at (%d, %d)", 5,5), rslt.get(5,5), is(1));
		assertThat(String.format("Failed at (%d, %d)", 5,6), rslt.get(5,6), is(1));
		assertThat(String.format("Failed at (%d, %d)", 6,6), rslt.get(6,6), is(1));
		assertThat(String.format("Failed at (%d, %d)", 8,8), rslt.get(8,8), is(0));
	}
	
	@Test
	public void testUnitCircle() {
		Renderer r = new ParallelGlyphs();
		Aggregator<Integer,Integer> combiner = new Numbers.Count<>();
		Aggregates<Integer> aggs = new FlatAggregates<Integer>(9,9,0);
		aggs.set(4, 4, 1);
		
		Spreader<Integer> spread2 = new General.Spread.UnitCircle<>(4);
		General.Spread<Integer> transfer2 = new General.Spread<Integer>(0, spread2, combiner);
		Specialized<Integer,Integer> s2 = transfer2.specialize(aggs);
		Aggregates<Integer> rslt = r.transfer(aggs, s2);
		
		assertThat(String.format("Failed at (%d, %d)", 0,0), rslt.get(0,0), is(0));
		assertThat(String.format("Failed at (%d, %d)", 1,1), rslt.get(1,1), is(0));
		assertThat(String.format("Failed at (%d, %d)", 3,3), rslt.get(3,3), is(1));
		assertThat(String.format("Failed at (%d, %d)", 3,5), rslt.get(3,5), is(1));
		assertThat(String.format("Failed at (%d, %d)", 3,6), rslt.get(3,6), is(1));
		assertThat(String.format("Failed at (%d, %d)", 5,5), rslt.get(5,5), is(1));
		assertThat(String.format("Failed at (%d, %d)", 5,6), rslt.get(5,6), is(1));
		assertThat(String.format("Failed at (%d, %d)", 6,6), rslt.get(6,6), is(1));
		assertThat(String.format("Failed at (%d, %d)", 8,8), rslt.get(8,8), is(0));
	}
	
	@Test
	public void testValueCircle4() {
		Renderer r = new ParallelGlyphs();
		Aggregator<Integer,Integer> combiner = new Numbers.Count<>();
		Aggregates<Integer> aggs = new FlatAggregates<Integer>(9,9,0);
		aggs.set(4, 4, 4);
		
		Spreader<Integer> spread2 = new General.Spread.ValueCircle<>();
		General.Spread<Integer> transfer2 = new General.Spread<Integer>(0, spread2, combiner);
		Specialized<Integer,Integer> s2 = transfer2.specialize(aggs);
		Aggregates<Integer> rslt = r.transfer(aggs, s2);
		System.out.println(AggregateUtils.toString(rslt));
		
		assertThat(String.format("Failed at (%d, %d)", 0,0), rslt.get(0,0), is(0));
		assertThat(String.format("Failed at (%d, %d)", 1,1), rslt.get(1,1), is(0));
		assertThat(String.format("Failed at (%d, %d)", 3,3), rslt.get(3,3), is(4));
		assertThat(String.format("Failed at (%d, %d)", 3,5), rslt.get(3,5), is(4));
		assertThat(String.format("Failed at (%d, %d)", 3,6), rslt.get(3,6), is(4));
		assertThat(String.format("Failed at (%d, %d)", 5,5), rslt.get(5,5), is(4));
		assertThat(String.format("Failed at (%d, %d)", 5,6), rslt.get(5,6), is(4));
		assertThat(String.format("Failed at (%d, %d)", 6,6), rslt.get(6,6), is(4));
		assertThat(String.format("Failed at (%d, %d)", 8,8), rslt.get(8,8), is(0));
	}
	
	@Test
	public void testValueCircle2() {
		Renderer r = new ParallelGlyphs();
		Aggregator<Integer,Integer> combiner = new Numbers.Count<>();
		Aggregates<Integer> aggs = new FlatAggregates<Integer>(9,9,0);
		aggs.set(4, 4, 2);
		
		Spreader<Integer> spread2 = new General.Spread.ValueCircle<>();
		General.Spread<Integer> transfer2 = new General.Spread<Integer>(0, spread2, combiner);
		Specialized<Integer,Integer> s2 = transfer2.specialize(aggs);
		Aggregates<Integer> rslt = r.transfer(aggs, s2);
		
		assertThat(String.format("Failed at (%d, %d)", 0,0), rslt.get(0,0), is(0));
		assertThat(String.format("Failed at (%d, %d)", 1,1), rslt.get(1,1), is(0));
		assertThat(String.format("Failed at (%d, %d)", 2,2), rslt.get(2,2), is(0));
		assertThat(String.format("Failed at (%d, %d)", 3,3), rslt.get(3,3), is(2));
		assertThat(String.format("Failed at (%d, %d)", 4,4), rslt.get(4,4), is(2));
		assertThat(String.format("Failed at (%d, %d)", 5,5), rslt.get(5,5), is(2));
		assertThat(String.format("Failed at (%d, %d)", 6,6), rslt.get(6,6), is(0));
		assertThat(String.format("Failed at (%d, %d)", 8,8), rslt.get(8,8), is(0));
	}
}
