package ar.ext.spark;

import org.apache.spark.api.java.function.Function;
import ar.glyphsets.implicitgeometry.Indexed;

/**Take a string, make an indexed entity from it by splitting on the passed pattern
 * ...basically a cheap CSV parser.**/
public class StringToIndexed extends Function<String, Indexed> {
	private static final long serialVersionUID = 127417360167125319L;
	private final String splitter;
	public StringToIndexed(String splitter) {this.splitter = splitter;}

	@Override
	public Indexed call(String s) throws Exception {
		String[] parts = s.split(splitter);
		float[] vals = new float[parts.length];
		for (int i=0; i<parts.length; i++) {
			try {vals[i] = Float.parseFloat(parts[i]);}
			catch (Exception e) {vals[i] = Float.NaN;}
		}
		return new Indexed.ArrayWrapper(vals);
	}
}
