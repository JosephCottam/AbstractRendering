package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.concurrent.RecursiveTask;

import ar.Glyphset;
import ar.util.Util;

/**Compute the bounds for a glyphset.  
 */
final class BoundsTask<G> extends RecursiveTask<Rectangle2D> {
	private final Glyphset<G, ?> glyphs;
	public static final long serialVersionUID = 1L;
	private final long low, high;
	private final long tasksize;

	public BoundsTask(Glyphset<G, ?> glyphs, long tasksize) {
		this(glyphs, tasksize, 0, glyphs.segments());
	}

	public BoundsTask(Glyphset<G, ?> glyphs, long tasksize, long low, long high) {
		this.glyphs = glyphs;
		this.tasksize = tasksize;
		this.low = low;
		this.high = high;
	}

	@Override
	protected Rectangle2D compute() {
		if (high-low > tasksize) {return split();}
		else {return local();}
	}

	private Rectangle2D split() {
		long mid = low+((high-low)/2);
		BoundsTask<G> top = new BoundsTask<G>(glyphs, tasksize, low, mid);
		BoundsTask<G> bottom = new BoundsTask<G>(glyphs, tasksize, mid, high);
		invokeAll(top, bottom);
		Rectangle2D bounds = Util.bounds(top.getRawResult(), bottom.getRawResult());
		return bounds;
	}

	private Rectangle2D local() {
		Glyphset<G,?> subset = glyphs.segment(low, high);
		return Util.bounds(subset);
	}

}