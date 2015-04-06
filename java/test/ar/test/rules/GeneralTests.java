package ar.test.rules;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import ar.*;

import java.awt.geom.AffineTransform;

import ar.renderers.*;
import ar.aggregates.AggregateUtils;
import ar.glyphsets.WrappedCollection;
import ar.rules.General;
import ar.selectors.TouchesPixel;
import ar.util.Util;

public class GeneralTests {
	public static Aggregates<Double> aggregates() {
		Aggregates<Double> aggs = AggregateUtils.make(-5, -5, 5, 5, 0d);
		for (int x=aggs.lowX(); x< aggs.highX(); x++) {
			for (int y=aggs.lowY(); y< aggs.highY(); y++) {
				aggs.set(x, y, (double) x*y);
			}
		}
		return aggs;
	}

	
	public static Glyphset<Rectangle2D, Double> glyphset(int size) {
		List<Integer> numbers =  IntStream.range(0,size).boxed().collect(Collectors.toList());
		Glyphset<Rectangle2D, Double> glyphs = WrappedCollection.toList(numbers, 
															(n) -> new Rectangle2D.Double(n,n,1,1), 
															n -> ((Double) n.doubleValue()));
		return glyphs;
	}
	
	@Test 
	public void testApply() {
		final int size = 100;
		Glyphset<Rectangle2D, Double> glyphs = glyphset(size);
		
		Renderer r = new ForkJoinRenderer();
		Selector<Rectangle2D> selector = TouchesPixel.make(glyphs);
		AffineTransform vt = Util.zoomFit(glyphs.bounds(), size, size);
				
		Aggregator<Double, Double> aggregator = new General.Apply<>(0d, Math::max);
		Aggregates<Double> aggs = r.aggregate(glyphs, selector, aggregator, vt, size, size);
		
		for (int i=0; i<size; i++) {assertThat(aggs.get(i,i), equalTo((double) i));}
		
		vt = Util.zoomFit(glyphs.bounds(), 1,1);
		aggs = r.aggregate(glyphs, selector, aggregator, vt, size, size);
		assertThat(aggs.get(0,0), equalTo((double) size-1));
	}

	@Test
	public void testLast() {
		Aggregator<Double, Double> aggregator = new General.Last<>(0d);

		assertThat(aggregator.combine(10d, 11d), is (11d));
		assertThat(aggregator.combine(11d, 10d), is (10d));
		assertThat(aggregator.rollup(10d, 11d), is (11d));
		assertThat(aggregator.rollup(11d, 10d), is (10d));
		assertThat(new General.Last<>(0d).identity(), is (0d));
		assertThat(new General.Last<>(5d).identity(), is (5d));
		assertThat(new General.Last<>("test").identity(), is ("test"));

		final int size = 100;
		Glyphset<Rectangle2D, Double> glyphs = glyphset(size);
		
		Renderer r = new ForkJoinRenderer();
		Selector<Rectangle2D> selector = TouchesPixel.make(glyphs);
		AffineTransform vt = Util.zoomFit(glyphs.bounds(), 1, 1);

		Aggregates<Double> aggs = r.aggregate(glyphs, selector, aggregator, vt, 1, 1);		
		assertThat(aggs.get(0,0), equalTo((double) size-1));		
	}

	@Test
	public void testFirst() {
		Aggregator<Double, Double> aggregator = new General.First<>(0d);

		assertThat(aggregator.combine(10d, 11d), is (10d));
		assertThat(aggregator.combine(11d, 10d), is (11d));
		assertThat(aggregator.rollup(10d, 11d), is (10d));
		assertThat(aggregator.rollup(11d, 10d), is (11d));
		assertThat(new General.First<>(0d).identity(), is (0d));
		assertThat(new General.First<>(5d).identity(), is (5d));
		assertThat(new General.First<>("test").identity(), is ("test"));

		
		final int size = 100;
		Glyphset<Rectangle2D, Double> glyphs = glyphset(size);
		
		Renderer r = new ForkJoinRenderer();
		Selector<Rectangle2D> selector = TouchesPixel.make(glyphs);
		AffineTransform vt = Util.zoomFit(glyphs.bounds(), 1, 1);

		Aggregates<Double> aggs = r.aggregate(glyphs, selector, aggregator, vt, 1, 1);		
		assertThat(aggs.get(0,0), equalTo((double) 0));		
	}

	
	@Test
	public void testEcho() {
		Transfer.ItemWise<Double, Double> t = new General.Echo<>(0d);
		
		Aggregates<Double> aggs = aggregates();
		for (int x=aggs.lowX(); x< aggs.highX(); x++) {
			for (int y=aggs.lowY(); y< aggs.highY(); y++) {
				assertThat(t.at(x, y, aggs), is((double) x*y));
			}
		}
	}

	@Test
	public void testConst() {
		
		assertThat(new General.Const<>(0d).at(0, 0, null), is(0d));
		assertThat(new General.Const<>(0d).at(3, 3, null), is(0d));
		assertThat(new General.Const<>(5d).at(0, 0, null), is(5d));
		assertThat(new General.Const<>(5d).at(6, 7, null), is(5d));
		
		
		Transfer.ItemWise<Double, Double> t = new General.Const<>(38d);
		Aggregates<Double> aggs = aggregates();
		for (int x=aggs.lowX(); x< aggs.highX(); x++) {
			for (int y=aggs.lowY(); y< aggs.highY(); y++) {
				assertThat(t.at(x, y, aggs), is(38d));
			}
		}
	}
}
