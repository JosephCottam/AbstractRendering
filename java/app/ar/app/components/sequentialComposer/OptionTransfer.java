package ar.app.components.sequentialComposer;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
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

public interface OptionTransfer<P extends OptionTransfer.ControlPanel> {
	public abstract Transfer<?,?> transfer(P params);
	public abstract P control(Holder app);
	
	public static final class RefArgMathTransfer implements OptionTransfer<RefArgMathTransfer.Controls> {
		
		@Override
		@SuppressWarnings({"unchecked","rawtypes"})
		public Transfer<?,?> transfer(Controls params) {
			return new General.ValuerTransfer(params.valuer(), params.convert(0, params.returnType()));
		}

		@Override public Controls control(Holder app) {return new Controls();}
		@Override public String toString() {return "Math (Num->Num->Num)";}

		private static final class Controls extends ControlPanel {
			private JComboBox<Entry> valuers = new JComboBox<>();
			public JSpinner value = new JSpinner(new SpinnerNumberModel(0, Integer.MIN_VALUE, Integer.MAX_VALUE,5));
			
			public Controls() {
				valuers.addItem(new Entry<>(MathValuers.Log.class, 10d));
				valuers.addItem(new Entry<>(MathValuers.AddInt.class, 1));
				valuers.addItem(new Entry<>(MathValuers.AddDouble.class, 1d));
				valuers.addItem(new Entry<>(MathValuers.DivideDouble.class, 10d));
				valuers.addItem(new Entry<>(MathValuers.DivideInt.class, 10));
				valuers.addItem(new Entry<>(MathValuers.EQ.class, 0d));
				valuers.addItem(new Entry<>(MathValuers.GT.class, 0d));
				valuers.addItem(new Entry<>(MathValuers.GTE.class, 0d));
				valuers.addItem(new Entry<>(MathValuers.LT.class, 10d));
				valuers.addItem(new Entry<>(MathValuers.LTE.class, 10d));
				valuers.addItem(new Entry<>(MathValuers.MultiplyDouble.class, 10d));
				valuers.addItem(new Entry<>(MathValuers.MultiplyInt.class, 10));
				valuers.addItem(new Entry<>(MathValuers.SubtractDouble.class, 1d));
				valuers.addItem(new Entry<>(MathValuers.SubtractInt.class, 1));
				
				
				//this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
				this.setLayout(new GridLayout(1,0));
				this.add(new LabeledItem("Operation:", valuers));
				this.add(new LabeledItem("Ref Arg:", value));

				value.addChangeListener(actionProvider.changeDelegate());
				valuers.addActionListener(actionProvider.actionDelegate());
				valuers.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						Controls.this.value.setValue(valuers.getItemAt(valuers.getSelectedIndex()).defVal);
					}
					
				});				
				
			}
			
			private static final class Entry<A extends Valuer<?,?>> {
				public final Class<A> valuerClass;
				public final Number defVal;
				public Entry(Class<A> valuerClass, Number defVal) {
					this.valuerClass = valuerClass;
					this.defVal = defVal;
				}
				public String toString() {return valuerClass.getSimpleName();}
			}
			
			public Valuer<?,?> valuer() {
				Entry e = valuers.getItemAt(valuers.getSelectedIndex());
				try {
					Constructor c = e.valuerClass.getConstructor(e.defVal.getClass());
					Valuer v = (Valuer) c.newInstance(convert((Number) value.getValue(), e.defVal.getClass()));
					return v;
				} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
					throw new IllegalArgumentException("Error constructing valuer " + e.valuerClass.getSimpleName(), e1);
				}
			}

			public Class returnType() {
				Class<?> t;
				try {
					t = valuer().getClass().getMethod("value", Number.class).getReturnType();
				} catch (NoSuchMethodException | SecurityException e) {
					throw new UnsupportedOperationException("Error construct zero for selected operation: " + valuer(),e);
				}
				return t;
			}
			
			public Number convert(Number v, Class t) {
				if (t.equals(Double.class)) {return v.doubleValue();}
				if (t.equals(Integer.class)) {return v.intValue();}
				if (t.equals(Float.class)) {return v.floatValue();}
				if (t.equals(Long.class)) {return v.longValue();}
				if (t.equals(Short.class)) {return v.shortValue();}
				throw new UnsupportedOperationException("Could not construct zero for selected operation: " + valuer());
			}
		}
	}
	
	public static final class OneArgMathTransfer implements OptionTransfer<OneArgMathTransfer.Controls> {
		
		@Override
		@SuppressWarnings({"unchecked","rawtypes"})
		public Transfer<?,?> transfer(Controls params) {
			return new General.ValuerTransfer(params.valuer(), params.zero());
		}

		@Override public Controls control(Holder app) {return new Controls();}
		@Override public String toString() {return "Math (Num->Num)";}

		private static final class Controls extends ControlPanel {
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
	
	public static final class HDInterpolate implements OptionTransfer<HDInterpolate.Controls> {
		@Override 
		public Transfer<Number,Color> transfer(Controls p) {
			return new Numbers.Interpolate<>(p.low.color(), p.high.color());
		}
		
		@Override public String toString() {return "HD Interpolate (Num->Color))";}
		@Override public Controls control(Holder app) {return new Controls();}
		
		private static class Controls extends ControlPanel {
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
		
	public static final class FixedInterpolate implements OptionTransfer<FixedInterpolate.Controls> {
		@Override 
		public Transfer<Number,Color> transfer(Controls p) {
			return new Numbers.FixedInterpolate<>(p.lowColor.color(), p.highColor.color(), ((int) p.low.getValue()), ((int) p.high.getValue()));
		}
		
		@Override public String toString() {return "Fixed Interpolate (Num->Color)";}
		@Override public Controls control(Holder app) {return new Controls();}
		
		private static class Controls extends ControlPanel {
			public JSpinner low = new JSpinner(new SpinnerNumberModel(0, Integer.MIN_VALUE, Integer.MAX_VALUE,5));
			public JSpinner high = new JSpinner(new SpinnerNumberModel(255, Integer.MIN_VALUE, Integer.MAX_VALUE,5));
			public ColorChooser lowColor = new ColorChooser(Color.white, "Low");
			public ColorChooser highColor = new ColorChooser(Color.red, "High");
			public Controls() {
				super("FixedAlpha");
				this.setLayout(new GridLayout(1,0));
				add(lowColor);
				add(highColor);
				add(new LabeledItem("Start:", low));
				add(new LabeledItem("End:", high));
				low.addChangeListener(actionProvider.changeDelegate());
				high.addChangeListener(actionProvider.changeDelegate());
				lowColor.addActionListener(actionProvider.actionDelegate());
				highColor.addActionListener(actionProvider.actionDelegate());
			}
		}
	}
	
	public static final class Present implements OptionTransfer<ControlPanel> {
		@Override 
		public Transfer<Integer,Color> transfer(ControlPanel p) {
			return new General.Present<Integer, Color>(Color.red, Color.white);
		}
		
		@Override public String toString() {return "Present (*)";}
		@Override public ControlPanel control(Holder app) {return new ControlPanel();}
	}
	
	public static final class Percent implements OptionTransfer<Percent.Controls> {
		@Override 
		public Transfer<CategoricalCounts<Color>,Color> transfer(Controls p) {
			int percent = (int) p.spinner.getValue();
			return new Categories.KeyPercent<Color>(
					percent/100d, 
					Color.blue, 
					Color.white, 
					p.aboveColor.color(), 
					p.belowColor.color());
		}
		
		@Override 
		public Controls control(ARComponent.Holder app) {return new Controls();}
		@Override public String toString() {return "Split on Percent (RLE)";}
		
		private static class Controls extends ControlPanel {
			public JSpinner spinner = new JSpinner(new SpinnerNumberModel(50, 0, 100,1));
			public ColorChooser aboveColor = new ColorChooser(Color.white, "Low");
			public ColorChooser belowColor = new ColorChooser(Color.red, "High");

			public Controls() {
				super("Percent");
				add(new LabeledItem("Percent:", spinner));
				add(new LabeledItem("Above:", aboveColor));
				add(new LabeledItem("Below:", belowColor));
				
				spinner.addChangeListener(actionProvider.changeDelegate());
				aboveColor.addActionListener(actionProvider.actionDelegate());
				belowColor.addActionListener(actionProvider.actionDelegate());
			}
		}
	}
	
	public static final class Echo implements OptionTransfer<ControlPanel> {
		public static final String NAME = "Echo (*)"; 
		@Override public Transfer<Object, Object> transfer(ControlPanel p) {return new General.Echo<>(null);}		
		@Override public String toString() {return NAME;}
		@Override public ControlPanel control(Holder app) {return new ControlPanel();}
	}

	public static final class Gradient implements OptionTransfer<ControlPanel> {
		@Override public Transfer<Object, Color> transfer(ControlPanel p) {return new Debug.Gradient();}
		@Override public String toString() {return "Gradient (color)";}
		@Override public ControlPanel control(Holder app) {return new ControlPanel();}
	} 

	//TODO: REMOVE the log option from Categories.HighAlpha by providing a category-map-with-valuer transfer
	public static final class HighAlphaLog implements OptionTransfer<ControlPanel> {
		@Override 
		public Transfer<CategoricalCounts<Color>,Color> transfer(ControlPanel p) {
			return new Categories.HighAlpha(Color.white, .1, true);
		}
		
		@Override public String toString() {return "Log HD Alpha (RLE)";}
		@Override public ControlPanel control(Holder app) {return new ControlPanel();}
	}
	
	public static final class HighAlphaLin implements OptionTransfer<ControlPanel> {
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
