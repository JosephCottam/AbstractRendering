package ar.rules;


import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import ar.Aggregates;
import ar.Renderer;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.aggregates.wrappers.TransposeWrapper;
import ar.rules.combinators.Seq;
import ar.util.Util;

/** Seam-carving is a content-sensitive image resizing technique.
 * 
 * The basic idea is that not all pixels are equally important.
 * Therefore, some can be removed without changing the image as 
 * much as others.  The low-importance pixels do not need to all be
 * in the same row (or column) but they do need to be contiguous and
 * only one removed from each column (or row).  
 * 
 * This collection of classes adapts the technique to the abstract
 * rendering framework and generalizes it from just pixels to arbitrary
 * aggregate sets.
 * 
 * 
 * Original paper and optimal seam-finding:
 * Avidan and Shamir; "Seam Carving for Content Aware Image Resizing"
 * http://www.win.tue.nl/~wstahw/edu/2IV05/seamcarving.pdf
 * 
 * Faster, approximate seam finding methods:
 * Huang, Fu, Rosin, Qi; "Real-time content-aware image resizing"
 * http://link.springer.com/article/10.1007%2Fs11432-009-0041-9#page-1
 *  
 */
public class SeamCarving {
	/**Calculate the difference between two values.**/
	public static interface Delta<A> {public double delta(A left, A right);}
	
	/**Carve horizontally or vertically?**/
	public enum Direction {H,V}
	
	/**Interface for instrumented Carvers.**/
	public static abstract class AbstractCarver<A> implements Transfer.Specialized<A,A>{
		protected final Delta<A> delta;
		protected final A empty;
		protected final Direction dir;
		protected final int seams;
		
		public AbstractCarver(Delta<A> delta, Direction dir, A empty, int seams)  {
			this.delta = delta;
			this.empty = empty;
			this.dir = dir;
			this.seams= seams;
		}
		
		@Override public final A emptyValue() {return empty;}

		@Override public final ar.Transfer.Specialized<A, A> specialize(Aggregates<? extends A> aggregates) {return this;}

		@Override
		public final Aggregates<A> process(Aggregates<? extends A> aggregates, Renderer rend) {
			if (seams ==0) {return AggregateUtils.copy(aggregates,emptyValue());}
			
			int[][] dropList = dropList(aggregates, rend);
			return carveN(aggregates, dropList);
		}

		
		/**Alternative entry point for Carver that just computes the drop list
		 * instead of modifying the image.
		 */
		public abstract int[][] dropList(Aggregates<? extends A> aggregates, Renderer rend);
	}
	
	/**Find and remove a seams, per the Avidan and Shamir method but only remove one seam at a time.
	 * 
	 * This class will calculate the full energy matrix each time a seam is removed.
	 * Since the whole matrix is calculated each time, its slow BUT it is also stateless.
	 * 
	 * For faster results, try other "Carve" classes.
	 * Each has a different set of tradeoffs, but they all run significantly faster.  
	 * 
	 * **/
	public static class CarveIncremental<A> extends AbstractCarver<A> {
		public CarveIncremental(Delta<A> delta, Direction dir, A empty, int seams)  {
			super(delta, dir, empty, seams);
		}
		
		@Override
		public int[][] dropList(Aggregates<? extends A> aggregates, Renderer rend) {return dropList(aggregates, rend, dir);}
		public int[][] dropList(Aggregates<? extends A> aggregates, Renderer rend, Direction d) {
			if (seams == 0) {return new int[0][0];}
			
			if (d == Direction.H) {
					return transpose(dropList(TransposeWrapper.transpose(aggregates), rend, Direction.V));
			} else {
				Aggregates<? extends Double> pixelEnergy = rend.transfer(aggregates, new Energy<>(delta));
				CumulativeEnergy cumulativeEnergy = new CumulativeEnergy();
				
				@SuppressWarnings("unchecked")
				Aggregates<A> rslt = (Aggregates<A>) aggregates;
				
				int[][] seamList = new int[seams][];
				for (int i=0; i<seams; i++) {
					Aggregates<Double> cumEng = rend.transfer(pixelEnergy, cumulativeEnergy);
					int selectedSeam = selectSeam(seamEnergies(cumEng), i);
					seamList[i] = findVSeam(cumEng, selectedSeam);
					rslt = carve(rslt, seamList[i]);
					pixelEnergy = carve(pixelEnergy,seamList[i]);
					correctSeam(seamList, i);
				}

				return  sortRows(transpose(seamList));
			}
		}


		/**
		 */
		public static final void correctSeam(int[][] seams, int focus) {
			for (int entry=0; entry < seams[focus].length; entry++) {
				int increase =0;
				for (int seam=focus-1; seam>=0; seam--) {
					if (seams[seam][entry] <= focus) {increase++;}
				}
				seams[focus][entry] += increase;
			}
		}
		
		/**Get just the seam energies.**/
		private static final double[] seamEnergies(Aggregates<? extends Double> cumEng) {
			final double[] energies = new double[cumEng.highX()-cumEng.lowX()];
			for (int x=0; x<energies.length; x++) {
				energies[x] = cumEng.get(x, cumEng.highY()-1);
			}
			return energies;
		}

		
		public static int[] findVSeam(Aggregates<? extends Double> cumEng, int selectedSeam) {
			int[] vseam = new int[cumEng.highY()-cumEng.lowY()];
			vseam[vseam.length-1] = selectedSeam;
			for (int y = cumEng.highY()-2; y>=cumEng.lowY(); y--) {
				int x = vseam[y-cumEng.lowY()+1]; //Get the x value for the next row down  

				double upLeft = x-1 >= cumEng.lowX() ? cumEng.get(x-1, y) : Double.MAX_VALUE;
				double up = cumEng.get(x, y);
				double upRight = x+1 < cumEng.highX() ? cumEng.get(x+1, y) : Double.MAX_VALUE;

				if (upLeft < up && upLeft < upRight) {x = x-1;}
				else if (up > upRight) {x = x+1;}
				
				vseam[y-cumEng.lowY()] = x;
			}
			return vseam;
		}
		
		
		/**
		 * @param seamEnergies Array of seam energies
		 * @param seamIdx Is this the 1st/2nd/3rd call to nextSeam?
		 * @return The index of the selected seam in seamEnergies
		 */
		private static final int selectSeam(final double[] seamEnergies, final int seamIdx) {
			int selected=0;
			double min = Integer.MAX_VALUE;
			int nearPosition=(7817*seamIdx)%seamEnergies.length;

			for (int x = 0; x < seamEnergies.length; x++) {
				Double eng = seamEnergies[x]; 

				//Use the current x as the start if it has lower energy than anything found before OR
				//   it has the same energy but is closer to the "nearPosition"
				if (min > eng   
						|| (min == eng && (Math.abs(nearPosition-x) < Math.abs(nearPosition-selected)))) {
					min = eng;
					selected = x;
				} 
			}

			return selected;
		}
		
		/**Carves a vertical seam out.**/
		private static final <A> Aggregates<A> carve(Aggregates<? extends A> aggs, int[] vseam) {
			Aggregates<A> rslt = 
					AggregateUtils.make(aggs.lowX(), aggs.lowY(), aggs.highX()-1, aggs.highY(), (A) aggs.defaultValue());
			
			for (int y = aggs.lowY(); y<aggs.highY(); y++) {
				int split = vseam[y-aggs.lowY()];
				for (int x=aggs.lowX(); x<split; x++) {rslt.set(x, y, aggs.get(x,y));}
				for (int x=split; x<aggs.highX(); x++) {rslt.set(x, y, aggs.get(x+1,y));}
			}
			return rslt;
		}
		
	}
	
	/**Find and remove seams per the w1 method of Huang, et al.
	 * 
	 * Each pair of adjacent rows is treated independently in a pre-processing step 
	 * to determine the best linkage between all items in the rows (thus "RowPair").
	 * 
	 * This carving method finds all of the seams it will remove entirely in the source
	 * image, and thus does not create as nice of images as the CarveIncremental BUT it is
	 * much faster.  The principal downfall of this method is that it only uses local information
	 * to create linkages.
	 */
	public static class CarveSweep<A> extends AbstractCarver<A> {
		/**
		 * @param delta Comparison function used to compute the energy matrix
		 * @param dir Direction seams run
		 * @param empty 
		 * @param seams How many seams to remove
		 */
		public CarveSweep(Delta<A> delta, Direction dir, A empty, int seams)  {super(delta, dir, empty, seams);}
		
		@Override
		public int[][] dropList(Aggregates<? extends A> aggregates, Renderer rend) {return dropList(aggregates, rend, dir);}
		public int[][] dropList(Aggregates<? extends A> aggregates, Renderer rend, Direction d) {
			if (seams == 0) {return new int[0][0];}
			
			if (d == Direction.H) {
					return transpose(dropList(TransposeWrapper.transpose(aggregates), rend, Direction.V));
			} else {
				Aggregates<Double> pixelEnergy = rend.transfer(aggregates, new Energy<>(delta));
				EdgeWeights weights = new EdgeWeights(pixelEnergy);

				//Matchings encode the offset to get to the matched node in the next level down.  Will always be -1/0/1
				Aggregates<Integer> matchings = matchings(weights, AggregateUtils.make(pixelEnergy, 0d));
				return computeDropList(seams, matchings, weights);
			}
		}
		
		/**Utility class, encapsulates a local energy matrix and computes
		 * a between-pixel energy matrix. 
		 */
		public static final class EdgeWeights implements Weights {
			final Aggregates<Double> pixelEnergy;
			public EdgeWeights(Aggregates<Double> pixelEnergy) {this.pixelEnergy = pixelEnergy;}

			public double between(int x1, int y1, int x2, int y2) {
				if (!validPoint(x1,y1) || !validPoint(x2,y2)) {return Double.NEGATIVE_INFINITY;}
				return pixelEnergy.get(x1, y1) * pixelEnergy.get(x2, y2); //w1 version
				//return pixelEnergy.get(x1, y1) + pixelEnergy.get(x2, y2); //w version
			}
			
			public boolean validPoint(int x, int y) {
				return x >= pixelEnergy.lowX() && x < pixelEnergy.highX()
						&& y >= pixelEnergy.lowY() && y < pixelEnergy.highY();
			}
		}
	}
	
	/**Find and remove seams per the w2 method of Huang, et al.
	 * 
	 * Uses a reverse cumulative energy matrix (similar to CarveIncremental) 
	 * but creates all seams at once (like CarveSweep).
	 * In a reverse cumulative energy matrix M, the value M(i,j) is the lowest-cost
	 * seam that starts at (i,j) and goes to the last row.  This integrates
	 * some global information into the matching equations with a cost of only
	 * one additional sweep through the matrix.
	 */
	public static class CarveTwoSweeps<A> extends AbstractCarver<A> {
		public CarveTwoSweeps(Delta<A> delta, Direction dir, A empty, int seams)  {
			super(delta, dir, empty, seams);
		}
		
		@Override
		public int[][] dropList(Aggregates<? extends A> aggregates, Renderer rend) {return dropList(aggregates, rend, dir);}
		public int[][] dropList(Aggregates<? extends A> aggregates, Renderer rend, Direction d) {
			if (seams == 0) {return new int[0][0];}
			
			if (d == Direction.H) {
					return transpose(dropList(TransposeWrapper.transpose(aggregates), rend, Direction.V));
			} else {
				Aggregates<Double> globalEnergy = rend.transfer(
						aggregates, 
						new Seq<>(new Energy<>(delta), new CumulativeEnergy()).specialize(aggregates));
				
				Aggregates<Double> cumEng = AggregateUtils.make(globalEnergy, Double.NEGATIVE_INFINITY);
				EdgeWeights weights = new EdgeWeights(globalEnergy, cumEng);
		
				Aggregates<Integer> matchings = matchings(weights, cumEng);
				
				return computeDropList(seams, matchings, weights);
			}
		}
	
		/**Utility class, encapsulates a local energy matrix and computes
		 * a between-pixel energy matrix. 
		 */
		public static final class EdgeWeights implements Weights {
			final Aggregates<Double> globalEnergy;
			final Aggregates<Double> cumEnergy;
			public EdgeWeights(Aggregates<Double> globalEnergy, Aggregates<Double> cumEnergy) {
				this.globalEnergy = globalEnergy;
				this.cumEnergy = cumEnergy;
			}
	
			public double between(int x1, int y1, int x2, int y2) {
				if (!validPoint(x1,y1) || !validPoint(x2,y2)) {return Double.NEGATIVE_INFINITY;}
				return cumEnergy.get(x1, y1) * globalEnergy.get(x2, y2); //cumEnergy to here * best-case energy to last row
				
			}
			
			public boolean validPoint(int x, int y) {
				return x >= globalEnergy.lowX() && x < globalEnergy.highX()
						&& y >= globalEnergy.lowY() && y < globalEnergy.highY();
			}
		}
	}
	
	/**Find exactly N seams and remove them.  Unlike CarveSweep and CarveTwoSweeps,
	 * which calculate seams for all pixels in all rows, this version only calculates 
	 * N total seams.
	 */
	public static class CarveSweepN<A> extends AbstractCarver<A> {
		public CarveSweepN(Delta<A> delta, Direction dir, A empty, int seams)  {
			super(delta, dir, empty, seams);
		}
				
		@Override
		public int[][] dropList(Aggregates<? extends A> aggregates, Renderer rend) {return dropList(aggregates, rend, dir);}
		public int[][] dropList(Aggregates<? extends A> aggregates, Renderer rend, Direction d) {
			if (seams == 0) {return new int[0][0];}
			
			if (d == Direction.H) {
					return transpose(dropList(TransposeWrapper.transpose(aggregates), rend, Direction.V));
			} else {
				Aggregates<Double> globalEnergy = rend.transfer(
						aggregates, 
						new Seq<>(new Energy<>(delta), new CumulativeEnergy()).specialize(aggregates));
				
				Aggregates<Double> cumEng = AggregateUtils.make(globalEnergy, Double.NEGATIVE_INFINITY);
				EdgeWeights energy = new EdgeWeights(globalEnergy, cumEng);
		
				Aggregates<Integer> matchings = nSeamsMatchings(seams, energy, cumEng, globalEnergy);
				
				return compileDropList(matchings, seams);
			}
		}
		
		public int[][] compileDropList(Aggregates<Integer> matchings, int seams) {
			int[][] dropList = new int[matchings.highY()-matchings.lowY()][seams];
			for (int y=matchings.lowY(); y<matchings.highY(); y++) {
				int offset=0;
				for (int x=matchings.lowX(); x<matchings.highX(); x++){
					int v = matchings.get(x, y);
					if (v != Integer.MIN_VALUE) {
						dropList[y-matchings.lowY()][offset] = x;
						offset++;
					}
				}
			}
			return dropList;
		}
		
		/** Concurrent find N seams.  Uses cumulative energy combined with 
		 * @param weights Function to determine the weight between two points
		 * @param cumEng Place to store the cumulative energy of a seam.  Will be DESTRUCTIVELY updated.
		 * @return
		 */
		private static final Integer ZERO = 0;
		private static final Integer ONE = 1;
		private static final Integer NEGATIVE_ONE = -1;
		private static final Aggregates<Integer> nSeamsMatchings(int seams, Weights weights, Aggregates<Double> cumEng, Aggregates<Double> globalEnergy) {
			
			//Order global energy starts
			SortedMap<Double, List<Integer>> seamOrder = new TreeMap<>();
		    for (int x = globalEnergy.lowX(); x < globalEnergy.highX(); x++) {
		        Double seamEnergy = globalEnergy.get(x, 0);
	    		List<Integer> idxs = seamOrder.get(seamEnergy);
		    	if (idxs==null) {idxs = new ArrayList<>();}
		    	int size = idxs.size()+1;
		    	idxs.add(((size+1)*7253)%size, x); //Mix up the order a bit; 7253 is just a prime number I knew
		    	seamOrder.put(seamEnergy, idxs);
		    }
			
		    //Select start positions in order of potential energy
			Aggregates<Integer> matches = AggregateUtils.make(cumEng, Integer.MIN_VALUE);
		    for (int i=0;i<seams; i++) {
		    	List<Integer> headList = seamOrder.get(seamOrder.firstKey());
		    	int x= headList.get(0);
		    	matches.set(x+matches.lowX(), matches.lowY(), 0);//Flag selected starts with a valid value
		    	cumEng.set(x+matches.lowX(), matches.lowY(), 0d);
		    	headList.remove(0);
		    	if (headList.size()==0) {seamOrder.remove(seamOrder.firstKey());}
		    }
			
			//Proceed by rows through the space
			//Naming imagines a slice of two rows arranged like this:
			//  col       m-2  m-1   m
		    //  row k:     A    B    C
			//  row k+1:   X    Y    Z
			// Processing tries to find the match for "B"
		    //
		    // The only unusual item is that if AY is currently selected, then BZ is disallowed and A may be switched to AX.  
		    // This keeps negative-slope parallel lines from forming, full handling of which is O(n^2).  Disallowing the left parallel case makes this O(n).
		    // (If the algorithm went from right-to-left through the rows then right-parallel lines would be disallowed).
			for (int y=cumEng.lowY(); y < cumEng.highY(); y++) {
				for (int x=cumEng.lowX(); x<cumEng.highX(); x++) {
					if (cumEng.get(x,y).equals(cumEng.defaultValue())) {continue;}

					double AX = weights.between(x-1, y, x-1, y+1);
					double AY = weights.between(x-1, y, x, y+1);
					boolean AXExists = !Double.isInfinite(AX);
					boolean AYExists = !Double.isInfinite(AY);

					double BX  = weights.between(x, y, x-1, y+1);
					double BY  = weights.between(x, y, x, y+1);
					double BZ  = weights.between(x, y, x+1, y+1);
					
					boolean doAX=false, doAY=false, doBX=false, doBY=false, doBZ=false;
					
					if (!AXExists && !AYExists) {
						if      (BX <= BY && BX <= BZ) {doBX=true;}
						else if (BY <= BX && BY <= BZ) {doBY=true;}
						else if (BZ <= BX && BZ <= BY) {doBZ=true;}
						else {throw new Error("Missed case...");}
					} else {//Need to check some compound options
						double o1 = AX+BY, o2=AY+BX, o3=AX+BZ;//compound options
						if      (BY <= BX && BY <= BZ) {doBY=true;}
						else if (BZ <= BX && BZ <= BY) {doBZ=true;}
						else if (o1 <= o2 && o1 <= o3) {doAX=true;doBY=true;}
						else if (o2 <= o1 && o2 <= o3) {doAY=true;doBX=true;}
						else if (o3 <= o1 && o3 <= o2) {doAX=true;doBZ=true;}
						else {throw new Error("Missed case...");}
					} 
					
					if (doAX) {
						matches.set(x-1, y, ZERO);
						cumEng.set(x-1, y+1, cumEng.get(x-1,y)+AX);
					}
					
					if (doAY) {
						matches.set(x, y, ONE);
						cumEng.set(x-1, y+1, cumEng.get(x-1,y)+BY);
					}
					
					if (doBX) {
						matches.set(x, y, NEGATIVE_ONE);
						cumEng.set(x-1, y+1, cumEng.get(x,y)+BX);
					}
					
					if (doBY) {
						matches.set(x, y, ZERO);
						cumEng.set(x, y+1, cumEng.get(x,y)+BY);
					}
					
					if (doBZ) {
						matches.set(x, y, ONE);
						cumEng.set(x+1, y+1, cumEng.get(x,y)+BZ);
					}
				}

			}
			return matches;
		}
	
		/**Utility class, encapsulates a local energy matrix and computes
		 * a between-pixel energy matrix. 
		 */
		public static final class EdgeWeights implements Weights {
			final Aggregates<Double> globalEnergy;
			final Aggregates<Double> cumEnergy;
			public EdgeWeights(Aggregates<Double> globalEnergy, Aggregates<Double> cumEnergy) {
				this.globalEnergy = globalEnergy;
				this.cumEnergy = cumEnergy;
			}
	
			public double between(int x1, int y1, int x2, int y2) {
				if (!validPoint(x1,y1) || !validPoint(x2,y2)) {return Double.POSITIVE_INFINITY;}
				return cumEnergy.get(x1, y1) * globalEnergy.get(x2, y2); //cumEnergy to here * best-case energy to last row
				
			}
			
			public boolean validPoint(int x, int y) {
				return x >= globalEnergy.lowX() && x < globalEnergy.highX()
						&& y >= globalEnergy.lowY() && y < globalEnergy.highY();
			}
		}
	}
	
	public interface Weights {public double between(int x1, int y1, int x2, int y2);}

	/**Transpose a 2D array of ints.  Assumes the 2D array is rectangular.**/
	public static int[][] transpose(int[][] in) {
		int width = in.length;
		int height = in[0].length;
		int[][] out = new int[height][width];
		
		for (int x=0; x<width;x++) {
			for (int y=0; y<height; y++) {
				out[y][x]=in[x][y];
			}
		}
		return out;
	}
	
	/**Sorts each row of the passed matrix IN PLACE.**/
	public static int[][] sortRows(int[][] in) {
    	for (int i=0;i<in.length;i++) {
    		Arrays.sort(in[i]);
    	}
    	return in;
	}
	
	/**Follow a seam, accumulate each index along the way so the seam can be followed
	 * without the matchings matrix.**/
	public static final int[] compileVSeam(int x, Aggregates<Integer> matchings) {
		final int[] seam = new int[matchings.highY()-matchings.lowY()];
		for (int y=0;y<seam.length;y++) {
			seam[y] =x;
			x = x+matchings.get(x, y+matchings.lowY());
		}
		return seam;
	}
	
	

	/** Match each pixel in each row with another pixel in the next row down.
	 * 
	 * All matchings are 1:1 and the total matching maximizes the sum of the energy 
	 * in pairs between the rows.  This maximization has been shown to increase 
	 * the variance, therefore it tends to produce seams that are either clearly 
	 * important or clearly not.  
	 * 
	 * This is a strictly local calculation.  There are other methods (method w2 in particular)
	 * that also take into account global information. 
	 *
	 * @param weights Function to determine the weight between two points
	 * @param cumEng Place to store the cumulative energy of a seam.  Will be DESTRUCTIVELY updated.
	 * @return
	 */
	private static final Integer ZERO = 0;
	private static final Integer ONE = 1;
	private static final Integer NEGATIVE_ONE = -1;
	public static final Aggregates<Integer> matchings(Weights weights, Aggregates<Double> cumEng) {
		Aggregates<Integer> matches = AggregateUtils.make(cumEng, Integer.MIN_VALUE);
		
		//Proceed by rows through the space
		//Naming imagines a slice of two rows arranged like this:
		//  col       m-2  m-1   m
		//  row k:     A    B    C
		//  row k+1:   X    Y    Z
		// Processing tries to find the match for "C" (either Y or Z) but may change A and B as well in one case
		for (int y=cumEng.lowY(); y < cumEng.highY(); y++) {
			double F1=0,F2=0,F3=0;
			for (int x=cumEng.lowX(); x<cumEng.highX(); x++) {
				double CZ = weights.between(x,y,x,y+1);
				double CY= weights.between(x,y,x-1,y+1);
				double BZ = weights.between(x-1,y,x,y+1);
				double AX = weights.between(x-2,y,x-2,y+1);
				
				if (matches.get(x-2,y)==1 && matches.get(x-1,y)==-1) { //Prior nodes are cross-linked, so things could get complicated...
					double FA=F1+CZ;		//C does down, simple to handle
					double FB=F3+CY+BZ+AX;  //C goes left, and A needs to change too
					if (FA >= FB) { // C points down (CZ linked), no changes required.
						matches.set(x, y, ZERO);  //point C's linkage
						cumEng.set(x, y+1, cumEng.get(x,y)+CZ); //point Z's cumulative energy
						F3=F2;
						F2=F1;
						F1=FA;
					} else { // C points left, B points right and A points down. 
						matches.set(x, y, NEGATIVE_ONE); //point C linkage
						matches.set(x-1, y, ONE);		 //point B
						matches.set(x-2, y, ZERO);		 //point A

						cumEng.set(x, y+1, cumEng.get(x,y)+CY);				 //Point Z cumulative energy
						cumEng.set(x-1, y+1, cumEng.get(x-1,y)+BZ);			 //Point Y 
						cumEng.set(x-2, y+1, cumEng.get(x-2,y)+AX);			 //Point X 
						
						F3=F2;
						F2=F1;
						F1=FB;
					}
				} else {
					double FA=F1+CZ;
					double FB=F2+CY+BZ;
					if (FA >= FB) { //B can keep pointing wherever it was, point C down
						matches.set(x, y, ZERO);
						cumEng.set(x, y+1, cumEng.get(x,y)+CZ); //point Z's cumulative energy

						F3=F2;
						F2=F1;
						F1=FA;
					} else { // B was already going down, now just point it right instead
						matches.set(x, y, NEGATIVE_ONE);
						matches.set(x-1, y, ONE);

						cumEng.set(x, y+1, cumEng.get(x,y)+CY);				 //Point Z cumulative energy
						cumEng.set(x-1, y+1, cumEng.get(x-1,y)+BZ);			 //Point Y 

						F3=F2;
						F2=F1;
						F1=FB;
					}
				}
			}
		}
		return matches;
	}
	
	/**Locate the indicated number of seams given the matchings and energy functions.
	 * Assumes the matching function is total (e.g., all values are -1/0/1).
	 * Will pick the lowest energy seams first.
	 * 
	 * @param seams Number of seams to remove
	 * @param matchings Total matching
	 * @param energy Energy function.
	 * @return For x=int[A][B], x is the B'th x index to drop in row A
	 */
	private static int[][] computeDropList(int seams, Aggregates<Integer> matchings, Weights energy) {
		//Compute seam totals by iterating down the matchings  matrix
		double[] seamEnergies = new double[matchings.highX()-matchings.lowX()]; 
		for (int x=matchings.lowX(); x<matchings.highX(); x++) {
			int sourceX=x;
			for (int y=matchings.lowY(); y<matchings.highY()-1; y++) {//If you look down past the last row, you get -inf....
				int targetX = sourceX + matchings.get(sourceX,y);
				seamEnergies[x-matchings.lowX()] += energy.between(sourceX, y, targetX, y+1);
				sourceX=targetX;
			}
		}
		
		
		//Order seams
		SortedMap<Double, List<Integer>> seamOrder = new TreeMap<>();
	    for (int i = 0; i < seamEnergies.length; i++) {
	        Double seamEnergy = seamEnergies[i];
    		List<Integer> idxs = seamOrder.get(seamEnergy);
	    	if (idxs==null) {idxs = new ArrayList<>();}
	    	int size = idxs.size()+1;
	    	idxs.add(((size+1)*7253)%size,i+matchings.lowX()); //Mix up the order a bit; 7253 is just a prime number I knew
	    	seamOrder.put(seamEnergy, idxs);
	    }
		
	    //Select seams in order
	    int[][] seamPoints = new int[seams][];
	    for (int i=0;i<seams; i++) {
	    	List<Integer> headList = seamOrder.get(seamOrder.firstKey());
	    	int targetSeam = headList.get(0);
	    	headList.remove(0);
	    	if (headList.size()==0) {seamOrder.remove(seamOrder.firstKey());}
	    	seamPoints[i] = compileVSeam(targetSeam, matchings);		    	
	    }
	    
    	int[][] dropList = sortRows(transpose(seamPoints));
	    
	    return dropList;
	}
	
	/**Remove N seams.*
	 * 
	 * @param aggregates Item to carve
	 * @param dropList List of items to remove.  For x=int[A][B], x is the B'th x index to drop in row A (i.e., a sorted transpose of a seam list).
	 * */
	public static <A> Aggregates<A> carveN(final Aggregates<? extends A> aggregates, final int[][] dropList) {
    	//Carve ALL seam-points out of the aggregates...
		Aggregates<A> result = AggregateUtils.make(aggregates.lowX(), 
													aggregates.lowY(), 
													aggregates.highX()-dropList[0].length, 
													aggregates.highY(), (A) aggregates.defaultValue());
		
    	for (int y=aggregates.lowY(); y<aggregates.highY(); y++) {
    		int i = y-aggregates.lowY();
    		int dropCount=0;
    		for (int x=aggregates.lowX(); x<aggregates.highX(); x++) {
    			if (dropCount < dropList[i].length && dropList[i][dropCount] == x) {
    				dropCount++; 
    				continue;
    			}
    			result.set(x-dropCount, y, aggregates.get(x, y));
    		}
    	}
    	return result;
	}
	
	
	/**Computes the energy of a set of aggregates.
	 */
	public static class Energy<A> implements Transfer.ItemWise<A, Double> {
		public Delta<A> delta;
		
		public Energy(Delta<A> delta) {this.delta=delta;}

		@Override public Double emptyValue() {return 0d;}
		@Override 
		public Specialized<A, Double> specialize(Aggregates<? extends A> aggregates) {return this;}

		@Override
		public Double at(int x, int y, Aggregates<? extends A> aggregates) {
			A empty = aggregates.defaultValue();
			return delta.delta(aggregates.get(x,y), empty);
		}

		@Override
		public Aggregates<Double> process(Aggregates<? extends A> aggregates, Renderer rend) {
			return rend.transfer(aggregates, this);
		}
	}

	public static class CumulativeEnergy implements Transfer.Specialized<Double, Double> {
		@Override public Double emptyValue() {return 0d;}
		@Override 
		public Specialized<Double, Double> specialize(Aggregates<? extends Double> aggregates) {return this;}

		//TODO: Parallelize
		@Override 
		public Aggregates<Double> process(Aggregates<? extends Double> aggregates, Renderer render) {
			Aggregates<Double> cached = AggregateUtils.make(aggregates, 0d);
			
			//Copy the first row over...
			for (int x = aggregates.lowX(); x<aggregates.highX(); x++) {
				cached.set(x,aggregates.lowY(), aggregates.get(x,aggregates.lowY()));
			}
			
			for (int y = aggregates.lowY()+1; y<aggregates.highY(); y++) {
				for (int x = aggregates.lowX(); x<aggregates.highX(); x++) {
					double upLeft = x-1 >= aggregates.lowX() ? cached.get(x-1, y-1) : Double.MAX_VALUE;
					double up = cached.get(x, y-1);
					double upRight = x+1 < aggregates.highX() ? cached.get(x+1, y-1) :Double.MAX_VALUE;
					
					double min = Math.min(upRight, Math.min(upLeft, up));
					cached.set(x, y, min+aggregates.get(x, y));
				}
			}
			return cached;
		}
	}
	

	
	public static final class LeftValue<A extends Number> implements Delta<A> {
		public double delta(Number left, Number right) {return left.doubleValue();}
	}

	
	public static final class DeltaDouble implements Delta<Double> {
		public double delta(Double left, Double right) {return left-right;}
	}

	public static final class DeltaInteger implements Delta<Integer> {
		public double delta(Integer left, Integer right) {return left-right;}
	}

	public static final class DeltaLuminance implements Delta<Color> {
		public double delta(Color left, Color right) {return lum(left)-lum(right);}
		public static double lum(Color c) {return 0.299*c.getRed() + 0.587*c.getGreen() + 0.114*c.getBlue();}
	}
	
	/** Euclidean distance between two colors in RGB space.**/
	public static final class RGBEuclid implements Delta<Color> {
		public double delta(Color left, Color right) {
			return Math.sqrt(
				  Math.pow(left.getRed()  - right.getRed(), 2)
				+ Math.pow(left.getGreen()- right.getGreen(), 2) 
				+ Math.pow(left.getBlue() - right.getBlue(), 2));
		}
	}
	
	public static final class DrawSeams<A> implements Transfer.Specialized<A, Color> {
		private final AbstractCarver<A> carver;
		private final Color seam;
		private final Color notSeam = Util.CLEAR;
		
		public DrawSeams(AbstractCarver<A> carver, Color seam) {
			this.carver = carver;
			this.seam = seam;
		}

		@Override public Color emptyValue() {return notSeam;}

		@Override public ar.Transfer.Specialized<A, Color> specialize(Aggregates<? extends A> aggregates) {return this;}

		@Override
		public Aggregates<Color> process(Aggregates<? extends A> aggregates, Renderer rend) {
			int[][] drops = carver.dropList(aggregates, rend);
			
			Aggregates<Color> seams = AggregateUtils.make(aggregates, notSeam);
			for (int y=0; y<drops.length;y++) {
				for (int x=0; x<drops.length;x++) {
					seams.set(x, y, seam);
				}
			}
			return seams;
		}
	}
}
