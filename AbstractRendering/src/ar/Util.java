package ar;

import java.awt.Color;

public final class Util {
	public static final Color CLEAR = new Color(0,0,0,0);

	private Util() {}

	
	public static Color interpolate(Color low, Color high, double min, double max, double v) {
		double distance = 1-((max-v)/(max-min));
		int r = (int) weightedAverage(high.getRed(), low.getRed(), distance);
		int g = (int) weightedAverage(high.getGreen(), low.getGreen(), distance);
		int b = (int) weightedAverage(high.getBlue(), low.getBlue(), distance);
		int a = (int) weightedAverage(high.getAlpha(), low.getAlpha(), distance);						
		return new java.awt.Color(r,g,b,a);
	}

	public static double weightedAverage(double v1, double v2, double weight) {
		return (v1 -v2) * weight + v2;
	}
	
	
	/**What is the min/max/mean/stdev in the collection of aggregates (assuming its over numbers)**/
	public static Stats stats(Aggregates<? extends Number> aggregates, boolean ignoreZeros) {
		//Single-pass std. dev: http://en.wikipedia.org/wiki/Standard_deviation#Rapid_calculation_methods 
		double count=0;
		double min = Double.POSITIVE_INFINITY, max=Double.NEGATIVE_INFINITY;
		double sum=0;
		
		for (Number n: aggregates) {
			double v = n.doubleValue();
			if (ignoreZeros && v == 0) {continue;}
			if (min > v) {min = v;}
			if (max < v) {max = v;}
			sum += v;
			count++;
		}
		
		final double mean = sum/count;
		double acc =0;

		for (Number n: aggregates) {
			final double v = n.doubleValue();
			acc = Math.pow((v-mean),2);
		}
		double stdev = Math.sqrt(acc/count);
		return new Stats(min,max,mean,stdev);
	}
	
	
	public static <T extends Number> Aggregates<Double> score(Aggregates<T> source, Stats extrema) {
		final Aggregates<Double> results = new Aggregates<Double>(source.width(), source.height());
		final double mean = extrema.mean;
		final double stdev = extrema.stdev;
		for (int x=0;x<results.width();x++) {
			for (int y=0; y<results.height(); y++) {
				final double v = source.at(x, y).doubleValue();
				final double z = Math.abs((v-mean)/stdev);
				results.set(x, y, z);
			}
		}
		return results;
	}
	
	public static final class Stats {
		public final double min;
		public final double max;
		public final double mean;
		public final double stdev;
		public Stats(double min, double max, double mean, double stdev) {
			this.min = min; 
			this.max=max;
			this.mean=mean;
			this.stdev = stdev;
		}
		public String toString() {return String.format("Min: %.3f; Max: %.3f; Mean: %.3f; Stdev: %.3f", min,max,mean,stdev);}
	}

}
