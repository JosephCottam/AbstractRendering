package ar.util.axis;


/**Wrapper for a pair of axes.  
 * @param <X> The type of the X-axis descriptor
 * @param <Y> The type of the Y-axis descriptor	
 */
public class DescriptorPair {
	public final AxisDescriptor<?> x;
	public final AxisDescriptor<?> y;
	public DescriptorPair(AxisDescriptor<?> x, AxisDescriptor<?> y) {
		this.x = x;
		this.y = y;
	}
}