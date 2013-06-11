package ar.renderers;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;
import ar.util.FlatAggregates;
import ar.util.Util;

/**Task stealing renderer that operates on a per-pixel basis, designed to be used with a spatially-decomposed glyph set.
 * Divides aggregates space into regions and works on each region in isolation
 * (i.e., bin-driven iteration).
 * **/
public final class ParallelSpatial<G,A> implements Renderer<G,A> {
	private final ForkJoinPool pool = new ForkJoinPool();

	private final int taskSize;
	private final RenderUtils.Progress recorder;

	public ParallelSpatial(int taskSize) {
		this.taskSize = taskSize;
		recorder = RenderUtils.recorder();
	}
	protected void finalize() {pool.shutdownNow();}
	
	
	public Aggregates<A> reduce(final Glyphset<G> glyphs, final Aggregator<G,A> op, 
			final AffineTransform inverseView, final int width, final int height) {
		final Aggregates<A> aggregates = new FlatAggregates<A>(width, height, op.identity()); 
		ReduceTask<G,A> t = new ReduceTask<G,A>(glyphs, inverseView, op, recorder, taskSize, aggregates, 0,0, width, height);
		pool.invoke(t);
		return aggregates;
	}
	
	public BufferedImage transfer(Aggregates<A> aggregates, Transfer<A> t, int width, int height, Color background) {
		BufferedImage i = Util.initImage(width, height, background);
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				i.setRGB(x, y, t.at(x, y, aggregates).getRGB());
			}
		}
		return i;
	}

	public double progress() {return recorder.percent();}
	
	private static final class ReduceTask<G,A> extends RecursiveAction {
		private static final long serialVersionUID = -6471136218098505342L;

		private final int taskSize;
		private final int lowx, lowy, highx, highy;
		private final Aggregates<A> aggs;
		private final Aggregator<G,A> op;
		private final Glyphset<G> glyphs;
		private final AffineTransform inverseView;
		private final RenderUtils.Progress recorder;
		
		public ReduceTask(Glyphset<G> glyphs, AffineTransform inverseView, 
				Aggregator<G,A> op, RenderUtils.Progress recorder, int taskSize,   
				Aggregates<A> aggs, int lowx, int lowy, int highx, int highy) {
			this.glyphs = glyphs;
			this.inverseView = inverseView;
			this.recorder = recorder;
			this.op=op;
			this.taskSize = taskSize;
			this.aggs = aggs;
			this.lowx = lowx;
			this.lowy = lowy;
			this.highx = highx > aggs.highX() ? aggs.highX()-1 : highx;  //Roundoff...
			this.highy = highy > aggs.highY() ? aggs.highY()-1 : highy;//Roundoff...
			
			if (highx> aggs.highX()) {throw new RuntimeException(String.format("%d > width of %d",  highx, aggs.highX()));}
			if (highy> aggs.highY()) {throw new RuntimeException(String.format("%d > height of %d",  highy, aggs.highY()));}
		}

		private int center(int low, int high) {return low+((high-low)/2);}
		
		@Override
		protected void compute() {
			int width = highx-lowx;
			int height = highy-lowy;

			if ((width*height) > taskSize) {
				int centerx = center(lowx, highx);
				int centery = center(lowy, highy);
				ReduceTask<G,A> SW = new ReduceTask<G,A>(glyphs, inverseView, op, recorder, taskSize, aggs, lowx,    lowy,    centerx, centery);
				ReduceTask<G,A> NW = new ReduceTask<G,A>(glyphs, inverseView, op, recorder, taskSize, aggs, lowx,    centery, centerx, highy);
				ReduceTask<G,A> SE = new ReduceTask<G,A>(glyphs, inverseView, op, recorder, taskSize, aggs, centerx, lowy,    highx,   centery);
				ReduceTask<G,A> NE = new ReduceTask<G,A>(glyphs, inverseView, op, recorder, taskSize, aggs, centerx, centery, highx,   highy);
				invokeAll(SW,NW,SE,NE);
			} else {
				Rectangle pixel = new Rectangle(0,0,1,1);
				for (int x=lowx; x<highx; x++) {
					for (int y=lowy; y<highy; y++) {
						pixel.setLocation(x, y);
						A value = op.at(pixel,glyphs,inverseView);
						aggs.set(x,y,value);
						recorder.update(1);
					}
				}
			}
		}
	}
}
