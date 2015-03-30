package ar.util.axis;


/**Wrapper for a pair of axes.  */
public class DescriptorPair<X,Y> {
	public final AxisDescriptor<X> x;
	public final AxisDescriptor<Y> y;
	public DescriptorPair(AxisDescriptor<X> x, AxisDescriptor<Y> y) {
		this.x = x;
		this.y = y;
	}
}