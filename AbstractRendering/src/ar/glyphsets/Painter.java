package ar.glyphsets;

import java.awt.Color;
import java.util.Map;


/**Painters provide a just-in-time transformation from data elements to color elements.
 * They are used by glyphsets that are based on implicit geometry geometry  
 * (instead of storing actual glyph items). 
 * **/
public  interface Painter<T> {
	public java.awt.Color from(T item);
		
	
	public static final class Constant<T> implements Painter<T> {
		private final Color c;
		public Constant(Color c) {this.c = c;}
		public Color from(T item) {return c;}
	}
	
	/**Coloring scheme when the full set of values is known.**/
	public static final class Listing<T> implements Painter<T> {
		private final Map<T, Color> mappings;
		private final Color other; 
		public Listing(Map<T,Color> mappings, Color other) {this.mappings=mappings; this.other = other;}
		public Color from(T item) {
			Color c = mappings.get(item);
			if (c == null) {return other;}
			else {return c;}
		}
	}
	
	/**Binary coloring scheme.  
	 * If an item equals the stored value, return color 'a'.
	 * Otherwise return color 'b'.
	 */
	public static final class AB<T> implements Painter<T> {
		private final Color a;
		private final Color b;
		private final T v;
		public AB(T v, Color a, Color b) {this.v = v; this.a = a; this.b=b;}
		public Color from(T item) {
			if (item == v || (v != null && v.equals(item))) {return a;}
			return b;
		}
	}
}