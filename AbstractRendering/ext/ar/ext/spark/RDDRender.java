package ar.ext.spark;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyph;
import ar.Transfer;
import ar.aggregates.FlatAggregates;
import ar.renderers.AggregationStrategies;
import ar.renderers.SerialSpatial;
import ar.util.Util;
import spark.api.java.JavaRDD;
import spark.api.java.function.Function;
import spark.api.java.function.Function2;

/**Near drop-in for the standard render using spark.
 * Also provides utility methods for working with RDDs.
 * 
 * Due to a few type restrictions, the implementation doesn't (yet) match the main renderer definition.
 * TODO: provide a a glyphset type that wraps an RDD
 */
public class RDDRender implements Serializable {
	private static final long serialVersionUID = 4036940240319014563L;

	/**Near drop-in for the standard render.aggregate.
	 * Due to the requirements of spark's reduce, the info-type must match
	 * the aggregate type.
	 * 
	 *  TODO: Add proper info support so the function signature can be from glyphs of arbitrary type to aggregates of arbitrary type
	 * 
	 * @param glyphs
	 * @param op
	 * @param inverseView
	 * @param width
	 * @param height
	 * @return Aggregate set that results from collecting all items
	 */
	public <A> Aggregates<A> aggregate(JavaRDD<Glyph<A>> glyphs,
			Aggregator<A, A> op, 
			AffineTransform inverseView, 
			int width,
			int height) {
		JavaRDD<Aggregates<A>> aggset = glyphs.map(new GlyphToAggregates<A>(inverseView));
		return aggset.reduce(new Rollup<A>(op));
	}
	

	public <IN, OUT> Aggregates<OUT> transfer(
			Aggregates<? extends IN> aggregates, Transfer<IN, OUT> t) {
		return new SerialSpatial().transfer(aggregates, t);
	}

	public double progress() {return -1;}


	/**Utility method for calculating the bounds on an RDD glyphset.
	 ***/
	public static <T> Rectangle2D bounds(JavaRDD<Glyph<T>> glyphs) {
		JavaRDD<Rectangle2D> rects = glyphs.map(new Function<Glyph<T>,Rectangle2D>() {
			private static final long serialVersionUID = 7387911686369652132L;

			public Rectangle2D call(Glyph<T> glyph) throws Exception {
				return glyph.shape().getBounds2D();
			}});
		
		return rects.reduce(new Function2<Rectangle2D, Rectangle2D,Rectangle2D>() {
			private static final long serialVersionUID = -70816436714762254L;

			public Rectangle2D call(Rectangle2D left, Rectangle2D right)
					throws Exception {
				return Util.bounds(left, right);
			}
		});
	}
	
	
	/**Utility class to wrap an aggregator's rollup function.
	 * 
	 * This class enables rollup to be called in a distributed environment.
	 * **/
	public class Rollup<V> extends Function2<Aggregates<V>,Aggregates<V>,Aggregates<V>> {
		private static final long serialVersionUID = -1121892395315765974L;
		
		final Aggregator<?,V> aggregator;
		public Rollup(Aggregator<?,V> aggregator) {this.aggregator = aggregator;}

		public Aggregates<V> call(Aggregates<V> left, Aggregates<V> right)
				throws Exception {
			return AggregationStrategies.horizontalRollup(left, right, aggregator);
		}
	}
	
	/**Render a single glyph.
	 * 
	 * Takes a single glyph and creates a set of aggregates, basically
	 * splats the value into the bounding box.
	 * 
	 * TODO: Put value into just cells the bounding box touches
	 * TODO: Extend with info function so it will convert Glyph<V> to Aggregates<A>
	 * 
	 * **/
	public class GlyphToAggregates<V> extends Function<Glyph<V>, Aggregates<V>> {
		private static final long serialVersionUID = 7666400467739718445L;
		
		private final AffineTransform vt;
		public GlyphToAggregates(AffineTransform vt) {this.vt = vt;}
		
		public Aggregates<V> call(Glyph<V> glyph) throws Exception {
			Shape s = vt.createTransformedShape(glyph.shape());
			Rectangle bounds = s.getBounds();
			V v = glyph.value();
			Aggregates<V> aggs = new FlatAggregates<V>(bounds.x, bounds.y, bounds.x+bounds.width, bounds.y+bounds.height, v);
			return aggs;
		}
	}
	
}
