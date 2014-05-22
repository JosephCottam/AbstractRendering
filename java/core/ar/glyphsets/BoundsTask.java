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
	private final int lowTask, highTask;
	private final int totalTasks;

	public BoundsTask(Glyphset<G, ?> glyphs, int totalTasks) {
		this(glyphs, 0, totalTasks, totalTasks);
	}

	public BoundsTask(Glyphset<G, ?> glyphs, int lowTask, int highTask, int totalTasks) {
		this.glyphs = glyphs;
		this.totalTasks = totalTasks;
		this.lowTask = lowTask;
		this.highTask = highTask;
	}

	@Override
	protected Rectangle2D compute() {
		if (lowTask != highTask) {return split();}
		else {return local();}
	}

	private Rectangle2D split() {
		int midTask = lowTask+((highTask-lowTask)/2);
		BoundsTask<G> top = new BoundsTask<G>(glyphs, lowTask, midTask, totalTasks);
		BoundsTask<G> bottom = new BoundsTask<G>(glyphs, lowTask, midTask, totalTasks);
		invokeAll(top, bottom);
		Rectangle2D bounds = Util.bounds(top.getRawResult(), bottom.getRawResult());
		return bounds;
	}

	private Rectangle2D local() {
		Glyphset<G,?> subset = glyphs.segmentAt(totalTasks, lowTask);
		return Util.bounds(subset);
	}

}