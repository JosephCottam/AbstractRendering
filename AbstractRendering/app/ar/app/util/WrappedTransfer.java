package ar.app.util;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JFrame;

import ar.Aggregates;
import ar.Transfer;
import ar.aggregates.FlatAggregates;
import ar.app.ARApp;
import ar.app.components.ScatterControl;
import ar.rules.Aggregators;
import ar.rules.Transfers;
import ar.util.Util;

public interface WrappedTransfer<A> {
	public void deselected();
	public void selected(ARApp app);
	public Transfer<A> op();
	public Class<A> type();
	
	public class SelectiveDistribution implements WrappedTransfer<Number> {
		JFrame flyAway;
		ScatterControl control = new ScatterControl();

		public Transfer<Number> op() {return control.getTransfer().op();}
		public Class<Number> type() {return Number.class;}
		public String toString() {return "Scatter-based selection (int)";}
		public void selected(ARApp app) {
			if (flyAway != null) {flyAway.dispose();}
			flyAway = new JFrame();
			flyAway.setTitle("Parameters");
			flyAway.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			flyAway.setLayout(new BorderLayout());		
			flyAway.setLocation(500,0);
			flyAway.setSize(300,300);
			flyAway.invalidate();
			flyAway.setVisible(true);

			flyAway.getContentPane().removeAll();
			flyAway.add(control, BorderLayout.CENTER);
			flyAway.revalidate();
			
			control.setSource(app);
		}
		public void deselected() {
			if (flyAway != null) {flyAway.dispose();}
		}
	}
	
	public class RedWhiteLinear implements WrappedTransfer<Integer> {
		public Transfer<Integer> op() {return new Transfers.Interpolate(new Color(255,0,0,38), Color.red);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Red luminance linear (int)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class RedWhiteLog implements WrappedTransfer<Integer> {
		public Transfer<Integer> op() {return new Transfers.Interpolate(new Color(255,0,0,38), Color.red, Util.CLEAR, 10);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Red luminance log-10 (int)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class FixedAlpha implements WrappedTransfer<Integer> {
		public Transfer<Integer> op() {return new Transfers.FixedAlpha(Color.white, Color.red, 0, 25.5);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "10% Alpha (int)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class OutlierHighlight implements WrappedTransfer<Integer> {
		public Transfer<Integer> op() {return new Transfers.ZScore(Color.white, Color.red, true);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Outlier Highlight (int)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class OutlierHighlightB implements WrappedTransfer<Integer> {
		public Transfer<Integer> op() {return new Transfers.ZScore(Color.white, Color.red, false);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Outlier Highlight w/0's (int)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}

	
	public class Percent90 implements WrappedTransfer<Aggregators.RLE> {
		public Transfer<Aggregators.RLE> op() {return new Transfers.FirstPercent(.9, Color.blue, Color.white, Color.blue, Color.red);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "90% Percent (RLE)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}

	public class Percent95 implements WrappedTransfer<Aggregators.RLE> {
		public Transfer<Aggregators.RLE> op() {return new Transfers.FirstPercent(.95, Color.blue, Color.white, Color.blue, Color.red);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "95% Percent (RLE)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}

	public class Percent25 implements WrappedTransfer<Aggregators.RLE> {
		public Transfer<Aggregators.RLE> op() {return new Transfers.FirstPercent(.25, Color.blue, Color.white, Color.blue, Color.red);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "25% Percent (RLE)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class EchoColor implements WrappedTransfer<Color> {
		public Transfer<Color> op() {return new Transfers.IDColor();}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Echo (Color)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class HighAlphaLog implements WrappedTransfer<Aggregators.RLE> {
		public Transfer<Aggregators.RLE> op() {return new Transfers.HighAlpha(Color.white, .1, true);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "Log HD Alpha (RLE)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class HighAlphaLin implements WrappedTransfer<Aggregators.RLE> {
		public Transfer<Aggregators.RLE> op() {return new Transfers.HighAlpha(Color.white, .1, false);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "Linear HD Alpha (RLE)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class DrawDarkVar implements WrappedTransfer<Integer> {
		private int distance;
		public DrawDarkVar() {this(10);}
		public DrawDarkVar(int distance) {this.distance = distance;}
		public Transfer<Integer> op() {return new DrawDark(Color.BLACK, Color.white, distance);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return String.format("Draw Dark (%d)", distance);}
		public void selected(ARApp app) {}
		public void deselected() {}
		
		private class DrawDark implements Transfer<Integer> {
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
	
	
	
	
}
