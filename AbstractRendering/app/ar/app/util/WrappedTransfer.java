package ar.app.util;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JFrame;

import ar.Aggregates;
import ar.Transfer;
import ar.aggregates.FlatAggregates;
import ar.app.ARApp;
import ar.app.components.DrawDarkControl;
import ar.app.components.ScatterControl;
import ar.rules.Aggregators;
import ar.rules.Transfers;
import ar.util.Util;

public interface WrappedTransfer<IN,OUT> extends Wrapped<Transfer<IN,OUT>> {
	public void deselected();
	public void selected(ARApp app);
	public Transfer<IN,OUT> op();
	
	public class SelectiveDistribution implements WrappedTransfer<Number,Color> {
		JFrame flyAway;
		ScatterControl control = new ScatterControl();

		public Transfer<Number,Color> op() {return control.getTransfer().op();}
		public String toString() {return "Scatter-based selection (int)";}
		public void selected(ARApp app) {
			if (flyAway == null) {
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
			} else {
				flyAway.setVisible(true);
			}
		} 
		public void deselected() {
			if (flyAway != null) {flyAway.setVisible(false);}
		}
	}
	
	
	public class DrawDarkVar implements WrappedTransfer<Number,Color> {
		JFrame flyAway;
		DrawDarkControl control = new DrawDarkControl();
		
		public Transfer<Number,Color> op() {return control.getTransfer().op();}
		public Class<Number> type() {return Number.class;}
		public String toString() {return String.format("Draw the Dark");}
		public void selected(ARApp app) {
			if (flyAway == null) {
				flyAway = new JFrame();
				flyAway.setTitle("Parameters");
				flyAway.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
				flyAway.setLayout(new BorderLayout());		
				flyAway.setLocation(500,0);
				flyAway.setSize(300,100);
				flyAway.invalidate();
				flyAway.setVisible(true);
	
				flyAway.getContentPane().removeAll();
				flyAway.add(control, BorderLayout.CENTER);
				flyAway.revalidate();
			
				control.setSource(app);
			} else {
				flyAway.setVisible(true);
			}
		}
		public void deselected() {
			if (flyAway != null) {flyAway.setVisible(false);}
		}
	}

	
	public class RedWhiteLinear implements WrappedTransfer<Number,Color> {
		public Transfer<Number,Color> op() {return new Transfers.Interpolate(new Color(255,0,0,38), Color.red);}
		public Class<Number> type() {return Number.class;}
		public String toString() {return "Red luminance linear (int)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class RedWhiteLog implements WrappedTransfer<Number,Color> {
		public Transfer<Number,Color> op() {return new Transfers.Interpolate(new Color(255,0,0,38), Color.red, Util.CLEAR, 10);}
		public Class<Number> type() {return Number.class;}
		public String toString() {return "Red luminance log-10 (int)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class FixedAlpha implements WrappedTransfer<Integer,Color> {
		public Transfer<Integer,Color> op() {return new Transfers.FixedAlpha(Color.white, Color.red, 0, 25.5);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "10% Alpha (int)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class FixedAlphaB implements WrappedTransfer<Integer,Color> {
		public Transfer<Integer,Color> op() {return new Transfers.FixedAlpha(Color.white, Color.red, 0, 255);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Min Alpha (int)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class Present implements WrappedTransfer<Integer,Color> {
		public Transfer<Integer,Color> op() {return new Transfers.Present<Integer>(Color.red, Color.white, Integer.class);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Present (int)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class OutlierHighlight implements WrappedTransfer<Integer,Color> {
		public Transfer<Integer,Color> op() {return new Transfers.ZScore(Color.white, Color.red, true);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Outlier Highlight (int)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class OutlierHighlightB implements WrappedTransfer<Integer,Color> {
		public Transfer<Integer,Color> op() {return new Transfers.ZScore(Color.white, Color.red, false);}
		public Class<Integer> type() {return Integer.class;}
		public String toString() {return "Outlier Highlight w/0's (int)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}

	
	public class Percent90 implements WrappedTransfer<Aggregators.RLE,Color> {
		public Transfer<Aggregators.RLE,Color> op() {return new Transfers.FirstPercent(.9, Color.blue, Color.white, Color.blue, Color.red);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "90% Percent (RLE)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}

	public class Percent95 implements WrappedTransfer<Aggregators.RLE,Color> {
		public Transfer<Aggregators.RLE,Color> op() {return new Transfers.FirstPercent(.95, Color.blue, Color.white, Color.blue, Color.red);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "95% Percent (RLE)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}

	public class Percent25 implements WrappedTransfer<Aggregators.RLE,Color> {
		public Transfer<Aggregators.RLE,Color> op() {return new Transfers.FirstPercent(.25, Color.blue, Color.white, Color.blue, Color.red);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "25% Percent (RLE)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class EchoColor implements WrappedTransfer<Color,Color> {
		public Transfer<Color,Color> op() {return new Transfers.IDColor();}
		public Class<Color> type() {return Color.class;}
		public String toString() {return "Echo (Color)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class HighAlphaLog implements WrappedTransfer<Aggregators.RLE,Color> {
		public Transfer<Aggregators.RLE,Color> op() {return new Transfers.HighAlpha(Color.white, .1, true);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "Log HD Alpha (RLE)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class HighAlphaLin implements WrappedTransfer<Aggregators.RLE,Color> {
		public Transfer<Aggregators.RLE,Color> op() {return new Transfers.HighAlpha(Color.white, .1, false);}
		public Class<Aggregators.RLE> type() {return Aggregators.RLE.class;}
		public String toString() {return "Linear HD Alpha (RLE)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	
	public class ClearCol implements WrappedTransfer<Number,Color> {

		@Override
		public Transfer<Number,Color> op() {
			return new Transfer<Number,Color> () {
				protected Aggregates<Color> cached;
				protected Aggregates<? extends Number> key;
				public Color at(int x, int y, Aggregates<? extends Number> aggs) {
					if (key == null || key != aggs) {
						cached = new FlatAggregates<>(aggs, Color.BLACK);
						key =aggs;
						for (int c=aggs.lowX(); c<aggs.highX(); c++) {
							Color v = Color.RED;
							for (int r=aggs.lowY(); r<aggs.highY(); r++) {
								if (!(aggs.at(c,r).equals(aggs.defaultValue()))) {
									v = Color.BLUE;
									break;
								}
							}
							
							for (int r=aggs.lowY(); r<aggs.highY(); r++) {cached.set(c, r, v);}
						}
						
					}
					return cached.at(x, y);
				}
				public Color identity() {return Util.CLEAR;}
				public Class<Number> input() {return Number.class;}
				public Class<Color> output() {return Color.class;}
			};
		}

		public void deselected() {}
		public void selected(ARApp app) {}
		public String toString() {return "Column filled (int)";}


	}
	
	
	
}
