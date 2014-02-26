package ar.test.rules;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.Test;

import ar.Aggregates;
import ar.test.TestResources;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.rules.SeamCarving;

public class SeamCarvingTests {
	@Test
	public void testFindLeftSeam() {
		Aggregates<Double> cumEng = AggregateUtils.make(9, 9, 0d);
		for (int x=cumEng.lowX(); x<cumEng.highX(); x++) {
			for (int y=cumEng.lowY(); y<cumEng.highY(); y++) {
				cumEng.set(x, y, (double) x); 
			}
		}
		int[] seam = SeamCarving.OptimalCarve.findVSeam(cumEng);
		
		assertThat(seam.length, is (cumEng.highY()-cumEng.lowY()));
		for (int i=0; i<seam.length;i++) {assertThat("Error at " + i, seam[i], is(0));}
	}
	
	@Test
	public void testFindRightSeam() {
		Aggregates<Double> cumEng = AggregateUtils.make(9, 9, 0d);
		for (int x=cumEng.lowX(); x<cumEng.highX(); x++) {
			for (int y=cumEng.lowY(); y<cumEng.highY(); y++) {
				cumEng.set(x, y, (double) -x); 
			}
		}
		int[] seam = SeamCarving.OptimalCarve.findVSeam(cumEng);
		
		assertThat(seam.length, is (cumEng.highY()-cumEng.lowY()));
		for (int i=0; i<seam.length;i++) {assertThat("Error at " + i, seam[i], is(cumEng.highX()-1));}
	}

	@Test
	public void testMidSeam() {
		Aggregates<Double> cumEng = AggregateUtils.make(9, 9, 0d);
		for (int x=cumEng.lowX(); x<cumEng.highX(); x++) {
			for (int y=cumEng.lowY(); y<cumEng.highY(); y++) {
				cumEng.set(x, y, (double) Math.abs(x-cumEng.highX()/2)); 
			}
		}
		int[] seam = SeamCarving.OptimalCarve.findVSeam(cumEng);
		
		assertThat(seam.length, is (cumEng.highY()-cumEng.lowY()));
		for (int e:seam) {assertThat(e, is(4));}
	}
	
	@Test
	public void testDiagSeam() {
		Aggregates<Double> cumEng = AggregateUtils.make(9, 9, 10d);
		for (int y=cumEng.lowY(); y<cumEng.highY(); y++) {
			cumEng.set(y,y,0d); 
		}
		int[] seam = SeamCarving.OptimalCarve.findVSeam(cumEng);
		
		assertThat(seam.length, is (cumEng.highY()-cumEng.lowY()));
		for (int i=0;i<seam.length;i++) {assertThat(seam[i], is(i));}
	}
	
	
	
	@Test
	public void testCumulativeEnergy() {
		Aggregates<Double> energy = AggregateUtils.make(10, 10, 0d);
		for (int x=energy.lowX(); x<energy.highX(); x++) {
			for (int y=energy.lowY(); y<energy.highY(); y++) {
				energy.set(x, y, (double) y+1); 
			}
		}
		
		Transfer<Double, Double> t = new SeamCarving.CumulativeEnergy();
		Aggregates<Double> cumEng = TestResources.RENDERER.transfer(energy, t.specialize(energy));
		
		double val =0;
		for (int y=energy.lowY(); y<energy.highY(); y++) {
			val= val + y+1;
			for (int x=energy.lowX(); x<energy.highX(); x++) {
				assertThat(String.format("Error at %s,%s",x,y), cumEng.get(x, y), is(val));
			}
		}		
	}
}
		