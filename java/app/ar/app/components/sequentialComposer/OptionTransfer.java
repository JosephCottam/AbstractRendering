package ar.app.components.sequentialComposer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ar.Aggregates;
import ar.Aggregator;
import ar.Renderer;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.app.util.ActionProvider;
import ar.app.util.ColorChooser;
import ar.app.util.GeoJSONTools;
import ar.app.util.LabeledItem;
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.rules.Advise;
import ar.rules.CategoricalCounts;
import ar.rules.Categories;
import ar.rules.Debug;
import ar.rules.General;
import ar.rules.ISOContours;
import ar.rules.Legend;
import ar.rules.SeamCarving;
import ar.rules.Legend.Formatter;
import ar.rules.SeamCarving.Delta;
import ar.rules.SeamCarving.Direction;
import ar.rules.Shapes;
import ar.rules.General.Spread.Spreader;
import ar.rules.Numbers;
import ar.rules.combinators.Fan;
import ar.rules.combinators.NTimes;
import ar.rules.combinators.Seq;
import ar.util.HasViewTransform;
import ar.util.Util;

@SuppressWarnings({"rawtypes", "unchecked", "unused"})
public abstract class OptionTransfer<P extends OptionTransfer.ControlPanel> {
	
	/**Create a new transfer that is based on passed parameters and subsequent transfer.
	 * 
	 * In most cases, the new transfer is a sequence.  Sometimes, it will be an 'inner' transfer
	 * instead.  The new transfer should not discard the old transfer.  
	 * 
	 * @param params Control panel with parameters for a new transfer 
	 *  @param subsequent Transfer functions called AFTER the new one (may be null)
	 *  @return A new transfer that will combines the new and the 'subsequent' in some way.
	 */
	public abstract Transfer<?,?> transfer(P params, Transfer<?,?> subsequent);
	
	/**Create a control panel to set/provide transfer-specific parameters.
	 * MUST be able to accept as arguments and still provide a reasonable set of defaults (to support non-interactive applications).
	 * 
	 * @param transformProvider
	 * @return
	 */
	public abstract P control(HasViewTransform transformProvider);
	
	/**Remove any resources that this transfer created.
	 * Called when the OptionTransfer object is being deleted. 
	 * **/
	public void cleanup() {}
	
	@Override public boolean equals(Object other) {return other!=null && this.getClass().equals(other.getClass());}
	@Override public final int hashCode() {return this.getClass().hashCode();}

	
	public static final class ToCount extends OptionTransfer<ControlPanel> {

		@Override
		public Transfer<?,?> transfer(ControlPanel params,Transfer subsequent) {
			return extend(new Categories.ToCount(), subsequent);
		}

		@Override
		public ControlPanel control(HasViewTransform transformProvider) {
			return new ControlPanel();
		}
		
		@Override public String toString() {return "To Counts (CoC->Int)";}
	}
	
	public static final class MathTransfer extends OptionTransfer<MathTransfer.Controls> {


		@Override
		public Transfer<?, ?> transfer(Controls params, Transfer<?, ?> subsequent) {
			Transfer t = new General.ValuerTransfer(params.valuer(), Controls.convert(0, params.returnType()));
			return extend(t, subsequent);
		}

		@Override public Controls control(HasViewTransform transformProvider) {return new Controls();}
		@Override public String toString() {return "Math (Num->Num)";}
		
		private static final class Controls extends ControlPanel {
			private JComboBox<Entry<?>> valuers = new JComboBox<>();
			public JSpinner value = new JSpinner(new SpinnerNumberModel(0, Integer.MIN_VALUE, Integer.MAX_VALUE,1));
			
			public Controls() {
				valuers.addItem(new Entry<>(MathValuers.Log.class, 10d));
				valuers.addItem(new Entry<>(MathValuers.CubeRoot.class, Double.NaN));
				valuers.addItem(new Entry<>(MathValuers.Sqrt.class, Double.NaN));

				valuers.addItem(new Entry<>("Abs", MathValuers.AbsDouble.class, Double.NaN));
//				valuers.addItem(new Entry<>(MathValuers.AbsFloat.class, Double.NaN));
//				valuers.addItem(new Entry<>(MathValuers.AbsInt.class, Double.NaN));
//				valuers.addItem(new Entry<>(MathValuers.AbsLong.class, Double.NaN));
				valuers.addItem(new Entry<>(MathValuers.Sin.class, Double.NaN));
				valuers.addItem(new Entry<>(MathValuers.Cos.class, Double.NaN));
				valuers.addItem(new Entry<>(MathValuers.Tan.class, Double.NaN));
				valuers.addItem(new Entry<>(MathValuers.Exp.class, Double.NaN));
				valuers.addItem(new Entry<>(MathValuers.Exponent.class, Double.NaN));
				valuers.addItem(new Entry<>(MathValuers.Floor.class, Double.NaN));
//				valuers.addItem(new Entry<>(MathValuers.RInt.class, Double.NaN));
				valuers.addItem(new Entry<>("Round", MathValuers.RoundDouble.class, Double.NaN));
//				valuers.addItem(new Entry<>(MathValuers.RoundFloat.class, Double.NaN));
				valuers.addItem(new Entry<>(MathValuers.Signum.class, Double.NaN));
				valuers.addItem(new Entry<>(MathValuers.ToDegrees.class, Double.NaN));
//				valuers.addItem(new Entry<>(MathValuers.ToDouble.class, Double.NaN));
//				valuers.addItem(new Entry<>(MathValuers.ToInteger.class, Double.NaN));
//				valuers.addItem(new Entry<>(MathValuers.ToLong.class, Double.NaN));
				valuers.addItem(new Entry<>(MathValuers.ToRadians.class, Double.NaN));
				
				valuers.addItem(new Entry<>("Add", MathValuers.AddDouble.class, 1d));
				valuers.addItem(new Entry<>("Sub", MathValuers.SubtractDouble.class, 1d));
				valuers.addItem(new Entry<>("Mult", MathValuers.MultiplyDouble.class, 10d));
				valuers.addItem(new Entry<>("Divide", MathValuers.DivideDouble.class, 10d));
//				valuers.addItem(new Entry<>(MathValuers.AddInt.class, 1));
//				valuers.addItem(new Entry<>(MathValuers.DivideInt.class, 10));
				valuers.addItem(new Entry<>(MathValuers.EQ.class, 0d));
				valuers.addItem(new Entry<>(MathValuers.GT.class, 0d));
				valuers.addItem(new Entry<>(MathValuers.LT.class, 10d));
//				valuers.addItem(new Entry<>(MathValuers.GTE.class, 0d));
//				valuers.addItem(new Entry<>(MathValuers.LTE.class, 10d));
//				valuers.addItem(new Entry<>(MathValuers.MultiplyInt.class, 10));
//				valuers.addItem(new Entry<>(MathValuers.SubtractInt.class, 1));
				
				
				this.setLayout(new GridLayout(1,0));
				this.add(new LabeledItem("Operation:", valuers));
				this.add(new LabeledItem("Ref Arg:", value));

				value.addChangeListener(actionProvider.changeDelegate());
				valuers.addActionListener(actionProvider.actionDelegate());
				valuers.addItemListener(new ItemListener() {
					@Override public void itemStateChanged(ItemEvent e) {
						Entry entry = valuers.getItemAt(valuers.getSelectedIndex());
						if (Double.isNaN(entry.refVal.doubleValue())) {
							value.setEnabled(false);
						} else {
							value.setEnabled(true);
						}
					}
					
				});

				valuers.addItemListener(new TransferDefaultRef());
				valuers.addActionListener(new TransferDefaultRef());
				valuers.setSelectedIndex(0);
				
			}
			
			public Valuer<?,?> valuer() {
				Entry<?> e = valuers.getItemAt(valuers.getSelectedIndex());
				try {
					if (Double.isNaN(e.refVal.doubleValue())) {
						  Constructor<?> c = e.valuerClass.getConstructor();
                          Valuer<?,?> v = (Valuer<?,?>) c.newInstance();
                          return v;

					} else {
						  Constructor<?> c = e.valuerClass.getConstructor(e.refVal.getClass());
                          Valuer<?,?> v = (Valuer<?,?>) c.newInstance(convert((Number) value.getValue(), e.refVal.getClass()));
                          return v;
					}
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
				public final String label;
				
				public Entry(Class<A> valuerClass, Number defVal) {this(valuerClass.getSimpleName(), valuerClass, defVal);}
				public Entry(String label, Class<A> valuerClass, Number defVal) {
					this.label = label;
					this.valuerClass = valuerClass;
					this.refVal = defVal;
				}

				public String toString() {return label;}
				
				@Override public int hashCode() {return valuerClass.hashCode();}
				@Override public boolean equals(Object other) { 
					return other != null 
							&& other instanceof Entry 
							&& valuerClass.equals(((Entry) other).valuerClass);
				}
				
			}
		}		
	}
	
	public static final class Interpolate extends OptionTransfer<Interpolate.Controls> {
		@Override 
		public Transfer<Number,Color> transfer(Controls p, Transfer subsequent) {
			if (p.highDef()) {
				return new Numbers.Interpolate<>(p.lowColor.color(), p.highColor.color());
			} else {
				return new Numbers.FixedInterpolate<>(p.lowColor.color(), p.highColor.color(), ((int) p.low.getValue()), ((int) p.high.getValue()));
			}
		}
		
		@Override public String toString() {return "Interpolate (Num->Color)";}
		@Override public Controls control(HasViewTransform transformProvider) {return new Controls();}
		
		private static class Controls extends ControlPanel {
			public JSpinner low = new JSpinner(new SpinnerNumberModel(0, Integer.MIN_VALUE, Integer.MAX_VALUE,5));
			public JSpinner high = new JSpinner(new SpinnerNumberModel(255, Integer.MIN_VALUE, Integer.MAX_VALUE,5));
			public ColorChooser lowColor = new ColorChooser(new Color(255,204,204), "Low");
			public ColorChooser highColor = new ColorChooser(Color.red, "High");
			public JCheckBox highDef = new JCheckBox("HD");
			
			public Controls() {
				super("Interpolate");
				this.setLayout(new GridLayout(1,0));
				add(lowColor);
				add(highColor);
				add(highDef);
				add(new LabeledItem("Start:", low));
				add(new LabeledItem("End:", high));
				low.addChangeListener(actionProvider.changeDelegate());
				high.addChangeListener(actionProvider.changeDelegate());
				lowColor.addActionListener(actionProvider.actionDelegate());
				highColor.addActionListener(actionProvider.actionDelegate());
				highDef.addActionListener(actionProvider.actionDelegate());
				
				highDef.addChangeListener(new ChangeListener() {
					@Override public void stateChanged(ChangeEvent e) {
						if (highDef.isSelected()) {
							low.setEnabled(false);
							high.setEnabled(false);
						} else {
							low.setEnabled(true);
							high.setEnabled(true);
						}
					}
				});
				
				highDef.setSelected(true);
			}
			
			public boolean highDef() {return highDef.isSelected();}
		}
	}
	
	public static final class Percent extends OptionTransfer<Percent.Controls> {
		@Override 
		public Transfer<CategoricalCounts<Color>,Color> transfer(Controls p, Transfer subsequent) {
			int percent = (int) p.spinner.getValue();
			Transfer t = new Categories.KeyPercent<Color>(
					percent/100d, 
					Color.blue, 
					Color.white, 
					p.aboveColor.color(), 
					p.belowColor.color());
			return extend(t, subsequent);
		}
		
		@Override public Controls control(HasViewTransform transformProvider) {return new Controls();}
		@Override public String toString() {return "Split on Percent (CoC)";}
		
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

	
	public static final class Seam extends OptionTransfer<Seam.Controls> {
		static final SeamCarving.Delta<Integer> delta = new SeamCarving.DeltaInteger();	//TODO: Generalize to auto-detect type...on specialization perhaps?  Similar to FlexSpread..

		@Override
		public Transfer<?, ?> transfer(Controls params, Transfer subsequent) {
			
			Transfer vt=null, ht=null;
			if (params.columns() >0) {vt = params.colCarver();}

			if (params.rows() >0) {ht = params.rowCarver();}
			
			//TODO: There are probably smarter things to do than just horizontal followed by vertical...
			if (vt == null && ht == null) {return subsequent;}
			if (vt == null && ht != null) {return extend(ht,subsequent);}
			if (vt != null && ht == null) {return extend(vt,subsequent);}
			Transfer t = extend(vt, subsequent);
			t = extend(ht, t);
			return t;
		}

		@Override public String toString() {return "Seam-Carve (int->int)";}
		@Override public Controls control(HasViewTransform transformProvider) {return new Controls();}

		public static final class Controls extends ControlPanel {
			private final JSpinner rows = new JSpinner(new SpinnerNumberModel(0, 0, 2000, 5));
			private final JSpinner cols = new JSpinner(new SpinnerNumberModel(0, 0, 2000, 5));
			private final JComboBox carver = new JComboBox();
			
			public Controls() {
				super("Seam-carve");
				//setLayout(new GridLayout(1,3));
				add(carver);
				add(new LabeledItem("Rows:", rows));
				add(new LabeledItem("Cols:", cols));
				
				carver.addItem("Sweep");
				carver.addItem("Two Sweeps");
				carver.addItem("Exactly N");
				carver.addItem("Incremental");
				
				rows.addChangeListener(actionProvider.changeDelegate());
				cols.addChangeListener(actionProvider.changeDelegate());
				carver.addActionListener(actionProvider.actionDelegate());
				
				
				
			}
			
			public int rows() {return (int) rows.getValue();}
			public int columns() {return (int) cols.getValue();}

			public Transfer<Integer,Integer> rowCarver() {return carver(Direction.H, rows());}
			public Transfer<Integer,Integer> colCarver() {return carver(Direction.V, columns());}

			private Transfer<Integer,Integer> carver(Direction d, int seams) {
				try {
					Constructor c = constructor();
					return (Transfer<Integer, Integer>) c.newInstance(delta, d, 0, seams);
				} catch (InstantiationException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException 
						| NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
					return new SeamCarving.CarveSweep(delta, d, 0, 0);
				} 
			}
			
			
			
			private Constructor constructor() throws NoSuchMethodException, SecurityException {
				Class[] params = {Delta.class, Direction.class, Object.class, int.class};
				
				if (carver.getSelectedItem().equals("Sweep")) {
					return SeamCarving.CarveSweep.class.getConstructor(params);
				} else if (carver.getSelectedItem().equals("Two Sweeps")) {
					return SeamCarving.CarveTwoSweeps.class.getConstructor(params);
				} else if (carver.getSelectedItem().equals("Exactly N")) {
					return SeamCarving.CarveSweepN.class.getConstructor(params);
				} else if (carver.getSelectedItem().equals("Incremental")) {
					return SeamCarving.CarveIncremental.class.getConstructor(params);
				} else {
					throw new IllegalArgumentException("Unknown carver selected: " + carver.getSelectedItem());
				}
			}
			
		}
	}

	public static final class Contour extends OptionTransfer<Contour.Controls> {
		@Override
		public Transfer<?, ?> transfer(Controls params, Transfer subsequent) {
			Transfer t = new ISOContours.NContours<>(params.contours(), params.fill());
			return extend(t, subsequent);
		}

		@Override public String toString() {return "Contour (Num->Num)";}
		@Override public Controls control(HasViewTransform transformProvider) {return new Controls();}

		public static final class Controls extends ControlPanel {
			private final JSpinner contours = new JSpinner(new SpinnerNumberModel(5, 0, 20, 1));
			private final JCheckBox fill =new JCheckBox("Fill");
			
			public Controls() {
				super("Seam-carve");				
				add(new LabeledItem
						("Contours:", contours));
				add(fill);
				contours.addChangeListener(actionProvider.changeDelegate());
				fill.addChangeListener(actionProvider.changeDelegate());
			}
			
			public int contours() {return (int) contours.getValue();}
			public boolean fill() {return fill.isSelected();}
			
		}
	}

	
	public static final class Spread extends OptionTransfer<Spread.Controls> {
		@Override
		public Transfer<?, ?> transfer(Controls params, Transfer subsequent) {
			Transfer t = new FlexSpread(params.spreader());
			return extend(t, subsequent);
		}

		@Override public String toString() {return "Spread (*->*)";}
		@Override public Controls control(HasViewTransform transformProvider) {return new Controls();}

		public static final class Controls extends ControlPanel {
			private final JSpinner up = new JSpinner(new SpinnerNumberModel(1, 0, 50,1));
			private final JSpinner down = new JSpinner(new SpinnerNumberModel(1, 0, 50,1));
			private final JSpinner left = new JSpinner(new SpinnerNumberModel(1, 0, 50,1));
			private final JSpinner right = new JSpinner(new SpinnerNumberModel(1, 0, 50,1));
			
			public Controls() {
				super("spread");				
				add(new LabeledItem("Up:", up));
				add(new LabeledItem("Down:", down));
				add(new LabeledItem("Left:", left));
				add(new LabeledItem("Right:", right));
				
				up.addChangeListener(actionProvider.changeDelegate());
				down.addChangeListener(actionProvider.changeDelegate());
				left.addChangeListener(actionProvider.changeDelegate());
				right.addChangeListener(actionProvider.changeDelegate());
			}
			
			public Spreader spreader() {return new General.Spread.UnitRectangle<Integer>(up(), down(), left(), right());}
			public int up() {return (int) up.getValue();}
			public int down() {return (int) down.getValue();}
			public int left() {return (int) left.getValue();}
			public int right() {return (int) right.getValue();}
		}
		
		public static class FlexSpread<V> implements Transfer<V,V> {
			final Aggregator[] combiners = new Aggregator[]{new Numbers.Count<Integer>(), new Categories.MergeCategories<Color>()};
			final Spreader<V> spreader;
			public FlexSpread(Spreader<V> spreader) {this.spreader = spreader;}
			
			@Override public V emptyValue() {throw new UnsupportedOperationException();}
			@Override public ar.Transfer.Specialized<V, V> specialize(Aggregates<? extends V> aggregates) {return new Specialized<>(spreader, aggregates);}

			
			public static class Specialized<V> extends FlexSpread<V> implements Transfer.Specialized<V,V> {
				final General.Spread<V> base;
				public Specialized(Spreader<V> spreader, Aggregates<? extends V> aggs) {
					super(spreader);
					Class<?> targetClass = aggs.defaultValue().getClass();
					Aggregator combiner = null;
					for (Aggregator a: combiners) {
						if (a.identity().getClass().isAssignableFrom(targetClass)) {
							combiner = a; break;
						}
					}					
					if (combiner == null) {throw new IllegalArgumentException("Could not match " + targetClass.getSimpleName() + " from provided aggregators.");} 
					base = new General.Spread<>(spreader, combiner);
				}

				@Override public V emptyValue() {return base.emptyValue();}
				
				@Override
				public Aggregates<V> process(Aggregates<? extends V> aggregates, Renderer rend) {return base.process(aggregates, rend);}
			}

		}
		
		
	}
	
	public static final class ColorKey extends OptionTransfer<ColorKey.Controls> {
		@Override
		public Transfer<?, ?> transfer(Controls params, Transfer subsequent) {
			Transfer t = new Categories.DynamicRekey(new CategoricalCounts<>(Util.COLOR_SORTER), params.palette(), params.reserve());
			return extend(t, subsequent);
		}

		@Override public String toString() {return "Color Keys (CoC->CoC)";}
		@Override public Controls control(HasViewTransform transformProvider) {return new Controls();}

		
		public static final class Controls extends ControlPanel {
			public static final Entry brewerColors = 
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
			
			public static final Entry cableColors = 
					new Entry(
							"Cable Colors",
							Color.BLACK,
					new Color[] {//Taken from Dustin Cable's racial dot-map http://www.coopercenter.org/demographics/Racial-Dot-Map; ordered so they match the letter-code sort-order
					new Color(255,69,0),
					new Color(0,200,0),
					new Color(255,165,0),
					new Color(136,90,68),
					new Color(0,0,200),
			});
			
			public static final Entry tractColors = 
					new Entry(
							"Tract Colors",
							Color.GRAY,
					new Color[] {//Taken from Dustin Cable's racial dot-map http://www.coopercenter.org/demographics/Racial-Dot-Map; ordered so they match the letter-code sort-order
					new Color(0,0,200),
					new Color(0,200,0),
					new Color(220,0,0)
			});
			
			public static final Entry redBlue = new Entry("Blue/Red", Color.white, new Color[]{Color.blue, Color.red}); 

			private final JComboBox<Entry> palette = new JComboBox<>();
			
			public Controls() {
				this.add(new LabeledItem("Palette:", palette));
				
				palette.addItem(cableColors);
				palette.addItem(brewerColors);
				palette.addItem(tractColors);
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

	public static final class Clipwarn extends OptionTransfer<Clipwarn.Controls> {
		@Override
		public Transfer<?, ?> transfer(Controls params, Transfer<?, ?> subsequent) {
			if (params.underDelta() == 0) {return subsequent;}
			return new Advise.Clipwarn(params.highColor.color(), params.lowColor.color(), subsequent, params.underDelta());
		}

		@Override
		public Controls control(HasViewTransform transformProvider) {return new Controls();}
		@Override public String toString() {return "Clipwarn (int->color)";} 
		
		private static class Controls extends ControlPanel {
			public JSpinner underDelta = new JSpinner(new SpinnerNumberModel(5.0, 0, 100,.5));
			public ColorChooser highColor = new ColorChooser(Color.black, "Over:");
			public ColorChooser lowColor = new ColorChooser(Color.gray, "Under:");
			public Controls() {
				super("Clipwarn");
				this.setLayout(new GridLayout(1,0));
				add(highColor);
				add(lowColor);
				add(new LabeledItem("Delta:", underDelta));
				underDelta.addChangeListener(actionProvider.changeDelegate());
				lowColor.addActionListener(actionProvider.actionDelegate());
				highColor.addActionListener(actionProvider.actionDelegate());
			}
			
			public double underDelta() {return (double) underDelta.getValue();}
		}
	}
	
	public static final class AutoLegend extends OptionTransfer<AutoLegend.Controls> {
		final JFrame flyaway = new JFrame("Legend");
		final JPanel root = new JPanel();
		
		public AutoLegend() {
			flyaway.setSize(200, 250);
			flyaway.setLayout(new BorderLayout());
			flyaway.add(root, BorderLayout.CENTER);
		}
		
		@Override
		public Transfer<?, ?> transfer(Controls params, Transfer<?, ?> subsequent) {
			root.removeAll();
			root.revalidate();
			Legend.AutoUpdater updater = new Legend.AutoUpdater(subsequent, new FlexFormatter(params.examples()), root, BorderLayout.CENTER);
			//Legend.AutoUpdater updater = new Legend.AutoUpdater(subsequent, new Legend.DiscreteComparable<>(), root, BorderLayout.CENTER);
			flyaway.setVisible(true);
			return updater;
		}

		@Override public String toString() {return "Legend (*->Color)";}
		@Override public Controls control(HasViewTransform transformProvider) {return new Controls();}
		@Override public void cleanup() {
			root.removeAll();
			flyaway.setVisible(false);
			flyaway.dispose();
		}
		
		public static final class Controls extends ControlPanel {
			public JSpinner examples = new JSpinner(new SpinnerNumberModel(10, 0,  50, 1));

			public Controls() {
				super("legend");
				this.add(new LabeledItem("Examples: ", examples));
				examples.addChangeListener(actionProvider.changeDelegate());
			}
			public int examples() {return (int) examples.getValue();}
		}
		
		public static final class FlexFormatter implements Legend.Formatter {
			final int examples;
			public FlexFormatter(int divisions) {this.examples = divisions;}
			
			
			private Formatter decide(Object val) {
				//if (val instanceof CategoricalCounts) {return new Legend.FormatCategoriesByOutputDistribution(examples);}
				if (val instanceof CategoricalCounts) {return new Legend.FormatCategoriesByOutputStream(examples);}
				else if (val instanceof Comparable) {return new Legend.DiscreteComparable(examples);}
				else {throw new IllegalArgumentException("Could not detect the type of formatter to use.  Please explicitly supply.");}
			}


			@Override
			public Map select(Aggregates inAggs, Aggregates outAggs) {
				Formatter inner = decide(inAggs.defaultValue());
				return inner.select(inAggs, outAggs);
			}


			@Override
			public JPanel display(Map exemplars) {
				Formatter inner = decide(exemplars.keySet().iterator().next());
				return inner.display(exemplars);
			}
			
		}


	}
	
	public static final class DataEdgeBoost extends OptionTransfer<DataEdgeBoost.Controls> {
		@Override public Transfer<Number, Color> transfer(Controls p, Transfer subsequent) {
			Transfer t = new Seq(
					new Advise.DataEdgeBoost(p.radius()), 
					new Numbers.Interpolate<>(p.lowColor(), p.highColor(), p.highColor()));
 
			if (subsequent == null) {
				return t;
			} else {
				return new Fan(new BlendLeftOver(), t, subsequent);
			}
		}
		@Override public String toString() {return "Edge Boost (*)";}
		@Override public Controls control(HasViewTransform transformProvider) {return new Controls();}

		private static class Controls extends ControlPanel {
			public JSpinner radius = new JSpinner(new SpinnerNumberModel(2, 0, 100,1));
			public ColorChooser highColor = new ColorChooser(Color.white, "Highlight");
			public ColorChooser lowColor = new ColorChooser(Color.black, "Lowlight");
			public Controls() {
				super("EdgeBoost");
				this.setLayout(new GridLayout(1,0));
				add(new LabeledItem("Radius:", radius));
				add(lowColor);
				add(highColor);
				radius.addChangeListener(actionProvider.changeDelegate());
				highColor.addActionListener(actionProvider.actionDelegate());
				lowColor.addActionListener(actionProvider.actionDelegate());
			}
			
			public int radius() {return (int) radius.getValue();}
			public Color highColor() {return highColor.color();}
			public Color lowColor() {return lowColor.color();}
		}
	}
	
	

	public static final class SubPixel extends OptionTransfer<SubPixel.Controls> {
		@Override public Transfer<Number, Color> transfer(Controls p, Transfer subsequent) {
			Transfer t = new Seq(
					new Advise.NeighborhoodDistribution(p.radius()), 
					new Numbers.Interpolate<>(p.lowColor(), p.highColor(), Util.CLEAR));
 
			if (subsequent == null) {
				return t;
			} else {
				return new Fan(new BlendLeftOver(), t, subsequent);
			}
		}
		
		@Override public String toString() {return "Sub Pixel (Num)";}
		@Override public Controls control(HasViewTransform transformProvider) {return new Controls();}

		private static class Controls extends ControlPanel {
			public JSpinner radius = new JSpinner(new SpinnerNumberModel(2, 0, 100,1));
			public ColorChooser highColor = new ColorChooser(Color.black, "Highlight");
			public ColorChooser lowColor = new ColorChooser(Util.CLEAR, "Lowlight");
			public Controls() {
				super("SubPixel");
				this.setLayout(new GridLayout(1,0));
				add(new LabeledItem("Radius:", radius));
				add(lowColor);
				add(highColor);
				radius.addChangeListener(actionProvider.changeDelegate());
				highColor.addActionListener(actionProvider.actionDelegate());
				lowColor.addActionListener(actionProvider.actionDelegate());
			}
			
			public int radius() {return (int) radius.getValue();}
			public Color highColor() {return highColor.color();}
			public Color lowColor() {return lowColor.color();}
		}
	}
	
	/**Blends two colors per alpha composition 'over' rule with the left on top.**/ 
	private static final class BlendLeftOver implements Fan.Merge<Color> {

		@Override
		public Aggregates<Color> merge(Aggregates<Color> left, Aggregates<Color> right) {
			final int lowX = Math.min(left.lowX(), right.lowX());
			final int lowY = Math.min(left.lowY(), right.lowY());
			final int highX = Math.max(left.highX(), right.highX());
			final int highY = Math.max(left.highY(), right.highY());
			
			Aggregates<Color> out = AggregateUtils.make(lowX, lowY, highX, highY, identity());
			for (int x=lowX; x<highX; x++) {
				for (int y=lowY; y<highY; y++) {
					Color over = left.get(x,y);
					if (over == left.defaultValue()) {
						out.set(x, y, right.get(x, y));
					} else {
						Color under = right.get(x, y);
						out.set(x,y, Util.premultiplyAlpha(over, under));
					}						
				}
			}
			return out;
		}

		@Override public Color identity() {return Util.CLEAR;}			
	}
	
	
//	public static final class WeaveStates extends OptionTransfer<WeaveStates.Controls> {
//		private static final Collection<Shape> shapes;
//		
//		static {
//			try {
//				shapes = GeoJSONTools.flipY(GeoJSONTools.loadShapesJSON(new File("../data/maps/USStates"), false)).values();
//				//shapes = GeoJSONTools.flipY(GeoJSONTools.loadShapesJSON(new File("../data/maps/USCounties"), true));
//			} catch (Exception e) {throw new RuntimeException(e);}
//		}
//		
//		@Override
//		public Transfer<?, ?> transfer(Controls params, Transfer subsequent) {
//			return extend(
//					Seq.start(new Shapes.ShapeGather(shapes, params.tp))
//						.then(new Categories.RandomWeave()),
//					subsequent);
//		}
//
//		@Override
//		public Controls control(final HasViewTransform transformProvider) {
//			return new Controls(transformProvider);
//		}
//		
//		public static final class Controls extends ControlPanel {
//			final HasViewTransform tp;
//			public Controls(HasViewTransform tp) {this.tp=tp;}
//		}
//		
//		@Override public String toString() {return "Weave States";}
//	}
	
	public static final class Present2 extends OptionTransfer<ControlPanel> {
		@Override 
		public Transfer<Integer,Integer> transfer(ControlPanel p, Transfer subsequent) {
			Transfer t = new General.Present<Integer, Integer>(0,1);
			return extend(t, subsequent);	

		}
		
		@Override public String toString() {return "Present (*->Int)";}
		@Override public ControlPanel control(HasViewTransform transformProvider) {return new ControlPanel();}
	}
	
	public static final class Present extends OptionTransfer<ControlPanel> {
		@Override 
		public Transfer<Integer,Color> transfer(ControlPanel p, Transfer subsequent) {
			Transfer t = new General.Present<Integer, Color>(Color.red, Color.white);
			return extend(t, subsequent);
		}
		
		@Override public String toString() {return "Present (*->Color)";}
		@Override public ControlPanel control(HasViewTransform transformProvider) {return new ControlPanel();}
	}
	
	public static final class Gradient extends OptionTransfer<ControlPanel> {
		@Override public Transfer<Object, Color> transfer(ControlPanel p, Transfer subsequent) {
			Transfer t = new Debug.Gradient();
			return extend(t, subsequent);	
		}
		
		@Override public String toString() {return "Gradient (color)";}
		@Override public ControlPanel control(HasViewTransform transformProvider) {return new ControlPanel();}
	} 

	public static final class PrintStats extends OptionTransfer<ControlPanel> {
		@Override public Transfer<Object, Color> transfer(ControlPanel p, Transfer subsequent) {
			return new Debug.Stats(subsequent);
		}
		
		@Override public String toString() {return "Print Statistics";}
		@Override public ControlPanel control(HasViewTransform transformProvider) {return new ControlPanel();}
	} 

	//TODO: REMOVE the log option from Categories.HighAlpha by providing a category-map-with-valuer transfer
	public static final class ColorCatInterpolate extends OptionTransfer<ColorCatInterpolate.Controls> {
		@Override
		public Transfer<?, ?> transfer(Controls params, Transfer subsequent) {
			return new Categories.HighDefAlpha(Color.white, .1, params.log());
		}

		@Override public String toString() {return "HD Alpha (CoC<Color>)";}
		@Override public Controls control(HasViewTransform transformProvider) {return new Controls();}

		public static final class Controls extends ControlPanel {
			private final JCheckBox log =new JCheckBox("Log");
			
			public Controls() {
				super("ColorCatInterpolate");
				add(log);
				
				log.setSelected(true);
				log.addActionListener(actionProvider.actionDelegate());
			}
			
			public boolean log() {return log.isSelected();}
			
		}
	}

	public static class ControlPanel extends JPanel {
		protected final ActionProvider actionProvider;
		
		public ControlPanel() {this("");}
		public ControlPanel(String id) {actionProvider = new ActionProvider(id);}
		public void addActionListener(ActionListener listener) {actionProvider.addActionListener(listener);}
		@Override public boolean equals(Object other) {return other!=null && this.getClass().equals(other.getClass());}
		@Override public final int hashCode() {return this.getClass().hashCode();}
	}
	
	protected static <IN,MID,OUT> Transfer extend(Transfer<IN,MID> first, Transfer<MID,OUT> second) {
    	if (first == null) {return second;}
    	if (second == null) {return first;}
    	if (first instanceof Seq) {return ((Seq<IN,?,MID>) first).then(second);}    	
    	return new Seq<>(first, second);
    }
    
	/**Convert a list of OptionTransfer items into a transfer.**/
	public static Transfer<?,?> toTransfer(List<OptionTransfer> transferList, List<? extends ControlPanel> optionPanels) {
		Transfer subsequent = null; 
		for (int i=transferList.size()-1; i>=0; i--) {
			OptionTransfer ot = transferList.get(i);
			ControlPanel panel = optionPanels == null || optionPanels.get(i) == null ? ot.control(null) : optionPanels.get(i);
			subsequent = ot.transfer(panel, subsequent);
		}
		return subsequent;
	}
	

}
