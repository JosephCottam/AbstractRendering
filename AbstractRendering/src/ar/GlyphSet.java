package ar;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.Shape;
import java.util.Collection;

public interface GlyphSet {
	
	/**Return all glyphs that intersect the passed rectangle.**/
	public Collection<Glyph> intersects(Rectangle2D r);
	
	/**Is this glyphset empty?*/
	public boolean isEmpty();
	
	/**How many items in this glyphset?*/
	public long size();
	
	/**What are the overall bounds of the items in this glyphset?**/
	public Rectangle2D bounds();
	
	/**Add a new item to this glyphset**/
	public void add(Glyph g);
	
	
	public static interface IterableGlyphs extends GlyphSet, Iterable<Glyph> {}
	public static interface RandomAccess extends GlyphSet {public Glyph get(long i);}
	
	public static final class Glyph {
		private static int IDCOUNTER=0;
		public final Shape shape;
		public final Color color;
		public final Object value;
		public final Integer id = IDCOUNTER++;
		
		public Glyph(Shape shape, Color color) {this(shape, color, null);}
		public Glyph(Shape shape, Color color, Object value) {
			this.shape=shape; 
			this.color=color; 
			this.value = value;
		}
		
		public boolean equals(Object other) {
			return (other instanceof Glyph) && id.equals(((Glyph) other).id);
		}
		public int hashCode() {return id;}
		
	}
}
