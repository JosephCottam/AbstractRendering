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
	
	/**Find and remove a seams, per the Avidan and Shamir method.
	 * 
	 * This class will calculate the full energy matrix each time a seam is removed.
	 * Since the whole matrix is calculated each time, its slow BUT it is also stateless.
	 * 
	 * For faster results, try other "Carve" classes.
	 * Each has a different set of tradeoffs, but they all run significantly faster.  
	 * 
	 * **/
	public static class OptimalCarve<A> implements Transfer.Specialized<A,A> {
		protected final Delta<A> delta;
		protected final A empty;
		protected final Direction dir;
		protected final int seams;
		
		public OptimalCarve(Delta<A> delta, Direction dir, A empty, int seams)  {
			this.delta = delta;
			this.empty = empty;
			this.dir = dir;
			this.seams = seams;
		}
		
		@Override public A emptyValue() {return empty;}
		
		@Override @SuppressWarnings("unused") 
		public Specialized<A, A> specialize(Aggregates<? extends A> aggregates) {return this;}
			
		@Override 
		public Aggregates<A> process(Aggregates<? extends A> aggregates, Renderer rend) { 
			if (dir == Direction.H) {return horizontal(aggregates, rend);}
			else {return vertical(aggregates, rend);}
		}
				
		public Aggregates<A> horizontal(Aggregates<? extends A> aggs, Renderer rend) {
			return TransposeWrapper.transpose(vertical(TransposeWrapper.transpose(aggs), rend));
		}
		
		public Aggregates<A> vertical(Aggregates<? extends A> aggs, Renderer rend) {
			Aggregates<? extends Double> pixelEnergy = rend.transfer(aggs, new Energy<>(delta));

			@SuppressWarnings("unchecked")
			Aggregates<A> rslt = (Aggregates<A>) aggs;
			
			for (int i=0; i<seams; i++) {
				Aggregates<? extends Double> cumEng = rend.transfer(pixelEnergy, new CumulativeEnergy());
				int selectedSeam = selectSeam(seamEnergies(cumEng), i);
				int[] vseam = findVSeam(cumEng, selectedSeam);
				rslt = carve(rslt, vseam);
				pixelEnergy = carve(pixelEnergy,vseam);
			}

			return rslt;
		}

		/**Get just the seam energies.
		 * TODO: Eliminate this method by having a wrapper that presents a row/col of aggregates as a list.  Have selectSeam take List<Double>
		 * **/
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
					//AggregateUtils.make(aggs, (A) aggs.defaultValue());
			
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
	 * The list of linkage describes seams that involve all pixels in the image,
	 * so multiple seams can be removed without recalculating (thus "GlobalCarve").
	 */
	public static class RowPairGlobalCarve<A> implements Transfer.Specialized<A, A> {
		final Delta<A> delta;
		final A empty;
		final Direction dir;
		final int seams;
		
		/**
		 * @param delta Comparison function used to compute the energy matrix
		 * @param dir Direction seams run
		 * @param empty 
		 * @param seams How many seams to remove
		 */
		public RowPairGlobalCarve(Delta<A> delta, Direction dir, A empty, int seams)  {
			this.delta = delta;
			this.empty = empty;
			this.dir = dir;
			this.seams= seams;
		}
		
		@Override public A emptyValue() {return empty;}

		@Override @SuppressWarnings("unused") 
		public ar.Transfer.Specialized<A, A> specialize(Aggregates<? extends A> aggregates) {return this;}

		@Override
		public Aggregates<A> process(Aggregates<? extends A> aggregates, Renderer rend) {
			if (dir == Direction.H) {return horizontal(aggregates, rend);}
			return vertical(aggregates, rend);
		}
			
		public Aggregates<A> horizontal(Aggregates<? extends A> aggregates, Renderer rend) {
			return TransposeWrapper.transpose(vertical(TransposeWrapper.transpose(aggregates), rend));
		}
		
		public Aggregates<A> vertical(Aggregates<? extends A> aggregates, Renderer rend) {
			Aggregates<Double> pixelEnergy = rend.transfer(aggregates, new Energy<>(delta));
			EdgeEnergy energy = new EdgeEnergy(pixelEnergy);

			//Matchings encode the offset to get to the matched node in the next level down.  Will always be -1/0/1
			Aggregates<Integer> matchings = matchings(pixelEnergy, energy);
			
			//Compute seam totals by iterating down the matchings and pixel matrices
			double[] seamEnergies = new double[pixelEnergy.highX()-pixelEnergy.lowX()]; 
			for (int x=pixelEnergy.lowX(); x<pixelEnergy.highX(); x++) {
				int sourceX=x;
				for (int y=pixelEnergy.lowY(); y<pixelEnergy.highY()-1; y++) {//If you look down past the last row, you get -inf....
					int targetX = sourceX + matchings.get(sourceX,y);
					seamEnergies[x] += energy.between(sourceX, y, targetX, y+1);
					sourceX=targetX;
				}
			}
			
			//Order seams
			SortedMap<Double, List<Integer>> seamOrder = new TreeMap<>();
		    for (int i = 0; i < seamEnergies.length; ++i) {
		        Double seamEnergy = seamEnergies[i];
	    		List<Integer> idxs = seamOrder.get(seamEnergy);
		    	if (idxs==null) {idxs = new ArrayList<>();}
		    	int size = idxs.size()+1;
		    	idxs.add(((size+1)*7253)%size, i); //Mix up the order a bit; 7253 is just a prime number I knew
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
					    
	    	seamPoints = transpose(seamPoints);
	    	for (int i=0;i<seamPoints.length;i++) {
	    		Arrays.sort(seamPoints[i]);
	    	}
		    
	    	//Carve ALL seam-points out of the aggregates...
			Aggregates<A> result = AggregateUtils.make(aggregates, (A) aggregates.defaultValue());
	    	for (int y=aggregates.lowY(); y<aggregates.highY(); y++) {
	    		int i = y-aggregates.lowY();
	    		int dropCount=0;
	    		for (int x=aggregates.lowX(); x<aggregates.highX(); x++) {
	    			if (seamPoints[i][dropCount] == x) {
	    				dropCount++; 
	    				continue;
	    			}
	    			result.set(x-dropCount, y, aggregates.get(x, y));
	    		}
	    	}
	    	return result;
		}
	
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
		 * @param aggregates
		 * @param energy
		 * @return
		 */
		private static final Integer ZERO = 0;
		private static final Integer ONE = 1;
		private static final Integer NEGATIVE_ONE = -1;
		public static final Aggregates<Integer> matchings(Aggregates<Double> aggregates, EdgeEnergy energy) {
			Aggregates<Integer> matches = AggregateUtils.make(aggregates, Integer.MIN_VALUE);
			//Proceed by rows through the space
			//Naming imagines a slice of two rows arranged like this:
			//  col       m-2  m-1   m
			//  row k:     A    B    C
			//  row k+1:   X    Y    Z
			// Processing tries to find the match for "C" (either Y or Z) but may change A and B as well in one case
			for (int y=aggregates.lowY(); y < aggregates.highY(); y++) {
				double F1=0,F2=0,F3=0;
				for (int x=aggregates.lowX(); x<aggregates.highX(); x++) {
					double CZ = energy.between(x,y,x,y+1);
					double CY= energy.between(x,y,x-1,y+1);
					double BZ = energy.between(x-1,y,x,y+1);
					double AX = energy.between(x-2,y,x-2,y+1);
					
					if (matches.get(x-2,y)==1 && matches.get(x-1,y)==-1) { //Prior nodes are cross-linked, so things could get complicated...
						double FA=F1+CZ;		//C does down, simple to handle
						double FB=F3+CY+BZ+AX;  //C goes left, and A needs to change too
						if (FA >= FB) { // C points down, no changes required.
							matches.set(x, y, ZERO);
							F3=F2;
							F2=F1;
							F1=FA;
						} else { // C points left, B points right and A points down. 
							matches.set(x, y, NEGATIVE_ONE);
							matches.set(x-1, y, ONE);
							matches.set(x-2, y, ZERO);
							F3=F2;
							F2=F1;
							F1=FB;
						}
					} else {
						double FA=F1+CZ;
						double FB=F2+CY+BZ;
						if (FA >= FB) { //B can keep pointing wherever it was, point C down
							matches.set(x, y, ZERO);
							F3=F2;
							F2=F1;
							F1=FA;
						} else { // B was already going down, now just point it right instead
							matches.set(x, y, NEGATIVE_ONE);
							matches.set(x-1, y, ONE);
							F3=F2;
							F2=F1;
							F1=FB;
						}
					}
				}
			}
			return matches;
		}
		
		/**After carving out seams, any crossing seams need to be straightened to preserve the 1:1 matching between rows.
		 * 
		 * 
		 * If items are matched like so and A is carved ....
		 * A   B            B
		 *  \ /     ===>   /
		 *  / \           / 
		 * C   D            C
		 * 
		 * Notice that B now points off the grid and C has no match.  But it is easy to patch up, all diagonals happen in pairs,
		 * so Just set B to match DOWN instead of LEFT and you are fixed.  Fixes only need to happen along the old seam
		 * where the link was not down.
		 * 
		 * @param seam  The seam removed
		 * @param oldMatching What the matching values were (used to check for non-down links)
		 * @param newMatching The post-carved links (updated in place)
		 */
		public static Aggregates<Integer> repairMatching(final int[] seam, Aggregates<Integer> oldMatching, Aggregates<Integer> newMatching) {
			for (int i=0; i<seam.length; i++) {
				int x=seam[i];
				int y=i+oldMatching.lowY();

				int oldDir = oldMatching.get(x,y);
				if (oldDir == 1) {
					newMatching.set(x, y, ZERO);
				} else if (oldDir == -1) {
					newMatching.set(x-1, y, ZERO);					
				}
			}
			return newMatching;
		}
		

		/**Utility class, encapsulates a local energy matrix and computes
		 * a between-pixel energy matrix. 
		 */
		public static final class EdgeEnergy {
			final Aggregates<Double> pixelEnergy;
			public EdgeEnergy(Aggregates<Double> pixelEnergy) {this.pixelEnergy = pixelEnergy;}

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

	
	/**Computes the energy of a set of aggregates.
	 */
	public static class Energy<A> implements Transfer.ItemWise<A, Double> {
		public Delta<A> delta;
		
		public Energy(Delta<A> delta) {this.delta=delta;}

		@Override public Double emptyValue() {return 0d;}
		@Override @SuppressWarnings("unused") 
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
		@Override @SuppressWarnings("unused") 
		public Specialized<Double, Double> specialize(Aggregates<? extends Double> aggregates) {return this;}

		//TODO: Parallelize
		@Override @SuppressWarnings("unused") 
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
	
	/** Euclidean distance between two colors.**/
	public static final class RGBEuclid implements Delta<Color> {
		public double delta(Color left, Color right) {
			return Math.sqrt(
				  Math.pow(left.getRed()  - right.getRed(), 2)
				+ Math.pow(left.getGreen()- right.getGreen(), 2) 
				+ Math.pow(left.getBlue() - right.getBlue(), 2));
		}
	}
}
