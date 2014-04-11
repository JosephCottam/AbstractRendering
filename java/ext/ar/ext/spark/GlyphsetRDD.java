package ar.ext.spark;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Iterator;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;

import ar.Glyph;
import ar.Glyphset;
import ar.util.Util;

public class GlyphsetRDD<G,I> implements Glyphset<G,I>, Serializable {

	private final JavaRDD<Glyph<G,I>> base;
	
	public GlyphsetRDD(JavaRDD<Glyph<G,I>> base) {this.base = base;}

	/**Return the backing RDD.**/
	public JavaRDD<Glyph<G,I>> base() {return base;}
	
	@Override public Iterator<Glyph<G,I>> iterator() {return base.collect().iterator();}

	@Override public Rectangle2D bounds() {
		JavaRDD<Rectangle2D> rects = base.map(new Function<Glyph<G,I>,Rectangle2D>() {
			public Rectangle2D call(Glyph<G,I> glyph) throws Exception {
				return Util.boundOne(glyph.shape());
			}});
		
		return rects.reduce(new Function2<Rectangle2D, Rectangle2D,Rectangle2D>() {
			public Rectangle2D call(Rectangle2D left, Rectangle2D right) throws Exception {
				return Util.bounds(left, right);
			}
		});
	}
	
	
	@Override public boolean isEmpty() {return size() >0;}
	@Override public long size() {return base.count();}
	@Override public long segments() {return size();}

	@Override
	public Glyphset<G,I> segment(long bottom, long top) throws IllegalArgumentException {
		throw new UnsupportedOperationException("Segmentation not supported.  Use Spark segmentation routines on RDD directly or convert to local glyphset (via iterator)");
	}

}
