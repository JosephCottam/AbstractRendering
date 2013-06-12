package ar.glyphsets;

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
	/**Convert a value into another value (often a color, but not always).
	 * <I> Input value type
	 * **/
	public static interface Shaper<I> {public Shape shape (I from);}
	
	/**Convert a value into a shape.
	 * <I> Input value type
	 * <V> Output value type
	 * **/
	public static interface Valuer<I,V> {public V value(I from);}
	
	/**Convenience interface for working with double-encoding on shape and value.**/
	public static interface Glypher<I,V> extends Shaper<I>, Valuer<I,V> {}
	
	/**Simple function for making glyphs from a Glypher.**/
	public static <I,V> Glyph<V> glyph(Shaper<I> s, Valuer<I,V> v, I value) {
		return new SimpleGlyph<V>(s.shape(value), v.value(value));
	}
	
	
	public static interface Indexed {public Object get(int f);}
	
	
	public static class IndexedToRect implements Shaper<Indexed> {
		private final double width,height;
		private final boolean flipY;
		private final int xIdx, yIdx;
		
		public IndexedToRect(double width, double height, boolean flipY, int xIdx, int yIdx) {
			this.width=width;
			this.height=height;
			this.flipY=flipY;
			this.xIdx = xIdx;
			this.yIdx = yIdx;
		}
		public Rectangle2D shape(Indexed from) {
			double x=((Number) from.get(xIdx)).doubleValue();
			double y=((Number) from.get(yIdx)).doubleValue();
			
			y = flipY ? -y : y; 
			return new Rectangle2D.Double(x, y, width, height);
		}	
	}
	
	public static class IndexedToValue<I,V> implements Valuer<Indexed,V> {
		private final int vIdx;
		private final Valuer<I,V> basis;
		
		public IndexedToValue(int vIdx, Valuer<I, V> basis) {
			this.vIdx = vIdx;
			this.basis = basis;
		}
		
		@SuppressWarnings("unchecked")
		public V value(Indexed from) {
			return basis.value((I) from.get(vIdx));
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
