package ar.app.components.sequentialComposer;

import java.awt.Color;

import ar.Aggregator;
import ar.rules.CategoricalCounts;
import ar.rules.Categories;
import ar.rules.General;
import ar.rules.Numbers;
import ar.util.Util;

public final class OptionAggregator<G,A> {
	private final Aggregator<G,A> agg;
	private final String name;
	
	public OptionAggregator(String name, Aggregator<G,A> agg) {
		this.agg = agg;
		this.name = name;
	}
	
	public String name() {return name;} 
	public Aggregator<G,A> aggregator() {return agg;}
	@Override public String toString() {return name;}
	
	public static final OptionAggregator<Object, Color> BLUE 
		= new OptionAggregator<>("Blue (color)", new General.Const<>(Color.BLUE));
	
	public static final OptionAggregator<Color, Color> OVERPLOT_FIRST  
		= new OptionAggregator<>("Overplot First (color)", new Categories.First());
	
	public static final OptionAggregator<Color, Color> OVERPLOT_LAST 
		= new OptionAggregator<>("Overplot Last (color)", new General.Last<>(Util.CLEAR));
	
	public static final OptionAggregator<Object, Integer> COUNT  
		= new OptionAggregator<>("Count (int)", new Numbers.Count<Object>());
	
	public static final OptionAggregator<Color, CategoricalCounts<Color>> COC_COLOR  
		= new OptionAggregator<>("Categorical Counts (CoC<Colors>)",new Categories.CountCategories<Color>(Util.COLOR_SORTER));
		
	public static final OptionAggregator<Comparable<?>, CategoricalCounts<Comparable<?>>> COC_COMP
		= new OptionAggregator<>("Categorical Counts (CoC<Comp>)",new Categories.CountCategories<Comparable<?>>());
		
	public static final OptionAggregator<CategoricalCounts<?>, CategoricalCounts<?>> MERGE_CATS 
		= new OptionAggregator<>("Merge CoC (CoC<Comp>)", new Categories.MergeCategories());
}
