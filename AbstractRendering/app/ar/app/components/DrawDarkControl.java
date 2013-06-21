package ar.app.components;

import java.awt.Color;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ar.Aggregates;
import ar.Transfer;
import ar.aggregates.FlatAggregates;
import ar.app.ARApp;
import ar.app.util.TransferWrapper;
import ar.app.util.WrappedTransfer;
import ar.rules.Transfers;

public class DrawDarkControl extends JPanel {
	private static final long serialVersionUID = 1L;
	
	protected final JSpinner distance = new JSpinner();
	protected ARApp source;

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

	public WrappedTransfer<Integer> getTransfer() {
		return new TransferWrapper<Integer>(new DrawDark(Color.black, Color.white, distance()), Integer.class);
	}
	
	
	public static class DrawDark implements Transfer<Integer> {
		final int distance;
		final Transfer<Integer> inner;
		Aggregates<Integer> cached;
		Aggregates<Integer> cacheKey;
		
		public DrawDark(Color low, Color high, int distance) {
			this.distance=distance;
			inner = new Transfers.Interpolate(low,high);
		}
	
		public Color at(int x, int y, Aggregates<Integer> aggregates) {
			if (cacheKey == null || cacheKey != aggregates) {
				preproc(aggregates); cacheKey=aggregates;
			}
			return inner.at(x,y,cached);
		}
		
		private void preproc(Aggregates<Integer> aggs) {
			Aggregates<Integer> out = new FlatAggregates<>(aggs.lowX(), aggs.lowY(), aggs.highX(), aggs.highY(), 0);
			
			for (int x=aggs.lowX(); x <aggs.highX(); x++) {
				for (int y=aggs.lowY(); y<aggs.highY(); y++) {
					if (aggs.at(x, y) >0) {
						out.set(x, y, preprocOne(x,y,aggs));
					} else {
						out.set(x,y, 0);
					}
				}
			}
			this.cached = out;
		}
		
		private int preprocOne(int x, int y, Aggregates<Integer> aggregates) {
			int surroundingSum =0;
			for (int d=-distance; d<=distance; d++) {
				for (int dx=0; dx<=d; dx++) {
					for (int dy=0; dy<=d; dy++) {
						int cx=x+dx;
						int cy=y+dy;
						if (cx < aggregates.lowX() || cy < aggregates.lowY() || cx>aggregates.highX() || cy> aggregates.highY()) {continue;}
						double dv = aggregates.at(cx,cy).doubleValue();
						if (dv != 0) {surroundingSum++;}
					}
				}
			}
			return surroundingSum;
	
		}
		
	}
}
