package ar.ext.spark;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyph;
import ar.aggregates.FlatAggregates;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.Util;
import spark.api.java.JavaRDD;
import spark.api.java.function.Function;
import spark.api.java.function.Function2;


//TODO: Implement the Renderer interface...probably will require some construction params to do it right
public class RDDRender {
	/* Glyphset:  (DONE) 
	 *    Node-local object.  
	 *    Can be a "parallelizedCollection" for now, 
	 *    Keep an idea on "Hadoop dataset" for actual usage with shark/hive tables
	 *    sc.parallelize(<collection>)
	 *    Transform raw dataset into glyphset via map (of a glypher and a valuer)
	 */    
	public static <V> JavaRDD<Glyph<V>> glyphs(JavaRDD<Indexed> baseData, 
												Shaper<Indexed> shaper, 
												Valuer<Indexed,V> valuer) {
		
		JavaRDD<Glyph<V>> glyphs = baseData.map(new Glypher(shaper,valuer));
		return glyphs;
	}
	
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

	
	/**Render a single glyph.
	 * 
	 * Takes a single glyph and creates a set of aggregates, basically
	 * splats the value into the bounding box.
	 * 
	 * TODO: Put value into just cells the bounding box touches
	 * 
	 * **/
	public static class RenderOne<V> extends Function<Glyph<V>, Aggregates<V>> {
		private static final long serialVersionUID = 7666400467739718445L;
		
		private final AffineTransform vt;
		public RenderOne(AffineTransform vt) {this.vt = vt;}
		
		public Aggregates<V> call(Glyph<V> glyph) throws Exception {
			Shape s = vt.createTransformedShape(glyph.shape());
			Rectangle bounds = s.getBounds();
			V v = glyph.value();
			Aggregates<V> aggs = new FlatAggregates<V>(bounds.x, bounds.y, bounds.x+bounds.width, bounds.y+bounds.height, v);
			return aggs;
		}
	}
	
	
	/**Render a collection of glyphs into aggregates
	 * 
	 * TODO: Consider changing to a flat map that creates an RDD of (x,y,Value)  
	 *       May lead to simpler phrasing but more communication
	 * ***/
	public static <V> JavaRDD<Aggregates<V>> renderAll(AffineTransform vt, JavaRDD<Glyph<V>> glyphs) {
		return glyphs.map(new RenderOne<V>(vt));
	}
	
	public static <V> Aggregates<V> collect(JavaRDD<Aggregates<V>> aggs, Aggregator<?,V> aggregator) {
		return aggs.reduce(new Rollup<V>(aggregator));
	}

		
}
