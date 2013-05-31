package ar.util;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import ar.Glyphset.Glyph;

/** Provide just-in-time conversion from a value to a glyph 
 * (or glyph components).  
 * 
 * To provide better control, flexibility, information in implicit
 * glyphsets and the shape and value are presented as separate interfaces.
 *  
 *  Be advised that classes implementing shaper/valuer should be deterministic 
 *  in their parameters because iteration order and access count are not guaranteed 
 *  by the renderers.
 */
public abstract class ImplicitGeometry {

	/**Combination shaper/valuer (for convenience).**/
	public static interface Glypher<I,V> extends Shaper<I>, Valuer<I,V> {}
	
	/**Convert a value into another value (often a color, but not always).
	 * <I> Input value type
	 * **/
	public static interface Shaper<I> {public Shape shape (I from);}
	
	/**Convert a value into a shape.
	 * <I> Input value type
	 * <V> Output value type
	 * **/
	public static interface Valuer<I,V> {public V value(I from);}	
	
	/**Simple function for making glyphs from a Glypher.**/
	public static <I,V> Glyph<V> glyph(Glypher<I,V> g, I value) {
		return new SimpleGlyph<V>(g.shape(value), g.value(value));
	}
	
	public static final class RainbowCheckerboard<T> implements Glypher<Integer, Color> {
		private static final Color[] COLORS = new Color[]{Color.RED, Color.BLUE, Color.GREEN,Color.PINK,Color.ORANGE};
		private final int columns;
		private final double size;
		
		public RainbowCheckerboard(int columns, double size) {
			this.columns = columns;
			this.size = size;
		}

		public Shape shape(Integer from) {
			from = from*2;
			int row = from/columns;
			int col = from%columns;
			
			if (row%2==0) {col=col-1;}
			
			return new Rectangle2D.Double(col*size, row*size, size,size);
		}
		
		public Color value(Integer from) {
			return COLORS[from%COLORS.length];
		}
	}
	
	/**Paint everything the same color (red, if no color is specified at construction).*/
	public static final class Constant<T> implements Valuer<T,Color> {
		private final Color c;
		public Constant() {this.c = Color.red;}
		public Constant(Color c) {this.c = c;}
		public Color value(T item) {return c;}
	}
	
	/**Coloring scheme when the full set of values is known.**/
	public static final class Listing<T> implements Valuer<T,Color> {
		private final Map<T, Color> mappings;
		private final Color other; 
		public Listing(Map<T,Color> mappings, Color other) {this.mappings=mappings; this.other = other;}
		public Color value(T item) {
			Color c = mappings.get(item);
			if (c == null) {return other;}
			else {return c;}
		}
	}
	
	/**Binary coloring scheme.  
	 * If an item equals the stored value, return color 'a'.
	 * Otherwise return color 'b'.
	 */
	public static final class AB<T> implements Valuer<T,Color> {
		private final Color a;
		private final Color b;
		private final T v;
		public AB(T v, Color a, Color b) {this.v = v; this.a = a; this.b=b;}
		public Color value(T item) {
			if (item == v || (v != null && v.equals(item))) {return a;}
			return b;
		}
	}
}
