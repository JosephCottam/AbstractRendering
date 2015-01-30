package ar.util.axis;


/**Wrapper for a pair of axes.  */
public class DescriptorPair {
	public final AxisDescriptor<?> x;
	public final AxisDescriptor<?> y;
	public DescriptorPair(AxisDescriptor<?> x, AxisDescriptor<?> y) {
		this.x = x;
		this.y = y;
	}
}