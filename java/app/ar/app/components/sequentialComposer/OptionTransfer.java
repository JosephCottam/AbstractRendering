package ar.app.components.sequentialComposer;

import java.awt.Color;

import javax.swing.JPanel;

import ar.Transfer;
import ar.app.display.ARComponent;
import ar.app.display.ARComponent.Holder;
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.rules.CategoricalCounts;
import ar.rules.Categories;
import ar.rules.Debug;
import ar.rules.General;
import ar.rules.Numbers;
import ar.rules.combinators.Seq;

public interface OptionTransfer {
	public abstract Transfer<?,?> transfer();
	public abstract JPanel control(Holder app);
	
	public class Echo implements OptionTransfer {
		public static String NAME = "Echo (*)"; //Static so it can be tested for; non-final so it can be changed in some cases
		@Override public JPanel control(Holder app) {return null;}
		@Override public Transfer<Object, Object> transfer() {return new General.Echo<>(null);}		
		@Override public String toString() {return NAME;}
	}

	public class Gradient implements OptionTransfer {
		@Override public Transfer<Object, Color> transfer() {return new Debug.Gradient();}
		@Override public String toString() {return "Gradient (color)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	} 
	
	public class RedWhiteLinear implements OptionTransfer {
		@Override 
		public Transfer<Number,Color> transfer() {
			return new Numbers.Interpolate<>(new Color(255,0,0,38), Color.red);
		}
		
		@Override public String toString() {return "Red luminance linear (int)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	}
	
	public class RedWhiteLog implements OptionTransfer {
		@Override 
		public Transfer<Number,Color> transfer() {
			return new Seq<Number, Double, Color>(
					new General.ValuerTransfer<>(new MathValuers.Log<>(10, false, true), 0d), 
					new Numbers.Interpolate<Double>(new Color(255,0,0,38), Color.red, Color.white));
		}
		
		@Override public String toString() {return "Red luminance log-10 (int)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	}
	
	public class FixedAlpha implements OptionTransfer {
		@Override 
		public Transfer<Number,Color> transfer() {
			return new Numbers.FixedInterpolate<>(Color.white, Color.red, 0, 25.5);
		}
		
		@Override public String toString() {return "10% Alpha (int)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	}
	
	public class FixedAlphaB implements OptionTransfer {
		@Override 
		public Transfer<Number,Color> transfer() {
			return new Numbers.FixedInterpolate<>(Color.white, Color.red, 0, 255);
		}
		
		@Override public String toString() {return "Min Alpha (int)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	}
	
	public class Present implements OptionTransfer {
		@Override 
		public Transfer<Integer,Color> transfer() {
			return new General.Present<Integer, Color>(Color.red, Color.white);
		}
		
		@Override public String toString() {return "Present (int)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	}
	
	public class Percent90 implements OptionTransfer {
		@Override 
		public Transfer<CategoricalCounts<Color>,Color> transfer() {
			return new Categories.KeyPercent<Color>(.9, Color.blue, Color.white, Color.blue, Color.red);
		}
		
		@Override public String toString() {return "90% Percent (RLE)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	}

	public class Percent95 implements OptionTransfer {
		@Override 
		public Transfer<CategoricalCounts<Color>,Color> transfer() {
			return new Categories.KeyPercent<Color>(.95, Color.blue, Color.white, Color.blue, Color.red);
		}

		@Override public String toString() {return "95% Percent (RLE)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	}
	
	
	public class Percent25 implements OptionTransfer {
		@Override 
		public Transfer<CategoricalCounts<Color>,Color> transfer() {
			return new Categories.KeyPercent<Color>(.25, Color.blue, Color.white, Color.blue, Color.red);
		}
		
		@Override public String toString() {return "25% Percent (RLE)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	}
	
	//TODO: REMOVE!!!!!
	public class HighAlphaLog implements OptionTransfer {
		@Override 
		public Transfer<CategoricalCounts<Color>,Color> transfer() {
			return new Categories.HighAlpha(Color.white, .1, true);
		}
		
		@Override public String toString() {return "Log HD Alpha (RLE)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	}
	
	public class HighAlphaLin implements OptionTransfer {
		public Transfer<CategoricalCounts<Color>,Color> transfer() {return new Categories.HighAlpha(Color.white, .1, false);}
		@Override public String toString() {return "Linear HD Alpha (RLE)";}
		@Override public JPanel control(ARComponent.Holder app) {return null;}
	}
}
