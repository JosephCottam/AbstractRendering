package ar.ext.spark;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyph;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.Transfer.ItemWise;
import ar.Transfer.Specialized;
import ar.aggregates.AggregateUtils;
import ar.glyphsets.GlyphSingleton;
import ar.renderers.AggregationStrategies;
import ar.renderers.ProgressReporter;
import ar.renderers.SerialRenderer;
import ar.util.Util;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;

import com.google.common.collect.Lists;

/**Near drop-in for the standard render using spark.
 * Also provides utility methods for working with RDDs.
 * 
 * Due to a few type restrictions, the implementation doesn't (yet) match the main renderer definition.
 * TODO: Is there value to having an RDDAggregates?  Might help with VERY LARGE aggregate sets (like the high-detail tilesets), esp. with item-wise transfer
 */
public class RDDRender implements Serializable, Renderer {
	private static final long serialVersionUID = 4036940240319014563L;
	public static boolean MAP_PARTITIONS = false;

	@Override
	public <I, G, A> Aggregates<A> aggregate(
			Glyphset<? extends G, ? extends I> glyphs, 
			Selector<G> selector,
			Aggregator<I, A> aggregator, 
			AffineTransform viewTransform,
			int width, int height) {
		
		//TODO: Can we do auto-conversion to RDD here?  
		if (!(glyphs instanceof GlyphsetRDD)) {throw new IllegalArgumentException("Can only use RDD Glyphsets");}
		
		@SuppressWarnings("unchecked") //Will only read from...so this is OK (I think...).  No heap polution as long as we don't try to change the rdd.
		JavaRDD<Glyph<G, I>> rdd = ((GlyphsetRDD<G, I>) glyphs).base();
		
		JavaRDD<Aggregates<A>> eachAggs;
		if (!MAP_PARTITIONS) {
			eachAggs = rdd.map(new GlyphToAggregates<I,G,A>(selector, aggregator, viewTransform));
		} else {
			eachAggs = rdd.mapPartitions(new GlyphsToAggregates<I,G,A>(selector, aggregator, viewTransform));
		}
		
		
		return eachAggs.reduce(new Rollup<A>(aggregator));
	}
	
	@Override
	public <IN, OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Specialized<IN, OUT> t) {
		if (t instanceof ItemWise) {return transfer(aggregates, (ItemWise<IN,OUT>) t);}
		return new SerialRenderer().transfer(aggregates, t);
	}


	@Override
	public <IN, OUT> Aggregates<OUT> transfer(
			Aggregates<? extends IN> aggregates, ItemWise<IN, OUT> t) {
		return new SerialRenderer().transfer(aggregates, t);
	}

	@Override public long taskSize(Glyphset<?, ?> glyphs) {return 1;}
	@Override public ProgressReporter progress() {return new ProgressReporter.NOP();}

	
	
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
	
	/**Render a single glyph into a set of aggregates.**/
	public class GlyphToAggregates<I,G,A> extends Function<Glyph<G, I>, Aggregates<A>> {
		private static final long serialVersionUID = 7666400467739718445L;
		
		private final AffineTransform vt;
		private final Selector<G> selector;
		private final Aggregator<I,A> op;
		public GlyphToAggregates(
				Selector<G> selector,
				Aggregator<I,A> op,
				AffineTransform vt) {
			
			this.selector = selector;
			this.op = op;
			this.vt = vt;
		}
		
		public Aggregates<A> call(Glyph<G, I> glyph) throws Exception {
			Shape s = vt.createTransformedShape(Util.boundOne(glyph.shape()));
			Rectangle bounds = s.getBounds();
			Aggregates<A> aggs = AggregateUtils.make(bounds.x, bounds.y, bounds.x+bounds.width, bounds.y+bounds.height, op.identity());
			return selector.processSubset(new GlyphSingleton<>(glyph), vt, aggs, op);
		}
	}

	
	public class BoundPartitions<G> extends FlatMapFunction<Iterator<Glyph<G,?>>, Rectangle> {

		private final AffineTransform vt;

		public BoundPartitions(AffineTransform vt) {this.vt = vt;}

		@Override
		public Iterable<Rectangle> call(Iterator<Glyph<G, ?>> glyphs) throws Exception {
			Shape s = vt.createTransformedShape(Util.bounds(glyphs));
			Rectangle bounds = s.getBounds();
			return Arrays.asList(bounds);
		}
		
	}
	
	/**Render a single glyph into a set of aggregates.**/
	public class GlyphsToAggregates<I,G,A> extends FlatMapFunction<Iterator<Glyph<G, I>>, Aggregates<A>> {
		private static final long serialVersionUID = 7666400467739718445L;
		
		private final AffineTransform vt;
		private final Selector<G> selector;
		private final Aggregator<I,A> op;
		public GlyphsToAggregates(
				Selector<G> selector,
				Aggregator<I,A> op,
				AffineTransform vt) {
			
			this.selector = selector;
			this.op = op;
			this.vt = vt;
		}
		
		public Iterable<Aggregates<A>> call(Iterator<Glyph<G, I>> glyphs) throws Exception {
			ArrayList<Glyph<G,I>> glyphList = Lists.newArrayList(new IterableIterator<>(glyphs));
			Shape s = vt.createTransformedShape(Util.bounds(glyphList));
			Rectangle bounds = s.getBounds();
			Aggregates<A> aggs = AggregateUtils.make(bounds.x, bounds.y, bounds.x+bounds.width, bounds.y+bounds.height, op.identity());
			return Arrays.asList(selector.processSubset(glyphList, vt, aggs, op));
		}
	}
	
}
