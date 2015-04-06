package ar.test.rules;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import junit.framework.TestSuite;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import ar.Aggregates;
import ar.Renderer;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.renderers.ForkJoinRenderer;
import ar.rules.SeamCarving;
import static ar.rules.SeamCarving.*;
import ar.rules.combinators.Predicates;

@RunWith(Enclosed.class)
public class SeamCarvingTests extends TestSuite {
	
	public static class CarveSweepTests {
		@Test
		public void testMatchingDown() {
			Aggregates<Double> pixelEnergy = AggregateUtils.make(5,3,0d);
			for (int x=pixelEnergy.lowX(); x<pixelEnergy.highX(); x++) {
				for (int y=pixelEnergy.lowY(); y<pixelEnergy.highY(); y++) {
					pixelEnergy.set(x, y, (double) y); 
				}
			}

			Aggregates<Double> cumEng = AggregateUtils.make(pixelEnergy, 0d);
			Weights weights = new CarveSweep.EdgeWeights(pixelEnergy);
			Aggregates<Integer> matchings = SeamCarving.matchings(weights, cumEng);
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

			Aggregates<Double> cumEng = AggregateUtils.make(pixelEnergy, 0d);
			Weights weights = new CarveSweep.EdgeWeights(pixelEnergy);
			Aggregates<Integer> matchings = SeamCarving.matchings(weights, cumEng);
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
		
		@Test
		public void testCarveRows() {
			testCarve(new SeamCarving.CarveSweep<>(new SeamCarving.DeltaDouble(), Direction.H, 0d, 3),
					Direction.H, 3);
		}

		@Test
		public void testCarveCols() {
			testCarve(new SeamCarving.CarveSweep<>(new SeamCarving.DeltaDouble(), Direction.V, 0d, 3),
					Direction.V, 3);
		}
	}

	
	public static class CarveTwoSweepsTests {
		@Test
		public void testCarveRows() {
			testCarve(new SeamCarving.CarveTwoSweeps<>(new SeamCarving.DeltaDouble(), Direction.H, 0d, 3),
					Direction.H, 3);
		}

		@Test
		public void testCarveCols() {
			testCarve(new SeamCarving.CarveTwoSweeps<>(new SeamCarving.DeltaDouble(), Direction.V, 0d, 3),
					Direction.V, 3);
		}
	}
	
	public static class CarveSweepNTests {
		@Test
		public void testCarveRows() {
			testCarve(new SeamCarving.CarveSweepN<>(new SeamCarving.DeltaDouble(), Direction.H, 0d, 3),
					Direction.H, 3);
		}

		@Test
		public void testCarveCols() {
			testCarve(new SeamCarving.CarveSweepN<>(new SeamCarving.DeltaDouble(), Direction.V, 0d, 3),
					Direction.V, 3);
		}
	}
	
	private static final void testCarve(Transfer.Specialized<Double, Double> carver, Direction d, int seams) {
		Aggregates<Double> input = AggregateUtils.make(5,7,0d);
		for (int x=input.lowX(); x<input.highX(); x++) {
			for (int y=input.lowY(); y<input.highY(); y++) {
				input.set(x, y, (double) y); 
			}
		}
		
		Renderer r = new ForkJoinRenderer();
		Aggregates<Double> rslt = r.transfer(input, carver);
		
		if (d == Direction.H) {
			assertThat("Incorrect number of rows.", rslt.highY()-rslt.lowY(), is(input.highY()-input.lowY()-seams));
		} else {
			assertThat("Incorrect number of cols.", rslt.highX()-rslt.lowX(), is(input.highX()-input.lowX()-seams));
		}
	}
	
	public static class CarveIncrementalTests {
		@Test
		public void testFindLeftSeam() {
			Aggregates<Double> cumEng = AggregateUtils.make(9, 9, 0d);
			for (int x=cumEng.lowX(); x<cumEng.highX(); x++) {
				for (int y=cumEng.lowY(); y<cumEng.highY(); y++) {
					cumEng.set(x, y, (double) x); 
				}
			}
			int[] seam = SeamCarving.CarveIncremental.findVSeam(cumEng,0);
			
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
			int[] seam = SeamCarving.CarveIncremental.findVSeam(cumEng,8);
			
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
			int[] seam = SeamCarving.CarveIncremental.findVSeam(cumEng,4);
			
			assertThat(seam.length, is (cumEng.highY()-cumEng.lowY()));
			for (int e:seam) {assertThat(e, is(4));}
		}
		
		@Test
		public void testDiagSeam() {
			Aggregates<Double> cumEng = AggregateUtils.make(9, 9, 10d);
			for (int y=cumEng.lowY(); y<cumEng.highY(); y++) {
				cumEng.set(y,y,0d); 
			}
			
			int[] seam = SeamCarving.CarveIncremental.findVSeam(cumEng,8);
			
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
			Aggregates<Double> cumEng = new ForkJoinRenderer().transfer(energy, t.specialize(energy));
			
			double val =0;
			for (int y=energy.lowY(); y<energy.highY(); y++) {
				val= val + y+1;
				for (int x=energy.lowX(); x<energy.highX(); x++) {
					assertThat(String.format("Error at %s,%s",x,y), cumEng.get(x, y), is(val));
				}
			}		
		}
		
		@Test 
		public void testCorrectSeam() {
			int[][] seams = {{1,2},{1,1},{2,1},{1,1}};
			for (int i=0; i<seams.length; i++) {
				SeamCarving.CarveIncremental.correctSeam(seams,i);
			}
			
			assertThat(seams[0][0], is(1));
			assertThat(seams[0][1], is(2));
			assertThat(seams[1][0], is(2));
			assertThat(seams[1][1], is(1));
			
			assertThat(seams[2][0], is(4));
			assertThat(seams[2][1], is(3));
			assertThat(seams[3][0], is(3));
			assertThat(seams[3][1], is(4));

			
		}
		
		@Test
		public void testCarveRows() {
			testCarve(new SeamCarving.CarveIncremental<>(new SeamCarving.DeltaDouble(), Direction.H, 0d, 3),
					Direction.H, 3);
		}

		@Test
		public void testCarveCols() {
			testCarve(new SeamCarving.CarveIncremental<>(new SeamCarving.DeltaDouble(), Direction.V, 0d, 3),
					Direction.V, 3);
		}
	}
}
		