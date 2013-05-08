package ar;

import java.awt.Rectangle;

/**Combine two aggregate sets into a third composite aggregate.**/
public interface AggregateReducer<IN1,IN2,OUT> {
	public OUT combine(IN1 left, IN2 right);
	
    public static final class Util {
    	
    	@SuppressWarnings("unchecked")
		public static <T> Aggregates<T> 
    	reduce(Aggregates<T> left, Aggregates<T> right, AggregateReducer<T,T,T> red) {
    		
    		Aggregates<T> [] sources;
			Aggregates<T> target;
			Rectangle bounds = right.bounds().union(left.bounds());

			if (left.bounds().contains(bounds)) {
				sources = new Aggregates[]{right};
				target = left;
			} else if (right.bounds().contains(bounds)) {
				sources = new Aggregates[]{left};
				target = right;
			} else {
				sources = new Aggregates[]{left, right};
				target = new Aggregates<T>(bounds, left.defaultValue());
			}
			
			for (Aggregates<T> source: sources) {
				for (int x=source.offsetX(); x<source.width(); x++) {
					for (int y=source.offsetY(); y<source.height(); y++) {
						target.set(x,y, red.combine(target.at(x,y), source.at(x,y)));
					}
				}
			}
			return target;
    	}
    }
}
