package ar.renderers.tasks;

import java.util.concurrent.RecursiveAction;

import ar.Aggregates;
import ar.Transfer;
import ar.util.Util;

public final class PixelParallelTransfer<IN, OUT> extends RecursiveAction {
	private static final long serialVersionUID = 7512448648194530526L;
	
	private final int lowx, lowy, highx, highy;
	private final Aggregates<OUT> out;
	private final Aggregates<? extends IN> in;
	private final Transfer.ItemWise<IN, OUT> t;
	private final long taskSize;
	
	public PixelParallelTransfer(
			Aggregates<? extends IN> input, Aggregates<OUT> result, 
			Transfer.ItemWise<IN, OUT> t,
			long taskSize,
			int lowX, int lowY, int highX, int highY) {
		
		this.lowx=lowX;
		this.lowy=lowY;
		this.highx=highX;
		this.highy=highY;
		this.out = result;
		this.in = input;
		this.t = t;
		this.taskSize = taskSize;
	}

	protected void compute() {
		int width = highx-lowx;
		int height = highy-lowy;
		if (width * height >= taskSize) {
			int centerx = Util.mean(lowx, highx);
			int centery = Util.mean(lowy, highy);
			PixelParallelTransfer<IN, OUT> SW = new PixelParallelTransfer<>(in, out, t, taskSize, lowx,    lowy,    centerx, centery);
			PixelParallelTransfer<IN, OUT> NW = new PixelParallelTransfer<>(in, out, t, taskSize, lowx,    centery, centerx, highy);
			PixelParallelTransfer<IN, OUT> SE = new PixelParallelTransfer<>(in, out, t, taskSize, centerx, lowy,    highx,   centery);
			PixelParallelTransfer<IN, OUT> NE = new PixelParallelTransfer<>(in, out, t, taskSize, centerx, centery, highx,   highy);
			invokeAll(SW,NW,SE,NE);
		} else {
			for (int x=lowx; x<highx; x++) {
				for (int y=lowy; y<highy; y++) {
					OUT val = t.at(x, y, in);
					out.set(x, y, val);
				}
			}				
		}
	}
}