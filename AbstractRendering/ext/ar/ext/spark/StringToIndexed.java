package ar.ext.spark;

import spark.api.java.function.Function;
import ar.glyphsets.implicitgeometry.Indexed;

/**Take a string, make an indexed entity from it.**/
public class StringToIndexed extends Function<String, Indexed> {
	private static final long serialVersionUID = 127417360167125319L;
	private final String splitter;
	public StringToIndexed(String splitter) {this.splitter = splitter;}

	@Override
	public Indexed call(String s) throws Exception {
		return new Indexed.ArrayWrapper(s.split(splitter));
	}
}
