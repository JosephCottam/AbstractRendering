package ar.renderers;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import ar.Aggregates;
import ar.Aggregator;
import ar.GlyphSet;
import ar.Renderer;
import ar.Transfer;
import ar.Util;

/**Task stealing renderer, designed to be used with a spatially-decomposed glyph set.
 * Divides aggregates space into regions and works on each region in isolation
 * (i.e., bin-driven iteration).
 * **/
public final class ParallelSpatial implements Renderer {
	private final int taskSize;
	private final ForkJoinPool pool = new ForkJoinPool();
	
	public ParallelSpatial(int taskSize) {this.taskSize = taskSize;}
	
	public <A> Aggregates<A> reduce(final GlyphSet glyphs, final AffineTransform inverseView, 
			final Aggregator<A> r, final int width, final int height) {
		Aggregates<A> aggregates = new Aggregates<A>(width, height, r.identity()); 
		ReduceTask<A> t = new ReduceTask<A>(glyphs, inverseView, r, aggregates, 0,0, width, height, taskSize, 0);
		pool.invoke(t);
		return aggregates;
	}
	
	public <A> BufferedImage transfer(Aggregates<A> aggregates, Transfer<A> t, int width, int height, Color background) {
		BufferedImage i = Util.initImage(width, height, background);
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				i.setRGB(x, y, t.at(x, y, aggregates).getRGB());
			}
		}
		return i;
	}


	private static final class ReduceTask<A> extends RecursiveAction {
		private static final long serialVersionUID = -6471136218098505342L;

		private final int taskSize;
		private final int lowx, lowy, highx, highy;
		private final int width,height;
		private final Aggregates<A> aggs;
		private final Aggregator<A> op;
		private final GlyphSet glyphs;
		private final int depth;
		private final AffineTransform inverseView;
		
		public ReduceTask(GlyphSet glyphs, AffineTransform inverseView, Aggregator<A> op,  
				Aggregates<A> aggs, int lowx, int lowy, int highx, int highy, int taskSize, int depth) {
			this.lowx = lowx;
			this.lowy = lowy;
			this.highx = highx > aggs.highX() ? aggs.highX()-1 : highx;  //Roundoff...
			this.highy = highy > aggs.highY() ? aggs.highY()-1 : highy;//Roundoff...
			this.aggs = aggs;
			this.op=op;
			this.glyphs = glyphs;
			this.inverseView = inverseView;
			this.taskSize = taskSize;
			this.width = highx-lowx;
			this.height = highy-lowy;
			this.depth = depth;
			
			if (highx> aggs.highX()) {throw new RuntimeException(String.format("%d > width of %d",  highx, aggs.highX()));}
			if (highy> aggs.highY()) {throw new RuntimeException(String.format("%d > height of %d",  highy, aggs.highY()));}
			//System.out.printf(Util.indent(depth) + "Task from (%d,%d) to (%d x %d); concernSize %d\n", lowx,lowy, highx,highy, width*height);
		}

		private int center(int low, int high) {return low+((high-low)/2);}
		
		@Override
		protected void compute() {
			if ((width*height) > taskSize) {
				int centerx = center(lowx, highx);
				int centery = center(lowy, highy);
				ReduceTask<A> SW = new ReduceTask<A>(glyphs, inverseView, op, aggs, lowx,    lowy,    centerx, centery, taskSize, depth+1);
				ReduceTask<A> NW = new ReduceTask<A>(glyphs, inverseView, op, aggs, lowx,    centery, centerx, highy,   taskSize, depth+1);
				ReduceTask<A> SE = new ReduceTask<A>(glyphs, inverseView, op, aggs, centerx, lowy,    highx,   centery, taskSize, depth+1);
				ReduceTask<A> NE = new ReduceTask<A>(glyphs, inverseView, op, aggs, centerx, centery, highx,   highy,   taskSize, depth+1);
				invokeAll(SW,NW,SE,NE);
			} else {
				Rectangle pixel = new Rectangle(0,0,1,1);
				for (int x=lowx; x<highx; x++) {
					for (int y=lowy; y<highy; y++) {
						pixel.setLocation(x, y);
						A value = op.at(pixel,glyphs,inverseView);
						aggs.set(x,y,value);
					}
				}
			}
		}
	}
}
