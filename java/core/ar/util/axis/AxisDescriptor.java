package ar.util.axis;

import java.util.Map;

/**Describes an axis.
 * 
 * The 'seeds' are value/location pairs along the axis.
 * The 'interpolate' function is used to modify the list of seeds to fill in the axis. 
 * **/
public final class AxisDescriptor<T> {
	public final Map<T, Double> seeds;
	public final Interpolate<T> interpolate;
	public final String label;
	
	public AxisDescriptor(final String label, final Map<T, Double> seeds, final Interpolate<T> interpolate) {
		this.label = label;
		this.seeds = seeds;
		this.interpolate = interpolate;
	}
}