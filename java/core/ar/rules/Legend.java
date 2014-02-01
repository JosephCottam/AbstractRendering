package ar.rules;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import ar.Aggregates;
import ar.Renderer;
import ar.Transfer;
import ar.rules.combinators.Seq;
import ar.util.Util;


/**Compares a set of aggregates with the color output and produces a color map input to output.
 * 
 * Input/output values are assumed to exactly align (so in.get(x,y) corresponds to out.get(x,y)).
 * Therefore, this can only be used on transfer chains that maintain that correspondence.
 * 
 * TODO: Legend building currently happens at specialization time...should it be done at transfer time instead?
 * TODO: The auto-watcher reveals some problems with Specialized extending Transfer...perhaps Specialized should not! 
 * 
 * @param <A> Input type
 */
public class Legend<A> implements Transfer<A, Color> {
	final Transfer<A,Color> basis;
	final Formatter<A> formatter;
	
	
	public Legend(Transfer<A,Color> basis, Formatter<A> formatter) {
		this.basis = basis;
		this.formatter = formatter;
		
		if (basis == null) {throw new IllegalArgumentException("Must supply non-null transfer as a basis for building legends.");}
	}
	
	@Override public Color emptyValue() {return basis.emptyValue();}

	@Override
	public Specialized<A> specialize(Aggregates<? extends A> aggregates) {
		return new Specialized<A>(basis, formatter, aggregates);
	}
	
	public static final class Specialized<A> extends Legend<A> implements Transfer.Specialized<A, Color> {
		final Transfer.Specialized<A,Color> basis;
		final Map<A, Set<Color>> mapping;
 		
		public Specialized(Transfer<A, Color> rootBasis, Formatter<A> formatter, Aggregates<? extends A> inAggs) {
			super(rootBasis, formatter);	
			this.basis = rootBasis.specialize(inAggs);
			
			Aggregates<Color> outAggs = Seq.SHARED_RENDERER.transfer(inAggs, basis);
			mapping = formatter.select(inAggs, outAggs);

		}
		
		@Override
		public Aggregates<Color> process(Aggregates<? extends A> aggregates, Renderer rend) {return basis.process(aggregates, rend);}
		
		public JPanel legend() {
			return formatter.display(mapping);
		}
	}
	
	public static final class ColorSwatch extends JPanel {
		private final Color c;
		public ColorSwatch(Color c) {this.c = c;}		
		@Override public void paintComponent(Graphics g) {
			g.setColor(c);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
		}
	}


	/**Keeps the legend display up to date as the values inside shift.
	 * 
	 * TODO: Shift to an event-based system that fires a "legendChanged" events
	 * */
	public static final class AutoUpdater<A> extends Legend<A> {
		final JComponent host;
		final Object layoutParams;
		private JPanel legend;

		/**
		 * @param basis Legend to watch
		 * @param host Where the legend value should be placed
		 * @param layoutParams How the legend should be added.
		 */
		public AutoUpdater(Transfer<A,Color> basis, Formatter<A> formatter, JComponent host, Object layoutParams) {
			super(basis, formatter);
			this.host = host;
			this.layoutParams = layoutParams;
		}

		@Override public Color emptyValue() {return basis.emptyValue();}
		@Override public Specialized<A> specialize(Aggregates<? extends A> aggregates) {
			Specialized<A> spec = super.specialize(aggregates);
			if (legend != null) {host.remove(legend);}
			legend = spec.legend();
			host.removeAll();
			host.add(legend, layoutParams);
			SwingUtilities.windowForComponent(legend).pack();
			return spec;
		}		
	}
	
	public static interface Formatter<A> {
		/**Select a subset of mappings to actually show in the display.**/
		public Map<A, Set<Color>> select(Aggregates<? extends A> inAggs, Aggregates<Color> outAggs);
		
		/**Generate a panel to show the selected mappings.**/
		public JPanel display(Map<A,Set<Color>> exemplars);
	}
	
	
	/**Shows a selection of categorical counts, selected based on the OUTPUT (e.g., color) distribution.**/
	public static final class FormatCategoriesByOutput<T> implements Formatter<CategoricalCounts<T>> {
		/**How many parts should each category be divided into?
		 * The number of exemplars is approximately dimensions*divisions.*/
		private final int examples;
		
		public FormatCategoriesByOutput() {this(10);}
		public FormatCategoriesByOutput(int examples) {this.examples = examples;}
		
		@Override
		public Map<CategoricalCounts<T>, Set<Color>> select(Aggregates<? extends CategoricalCounts<T>> inAggs, Aggregates<Color> outAggs) {
			Map<Color, CategoricalCounts<T>> rawList = new TreeMap<>(Util.COLOR_SORTER);

			for (int x=outAggs.lowX(); x<outAggs.highX(); x++) {
				for (int y=outAggs.lowY(); y<outAggs.highY(); y++) {
					CategoricalCounts<T> in = inAggs.get(x, y);
					Color c = outAggs.get(x,y);
					rawList.put(c, in);
				}
			}
						
			//Map out the used color space
			int rMax = Integer.MIN_VALUE, gMax = Integer.MIN_VALUE, bMax = Integer.MIN_VALUE;
			int rMin = Integer.MAX_VALUE, gMin = Integer.MAX_VALUE, bMin = Integer.MAX_VALUE;
			for (Color c: outAggs) {
				Color display = Util.premultiplyAlpha(c, Color.white);
				rMax = Math.max(rMax, display.getRed());
				gMax = Math.max(gMax, display.getBlue());
				bMax = Math.max(bMax, display.getGreen());
				rMin = Math.min(rMin, display.getRed());
				gMin = Math.min(gMin, display.getBlue());
				bMin = Math.min(bMin, display.getGreen());
			}

			//Create a partition scheme in each dimension and a storage location			
			//Iterate the input again, keep/replace values in the bins (Reservoir sampling for a single item; select nth-item with probability 1/n) 
			Binner binner = new Binner(examples, rMax, gMax, bMax, rMin, gMin, bMin);
			
			@SuppressWarnings("unchecked")
			CategoricalCounts<T>[] pickedInputs = new CategoricalCounts[binner.binCount()];
			Color[] pickedColors = new Color[binner.binCount()];
			int[] binSize = new int[binner.binCount()]; //How many items have landed in each bin?
			for (int x=outAggs.lowX(); x<outAggs.highX(); x++) {
				for (int y=outAggs.lowY(); y<outAggs.highY(); y++) {
					int bin = binner.bin(outAggs.get(x, y));
					binSize[bin] = binSize[bin] + 1;
					double replaceTest = Math.random();
					double replaceThreshold = 1/(double) binSize[bin];
					if (replaceTest < replaceThreshold) {
						pickedColors[bin] =  outAggs.get(x,y);
						pickedInputs[bin] = inAggs.get(x,y);
					}
				}
			}
			
			//Select from the grouped examples values down to the requested number of exemplars
			Map<CategoricalCounts<T>,Set<Color>> exemplars = new TreeMap<>(new CategoricalCounts.MangitudeComparator<T>());
			
			for (int offset=0; offset<examples && exemplars.size() < examples; offset++) {
				for (int i=0; i<pickedColors.length; i+=binner.searchStride()) {
					int idx = offset+i;
					if (pickedInputs[idx] == null) {continue;}
					exemplars.put(pickedInputs[idx], Collections.singleton(pickedColors[idx]));
				}
			}
			return exemplars;
		}

		@Override
		public JPanel display(Map<CategoricalCounts<T>, Set<Color>> exemplars) {
			JPanel labels = new JPanel(new GridLayout(0,1));
			JPanel examples = new JPanel(new GridLayout(0,1));			

			for (Map.Entry<CategoricalCounts<T>, Set<Color>> entry: exemplars.entrySet()) {
				labels.add(new JLabel(entry.getKey().toString()));
				JPanel exampleSet = new JPanel(new GridLayout(1,0));
				for (Color c: entry.getValue()) {
					exampleSet.add(new ColorSwatch(c));
				}
				examples.add(exampleSet);
			}
			JPanel legend = new JPanel(new BorderLayout());
			legend.add(labels, BorderLayout.EAST);
			legend.add(examples, BorderLayout.WEST);			
			return legend;
		}
		
		/**Converts a set of categorical counts into a specific bin based on its result color.**/
		private static final class Binner {
			final int examples;
			final int divisions;
			final int rMax, gMax, bMax, rMin, gMin,  bMin;			
			
			public Binner(int examples, int rMax, int gMax, int bMax, int rMin, int gMin,  int bMin) {
				this.examples = examples;
				this.divisions = examples*10; //Over-dividing the space to helps get to the target number of examples
				this.rMax = rMax+1;
				this.gMax = gMax+1;
				this.bMax = bMax+1;
				this.rMin = rMin;
				this.gMin = gMin;
				this.bMin = bMin;
			}
			
			/**What bin does a particular color land in?*/
			public int bin(Color color){
				int rcat = (int) ((color.getRed()-rMin)/((rMax-rMin)/(float) divisions));
				int gcat = (int) ((color.getGreen()-gMin)/((gMax-gMin)/(float) divisions));
				int bcat = (int) ((color.getBlue()-bMin)/((bMax-bMin)/(float) divisions));
				int bin = rcat + (divisions + gcat) + (1*divisions + bcat);
				return bin;
			}

			/**How many bins are in this color space?**/
			public int binCount() {return (int) Math.pow(divisions, 3);} //3--one each for RGB
			
			/**How far apart should increments be made to ensure a different bin-group is hit with each increment.
			 * Level 0 is the most coarse, any level is acceptable, but eventually the return value is just 1.
			 * **/ 
			public int searchStride() {return divisions/examples;}
		}

	}
	
	/**Shows a selection of categorical counts, selected based on the input distribution.**/
	public static final class FormatCategoriesByInput<T> implements Formatter<CategoricalCounts<T>> {
		/**How many parts should each category be divided into?
		 * The number of exemplars is approximately dimensions*divisions.*/
		private final int divisions;
		
		public FormatCategoriesByInput() {this(2);}
		public FormatCategoriesByInput(int divisions) {this.divisions = divisions;}
		
		@Override
		public Map<CategoricalCounts<T>, Set<Color>> select(Aggregates<? extends CategoricalCounts<T>> inAggs, Aggregates<Color> outAggs) {
			Map<CategoricalCounts<T>,Set<Color>> rawList = new HashMap<>();

			for (int x=inAggs.lowX(); x<inAggs.highX(); x++) {
				for (int y=inAggs.lowY(); y<inAggs.highY(); y++) {
					CategoricalCounts<T> in = inAggs.get(x, y);					
					if (!rawList.containsKey(in)) {rawList.put(in, new TreeSet<>(Util.COLOR_SORTER));}
					Set<Color> outs = rawList.get(in);
					outs.add(outAggs.get(x,y));
				}
			}
			
			//Get max/min (and possibly count) for each category
			SortedMap<T,Integer> catMaxs = new TreeMap<>();
			SortedMap<T,Integer> catMins = new TreeMap<>();
			for (Map.Entry<CategoricalCounts<T>, Set<Color>> e: rawList.entrySet()) {
				CategoricalCounts<T> cats = e.getKey();
				for (int i=0; i<cats.size(); i++) {
					T cat = cats.key(i);
					if (!catMaxs.containsKey(cat)) {catMaxs.put(cat, 0);}
					if (!catMins.containsKey(cat)) {catMins.put(cat, Integer.MIN_VALUE);}
					Integer max = catMaxs.get(cat);
					Integer min = catMins.get(cat);
					max = Math.max(max, cats.count(i));
					min = Math.min(min, cats.count(i));
					catMaxs.put(cat, max);
					catMins.put(cat, min);
				}
			}
			
			assert catMaxs.size() == catMins.size() : "Unequal number of maxes and mins";
			
			
			//Create a partition scheme in each dimension and a storage location			
			//Iterate the input again, keep/replace values in the bins (Reservoir sampling for a single item; select nth-item with probability 1/n) 
			Binner<T> binner = new Binner<>(divisions, catMaxs, catMins);			
			int[] binSize = new int[binner.binCount()]; //How many items have landed in each bin?
			Map<CategoricalCounts<T>,Set<Color>> exemplars = new TreeMap<>(new CategoricalCounts.MangitudeComparator<T>());
			for (Map.Entry<CategoricalCounts<T>, Set<Color>> e: rawList.entrySet()) {
				int bin = binner.bin(e.getKey());
				binSize[bin] = binSize[bin]+1;
				double replaceTest = Math.random();
				double replaceThreshold = 1/(double) binSize[bin];
				if (replaceTest < replaceThreshold) {exemplars.put(e.getKey(), e.getValue());}
			}

			return exemplars;
		}

		@Override
		public JPanel display(Map<CategoricalCounts<T>, Set<Color>> exemplars) {
			JPanel labels = new JPanel(new GridLayout(0,1));
			JPanel examples = new JPanel(new GridLayout(0,1));			

			for (Map.Entry<CategoricalCounts<T>, Set<Color>> entry: exemplars.entrySet()) {
				labels.add(new JLabel(entry.getKey().toString()));
				JPanel exampleSet = new JPanel(new GridLayout(1,0));
				for (Color c: entry.getValue()) {
					exampleSet.add(new ColorSwatch(c));
				}
				examples.add(exampleSet);
			}
			JPanel legend = new JPanel(new BorderLayout());
			legend.add(labels, BorderLayout.EAST);
			legend.add(examples, BorderLayout.WEST);			
			return legend;
		}
		
		/**Converts a set of categorical counts into a specific bin.**/
		private static final class Binner<T> {
			final int divisions;
			final SortedMap<T,Integer> maxes;
			final SortedMap<T,Integer> mins;
			
			public Binner(int divisions, SortedMap<T,Integer> maxes, SortedMap<T,Integer> mins) {
				this.divisions = divisions;
				this.maxes = maxes;
				this.mins = mins;
			}
			
			/**What bin does a particular categorization land in?*/
			public int bin(CategoricalCounts<T> cats){
				int bin=0;
				for (int i=0; i< cats.size(); i++) {
					T cat = cats.key(i);
					int catIdx = maxes.headMap(cat).size();
					int catMax = maxes.get(cat);
					int catMin = mins.get(cat);
					int count = cats.count(i);
					
					float span = catMax-catMin/((float) divisions);
					int catBin = (int) (count/span); 
					bin = bin + (catBin + (catIdx * divisions));
				}
				return bin;
			}

			/**How many bins are in this dataspace?**/
			public int binCount() {return (int) Math.pow(divisions, maxes.size());}
		}
		
	}
 	
	/**Shows a set of discrete values in a sortable set of inputs.**/
	public static final class DiscreteComparable<A> implements Formatter<A> {
		final Comparator<A> comp;
		final int examples;
		
		/**Assumes the A is comparable.**/
		public DiscreteComparable() {this(10);}
		@SuppressWarnings("unchecked")
		public DiscreteComparable(int divisions) {this((Comparator<A>) new Util.ComparableComparator<>(), divisions);}
		public DiscreteComparable(Comparator<A> comp, int examples) {
			this.comp = comp;
			this.examples = examples;
		}
		
		@Override
		public Map<A,Set<Color>> select(final Aggregates<? extends A> inAggs, final Aggregates<Color> outAggs) { 
			Map<A,Set<Color>> rawList = new TreeMap<>();

			for (int x=inAggs.lowX(); x<inAggs.highX(); x++) {
				for (int y=inAggs.lowY(); y<inAggs.highY(); y++) {
					A in = inAggs.get(x, y);					
					if (!rawList.containsKey(in)) {rawList.put(in, new TreeSet<>(Util.COLOR_SORTER));}
					Set<Color> outs = rawList.get(in);
					outs.add(outAggs.get(x,y));
				}
			}
			
			int size = rawList.size();
			if (size > examples) {
				@SuppressWarnings("unchecked")
				Map.Entry<A,Set<Color>>[] entries = rawList.entrySet().toArray(new Map.Entry[size]);
				Map<A,Set<Color>> output = new TreeMap<>();
				for (int i=0; i<entries.length; i=i+(size/examples-1)) {
					Map.Entry<A, Set<Color>> entry = entries[i];
					output.put(entry.getKey(), entry.getValue()); 
				}
				Map.Entry<A,Set<Color>> last = entries[entries.length-1];
				output.put(last.getKey(), last.getValue());
				return output;
			} else {
				return rawList;
			}
		}

		@Override
		public JPanel display(Map<A,Set<Color>> exemplars) {
			JPanel labels = new JPanel(new GridLayout(0,1));
			JPanel examples = new JPanel(new GridLayout(0,1));			

			for (Map.Entry<A, Set<Color>> entry: exemplars.entrySet()) {
				labels.add(new JLabel(entry.getKey().toString()));
				JPanel exampleSet = new JPanel(new GridLayout(1,0));
				for (Color c: entry.getValue()) {
					exampleSet.add(new ColorSwatch(c));
				}
				examples.add(exampleSet);
			}
			JPanel legend = new JPanel(new BorderLayout());
			legend.add(labels, BorderLayout.EAST);
			legend.add(examples, BorderLayout.WEST);			
			return legend;
		}
		
	}
}
