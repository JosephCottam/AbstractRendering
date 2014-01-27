package ar.rules;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
 * TODO: Currently happens at specialization time...should it be done at transfer time instead?
 * TODO: The auto-watcher reveals some other problems with Specialized extending Transfer...perhaps Specialized should not! 
 * 
 * @param <A> Input type
 */
public class Legend<A> implements Transfer<A, Color> {
	final Transfer<A,Color> basis;
	final Comparator<A> comp;
	int exemplars = 10;
	
	public Legend(Transfer<A,Color> basis, Comparator<A> comp) {
		this.basis = basis;
		this.comp = comp;
		
		if (basis == null) {throw new IllegalArgumentException("Must supply non-null transfer as a basis for building legends.");}
	}
	
	@Override public Color emptyValue() {return basis.emptyValue();}

	@Override
	public Specialized<A> specialize(Aggregates<? extends A> aggregates) {
		return new Specialized<A>(basis, aggregates, comp);
	}
	
	public static final class Specialized<A> extends Legend<A> implements Transfer.Specialized<A, Color> {
		final Transfer.Specialized<A,Color> basis;
		final Map<A, Set<Color>> mapping;
 		
		public Specialized(Transfer<A, Color> rootBasis, Aggregates<? extends A> inAggs, Comparator<A> comp) {
			super(rootBasis, comp);	

			this.basis = rootBasis.specialize(inAggs);
			mapping = new TreeMap<>(comp);

			Aggregates<Color> outAggs = Seq.SHARED_RENDERER.transfer(inAggs, basis);
			for (int x=inAggs.lowX(); x<inAggs.highX(); x++) {
				for (int y=inAggs.lowY(); y<inAggs.highY(); y++) {
					A in = inAggs.get(x, y);					
					if (!mapping.containsKey(in)) {mapping.put(in, new TreeSet<>(Util.COLOR_SORTER));}
					Set<Color> outs = mapping.get(in);
					outs.add(outAggs.get(x,y));
				}
			}
			
			int size = mapping.size();
			if (size > exemplars) {
				@SuppressWarnings("unchecked")
				Map.Entry<A,Set<Color>>[] entries = mapping.entrySet().toArray(new Map.Entry[size]);
				mapping.clear();
				for (int i=0; i<entries.length; i=i+(size/exemplars-1)) {
					Map.Entry<A, Set<Color>> entry = entries[i];
					mapping.put(entry.getKey(), entry.getValue()); 
				}
				Map.Entry<A,Set<Color>> last = entries[entries.length-1];
				mapping.put(last.getKey(), last.getValue());
			}
		}
		
		@Override
		public Aggregates<Color> process(Aggregates<? extends A> aggregates, Renderer rend) {return basis.process(aggregates, rend);}
		
		public JPanel legend() {
			JPanel labels = new JPanel(new GridLayout(0,1));
			JPanel examples = new JPanel(new GridLayout(0,1));			

			for (Map.Entry<A, Set<Color>> entry: mapping.entrySet()) {
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
		public AutoUpdater(Transfer<A,Color> basis, Comparator<A> comp, JComponent host, Object layoutParams) {
			super(basis, comp);
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
			host.revalidate();
			return spec;
		}		
	}
}
