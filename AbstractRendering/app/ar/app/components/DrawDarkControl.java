package ar.app.components;

import java.awt.Color;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ar.Aggregates;
import ar.Transfer;
import ar.aggregates.FlatAggregates;
import ar.app.ARApp;
import ar.rules.Numbers;
import ar.util.Util;

public class DrawDarkControl extends JPanel {
	private static final long serialVersionUID = 1L;
	
	protected final JSpinner distance = new JSpinner();
	protected ARApp source;
	protected DrawDark cached;

	public DrawDarkControl() {
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(distance);
		distance.setValue(10);
	
		distance.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {updateImage();}
		});
	}
	
	public void setSource(ARApp source) {this.source=source;}
	public int distance() {return (Integer) distance.getValue();}
	public void updateImage() {
		ARPanel p = source.getPanel().withTransfer(DrawDarkControl.this.getTransfer());
		source.changeImage(p);
	}

	public Transfer<Number,Color> getTransfer() {
		if (cached == null || distance() != cached.distance) {
			cached = new DrawDark(Color.black, Color.white, distance());
		}
		return cached;
	}
	
	public static class DrawDark implements Transfer<Number, Color> {
		final int distance;
		final Transfer<Number, Color> inner;
		Aggregates<Double> cached;
		Aggregates<? extends Number> cacheKey;
		
		public DrawDark(Color low, Color high, int distance) {
			this.distance=distance;
			inner = new Numbers.Interpolate(low,high,high,-1);
		}
	
		public Color at(int x, int y, Aggregates<? extends Number> aggregates) {
			if (cacheKey == null || cacheKey != aggregates) {
				preproc(aggregates); cacheKey=aggregates;
			}
			return inner.at(x,y,cached);
		}
		
		private void preproc(Aggregates<? extends Number> aggs) {
			Aggregates<Double> out = new FlatAggregates<>(aggs.lowX(), aggs.lowY(), aggs.highX(), aggs.highY(), Double.NaN);
			
			for (int x=aggs.lowX(); x <aggs.highX(); x++) {
				for (int y=aggs.lowY(); y<aggs.highY(); y++) {
					if (aggs.at(x, y).doubleValue() > 0) {
						out.set(x, y, preprocOne(x,y,aggs));
					} else {
						out.set(x,y, Double.NaN);
					}
				}
			}
			this.cached = out;
		}
		
		private double preprocOne(int x, int y, Aggregates<? extends Number> aggregates) {
			double surroundingSum =0;
			int cellCount = 0;
			for (int dx=-distance; dx<=distance; dx++) {
				for (int dy=-distance; dy<=distance; dy++) {
					int cx=x+dx;
					int cy=y+dy;
					if (cx < aggregates.lowX() || cy < aggregates.lowY() 
							|| cx>aggregates.highX() || cy> aggregates.highY()) {continue;}
					cellCount++;
					double dv = aggregates.at(cx,cy).doubleValue();
					if (dv != 0) {surroundingSum++;}
				}
			}
			return surroundingSum/cellCount;
		}

		public Color emptyValue() {return Util.CLEAR;}
		public Class<Number> input() {return Number.class;}
		public Class<Color> output() {return Color.class;}
	}
}
