package ar.renderers.tasks;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RecursiveTask;

import javax.sound.sampled.Line;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyph;
import ar.Glyphset;
import ar.aggregates.AggregateUtils;
import ar.aggregates.ConstantAggregates;
import ar.renderers.AggregationStrategies;
import ar.renderers.RenderUtils;

public class GlyphParallelAggregate<I,A> extends RecursiveTask<Aggregates<A>> {
	private static final long serialVersionUID = 705015978061576950L;

	private final int taskSize;
	private final long low;
	private final long high;
	private final Glyphset<? extends I> glyphs;
	private final AffineTransform view;
	private final Rectangle viewport;
	private final Aggregator<I,A> op;
	private final RenderUtils.Progress recorder;

	
	public GlyphParallelAggregate(
			Glyphset<? extends I> glyphs, 
			AffineTransform view,
			Aggregator<I,A> op, 
			Rectangle viewport,
			int taskSize,
			RenderUtils.Progress recorder,
			long low, long high) {
		this.glyphs = glyphs;
		this.view = view;
		this.op = op;
		this.viewport =viewport;
		this.taskSize = taskSize;
		this.recorder = recorder;
		this.low = low;
		this.high = high;
	}

	protected Aggregates<A> compute() {
		if ((high-low) > taskSize) {return split();}
		else {return local();}
	}
	
	private final Aggregates<A> split() {
		long mid = low+((high-low)/2);

		GlyphParallelAggregate<I,A> top = new GlyphParallelAggregate<I,A>(glyphs, view, op, viewport, taskSize, recorder, low, mid);
		GlyphParallelAggregate<I,A> bottom = new GlyphParallelAggregate<I,A>(glyphs, view, op, viewport, taskSize, recorder, mid, high);
		invokeAll(top, bottom);
		Aggregates<A> aggs;
		try {aggs = AggregationStrategies.horizontalRollup(top.get(), bottom.get(), op);}
		catch (InterruptedException | ExecutionException e) {throw new RuntimeException(e);}
		return aggs;
	}
	
	//TODO: Consider the actual shape.  Currently assumes that the bounds box matches the actual item bounds..
	private final Aggregates<A> local() {
		Glyphset<? extends I> subset = glyphs.segment(low,  high);
		
		//Intersect the subset data with the region to be rendered; skip rendering if there is nothing to render
		Rectangle bounds = view.createTransformedShape(subset.bounds()).getBounds();
		bounds = bounds.intersection(viewport);
		if (bounds.isEmpty()) {
			int x2 = bounds.x+bounds.width;
			int y2 = bounds.y+bounds.height;
			recorder.update(high-low);
			return new ConstantAggregates<A>(Math.min(x2, bounds.x), Math.min(y2, bounds.y),
											Math.max(x2, bounds.x), Math.min(y2, bounds.y),
											op.identity());
		}				
		Aggregates<A> aggregates = AggregateUtils.make(bounds.x, bounds.y,
													 bounds.x+bounds.width, bounds.y+bounds.height, 
													 op.identity());
		
		
		Point2D lowP = new Point2D.Double();
		Point2D highP = new Point2D.Double();
		
		final int width = viewport.width;
		final int height =viewport.height;
		for (Glyph<? extends I> g: subset) {
			//Discretize the glyph into the aggregates array
			Rectangle2D b = g.shape().getBounds2D();
			lowP.setLocation(b.getMinX(), b.getMinY());
			highP.setLocation(b.getMaxX(), b.getMaxY());
			
			view.transform(lowP, lowP);
			view.transform(highP, highP);
			
			int lowx = (int) Math.floor(lowP.getX());
			int lowy = (int) Math.floor(lowP.getY());
			int highx = (int) Math.ceil(highP.getX());
			int highy = (int) Math.ceil(highP.getY());

			I v = g.info();
			
			for (int x=Math.max(0,lowx); x<highx && x< width; x++){
				for (int y=Math.max(0, lowy); y<highy && y< height; y++) {
					A existing = aggregates.get(x,y);
					A update = op.combine(x,y,existing, v);
					aggregates.set(x, y, update);
				}
			}
		}

		recorder.update(subset.size());
		return aggregates;
	}

}