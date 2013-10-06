package ar.aggregates;

/**Indicate that bounds were not properly provided at construction.**/
public final class BoundsInversionException extends RuntimeException {
	private static final long serialVersionUID = 7513866317256715349L;
	protected BoundsInversionException(int low, int high, String dim) {
		super(String.format("Inverted bounds: low%1$s (%2$d) must be lower than high%1$s (%2$d)", dim,low,high));
	}
}