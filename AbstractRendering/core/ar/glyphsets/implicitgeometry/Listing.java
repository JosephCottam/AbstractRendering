package ar.glyphsets.implicitgeometry;

import java.awt.Color;
import java.util.Map;

/**Coloring scheme when the full set of values is known.**/
public final class Listing<T> implements Valuer<T,Color> {
	private final Map<T, Color> mappings;
	private final Color other; 
	public Listing(Map<T,Color> mappings, Color other) {this.mappings=mappings; this.other = other;}
	public Color value(T item) {
		Color c = mappings.get(item);
		if (c == null) {return other;}
		else {return c;}
	}
}