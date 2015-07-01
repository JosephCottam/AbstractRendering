package ar.ext.spark;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;

import com.google.common.collect.Lists;

import ar.Glyph;
import ar.Glyphset;
import ar.util.Util;
import ar.util.axis.Axis;
import ar.util.axis.DescriptorPair;

public class GlyphsetRDD<G,I> implements Glyphset<G,I>, Serializable {
	public static boolean MAP_PARTITIONS = false;

	private final JavaRDD<Glyph<G,I>> base;
	private final boolean partitions;
	
	public GlyphsetRDD(JavaRDD<Glyph<G,I>> base) {this(base, true,true);}
	public GlyphsetRDD(JavaRDD<Glyph<G,I>> base, boolean cache, boolean partitions) {
		this.base = base;
		this.partitions = partitions;
		if (cache) {base.cache();}
	}

	/**Return the backing RDD.**/
	public JavaRDD<Glyph<G,I>> base() {return base;}
	
	public Glyph<G,I> exemplar() {return base.first();}
	public boolean mapPartitions() {return partitions;}
	
	@Override public Iterator<Glyph<G,I>> iterator() {return base.collect().iterator();}

	@Override public Rectangle2D bounds() {
		final JavaRDD<Rectangle2D> rects;
		if (partitions) {
			rects = base.mapPartitions(
				new FlatMapFunction<Iterator<Glyph<G,I>>,Rectangle2D>() {
					public Iterable<Rectangle2D> call(Iterator<Glyph<G, I>> glyphs) throws Exception {
						ArrayList<Glyph<G,I>> glyphList = Lists.newArrayList(new IterableIterator<>(glyphs));
						return Arrays.asList(Util.bounds(glyphList));
					}});
		} else {
			rects = base.map(new Function<Glyph<G,I>,Rectangle2D>() {
				public Rectangle2D call(Glyph<G,I> glyph) throws Exception {
					return Util.boundOne(glyph.shape());
				}});
		}
		
		return rects.reduce(new Function2<Rectangle2D, Rectangle2D,Rectangle2D>() {
			public Rectangle2D call(Rectangle2D left, Rectangle2D right) throws Exception {
				return Util.bounds(left, right);
			}
		});

	}
	
	
	@Override public boolean isEmpty() {return size() >0;}
	@Override public long size() {return base.count();}
	
	@Override
	public List<Glyphset<G,I>> segment(int count) throws IllegalArgumentException {
		throw new UnsupportedOperationException("Segmentation not supported.  Use Spark segmentation routines on RDD directly or convert to local glyphset (via iterator)");
	}

	private DescriptorPair<?,?> axisDescriptor;
	@Override public DescriptorPair<?,?> axisDescriptors() {return axisDescriptor != null ? axisDescriptor : Axis.coordinantDescriptors(this);}
	@Override public void axisDescriptors(DescriptorPair<?,?> descriptor) {this.axisDescriptor = descriptor;} 
	
}
