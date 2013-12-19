package ar.test.rules;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.Test;

import ar.Aggregates;
import ar.Aggregator;
import ar.Renderer;
import ar.Resources;
import ar.Transfer;
import ar.Transfer.Specialized;
import ar.aggregates.AggregateUtils;
import ar.aggregates.implementations.RefFlatAggregates;
import ar.renderers.ParallelRenderer;
import ar.rules.General;
import ar.rules.General.Spread.Spreader;
import ar.rules.SeamCarving;

public class SeamCarvingTests {

	@Test
	public void testCumulativeEnergy() {
		Aggregates<Double> energy = AggregateUtils.make(10, 10, 0d);
		for (int x=energy.lowX(); x<energy.highX(); x++) {
			for (int y=energy.lowY(); y<energy.highY(); y++) {
				energy.set(x, y, (double) y); 
			}
		}
		
		Transfer<Double, Double> t = new SeamCarving.CumulativeEnergy();
		Aggregates<Double> cumEng = Resources.DEFAULT_RENDERER.transfer(energy, t.specialize(energy));
		
		double val =0;
		for (int y=energy.lowY(); y<energy.highY(); y++) {
			val= val + y;
				for (int x=energy.lowX(); x<energy.highX(); x++) {
				assertThat(String.format("Error at %s,%s",x,y), cumEng.get(x, y), is(val));
			}
		}
		
	}
}
		