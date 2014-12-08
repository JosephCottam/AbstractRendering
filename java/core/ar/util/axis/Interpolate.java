package ar.util.axis;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**Given a set of seeds, produce a new set of seeds of the requested target size.**/
public interface Interpolate<T> {
	public Map<T, Double> interpolate(Map<T,Double> seeds, int targetSize);
	
	
	/**If seeds is not a SortedMap, returns the input value unchanged. 
	 * If seeds is a SortedMap, returns a strided selection of the seeds.
	 * If forceLast is true, will return a set of targetSize+1 to ensure the last item is present.
	 * **/
	public static class Discrete<T> implements Interpolate<T> {
		public final boolean forceLast;
		
		public Discrete() {this(false);}
		public Discrete(boolean forceLast) {this.forceLast = forceLast;}
		
		@Override
		public Map<T, Double> interpolate(Map<T, Double> seeds, int targetSize) {
			if (!(seeds instanceof SortedMap)) {return seeds;}
			
			SortedMap<T, Double> rslt = new TreeMap<T, Double>();
			int tick =0;
			Map.Entry<T,Double> last = null;
			for (Map.Entry<T,Double> e: seeds.entrySet()) {
				last = e;
				if (tick ==0) {rslt.put(e.getKey(), e.getValue());}
				tick = (tick+1)%targetSize;
			}
			
			if (forceLast && last != null) {rslt.put(last.getKey(), last.getValue());}
			
			return rslt;
		}		
	}
	
	//TODO: Add switch for log 
	//TODO: Add the 'nice' ticks logic 
	//TODO: Add logic for non-double keys
	public static class LinearSmooth implements Interpolate<Double> {
	
		@Override
		public Map<Double, Double> interpolate(Map<Double, Double> seeds, int targetSize) {
			double min, max;
			double high, low;
	
			SortedMap<Double, Double> m;
			if (seeds instanceof SortedMap) {
				m = (SortedMap<Double,Double>) seeds;
			} else {
				m = new TreeMap<>();
				m.putAll(seeds);								
			}
			
			min = m.firstKey();
			low = m.get(min);
			max = m.lastKey();
			high = m.get(max);
			
			double keySpan = max-min; //HACK!!
			double keyStride = keySpan/(targetSize-1);
			double valSpan = high-low;
			double valStride = valSpan/(targetSize-1);
			
			Map<Double, Double> rslt = new TreeMap<>();
			for (int i=0; i<targetSize; i++) {
				rslt.put(min+(keyStride*i), low+(valStride*i));
			}
			rslt.put(max, high);
			return rslt;
		}
		
	}
}