package ar.glyphsets;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import ar.GlyphSet;

public class GlyphSingleton implements GlyphSet {
	private final List<Glyph> glyphs;
	private final Glyph glyph;
	private final Rectangle2D bounds;
	
	public GlyphSingleton(Glyph g) {
		glyphs = Collections.singletonList(g);
		glyph = g;
		bounds = g.shape.getBounds2D();
	}
	
	public Iterator<Glyph> iterator() {return glyphs.iterator();}
	public Glyph get(int i) {return glyphs.get(i);}
	public boolean isEmpty() {return glyphs.isEmpty();}
	public void add(Glyph g) {throw new UnsupportedOperationException();}
	public int size() {return glyphs.size();}
	public Rectangle2D bounds() {return bounds;}

	public Collection<Glyph> containing(Point2D p) {
		if (glyph.shape.contains(p)) {return glyphs;}
		else {return Collections.emptyList();}
	}

	


}
