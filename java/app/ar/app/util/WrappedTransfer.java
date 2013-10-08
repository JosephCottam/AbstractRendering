package ar.app.util;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JFrame;

import ar.Transfer;
import ar.app.ARApp;
import ar.app.components.DrawDarkControl;
import ar.app.components.ScatterControl;
import ar.rules.Advise;
import ar.rules.CategoricalCounts;
import ar.rules.Categories;
import ar.rules.General;
import ar.rules.Numbers;
import ar.util.Util;

public interface WrappedTransfer<IN,OUT> extends Wrapped<Transfer<IN,OUT>> {
	public void deselected();
	public void selected(ARApp app);
	public Transfer<IN,OUT> op();
	
	public class SelectiveDistribution implements WrappedTransfer<Number,Color> {
		JFrame flyAway;
		ScatterControl control = new ScatterControl();

		public Transfer<Number,Color> op() {return control.getTransfer();}
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
			} else {
				flyAway.setVisible(true);
			}
			control.setSource(app);
		} 

		public void deselected() {
			if (flyAway != null) {flyAway.setVisible(false);}
		}
	}
	
	
	public class DrawDarkVar implements WrappedTransfer<Number,Color> {
		JFrame flyAway;
		DrawDarkControl control = new DrawDarkControl();
		
		public Transfer<Number,Color> op() {return control.getTransfer();}
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
		public Transfer<Number,Color> op() {return new Numbers.Interpolate(new Color(255,0,0,38), Color.red);}
		public String toString() {return "Red luminance linear (int)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class RedWhiteLog implements WrappedTransfer<Number,Color> {
		public Transfer<Number,Color> op() {return new Numbers.Interpolate(new Color(255,0,0,38), Color.red, Util.CLEAR, 10);}
		public String toString() {return "Red luminance log-10 (int)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class FixedAlpha implements WrappedTransfer<Number,Color> {
		public Transfer<Number,Color> op() {return new Numbers.FixedInterpolate(Color.white, Color.red, 0, 25.5);}
		public String toString() {return "10% Alpha (int)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class FixedAlphaB implements WrappedTransfer<Number,Color> {
		public Transfer<Number,Color> op() {return new Numbers.FixedInterpolate(Color.white, Color.red, 0, 255);}
		public String toString() {return "Min Alpha (int)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class Present implements WrappedTransfer<Integer,Color> {
		public Transfer<Integer,Color> op() {return new General.Present<Integer, Color>(Color.red, Color.white);}
		public String toString() {return "Present (int)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class Percent90 implements WrappedTransfer<CategoricalCounts<Color>,Color> {
		public Transfer<CategoricalCounts<Color>,Color> op() {return new Categories.KeyPercent<Color>(.9, Color.blue, Color.white, Color.blue, Color.red);}
		public String toString() {return "90% Percent (RLE)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}

	public class Percent95 implements WrappedTransfer<CategoricalCounts<Color>,Color> {
		public Transfer<CategoricalCounts<Color>,Color> op() {return new Categories.KeyPercent<Color>(.95, Color.blue, Color.white, Color.blue, Color.red);}
		public String toString() {return "95% Percent (RLE)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}

	public class Percent25 implements WrappedTransfer<CategoricalCounts<Color>,Color> {
		public Transfer<CategoricalCounts<Color>,Color> op() {return new Categories.KeyPercent<Color>(.25, Color.blue, Color.white, Color.blue, Color.red);}
		public String toString() {return "25% Percent (RLE)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class EchoColor implements WrappedTransfer<Color,Color> {
		public Transfer<Color,Color> op() {return new General.Echo<Color>(Util.CLEAR);}
		public String toString() {return "Echo (Color)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class HighAlphaLog implements WrappedTransfer<CategoricalCounts<Color>,Color> {
		public Transfer<CategoricalCounts<Color>,Color> op() {return new Categories.HighAlpha(Color.white, .1, true);}
		public String toString() {return "Log HD Alpha (RLE)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class HighAlphaLin implements WrappedTransfer<CategoricalCounts<Color>,Color> {
		public Transfer<CategoricalCounts<Color>,Color> op() {return new Categories.HighAlpha(Color.white, .1, false);}
		public String toString() {return "Linear HD Alpha (RLE)";}
		public void selected(ARApp app) {}
		public void deselected() {}
	}
	
	public class OverUnder implements WrappedTransfer<Number, Color> {
		public void deselected() {}
		public void selected(ARApp app) {}
		public Transfer<Number, Color> op() {
			Transfer<Number, Color> basis = new FixedAlpha().op();
			return new Advise.OverUnder<>(Color.BLACK, Color.BLACK, basis, 20);
		}
		public String toString() {return "Clip Warn 10% alpha (int)";}
	}

	public class OverUnder2 implements WrappedTransfer<Number, Color> {
		public void deselected() {}
		public void selected(ARApp app) {}
		public Transfer<Number, Color> op() {
			Transfer<Number, Color> basis =new RedWhiteLog().op();
			return new Advise.OverUnder<>(Color.BLACK, Color.BLACK, basis, 20);
		}
		public String toString() {return "Clip Warn HDALpha log (int)";}
	}
}
