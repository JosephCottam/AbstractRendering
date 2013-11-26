package ar.rules;

import java.awt.Point;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import ar.Aggregates;
import ar.Aggregator;
import ar.Transfer;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.Util;

/**Tools that don't apply to a particular data type.**/
public class General {

	/**Wrap a valuer in a transfer function.**/
	public static final class ValuerTransfer<IN,OUT> implements Transfer.Specialized<IN, OUT> {
		private final Valuer<IN,OUT> valuer;
		private final OUT empty;
		public ValuerTransfer(Valuer<IN,OUT> valuer, OUT empty) {
			this.valuer = valuer;
			this.empty = empty;
		}

		public OUT emptyValue() {return empty;}
		public ar.Transfer.Specialized<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {return this;}
		public OUT at(int x, int y, Aggregates<? extends IN> aggregates) {
			return valuer.value(aggregates.get(x,y));
		}
	}
	
	public static final class Last<IN> implements Aggregator<IN,IN> {
		private final IN id;
		public <V extends IN> Last(V id) {this.id = id;}
		public IN combine(IN current, IN update) {return update;}
		public IN rollup(IN left, IN right) {return right;}
		public IN identity() {return id;}
	}
	
	/**Performs a type-preserving replacement.  
	 * Specified values are replaced, others as passed through.
	 * For more control or type-converting replace, use MapWrapper instead.
	 * **/
	public static class Replace<T> implements Transfer.Specialized<T,T> {
		private final Map<T,T> mapping;
		private final T empty;
		
		public Replace(T in, T out, T empty) {this(MapWrapper.map(in,out), empty);}
		public Replace(Map<T,T> mapping, T empty) {
			this.mapping = mapping;
			this.empty = empty;
		}
		
		public T emptyValue() {return empty;}
		public Specialized<T,T> specialize(Aggregates<? extends T> aggregates) {return this;}
		public T at(int x, int y, Aggregates<? extends T> aggregates) {
			T val = aggregates.get(x,y);
			if (mapping.containsKey(val)) {return mapping.get(val);}
			return val;
		}
	}
	
	
	
	/**Changes a cell to empty if it and all of its neighbors are the same value.**/
	public static class Simplify<V> implements Transfer.Specialized<V, V> {
		private final V empty;
		public Simplify(V empty) {this.empty = empty;}
		public V emptyValue() {return empty;}
		public Specialized<V, V> specialize(Aggregates<? extends V> aggregates) {return this;}
		public V at(int x, int y, Aggregates<? extends V> aggregates) {
			V val = aggregates.get(x,y);
			if (Util.isEqual(val, aggregates.get(x-1,y-1))
					&& Util.isEqual(val, aggregates.get(x,y-1))
					&& Util.isEqual(val, aggregates.get(x+1,y-1))
					&& Util.isEqual(val, aggregates.get(x-1,y))
					&& Util.isEqual(val, aggregates.get(x+1,y))
					&& Util.isEqual(val, aggregates.get(x-1,y+1))
					&& Util.isEqual(val, aggregates.get(x,y+1))
					&& Util.isEqual(val, aggregates.get(x+1,y+1))) {
				return empty;
			}
			
			return val;
		}
		
	}
	
	/**Fill in empty values based on a function of nearby values.
	 *
	 * TODO: Add support for a smearing function....Takes a list of "nearby" and distances 
	 * TODO: Add support for a searching function...
	 ***/ 
	public static class Smear<V> implements Transfer.Specialized<V,V> {
		final V empty;
		public Smear(V empty) {
			this.empty = empty;
		}

		public V emptyValue() {return empty;}
		public Specialized<V, V> specialize(Aggregates<? extends V> aggregates) {return this;}
		
		@Override
		public V at(int x, int y, Aggregates<? extends V> aggregates) {
			Point p = new Point(x,y);
			for (int i=0; outOfBounds(p, aggregates); i++) {
				spiralFrom(x,y,i,p);
				V val = aggregates.get(p.x, p.y);
				if (!Util.isEqual(val, empty)) {return val;}
			}
			throw new RuntimeException("Reached illegal state...");
		}

		public Point spiralFrom(int X, int Y, int n, Point into) {
			int x=0,y=0;
			int dx = 0;
			int dy = -1;
			for (int i=0; i<n; i++) {
				if ((-X/2 < x) && (x <= X/2) && (-Y/2 < y) && (y <= Y/2)) {
					into.setLocation(x,y);
					return into;
				}

				if ((x == y) || (x < 0 && x == -y) || (x > 0 && x == 1-y)) {
					int temp = dx;
					dx = -dy;
					dy = temp;
				}
				x = x+dx;
				y = y+dy;
			}
			throw new RuntimeException("Reached illegal state...");
		}
		
		public boolean outOfBounds(Point p, Aggregates<?> aggregates) {
			return p.x >= aggregates.lowX() && p.x < aggregates.highX()
					&& p.y >= aggregates.lowY() && p.y < aggregates.highY();
		}
	}
	
	/**Spread a value out in a general geometric shape.**/
	public static class Spread<V> implements Transfer<V,V> {
		final Spreader<V> spreader;
		final Aggregator<V,V> combiner;
		
		public Spread(Spreader<V> spreader, Aggregator<V,V> combiner) {
			this.spreader = spreader;
			this.combiner = combiner;
		}

		public V emptyValue() {return combiner.identity();}
		
		/**Calculations are done at specialization time, so transfer is fast but specialization is slow.**/
		public ar.Transfer.Specialized<V, V> specialize(Aggregates<? extends V> aggregates) {
			return new Specialized<>(spreader, aggregates, combiner);
		}
		
		public static class Specialized<V> extends Spread<V> implements Transfer.Specialized<V, V>  {
			private Object cacheGuard = new Object(){};
			private Aggregates<?> cacheKey;
			private Aggregates<V> cachedAggs;

			public Specialized(Spreader<V> spreader, Aggregates<? extends V> base, Aggregator<V,V> combiner) {
				super(spreader, combiner);
				synchronized(cacheGuard) {
					cacheKey =base;
					cachedAggs = spread(base);
				}
			}

			private Aggregates<V> spread(Aggregates<? extends V> aggregates) {
				Aggregates<V> target = ar.aggregates.AggregateUtils.make(aggregates, emptyValue());
			
				for (int x=aggregates.lowX(); x<aggregates.highX(); x++) {
					for (int y=aggregates.lowY(); y<aggregates.highY(); y++) {
						V baseVal = aggregates.get(x,y);
						if (Util.isEqual(combiner.identity(), baseVal)) {continue;}
						super.spreader.spread(target , x,y, baseVal, combiner);
					}
				}
				return target;
			}
			
			public V at(int x, int y, Aggregates<? extends V> aggregates) {
				synchronized(cacheGuard) {
					if (cacheKey != aggregates || aggregates == null) {
						cachedAggs = spread(aggregates);
						cacheKey = aggregates;
					}
				}

				return cachedAggs.get(x, y);}			
		}
		
		/**Spreader takes a type argument in case the spreading depends on the value.
		 * This capability can be used to implement (for example) a map with circles centered-on and proportional to a value. 
		 */
		public static interface Spreader<V> {
			public void spread(Aggregates<V> target, int x, int y, V base, Aggregator<V,V> op);
		}
		
		/**Spread in a square pattern of a fixed size.  The location is in the center.
		 * Size is the number of units up/down/left/right of center to go, so total
		 * length will be 2*size+1.
		 */
		public static class UnitSquare<V> implements Spreader<V> {
			private final int size;
			public UnitSquare(int size) {this.size=Math.abs(size);}
			
			public void spread(Aggregates<V> target, final int x, final int y, V base, Aggregator<V,V> op) {
				for (int xx=-size; xx<=size; xx++) {
					for (int yy=-size; yy<=size; yy++) {
						int xv = x+xx;
						int yv = y+yy;
						V update = target.get(xv, yv);
						target.set(xv, yv, op.rollup(base, update));
					}
				}
			}
			
		}
		
		public static class UnitCircle<V> implements Spreader<V> {
			private final int radius;
			public UnitCircle(int radius) {this.radius=Math.abs(radius);}
			
			public void spread(Aggregates<V> target, final int x, final int y, V base, Aggregator<V,V> op) {
				Ellipse2D e = new Ellipse2D.Double(x-radius,y-radius,2*radius,2*radius);
				Point2D p = new Point2D.Double();
				for (int xx=-radius; xx<=radius; xx++) {
					for (int yy=-radius; yy<=radius; yy++) {
						int xv = x+xx;
						int yv = y+yy;
						p.setLocation(xv, yv);
						if (!e.contains(p)) {continue;}
						V update = target.get(xv, yv);
						target.set(xv, yv, op.rollup(base, update));
					}
				}
			}
		}
		
		public static class ValueCircle<N extends Number> implements Spreader<N> {
			public void spread(Aggregates<N> target, final int x, final int y, N base, Aggregator<N,N> op) {
				int radius = (int) base.doubleValue();
				Ellipse2D e = new Ellipse2D.Double(x-radius,y-radius,2*radius,2*radius);
				Point2D p = new Point2D.Double();
				for (int xx=-radius; xx<=radius; xx++) {
					for (int yy=-radius; yy<=radius; yy++) {
						int xv = x+xx;
						int yv = y+yy;
						p.setLocation(xv, yv);
						if (!e.contains(p)) {continue;}
						N update = target.get(xv, yv);
						target.set(xv, yv, op.rollup(base, update));
					}
				}
			}
		}
	}
	
	/**Aggregator and Transfer that always returns the same value.**/
	public static final class Const<A,OUT> implements Aggregator<A,OUT>, Transfer.Specialized<A, OUT> {
		private static final long serialVersionUID = 2274344808417248367L;
		private final OUT val;
		
		/**Two-argument version to help type inference.
		 * 
		 * @param val Value to always return
		 * @param ref Input value that matches type of expected input.  THIS VALUE IS IGNORED.
		 */
		public Const(OUT val, A ref) {this.val = val;}
		/**@param val Value to return**/
		public Const(OUT val) {this.val = val;}
		public OUT combine(OUT left, A update) {return val;}
		public OUT rollup(OUT left, OUT right) {return val;}
		public OUT identity() {return val;}
		public OUT emptyValue() {return val;}
		public ar.Transfer.Specialized<A, OUT> specialize(Aggregates<? extends A> aggregates) {return this;}
		public OUT at(int x, int y, Aggregates<? extends A> aggregates) {return val;}
	}


	/**Return what is found at the given location.**/
	public static final class Echo<T> implements Transfer.Specialized<T,T>, Aggregator<T,T> {
		private static final long serialVersionUID = -7963684190506107639L;
		private final T empty;
		/** @param empty Value used for empty; "at" always echos what's in the aggregates, 
		 *               but some methods need an empty value independent of the aggregates set.**/
		public Echo(T empty) {this.empty = empty;}
		public T at(int x, int y, Aggregates<? extends T> aggregates) {return aggregates.get(x, y);}

		public T emptyValue() {return empty;}
		
		public T combine(T left, T update) {return update;}
		public T rollup(T left, T right) {
			if (left != null) {return left;}
			if (right != null) {return right;}
			return emptyValue();
		}
		public T identity() {return emptyValue();}
		public Echo<T> specialize(Aggregates<? extends T> aggregates) {return this;}
	}

	/**Return the given value when presented with a non-empty value.**/
	public static final class Present<IN, OUT> implements Transfer.Specialized<IN,OUT> {
		private static final long serialVersionUID = -7511305102790657835L;
		private final OUT present, absent;
		
		/**
		 * @param present Value to return on not-null
		 * @param absent Value to return on null
		 */
		public Present(OUT present, OUT absent) {
			this.present = present; 
			this.absent=absent;
		}
		
		public OUT at(int x, int y, Aggregates<? extends IN> aggregates) {
			Object v = aggregates.get(x, y);
			if (v != null && !v.equals(aggregates.defaultValue())) {return present;}
			return absent;
		}
		
		public Present<IN, OUT> specialize(Aggregates<? extends IN> aggregates) {return this;}
		
		public OUT emptyValue() {return absent;}
	}
	
	/**Transfer function that wraps a java.util.map.
	 * The empty value is returned if the input value is not found in the mapping.
	 * **/
	public static class MapWrapper<IN,OUT> implements Transfer.Specialized<IN,OUT> {
		private static final long serialVersionUID = -4326656735271228944L;
		private final Map<IN, OUT> mappings;
		private final OUT other; 

		
		public MapWrapper(IN in, OUT out, OUT empty) {this(map(in,out), empty);}
		private static <IN,OUT> Map<IN,OUT> map(IN in, OUT out) {
			Map<IN,OUT> m = new HashMap<>();
			m.put(in, out);
			return m;
		}
		
		/**
		 * @param mappings Backing map
		 * @param other Value to return if the backing map does not include a requested key
		 * @param nullIsValue Should 'null' be considered a valid return value from the map, or should it be converted to 'other' instead
		 */
		public MapWrapper(Map<IN, OUT> mappings, OUT other) {
			this.mappings=mappings;
			this.other = other;
		}

		@Override
		public OUT at(int x, int y, Aggregates<? extends IN> aggregates) {
			IN key = aggregates.get(x, y);
			if (!mappings.containsKey(key)) {return other;}
			return mappings.get(key);
		}

		public OUT emptyValue() {return other;}
		public MapWrapper<IN,OUT> specialize(Aggregates<? extends IN> aggregates) {return this;}

		/**From a reader, make a map wrapper.  
		 * 
		 * This is stream-based, line-oriented conversion.
		 * Lines are read and processed one at time.
		 **/
		@SuppressWarnings("resource")
		public static <K,V> MapWrapper<K,V> fromReader(
				Reader in, Valuer<String,K> keyer, Valuer<String,V> valuer,
				V other, boolean nullIsValue) throws Exception {
			BufferedReader bf;

			if (in instanceof BufferedReader) {
				bf = (BufferedReader) in;
			} else {
				bf = new BufferedReader(in);
			}
			
			Map<K,V> dict = new HashMap<K,V>();
			String line = bf.readLine();
			while(line != null) {
				dict.put(keyer.value(line), valuer.value(line));
			}

			return new MapWrapper<K,V>(dict,other);
		}
	}
}
