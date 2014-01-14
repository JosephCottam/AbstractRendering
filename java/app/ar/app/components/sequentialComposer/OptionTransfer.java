package ar.app.components.sequentialComposer;

import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import ar.Transfer;
import ar.app.components.LabeledItem;
import ar.app.display.ARComponent;
import ar.app.display.ARComponent.Holder;
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.rules.CategoricalCounts;
import ar.rules.Categories;
import ar.rules.Debug;
import ar.rules.General;
import ar.rules.Numbers;
import ar.rules.combinators.Seq;

public interface OptionTransfer<P extends JPanel> {
	public abstract Transfer<?,?> transfer(P params);
	public abstract P control(Holder app);
	
	public class Echo implements OptionTransfer<JPanel> {
		public static String NAME = "Echo (*)"; //Static so it can be tested for; non-final so it can be changed in some cases
		@Override public JPanel control(Holder app) {return null;}
		@Override public Transfer<Object, Object> transfer(JPanel p) {return new General.Echo<>(null);}		
		@Override public String toString() {return NAME;}
	}

	public class Gradient implements OptionTransfer<JPanel> {
		@Override public Transfer<Object, Color> transfer(JPanel p) {return new Debug.Gradient();}
		@Override public String toString() {return "Gradient (color)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	} 
	
	public class RedWhiteLinear implements OptionTransfer<JPanel> {
		@Override 
		public Transfer<Number,Color> transfer(JPanel p) {
			return new Numbers.Interpolate<>(new Color(255,0,0,38), Color.red);
		}
		
		@Override public String toString() {return "Red luminance linear (int)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	}
	
	public class RedWhiteLog implements OptionTransfer<JPanel> {
		@Override 
		public Transfer<Number,Color> transfer(JPanel p) {
			return new Seq<Number, Double, Color>(
					new General.ValuerTransfer<>(new MathValuers.Log<>(10, false, true), 0d), 
					new Numbers.Interpolate<Double>(new Color(255,0,0,38), Color.red, Color.white));
		}
		
		@Override public String toString() {return "Red luminance log-10 (int)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	}
	
	public class FixedAlpha implements OptionTransfer<JPanel> {
		@Override 
		public Transfer<Number,Color> transfer(JPanel p) {
			return new Numbers.FixedInterpolate<>(Color.white, Color.red, 0, 25.5);
		}
		
		@Override public String toString() {return "10% Alpha (int)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	}
	
	public class FixedAlphaB implements OptionTransfer<JPanel> {
		@Override 
		public Transfer<Number,Color> transfer(JPanel p) {
			return new Numbers.FixedInterpolate<>(Color.white, Color.red, 0, 255);
		}
		
		@Override public String toString() {return "Min Alpha (int)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	}
	
	public class Present implements OptionTransfer<JPanel> {
		@Override 
		public Transfer<Integer,Color> transfer(JPanel p) {
			return new General.Present<Integer, Color>(Color.red, Color.white);
		}
		
		@Override public String toString() {return "Present (int)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	}
	
	public class Percent implements OptionTransfer<Percent.Controls> {
		@Override 
		public Transfer<CategoricalCounts<Color>,Color> transfer(Controls p) {
			int percent = (int) p.spinner.getValue();
			return new Categories.KeyPercent<Color>(percent/100d, Color.blue, Color.white, Color.blue, Color.red);
		}
		
		@Override 
		public Controls control(ARComponent.Holder app) {return new Controls();}
		@Override public String toString() {return "Split on Percent (RLE)";}
		
		private class Controls extends JPanel {
			public JSpinner spinner = new JSpinner(new SpinnerNumberModel(50, 1, 100,1));
			public Controls() {add(new LabeledItem("Percent:", spinner));}
		}
	}
	
	//TODO: REMOVE by providing a category-map-with-valuer transfer
	public class HighAlphaLog implements OptionTransfer<JPanel> {
		@Override 
		public Transfer<CategoricalCounts<Color>,Color> transfer(JPanel p) {
			return new Categories.HighAlpha(Color.white, .1, true);
		}
		
		@Override public String toString() {return "Log HD Alpha (RLE)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	}
	
	public class HighAlphaLin implements OptionTransfer<JPanel> {
		public Transfer<CategoricalCounts<Color>,Color> transfer(JPanel p) {return new Categories.HighAlpha(Color.white, .1, false);}
		@Override public String toString() {return "Linear HD Alpha (RLE)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	}
}
