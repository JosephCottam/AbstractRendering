package ar.ext.spark;

import ar.Aggregates;
import ar.Aggregator;
import ar.renderers.AggregationStrategies;
import spark.api.java.function.Function2;

/**Wrap an aggregator's rollup function.**/
public class Rollup<V> extends Function2<Aggregates<V>,Aggregates<V>,Aggregates<V>> {
	private static final long serialVersionUID = -1121892395315765974L;
	
	final Aggregator<?,V> aggregator;
	public Rollup(Aggregator<?,V> aggregator) {this.aggregator = aggregator;}

	public Aggregates<V> call(Aggregates<V> left, Aggregates<V> right)
			throws Exception {
		return AggregationStrategies.horizontalRollup(left, right, aggregator);
	}

}
