package ar.ext.spark;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyph;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.Transfer;
import ar.Transfer.ItemWise;
import ar.Transfer.Specialized;
import ar.aggregates.AggregateUtils;
import ar.renderers.AggregationStrategies;
import ar.renderers.ProgressReporter;
import ar.renderers.SerialRenderer;
import ar.util.Util;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;

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
	public <G,A> Aggregates<A> aggregate(
			JavaRDD<Glyph<G,A>> glyphs,
			Aggregator<A, A> op, 
			AffineTransform view, 
			int width,
			int height) {
		JavaRDD<Aggregates<A>> aggset = glyphs.map(new GlyphToAggregates<G,A>(view));
		return aggset.reduce(new Rollup<A>(op));
	}
	
//
//	@Override
//	public <I, G, A> Aggregates<A> aggregate(
//			Glyphset<? extends G, ? extends I> glyphs, 
//			Selector<G> selector,
//			Aggregator<I, A> aggregator, 
//			AffineTransform viewTransform,
//			int width, int height) {
//		
//		Use Map: SELECTOR becomes the splatting function (passed to GlyphToAggregates)
//		Use aggregate: OP needs to get wrapped so it works on whole aggregate sets (idSet<A> --> set<I> --> set<A> --> set<A>), 
//		    not just on pixels (I -> A -> A)
//		    This includes creating a ConstantAggregates that is an identity
//		    Similar to Rollup wrapper, but better....
//		
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//	@Override
//	public <IN, OUT> Aggregates<OUT> transfer(
//			Aggregates<? extends IN> aggregates, Specialized<IN, OUT> t) {
//		if (t instanceof ItemWise) {return transfer(aggregates, (ItemWise<IN,OUT>) t);}
//		return new SerialRenderer().transfer(aggregates, t);
//	}
//
//
//	@Override
//	public <IN, OUT> Aggregates<OUT> transfer(
//			Aggregates<? extends IN> aggregates, ItemWise<IN, OUT> t) {
//		//Use Map
//		return null;
//	}

//	@Override public long taskSize(Glyphset<?, ?> glyphs) {return 1;}
//	@Override public ProgressReporter progress() {return new ProgressReporter.NOP();}


	/**Utility method for calculating the bounds on an RDD glyphset.
	 ***/
	public static <G,I> Rectangle2D bounds(JavaRDD<Glyph<G,I>> glyphs) {
		JavaRDD<Rectangle2D> rects = glyphs.map(new Function<Glyph<G,I>,Rectangle2D>() {
			private static final long serialVersionUID = 7387911686369652132L;

			public Rectangle2D call(Glyph<G,I> glyph) throws Exception {
				return Util.boundOne(glyph.shape());
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
	public class GlyphToAggregates<G,A> extends Function<Glyph<G,A>, Aggregates<A>> {
		private static final long serialVersionUID = 7666400467739718445L;
		
		private final AffineTransform vt;
		public GlyphToAggregates(AffineTransform vt) {this.vt = vt;}
		
		public Aggregates<A> call(Glyph<G,A> glyph) throws Exception {
			//TODO: Generalize to shape/point system
			Shape s = vt.createTransformedShape(Util.boundOne(glyph.shape()));
			Rectangle bounds = s.getBounds();
			A v = glyph.info();
			Aggregates<A> aggs = AggregateUtils.make(bounds.x, bounds.y, bounds.x+bounds.width, bounds.y+bounds.height, v);
			return aggs;
		}
	}

}
