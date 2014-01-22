package ar.app.components.sequentialComposer;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import ar.Aggregator;
import ar.Transfer;
import ar.app.components.LabeledItem;
import ar.app.util.ActionProvider;
import ar.app.util.AppUtil;
import ar.app.util.ColorChooser;
import ar.app.util.SimpleNameRenderer;
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.rules.CategoricalCounts;
import ar.rules.Categories;
import ar.rules.Debug;
import ar.rules.General;
import ar.rules.General.Spread.Spreader;
import ar.rules.Numbers;
import ar.util.Util;

public interface OptionTransfer<P extends OptionTransfer.ControlPanel> {
	public abstract Transfer<?,?> transfer(P params);
	public abstract P control(SequentialComposer composer);
	
	public static final class ToCount implements OptionTransfer<ControlPanel> {

		@Override
		@SuppressWarnings("rawtypes")
		public Transfer<?,?> transfer(
				ar.app.components.sequentialComposer.OptionTransfer.ControlPanel params) {
			return new Categories.ToCount();
		}

		@Override
		public ar.app.components.sequentialComposer.OptionTransfer.ControlPanel control(SequentialComposer composer) {
			return new ControlPanel();
		}
		
		@Override public String toString() {return "To Counts (CoC->Int)";}
		@Override public boolean equals(Object other) {return other!=null && this.getClass().equals(other.getClass());}
	}
	
	public static final class RefArgMathTransfer implements OptionTransfer<RefArgMathTransfer.Controls> {
		
		@Override
		@SuppressWarnings({"unchecked","rawtypes"})
		public Transfer<?,?> transfer(Controls params) {
			return new General.ValuerTransfer(params.valuer(), Controls.convert(0, params.returnType()));
		}

		@Override public Controls control(SequentialComposer composer) {return new Controls();}
		@Override public String toString() {return "Math (Num->Num->Num)";}
		@Override public boolean equals(Object other) {return other!=null && this.getClass().equals(other.getClass());}


		private static final class Controls extends ControlPanel {
			private JComboBox<Entry<?>> valuers = new JComboBox<>();
			public JSpinner value = new JSpinner(new SpinnerNumberModel(0, Integer.MIN_VALUE, Integer.MAX_VALUE,1));
			
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
				
				
				this.setLayout(new GridLayout(1,0));
				this.add(new LabeledItem("Operation:", valuers));
				this.add(new LabeledItem("Ref Arg:", value));

				value.addChangeListener(actionProvider.changeDelegate());
				valuers.addActionListener(actionProvider.actionDelegate());

				valuers.addItemListener(new TransferDefaultRef());
				valuers.addActionListener(new TransferDefaultRef());
				valuers.setSelectedIndex(0);
				
			}
			
			public Valuer<?,?> valuer() {
				Entry<?> e = valuers.getItemAt(valuers.getSelectedIndex());
				try {
					Constructor<?> c = e.valuerClass.getConstructor(e.refVal.getClass());
					Valuer<?,?> v = (Valuer<?,?>) c.newInstance(convert((Number) value.getValue(), e.refVal.getClass()));
					return v;
				} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
					throw new IllegalArgumentException("Error constructing valuer " + e.valuerClass.getSimpleName(), e1);
				}
			}

			public Class<?> returnType() {
				Class<?> t;
				try {
					t = valuer().getClass().getMethod("value", Number.class).getReturnType();
				} catch (NoSuchMethodException | SecurityException e) {
					throw new UnsupportedOperationException("Error construct zero for selected operation: " + valuer(),e);
				}
				return t;
			}
			
			@SuppressWarnings("unchecked")
			public static <T> T convert(Number v, Class<T> t) {
				if (t.equals(Double.class)) {return (T) (Double) v.doubleValue();}
				if (t.equals(Integer.class)) {return (T) (Integer) v.intValue();}
				if (t.equals(Float.class)) {return (T) (Float) v.floatValue();}
				if (t.equals(Long.class)) {return (T) (Long) v.longValue();}
				if (t.equals(Short.class)) {return (T) (Short) v.shortValue();}
				if (t.equals(Boolean.class)) {return (T) Boolean.FALSE;} 
				throw new UnsupportedOperationException("Could not construct zero for selected operation.  Requested zero for type: " + t.getSimpleName());
			}

			private final class TransferDefaultRef implements ItemListener, ActionListener {
				public void itemStateChanged(ItemEvent e) {transfer();}
				public void actionPerformed(ActionEvent e) {transfer();}
				public void transfer() {Controls.this.value.setValue(valuers.getItemAt(valuers.getSelectedIndex()).refVal);}
			}
			
			private static final class Entry<A extends Valuer<?,?>> {
				public final Class<A> valuerClass;
				public final Number refVal;
				
				public Entry(Class<A> valuerClass, Number defVal) {
					this.valuerClass = valuerClass;
					this.refVal = defVal;
				}

				public String toString() {return valuerClass.getSimpleName();}
			}
		}
	}
	
	public static final class OneArgMathTransfer implements OptionTransfer<OneArgMathTransfer.Controls> {
		
		@Override
		@SuppressWarnings({"unchecked","rawtypes"})
		public Transfer<?,?> transfer(Controls params) {
			return new General.ValuerTransfer(params.valuer(), params.zero());
		}

		@Override public Controls control(SequentialComposer composer) {return new Controls();}
		@Override public String toString() {return "Math (Num->Num)";}
		@Override public boolean equals(Object other) {return other!=null && this.getClass().equals(other.getClass());}

		private static final class Controls extends ControlPanel {
			private JComboBox<Valuer<?,?>> valuers = new JComboBox<>();
			
			public Controls() {
				
				AppUtil.loadInstances(valuers, MathValuers.class, Valuer.class, null); 
				valuers.setRenderer(new SimpleNameRenderer<Valuer<?,?>>());
				this.add(new LabeledItem("Operation:", valuers));
				valuers.addActionListener(actionProvider.actionDelegate());
				
				for (int i=0; i<valuers.getItemCount(); i++) {
					Valuer<?,?> item = valuers.getItemAt(i);
					if (item instanceof MathValuers.CubeRoot) {valuers.setSelectedIndex(i); break;}
				}		
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
		@Override public Controls control(SequentialComposer composer) {return new Controls();}
		@Override public boolean equals(Object other) {return other!=null && this.getClass().equals(other.getClass());}
		
		private static class Controls extends ControlPanel {
			public ColorChooser low = new ColorChooser(new Color(255,204,204), "Low");
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
		@Override public Controls control(SequentialComposer composer) {return new Controls();}
		@Override public boolean equals(Object other) {return other!=null && this.getClass().equals(other.getClass());}
		
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
		
		@Override public Controls control(SequentialComposer composer) {return new Controls();}
		@Override public String toString() {return "Split on Percent (CoC)";}
		@Override public boolean equals(Object other) {return other!=null && this.getClass().equals(other.getClass());}
		
		private static class Controls extends ControlPanel {
			public JSpinner spinner = new JSpinner(new SpinnerNumberModel(50, 0, 100,1));
			public ColorChooser aboveColor = new ColorChooser(Color.blue, "Above");
			public ColorChooser belowColor = new ColorChooser(Color.red, "Below");

			public Controls() {
				super("Percent");
				this.setLayout(new GridLayout(1,0));
				add(new LabeledItem("Percent:", spinner));
				add(aboveColor);
				add(belowColor);
				
				spinner.addChangeListener(actionProvider.changeDelegate());
				aboveColor.addActionListener(actionProvider.actionDelegate());
				belowColor.addActionListener(actionProvider.actionDelegate());
			}
		}
	}
	
	public static final class Spread implements OptionTransfer<Spread.Controls> {
		@Override
		public Transfer<?, ?> transfer(Controls params) {
			return new General.Spread(params.spreader(), params.combiner());
		}

		@Override public String toString() {return "Spread (*->*)";}
		@Override public boolean equals(Object other) {return other!=null && this.getClass().equals(other.getClass());}
		@Override public Controls control(SequentialComposer composer) {return new Controls(composer);}

		public static final class Controls extends ControlPanel {
			private final JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 0, 50,1));
			private final JComboBox<String> combiner = new JComboBox<>();
			private final SequentialComposer composer;
			
			public Controls(SequentialComposer composer) {
				super("spread");
				this.composer = composer;
				
				combiner.addItem("Auto");
				combiner.addItem("Num");
				combiner.addItem("Categories");
				
				add(new LabeledItem("Radius:", spinner));
				add(new LabeledItem("Combiner:", combiner));
				
				spinner.addChangeListener(actionProvider.changeDelegate());
				combiner.addActionListener(actionProvider.actionDelegate());
			}
			
			public Spreader spreader() {return new General.Spread.UnitSquare<Integer>(radius());}
			public int radius() {return (int) spinner.getValue();}
			public Aggregator combiner() {
				String selected = combiner.getItemAt(combiner.getSelectedIndex());
				if (selected.equals("Auto")) {
					Object o = composer.aggregator().identity();	//TODO: This doesn't work if a transfer changed the aggregates type... 
					if (o instanceof Number) {selected = "Num";}
					else if (o instanceof CategoricalCounts) {selected="Categories";}
					else {selected = o.getClass().getSimpleName();}
				}
				
				if (selected.equals("Num")) {
					return new Numbers.Count<Integer>();					
				} else if (selected.equals("Categories")) {
					return new Categories.MergeCategories<Color>();
				} else {
					throw new IllegalArgumentException("Cannot create combiner for " + selected);
				}
			}
		}
	}
	
	public static final class ColorKey implements OptionTransfer<ColorKey.Controls> {
		@Override
		public Transfer<?, ?> transfer(Controls params) {
			return new Categories.DynamicRekey(new CategoricalCounts<>(Util.COLOR_SORTER), params.palette(), params.reserve());
		}

		@Override public String toString() {return "Color Keys (CoC<*>->CoC<Color>)";}
		@Override public boolean equals(Object other) {return other!=null && this.getClass().equals(other.getClass());}
		@Override public Controls control(SequentialComposer composer) {return new Controls();}

		
		public static final class Controls extends ControlPanel {
			private Entry brewerColors = 
					new Entry(
							"Brewer 12",
							Color.black,
					new Color[]{//A set of default Colors, taken from colorbrewer but re-ordered
					new Color(166,206,227), new Color(31,120,180),
					new Color(178,223,138), new Color(51,160,44),
					new Color(251,154,153), new Color(227,26,28),					
					new Color(253,191,111), new Color(255,127,0),
					new Color(202,178,214), new Color(106,61,154), 
					new Color(255,255,153), new Color(177,89,40) 
			});
			
			private final Entry cableColors = 
					new Entry(
							"Cable",
							new Color(136,90,68),
					new Color[] {//Taken from Dustin Cable's racial dot-map http://www.coopercenter.org/demographics/Racial-Dot-Map
					new Color(0,0,200),
					new Color(0,200,0),
					new Color(136,90,68),
					new Color(255,69,0),
					
			});
			
			private final Entry redBlue = new Entry("Red/Blue", Color.white, new Color[]{new Color(255,0,0,25), Color.red}); 

			private final JComboBox<Entry> palette = new JComboBox<>();
			
			public Controls() {
				this.add(new LabeledItem("Palette:", palette));
				
				palette.addItem(cableColors);
				palette.addItem(brewerColors);
				palette.addItem(redBlue);
				
				palette.addActionListener(actionProvider.actionDelegate());
			}
			
			public List<Color> palette() {return Arrays.asList(selected().colors);}
			public Color reserve() {return selected().reserve;}
			private Entry selected() {return palette.getItemAt(palette.getSelectedIndex());}
			
			private static final class Entry {
				public Color[] colors;
				public Color reserve;
				public String name;
				public Entry(String name, Color reserve, Color[] colors) {
					this.colors = colors;
					this.name = name;
					this.reserve = reserve;
				}
				public String toString() {return name;}
			}
		}
	}

	
	public static final class Present implements OptionTransfer<ControlPanel> {
		@Override 
		public Transfer<Integer,Color> transfer(ControlPanel p) {
			return new General.Present<Integer, Color>(Color.red, Color.white);
		}
		
		@Override public String toString() {return "Present (*)";}
		@Override public ControlPanel control(SequentialComposer composer) {return new ControlPanel();}
		@Override public boolean equals(Object other) {return other!=null && this.getClass().equals(other.getClass());}
	}

	
	public static final class Echo implements OptionTransfer<ControlPanel> {
		public static final String NAME = "Echo (*)"; 
		@Override public Transfer<Object, Object> transfer(ControlPanel p) {return new General.Echo<>(null);}		
		@Override public String toString() {return NAME;}
		@Override public ControlPanel control(SequentialComposer composer) {return new ControlPanel();}
		@Override public boolean equals(Object other) {return other!=null && this.getClass().equals(other.getClass());}
	}

	public static final class Gradient implements OptionTransfer<ControlPanel> {
		@Override public Transfer<Object, Color> transfer(ControlPanel p) {return new Debug.Gradient();}
		@Override public String toString() {return "Gradient (color)";}
		@Override public ControlPanel control(SequentialComposer composer) {return new ControlPanel();}
		@Override public boolean equals(Object other) {return other!=null && this.getClass().equals(other.getClass());}
	} 

	//TODO: REMOVE the log option from Categories.HighAlpha by providing a category-map-with-valuer transfer
	public static final class HighAlphaLog implements OptionTransfer<ControlPanel> {
		@Override 
		public Transfer<CategoricalCounts<Color>,Color> transfer(ControlPanel p) {
			return new Categories.HighDefAlpha(Color.white, .1, true);
		}
		
		@Override public String toString() {return "Log HD Alpha (CoC)";}
		@Override public ControlPanel control(SequentialComposer composer) {return new ControlPanel();}
		@Override public boolean equals(Object other) {return other!=null && this.getClass().equals(other.getClass());}
	}
	
	public static final class HighAlphaLin implements OptionTransfer<ControlPanel> {
		public Transfer<CategoricalCounts<Color>,Color> transfer(ControlPanel p) {return new Categories.HighDefAlpha(Color.white, .1, false);}
		@Override public String toString() {return "Linear HD Alpha (CoC)";}
		@Override public ControlPanel control(SequentialComposer composer) {return new ControlPanel();}
		@Override public boolean equals(Object other) {return other!=null && this.getClass().equals(other.getClass());}
	}
	
	
	public static class ControlPanel extends JPanel {
		protected final ActionProvider actionProvider;
		
		public ControlPanel() {this("");}
		public ControlPanel(String id) {actionProvider = new ActionProvider(id);}
		public void addActionListener(ActionListener listener) {actionProvider.addActionListener(listener);}
		@Override public boolean equals(Object other) {return other!=null && this.getClass().equals(other.getClass());}
	}
}
