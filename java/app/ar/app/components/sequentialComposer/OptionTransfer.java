package ar.app.components.sequentialComposer;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;

import ar.Transfer;
import ar.app.components.ColorChooser;
import ar.app.components.LabeledItem;
import ar.app.display.ARComponent;
import ar.app.display.ARComponent.Holder;
import ar.app.util.ActionProvider;
import ar.app.util.AppUtil;
import ar.app.util.SimpleNameRenderer;
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.rules.CategoricalCounts;
import ar.rules.Categories;
import ar.rules.Debug;
import ar.rules.General;
import ar.rules.Numbers;
import ar.rules.combinators.Seq;

public interface OptionTransfer<P extends OptionTransfer.ControlPanel> {
	public abstract Transfer<?,?> transfer(P params);
	public abstract P control(Holder app);
	
	public class Echo implements OptionTransfer<ControlPanel> {
		public static final String NAME = "Echo (*)"; 
		@Override public ControlPanel control(Holder app) {return new ControlPanel();}
		@Override public Transfer<Object, Object> transfer(ControlPanel p) {return new General.Echo<>(null);}		
		@Override public String toString() {return NAME;}
	}

	public class Gradient implements OptionTransfer<ControlPanel> {
		@Override public Transfer<Object, Color> transfer(ControlPanel p) {return new Debug.Gradient();}
		@Override public String toString() {return "Gradient (color)";}
		@Override public ControlPanel control(Holder app) {return new ControlPanel();}
	} 
	
	public class RedWhiteLinear implements OptionTransfer<ControlPanel> {
		@Override 
		public Transfer<Number,Color> transfer(ControlPanel p) {
			return new Numbers.Interpolate<>(new Color(255,0,0,38), Color.red);
		}
		
		@Override public String toString() {return "Red luminance linear (int)";}
		@Override public ControlPanel control(Holder app) {return new ControlPanel();}
	}
	
	public class RedWhiteLog implements OptionTransfer<ControlPanel> {
		@Override 
		public Transfer<Number,Color> transfer(ControlPanel p) {
			return new Seq<Number, Double, Color>(
					new General.ValuerTransfer<>(new MathValuers.Log<>(10, false, true), 0d), 
					new Numbers.Interpolate<Double>(new Color(255,0,0,38), Color.red, Color.white));
		}
		
		@Override public String toString() {return "Red luminance log-10 (int)";}
		@Override public ControlPanel control(Holder app) {return new ControlPanel();}
	}
	
	public class MathTransfer implements OptionTransfer<MathTransfer.Controls> {
		
		@Override
		public Transfer transfer(Controls params) {
			return new General.ValuerTransfer(params.valuer(), params.zero());
		}

		@Override public Controls control(Holder app) {return new Controls();}
		@Override public String toString() {return "Single-argument Math";}

		private final class Controls extends ControlPanel {
			private JComboBox<Valuer<?,?>> valuers = new JComboBox<>();
			
			public Controls() {
				
				AppUtil.loadInstances(valuers, MathValuers.class, Valuer.class, "Sqrt"); //TODO: The default isn't working right...
				valuers.setRenderer(new SimpleNameRenderer<Valuer<?,?>>());
				this.add(new LabeledItem("Operation:", valuers));
				valuers.addActionListener(actionProvider.actionDelegate());
			}
			
			public Valuer<?,?> valuer() {return valuers.getItemAt(valuers.getSelectedIndex());}
		
			public Number zero() {
				Class<?> t;
				try {
					t = valuer().getClass().getMethod("value", Number.class).getReturnType();
				} catch (NoSuchMethodException | SecurityException e) {
					throw new UnsupportedOperationException("Error construct zero for selected operation: " + valuer(),e);
				}
				
				if (t.equals(Double.class)) {return new Double(0d);}
				if (t.equals(Integer.class)) {return new Integer(0);}
				if (t.equals(Float.class)) {return new Float(0d);}
				if (t.equals(Long.class)) {return new Long(0);}
				if (t.equals(Short.class)) {return new Short((short) 0);}
				throw new UnsupportedOperationException("Could not construct zero for selected operation: " + valuer());
			}
		}

	}
	
	public class HDInterpolate implements OptionTransfer<HDInterpolate.Controls> {
		@Override 
		public Transfer<Number,Color> transfer(Controls p) {
			return new Numbers.Interpolate<>(p.low.color(), p.high.color());
		}
		
		@Override public String toString() {return "HD Interpolate (Number)";}
		@Override public Controls control(Holder app) {return new Controls();}
		
		private class Controls extends ControlPanel {
			public ColorChooser low = new ColorChooser(Color.white, "Low");
			public ColorChooser high = new ColorChooser(Color.red, "High");
			public Controls() {
				super("FixedAlpha");
				this.setLayout(new GridLayout(1,0));
				add(low);
				add(high);
				low.addActionListener(actionProvider.actionDelegate());
				high.addActionListener(actionProvider.actionDelegate());
			}
		}
	}

	
	public class FixedInterpolate implements OptionTransfer<FixedInterpolate.Controls> {
		@Override 
		public Transfer<Number,Color> transfer(Controls p) {
			return new Numbers.FixedInterpolate<>(p.low.color(), p.high.color(), 0, ((int) p.spinner.getValue()));
		}
		
		@Override public String toString() {return "Fixed Interpolate (Number)";}
		@Override public Controls control(Holder app) {return new Controls();}
		
		private class Controls extends ControlPanel {
			public JSpinner spinner = new JSpinner(new SpinnerNumberModel(255, 1, Integer.MAX_VALUE,5));
			public ColorChooser low = new ColorChooser(Color.white, "Low");
			public ColorChooser high = new ColorChooser(Color.red, "High");
			public Controls() {
				super("FixedAlpha");
				this.setLayout(new GridLayout(1,0));
				add(low);
				add(high);
				add(new LabeledItem("Steps:", spinner));
				spinner.addChangeListener(actionProvider.changeDelegate());
				low.addActionListener(actionProvider.actionDelegate());
				high.addActionListener(actionProvider.actionDelegate());
			}
		}
	}
	
	public class Present implements OptionTransfer<ControlPanel> {
		@Override 
		public Transfer<Integer,Color> transfer(ControlPanel p) {
			return new General.Present<Integer, Color>(Color.red, Color.white);
		}
		
		@Override public String toString() {return "Present (int)";}
		@Override public ControlPanel control(Holder app) {return new ControlPanel();}
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
		
		private class Controls extends ControlPanel {
			public JSpinner spinner = new JSpinner(new SpinnerNumberModel(50, 0, 100,1));
			public Controls() {
				super("Percent");
				add(new LabeledItem("Percent:", spinner));
				spinner.addChangeListener(actionProvider.changeDelegate());
			}
		}
	}
	
	//TODO: REMOVE by providing a category-map-with-valuer transfer
	public class HighAlphaLog implements OptionTransfer<ControlPanel> {
		@Override 
		public Transfer<CategoricalCounts<Color>,Color> transfer(ControlPanel p) {
			return new Categories.HighAlpha(Color.white, .1, true);
		}
		
		@Override public String toString() {return "Log HD Alpha (RLE)";}
		@Override public ControlPanel control(Holder app) {return new ControlPanel();}
	}
	
	public class HighAlphaLin implements OptionTransfer<ControlPanel> {
		public Transfer<CategoricalCounts<Color>,Color> transfer(ControlPanel p) {return new Categories.HighAlpha(Color.white, .1, false);}
		@Override public String toString() {return "Linear HD Alpha (RLE)";}
		@Override public ControlPanel control(Holder app) {return new ControlPanel();}
	}
	
	public static class ControlPanel extends JPanel {
		protected final ActionProvider actionProvider;
		
		public ControlPanel() {this("");}
		public ControlPanel(String id) {actionProvider = new ActionProvider(id);}
		public void addActionListener(ActionListener listener) {actionProvider.addActionListener(listener);}
	}
}
