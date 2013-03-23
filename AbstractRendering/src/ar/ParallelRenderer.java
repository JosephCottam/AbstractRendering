package ar;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public final class ParallelRenderer implements Renderer {
	private final int taskSize;
	private final ForkJoinPool pool = new ForkJoinPool();
	
	public ParallelRenderer(int taskSize) {this.taskSize = taskSize;}
	
	public <A> Aggregates<A> reduce(final GlyphSet glyphs, final AffineTransform inverseView, 
			final Reduction<A> r, final int width, final int height) {		
		Aggregates<A> aggregates = new Aggregates<A>(width, height); 
		ReduceTask<A> t = new ReduceTask<A>(glyphs, inverseView, r, aggregates, 0,0, width, height, taskSize, 0);
		pool.invoke(t);
		return aggregates;
	}
	
	public <A> BufferedImage transfer(Aggregates<A> aggregates, Transfer<A> t) {
		final int width = aggregates.width();
		final int height = aggregates.height();
		BufferedImage i = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				i.setRGB(x, y, t.at(x, y, aggregates).getRGB());
			}
		}
		return i;
	}


	private static class ReduceTask<A> extends RecursiveAction {
		private static final long serialVersionUID = -6471136218098505342L;

		private final int taskSize;
		private final int lowx, lowy, highx, highy;
		private final int width,height;
		private final Aggregates<A> aggs;
		private final Reduction<A> op;
		private final GlyphSet glyphs;
		private final int depth;
		private final AffineTransform inverseView;
		
		public ReduceTask(GlyphSet glyphs, AffineTransform inverseView, Reduction<A> op,  
				Aggregates<A> aggs, int lowx, int lowy, int highx, int highy, int taskSize, int depth) {
			this.lowx = lowx;
			this.lowy = lowy;
			this.highx = highx > aggs.width() ? aggs.width()-1 : highx;  //Roundoff...
			this.highy = highy > aggs.height() ? aggs.height()-1 : highy;//Roundoff...
			this.aggs = aggs;
			this.op=op;
			this.glyphs = glyphs;
			this.inverseView = inverseView;
			this.taskSize = taskSize;
			this.width = highx-lowx;
			this.height = highy-lowy;
			this.depth = depth;
			
			if (highx> aggs.width()) {throw new RuntimeException(String.format("%d > width of %d",  highx, aggs.width()));}
			if (highx> aggs.width()) {throw new RuntimeException(String.format("%d > height of %d",  highy, aggs.height()));}
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
				for (int x=lowx; x<highx; x++) {
					for (int y=lowy; y<highy; y++) {
						A value = op.at(x,y,glyphs,inverseView);
						aggs.set(x,y,value);
					}
				}
			}
		}
	}
}
