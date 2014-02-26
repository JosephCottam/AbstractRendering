package ar.test.rules;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.Test;

import ar.Aggregates;
import ar.test.TestResources;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.rules.SeamCarving;
import ar.rules.SeamCarving.RowPairGlobalCarve.EdgeEnergy;
import ar.rules.combinators.Predicates;

public class SeamCarvingTests {
	
	public static class LocalCarve {
		@Test
		public void testMatchingDown() {
			Aggregates<Double> pixelEnergy = AggregateUtils.make(5,3,0d);
			for (int x=pixelEnergy.lowX(); x<pixelEnergy.highX(); x++) {
				for (int y=pixelEnergy.lowY(); y<pixelEnergy.highY(); y++) {
					pixelEnergy.set(x, y, (double) y); 
				}
			}

			EdgeEnergy energy = new EdgeEnergy(pixelEnergy);
			Aggregates<Integer> matchings = SeamCarving.RowPairGlobalCarve.matchings(pixelEnergy, energy);
			assertThat("Matching matrix has incorrect shape.", AggregateUtils.bounds(matchings), is(AggregateUtils.bounds(pixelEnergy)));
			
			for (int x=matchings.lowX(); x<matchings.highX(); x++) {
				for (int y=matchings.lowY(); y<matchings.highY(); y++) {
					assertThat(matchings.get(x, y), is(0)); 
				}
			}
		}
		
		@Test
		public void testMatchingCrossedPairs() {
			Aggregates<Double> pixelEnergy = AggregateUtils.make(6,3,0d);
			for (int x=pixelEnergy.lowX(); x<pixelEnergy.highX(); x++) {
				for (int y=pixelEnergy.lowY(); y<pixelEnergy.highY(); y++) {
					if (y%2==x%2) {pixelEnergy.set(x, y, (double) 5);}
					else {pixelEnergy.set(x, y, (double) 1);}
				}
			}

			EdgeEnergy energy = new EdgeEnergy(pixelEnergy);
			Aggregates<Integer> matchings = SeamCarving.RowPairGlobalCarve.matchings(pixelEnergy, energy);
			assertThat("Matching matrix has incorrect shape.", AggregateUtils.bounds(matchings), is(AggregateUtils.bounds(pixelEnergy)));
			
			for (int x=matchings.lowX(); x<matchings.highX(); x++) {
				for (int y=matchings.lowY(); y<matchings.highY()-1; y++) {
					if (x%2==0) {assertThat(matchings.get(x, y), is(1));}
					else {assertThat(matchings.get(x, y), is(-1));}
				}
			}
		}
		
		/**Returns true if and only if and only if all cells in a matching are reachable.**/
		public static boolean verifyFullMatching(Aggregates<Integer> matching, boolean detailReport) {
			Aggregates<Boolean> reached = AggregateUtils.make(matching,false);
			
			for (int x=matching.lowX(); x<matching.highX(); x++) {reached.set(x, reached.lowY(), true);}
			
			for (int y=matching.lowY(); y<matching.highY(); y++) {
				for (int x=matching.lowX(); x<matching.highX();x++) {
					int dir =matching.get(x, y);
					reached.set(x+dir, y+1, true);
				}				
			}
			
			if (detailReport) {
				for (int y=reached.lowY(); y<reached.highY(); y++) {
					for (int x=reached.lowX(); x<reached.highX();x++) {
						if (!reached.get(x, y)) {
							System.out.printf("Unreachable at %d,%d\n", x,y);
						} 
					}				
				}
			}
			
			return Predicates.All.all(reached);
		}

	}
	
	public static class OptimalCarve {
		@Test
		public void testFindLeftSeam() {
			Aggregates<Double> cumEng = AggregateUtils.make(9, 9, 0d);
			for (int x=cumEng.lowX(); x<cumEng.highX(); x++) {
				for (int y=cumEng.lowY(); y<cumEng.highY(); y++) {
					cumEng.set(x, y, (double) x); 
				}
			}
			int[] seam = SeamCarving.OptimalCarve.findVSeam(cumEng,1);
			
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
			int[] seam = SeamCarving.OptimalCarve.findVSeam(cumEng,1);
			
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
			int[] seam = SeamCarving.OptimalCarve.findVSeam(cumEng,1);
			
			assertThat(seam.length, is (cumEng.highY()-cumEng.lowY()));
			for (int e:seam) {assertThat(e, is(4));}
		}
		
		@Test
		public void testDiagSeam() {
			Aggregates<Double> cumEng = AggregateUtils.make(9, 9, 10d);
			for (int y=cumEng.lowY(); y<cumEng.highY(); y++) {
				cumEng.set(y,y,0d); 
			}
			int[] seam = SeamCarving.OptimalCarve.findVSeam(cumEng,1);
			
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
}
		