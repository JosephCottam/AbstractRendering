package ar.rules;

import java.awt.Point;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import ar.Aggregates;
import ar.Aggregator;
import ar.Renderer;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.Util;

/**Tools that don't apply to a particular data type.**/
public class General {

	/**What is the last item in the given pixel.
	 * 
	 * Unless the 'nullIsValue' flag is set, 
	 * a null right-hand value results in the left value being returned.
	 * A null right and left value results in the identity being returned;   
	 * **/
	public static final class Last<A> implements Aggregator<A,A> {
		private static final long serialVersionUID = -3640093539839073637L;
		final boolean nullIsValue;
		final A id;
		
		public Last(A id) {this(id, false);}
		public Last(A id, boolean nullIsValue) {
			this.id = id;
			this.nullIsValue = nullIsValue;
		}
		
		@Override 
		public A rollup(A left, A right) {
			if (nullIsValue) {return right;}
			if (right != null) {return right;}
			if (left != null) {return left;}
			return identity();
		}
		
		@Override  
		public A combine(A left, A update) {return update;}
		
		@Override public A identity() {return id;}
		@Override public boolean equals(Object other) {return other instanceof Last;}
		@Override public int hashCode() {return Last.class.hashCode();}
	}
	
	/**What is the first item in the given aggregate cell (an over-plotting strategy)**/
	public static final class First<A> implements Aggregator<A, A> {
		private static final long serialVersionUID = 5899328174090941310L;
		private final A identity;
		
		public First(A identity) {this.identity = identity;}
		public A combine(A left, A update) {
			if (left == identity) {return update;}
			else {return left;}
		}

		public A rollup(A left, A right) {
			if (left != null) {return left;}
			if (right != null) {return right;}
			return identity();
		}
		
		public A identity() {return identity;}
		public boolean equals(Object other) {return other instanceof First;}
		public int hashCode() {return First.class.hashCode();}
	}

	/**Wraps a BiFunction as an aggregator.**/
	public static final class Apply<A> implements Aggregator<A,A> {
		private final A identity;
		private final BiFunction<A,A,A> func;
		
		public Apply(A identity, BiFunction<A,A,A> func) {
			this.identity = identity;
			this.func = func;
		}

		@Override public A combine(A current, A update) {return func.apply(current, update);}
		@Override public A rollup(A left, A right) {return func.apply(left, right);}
		@Override public A identity() {return identity;}		
	}
	

	/**Wrap a valuer in a transfer function.**/
	public static final class ValuerTransfer<IN,OUT> implements Transfer.ItemWise<IN, OUT> {
		private final Valuer<IN,OUT> valuer;
		private final OUT empty;
		public ValuerTransfer(Valuer<IN,OUT> valuer, OUT empty) {
			this.valuer = valuer;
			this.empty = empty;
		}

		@Override 
		public OUT at(int x, int y, Aggregates<? extends IN> aggregates) {
			return valuer.apply(aggregates.get(x,y));
		}

		@Override public OUT emptyValue() {return empty;}		
	}
	
	/**Performs a type-preserving replacement.  
	 * Specified values are replaced, others as passed through.
	 * For more control or type-converting replace, use MapWrapper instead.
	 * **/
	public static class Replace<T> implements Transfer.ItemWise<T,T> {
		private final Map<T,T> mapping;
		private final T empty;
		
		public Replace(T in, T out, T empty) {this(MapWrapper.map(in,out), empty);}
		public Replace(Map<T,T> mapping, T empty) {
			this.mapping = mapping;
			this.empty = empty;
		}
		
		@Override public T emptyValue() {return empty;}
		
		@Override 
		public T at(int x, int y, Aggregates<? extends T> aggregates) {
			T val = aggregates.get(x,y);
			if (mapping.containsKey(val)) {return mapping.get(val);}
			return val;
		}		
	}
	
	
	
	/**Changes a cell to empty if it and all of its neighbors are the same value.**/
	public static class Simplify<V> implements Transfer.ItemWise<V, V> {
		private final V empty;
		public Simplify(V empty) {this.empty = empty;}
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
		
		@Override public V emptyValue() {return empty;}
	}
	
	/**Fill in empty values based on a function of nearby values.
	 * Does it by searching from each empty point outwards until it finds a non-empty value.
     * 
	 * TODO: Add support for a smearing function so search pattern can be controlled...(vertical, horizontal, spiral, limit range...)
	 * TODO: Add support for a predicate on whether to smear or not to given location (may be part of the pattern function...) 
	 ***/ 
	public static class Smear<V> implements Transfer.ItemWise<V,V> {
		final V empty;
		
		public Smear(V empty) {this.empty = empty;}

		@Override public V emptyValue() {return empty;}
		
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
	public static class Spread<V> implements Transfer.Specialized<V,V> {
		final Spreader<V> spreader;
		final Aggregator<?,V> combiner;
		
		public Spread(Spreader<V> spreader, Aggregator<?,V> combiner) {
			this.spreader = spreader;
			this.combiner = combiner;
		}

		@Override public V emptyValue() {return combiner.identity();}
		
		@Override 
		//TODO: Parallelize...
		public Aggregates<V> process(Aggregates<? extends V> aggregates, Renderer rend) {
			Aggregates<V> target = spreader.extend(aggregates, emptyValue());
			
			for (int x=aggregates.lowX(); x<aggregates.highX(); x++) {
				for (int y=aggregates.lowY(); y<aggregates.highY(); y++) {
					V baseVal = aggregates.get(x,y);
					if (Util.isEqual(combiner.identity(), baseVal)) {continue;}
					spreader.spread(target , x,y, baseVal, combiner);
				}
			}
			return target;
		}
			
		
		
		/**Spreader takes a type argument in case the spreading depends on the value.
		 * This capability can be used to implement (for example) a map with circles centered-on and proportional to a value. 
		 */
		public static interface Spreader<V> {
			public void spread(Aggregates<V> target, int x, int y, V base, Aggregator<?,V> op);
			public Aggregates<V> extend(Aggregates<? extends V> source, V empty);
		}
		
		
		/**Spread in a rectangular pattern from the focus cell.  
		 * 
		 * Will go from (x-left, y-down) to (x+right, y+up).
		 */
		public static class UnitRectangle<V> implements Spreader<V> {
			final int up,down,left,right;

			public UnitRectangle(int size) {this(size,size,size,size);}
			public UnitRectangle(int up, int down, int left, int right) {
				this.up =up; this.down=down; this.left=left; this.right=right;
			}

			@Override
			public void spread(Aggregates<V> target, int x, int y, V base, Aggregator<?, V> op) {
				for (int xx=-left; xx<=right; xx++) {
					for (int yy=-up; yy<=down; yy++) {

						int xv = x+xx;
						int yv = y+yy;
						V update = target.get(xv, yv);
						target.set(xv, yv, op.rollup(base, update));
					}
				}
			}
			@Override
			public Aggregates<V> extend(Aggregates<? extends V> source, V empty) {
				return ar.aggregates.AggregateUtils.make(
								source.lowX()-left,
								source.lowY()-up,
								source.highX()+right,
								source.highY()+down,
								empty);
			}
		}
		
		/**Spread in a circular pattern with original value at the center**/
		public static class UnitCircle<V> implements Spreader<V> {
			private final int radius;
			public UnitCircle(int radius) {this.radius=Math.abs(radius);}
			
			public void spread(Aggregates<V> target, final int x, final int y, V base, Aggregator<?,V> op) {
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
			
			public Aggregates<V> extend(Aggregates<? extends V> source, V empty) {
				return ar.aggregates.AggregateUtils.make(
								source.lowX()-radius,
								source.lowY()-radius,
								source.highX()+radius,
								source.highY()+radius,
								empty);
			}
		}
		
		/**Spread in a circular pattern with original value at center, 
		 * circle area is determined derived from the value found in the cell.
		 * (Radius is sqrt of the passed cell value).
		 */
		public static class ValueCircle<N extends Number> implements Spreader<N> {
			public void spread(Aggregates<N> target, final int x, final int y, N base, Aggregator<?,N> op) {
				int radius = (int) Math.ceil(Math.sqrt(base.doubleValue()));
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
			
			public Aggregates<N> extend(Aggregates<? extends N> source, N empty) {
				Util.Stats<N> stats =  Util.stats(source);
				int maxRadius = (int) Math.ceil(Math.sqrt(stats.max.doubleValue()));
				return ar.aggregates.AggregateUtils.make(
								source.lowX()-maxRadius,
								source.lowY()-maxRadius,
								source.highX()+maxRadius,
								source.highY()+maxRadius,
								empty);
			}
		}
	}
	
	/**Aggregator/Transfer that always returns the same value.
	 **/
	public static final class Const<A,OUT> implements Aggregator<A,OUT>, Transfer.ItemWise<A, OUT> {
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
		
		@Override public OUT combine(OUT left, A update) {return val;}
		@Override public OUT rollup(OUT left, OUT right) {return val;}
		@Override public OUT identity() {return val;}
		@Override public OUT emptyValue() {return val;}
		@Override public OUT at(int x, int y, Aggregates<? extends A> aggregates) {return val;}
	}


	/**Return what is found at the given location.**/
	public static final class Echo<T> implements Transfer.ItemWise<T,T> {
		private static final long serialVersionUID = -7963684190506107639L;
		private final T empty;
		
		/** @param empty Value used for empty; "at" always echos what's in the aggregates, 
		 *               but some methods need an empty value independent of the aggregates set.**/
		public Echo(T empty) {this.empty = empty;}
		
		@Override public T at(int x, int y, Aggregates<? extends T> aggregates) {return aggregates.get(x, y);}
		@Override public T emptyValue() {return empty;}
		
		@Override
		public Aggregates<T> process(Aggregates<? extends T> aggregates, Renderer rend) {
			return AggregateUtils.copy(aggregates, emptyValue());
		}
	}

	/**Return the given value when presented with a non-empty value.**/
	public static final class Present<IN, OUT> implements Transfer.ItemWise<IN,OUT> {
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
		
		@Override public OUT emptyValue() {return absent;}

		@Override public OUT at(int x, int y, Aggregates<? extends IN> aggregates) {
			Object v = aggregates.get(x, y);
			if (v != null && !v.equals(aggregates.defaultValue())) {return present;}
			return absent;
		}
		
	}
	
	/**Transfer function that wraps a java.util.map.
	 * The empty value is returned if the input value is not found in the mapping.
	 * **/
	public static class MapWrapper<IN,OUT> implements Transfer.ItemWise<IN,OUT> {
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

		@Override public OUT emptyValue() {return other;}
		
		/**From a reader, make a map wrapper.  
		 * 
		 * This is stream-based, line-oriented conversion.
		 * Lines are read and processed one at time.
		 **/
		@SuppressWarnings("resource")
		public static <K,V> MapWrapper<K,V> fromReader(
				Reader in, Valuer<String,K> keyer, Valuer<String,V> valuer,
				V other) throws Exception {
			BufferedReader bf;

			if (in instanceof BufferedReader) {
				bf = (BufferedReader) in;
			} else {
				bf = new BufferedReader(in);
			}
			
			Map<K,V> dict = new HashMap<K,V>();
			String line = bf.readLine();
			while(line != null) {
				dict.put(keyer.apply(line), valuer.apply(line));
				line = bf.readLine();
			}

			return new MapWrapper<K,V>(dict,other);
		}
	}
}
