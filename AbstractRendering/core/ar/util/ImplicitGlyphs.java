package ar.util;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import ar.glyphsets.ImplicitGlyph;

public class ImplicitGlyphs {
	
	public static final class RainbowCheckerboard<T> implements ImplicitGlyph<Integer, Color> {
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
	public static final class Constant<T> implements ImplicitGlyph<T,Color> {
		private final Color c;
		public Constant() {this.c = Color.red;}
		public Constant(Color c) {this.c = c;}
		public Color value(T item) {return c;}
		public Shape shape(T item) {throw new UnsupportedOperationException();}
	}
	
	/**Coloring scheme when the full set of values is known.**/
	public static final class Listing<T> implements ImplicitGlyph<T,Color> {
		private final Map<T, Color> mappings;
		private final Color other; 
		public Listing(Map<T,Color> mappings, Color other) {this.mappings=mappings; this.other = other;}
		public Color value(T item) {
			Color c = mappings.get(item);
			if (c == null) {return other;}
			else {return c;}
		}
		public Shape shape(T item) {throw new UnsupportedOperationException();}

	}
	
	/**Binary coloring scheme.  
	 * If an item equals the stored value, return color 'a'.
	 * Otherwise return color 'b'.
	 */
	public static final class AB<T> implements ImplicitGlyph<T,Color> {
		private final Color a;
		private final Color b;
		private final T v;
		public AB(T v, Color a, Color b) {this.v = v; this.a = a; this.b=b;}
		public Color value(T item) {
			if (item == v || (v != null && v.equals(item))) {return a;}
			return b;
		}
		public Shape shape(T item) {throw new UnsupportedOperationException();}
	}
}
