package ar.glyphsets;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import ar.Glyph;
import ar.Glyphset;
import ar.util.axis.Axis;
import ar.util.axis.DescriptorPair;

/**Glyphset derived from a Generator.
 * **/
public class SyntheticGlyphset<G,I> implements Glyphset.RandomAccess<G,I>{
	private final long size;
	private final Function<Long,G> shaper;
	private final Function<Long,I> valuer;
	private DescriptorPair<?,?> axisDescriptor;

	public SyntheticGlyphset(long size, Function<Long, G> shaper, Function<Long, I> valuer) {
		this.size = size;
		this.shaper = shaper;
		this.valuer = valuer;
	}
	
	@Override public Iterator<Glyph<G, I>> iterator() {return new GlyphsetIterator<>(this);}
	@Override public boolean isEmpty() {return size <= 0;}
	@Override public Rectangle2D bounds() {return new Rectangle2D.Double(0,0,100,100);}
	@Override public long size() {return size;}

	@Override
	public List<Glyphset<G, I>> segment(int count) throws IllegalArgumentException {
		long stride = (size()/count)+1; //+1 for the round-down
		List<Glyphset<G,I>> segments = new ArrayList<>();
		for (long offset=0; offset<size(); offset+=stride) {
			segments.add(new GlyphSubset.Uncached<>(this, offset, offset+stride));
		}
		return segments;
	}
	
	@Override
	public Glyph<G, I> get(long l) {
		return new SimpleGlyph<>(shaper.apply(l), valuer.apply(l));
	}
		
	/**Points generated from a uniform distribution points.**/
	public static class UniformPoints implements Function<Long, Point2D> {
		final int maxX, maxY;
		
		public UniformPoints() {this(100,100);}
		public UniformPoints(int maxX, int maxY) {
			this.maxX = maxX;
			this.maxY = maxY;
		}

		@Override
		public Point2D apply(Long from) {return new Point2D.Double(Math.random()*maxX, Math.random()*maxY);}
	}

	@Override public DescriptorPair<?,?> axisDescriptors() {return axisDescriptor != null ? axisDescriptor : Axis.coordinantDescriptors(this);}
	@Override public void axisDescriptors(DescriptorPair<?,?> descriptor) {this.axisDescriptor = descriptor;} 

	
}
