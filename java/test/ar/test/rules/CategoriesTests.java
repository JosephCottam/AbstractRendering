package ar.test.rules;

import static org.junit.Assert.*;

import org.junit.Test;

import java.awt.Color;

import ar.Aggregates;
import ar.aggregates.implementations.RefFlatAggregates;
import ar.renderers.AggregationStrategies;
import ar.rules.CategoricalCounts;
import ar.rules.Categories;
import ar.util.Util;
import static java.lang.String.format;

public class CategoriesTests {
	
	@Test
	public void CountCategoriesLeft() {
		Categories.CountCategories<Color> counter = new Categories.CountCategories<Color>(Util.COLOR_SORTER);
		CategoricalCounts<Color> coc = new CategoricalCounts<Color>(Util.COLOR_SORTER);
		coc = coc.extend(Color.BLUE, 1);
		coc = coc.extend(Color.RED, 2);
		Aggregates<CategoricalCounts<Color>> aggs = new RefFlatAggregates<CategoricalCounts<Color>>(10,10,coc);
		
		aggs = AggregationStrategies.horizontalRollup(aggs, aggs, counter);
		
		for (int x=aggs.lowX(); x<aggs.highX(); x++) {
			for (int y=aggs.lowY(); y<aggs.highY(); y++) {
				assertEquals(format("Incorrect number of categories found at (%d, %d)", x,y), 2, aggs.get(x,y).size());
				assertEquals(format("Unexpected aggregate value 0 at (%d, %d)", x,y), 2, aggs.get(x, y).count(0));
				assertEquals(format("Unexpected aggregate value 1 at (%d, %d)", x,y), 4, aggs.get(x, y).count(1));
			}
		}
	}
}
