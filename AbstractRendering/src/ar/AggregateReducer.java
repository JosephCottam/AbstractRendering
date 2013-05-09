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
			Rectangle rb = new Rectangle(right.lowX(), right.lowY(), right.highX()-right.lowX(), right.highY()-right.lowY());
			Rectangle lb = new Rectangle(left.lowX(), left.lowY(), left.highX()-left.lowX(), left.highY()-left.lowY());
			Rectangle bounds = rb.union(lb);

			if (lb.contains(bounds)) {
				sources = new Aggregates[]{right};
				target = left;
			} else if (rb.contains(bounds)) {
				sources = new Aggregates[]{left};
				target = right;
			} else {
				sources = new Aggregates[]{left, right};
				target = new Aggregates<T>(bounds.x, bounds.y, bounds.x+bounds.width, bounds.y+bounds.height, left.defaultValue());
			}
			
			for (Aggregates<T> source: sources) {
				for (int x=source.lowX(); x<source.highX(); x++) {
					for (int y=source.lowY(); y<source.highY(); y++) {
						target.set(x,y, red.combine(target.at(x,y), source.at(x,y)));
					}
				}
			}
			return target;
    	}
    }
}
