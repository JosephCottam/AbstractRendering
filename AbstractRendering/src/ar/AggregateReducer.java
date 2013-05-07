package ar;

import java.awt.Dimension;

/**Combine two aggregate sets into a third composite aggregate.**/
public interface AggregateReducer<IN1,IN2,OUT> {
	public OUT combine(IN1 left, IN2 right);
	
    public static final class Util {
    	
    	@SuppressWarnings("unchecked")
		public static <T> Aggregates<T> 
    	reduce(Aggregates<T> left, Aggregates<T> right, AggregateReducer<T,T,T> red) {
    		
    		Aggregates<T> [] sources;
			Aggregates<T> target;
			Dimension d = AggregateReducer.Util.dimension(left, right);
			
			if (AggregateReducer.Util.sameSize(left, d)) {
				sources = new Aggregates[]{right};
				target = left;
			} else if (AggregateReducer.Util.sameSize(right, d)) {
				sources = new Aggregates[]{left};
				target = right;
			} else {
				sources = new Aggregates[]{left, right};
				target = new Aggregates<T>(d.width, d.height, left.defaultValue());
			}
			
			for (Aggregates<T> source: sources) {
				for (int x=0; x<source.width(); x++) {
					for (int y=0; y<source.height(); y++) {
						target.set(x,y, red.combine(target.at(x,y), source.at(x,y)));
					}
				}
			}
			return target;
    	}
    	
    	/**How big is the union space of these two aggregate sets?
    	 * (Largest width x largest height)
    	 ***/
    	public static Dimension dimension(Aggregates<?> a, Aggregates<?> b) {
    		int width = Math.max(a.width(), b.width());
    		int height = Math.max(a.height(), b.height());
    		return new Dimension(width, height);
    	}
    	 
    	public static boolean sameSize(Aggregates<?> a, Dimension d) {
    		return a.width() == d.width && a.height() == d.height;
    	}
    }
}
