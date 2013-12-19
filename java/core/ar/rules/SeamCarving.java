package ar.rules;


import java.awt.Color;

import ar.Aggregates;
import ar.Resources;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.aggregates.TransposeWrapper;
import ar.rules.combinators.Seq;
import ar.util.CacheProvider;
import ar.util.Util;

//Paper: http://www.win.tue.nl/~wstahw/edu/2IV05/seamcarving.pdf

public class SeamCarving {
	/**Calculate the difference between two values.**/
	public interface Delta<A> {public double delta(A left, A right);}

	
	/**Find and remove a single seam.**/
	public static class Carve<A> implements Transfer<A,A> {
		public enum Direction {H,V}
		
		final Delta<A> delta;
		final A empty;
		Direction dir;
		
		public Carve(Delta<A> delta, Direction dir, A empty)  {
			this.delta = delta;
			this.empty = empty;
			this.dir = dir;
		}
		
		@Override public A emptyValue() {return empty;}
		
		@Override 
		public Specialized<A, A> specialize(Aggregates<? extends A> aggregates) { 
				return new Specialize<>(aggregates, delta, dir, empty);
		}
		
		public static class Specialize<A> extends Carve<A> implements Transfer.ControlFlow<A,A>, CacheProvider.CacheTarget<A,A> {
			final CacheProvider<A,A> cache;

			public Specialize(Aggregates<? extends A> aggs, Delta<A> delta, Direction dir, A empty) {
				super(delta, dir, empty);
				cache = new CacheProvider<>(this);
			}

			
			@Override public Aggregates<A> process(Aggregates<? extends A> aggregates) {return (Aggregates<A>) cache.get(aggregates);}
			@Override public A at(int x, int y, Aggregates<? extends A> aggregates) {return cache.get(aggregates).get(x, y);}
			
			public void direction(Direction dir) {this.dir = dir;}

			public Aggregates<? extends A> build(Aggregates<? extends A> aggs) {
				if (dir == Direction.H) {return horizontal(aggs);}
				else {return vertical(aggs);}
			}
			
			public Aggregates<? extends A> horizontal(Aggregates<? extends A> aggs) {
				return TransposeWrapper.transpose(vertical(TransposeWrapper.transpose(aggs)));
			}
			
			public Aggregates<? extends A> vertical(Aggregates<? extends A> aggs) {
				Aggregates<Double> energy = Resources.DEFAULT_RENDERER.transfer(aggs, new Energy<>(delta));
				Aggregates<Double> cumEng = Resources.DEFAULT_RENDERER.transfer(energy, new CumulativeEnergy().specialize(energy));
				
				//find seam
				int[] vseam = new int[cumEng.highY()-cumEng.lowY()];			
				double min = Integer.MAX_VALUE;
				for (int x = cumEng.lowX(); x < cumEng.highX(); x++) {
					Double eng = cumEng.get(x, cumEng.highY()-1); 
					if (min > eng) {
						min = eng;
						vseam[vseam.length-1] = x;
					}
				}
					
				for (int y = cumEng.highY()-2; y>=cumEng.lowY(); y--) {
					int x = vseam[y-cumEng.lowY()+1];  
					double upLeft = cumEng.get(x-1, y-1);
					double up = cumEng.get(x, y-1);
					double upRight = cumEng.get(x+1, y-1);
					
					if (upLeft < up && upLeft < upRight) {x = x -1;}
					else if (up > upRight) {x = x+1;}
					
					vseam[y-cumEng.lowY()] = x;
				}
				
				//System.out.print(cumEng.get(vseam[vseam.length-1], cumEng.highX()-1));
				for (int i=0; i<vseam.length;i++) {
					System.out.print(cumEng.get(vseam[i], i+cumEng.lowX()));
					System.out.print(", ");
				}
				System.out.println();
								
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
	}
	
	/**Computes the energy of a set of aggregates.
	 */
	public static class Energy<A> implements Transfer.Specialized<A, Double> {
		public Delta<A> delta;
		
		public Energy(Delta<A> delta) {this.delta=delta;}

		@Override public Double emptyValue() {return 0d;}
		@Override public Specialized<A, Double> specialize(Aggregates<? extends A> aggregates) {return this;}

		@Override
		public Double at(int x, int y, Aggregates<? extends A> aggregates) {
			A empty = aggregates.defaultValue();
			return delta.delta(aggregates.get(x,y), empty);
		}
	}

	public static class CumulativeEnergy implements Transfer.Specialized<Double, Double>, CacheProvider.CacheTarget<Double, Double> {
		private final CacheProvider<Double, Double> cache;
		public CumulativeEnergy() {cache = new CacheProvider<>(this);}
		
		@Override public Double emptyValue() {return 0d;}
		@Override public Specialized<Double, Double> specialize(Aggregates<? extends Double> aggregates) {return this;}
		@Override public Double at(int x, int y, Aggregates<? extends Double> aggregates) {return cache.get(aggregates).get(x, y);}

		@Override
		public Aggregates<? extends Double> build(Aggregates<? extends Double> aggregates) {
			Aggregates<Double> cached = AggregateUtils.make(aggregates, 0d);
			
			//Copy the first row over...
			for (int x = aggregates.lowX(); x<aggregates.highX(); x++) {
				cached.set(x,aggregates.lowY(), aggregates.get(x,aggregates.lowY()));
			}
			
			for (int y = aggregates.lowY()+1; y<aggregates.highY(); y++) {
				for (int x = aggregates.lowX(); x<aggregates.highX(); x++) {
					double upLeft = x-1 > aggregates.lowX() ? cached.get(x-1, y-1) : Double.MAX_VALUE;
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
