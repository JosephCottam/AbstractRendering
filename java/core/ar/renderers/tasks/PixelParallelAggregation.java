package ar.renderers.tasks;

import java.awt.geom.AffineTransform;
import java.util.concurrent.RecursiveAction;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.renderers.AggregationStrategies;
import ar.renderers.RenderUtils;
import ar.util.Util;

public final class PixelParallelAggregation<I,A> extends RecursiveAction {
	private static final long serialVersionUID = -6471136218098505342L;

	private final int taskSize;
	private final int lowx, lowy, highx, highy;
	private final Aggregates<A> aggs;
	private final Aggregator<I,A> op;
	private final Glyphset<? extends I> glyphs;
	private final AffineTransform inverseView;
	private final RenderUtils.Progress recorder;
	
	public PixelParallelAggregation(Glyphset<? extends I> glyphs, AffineTransform view, 
			Aggregator<I,A> op, RenderUtils.Progress recorder, int taskSize,   
			Aggregates<A> aggs, int lowx, int lowy, int highx, int highy) {
		this.glyphs = glyphs;
		this.inverseView = view;
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

	@Override
	protected void compute() {
		int width = highx-lowx;
		int height = highy-lowy;

		if ((width*height) > taskSize) {
			int centerx = Util.mean(lowx, highx);
			int centery = Util.mean(lowy, highy);
			PixelParallelAggregation<I,A> SW = new PixelParallelAggregation<I,A>(glyphs, inverseView, op, recorder, taskSize, aggs, lowx,    lowy,    centerx, centery);
			PixelParallelAggregation<I,A> NW = new PixelParallelAggregation<I,A>(glyphs, inverseView, op, recorder, taskSize, aggs, lowx,    centery, centerx, highy);
			PixelParallelAggregation<I,A> SE = new PixelParallelAggregation<I,A>(glyphs, inverseView, op, recorder, taskSize, aggs, centerx, lowy,    highx,   centery);
			PixelParallelAggregation<I,A> NE = new PixelParallelAggregation<I,A>(glyphs, inverseView, op, recorder, taskSize, aggs, centerx, centery, highx,   highy);
			invokeAll(SW,NW,SE,NE);
		} else {
			for (int x=lowx; x<highx; x++) {
				for (int y=lowy; y<highy; y++) {
					A val = AggregationStrategies.pixel(aggs, op, glyphs, inverseView, x, y);
					aggs.set(x, y, val);
					recorder.update(1);
				}
			}
		}
	}	
}