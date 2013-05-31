package ar.glyphsets;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Collections;

import ar.Glyphset;
import ar.util.ImplicitGlyphs;
import ar.util.SimpleGlyph;



/**Implicit geometry, spatially arranged glyphset for accessing 2D data structures.
 * 
 * When representing a 2D data structure as a matrix-like visualization, 
 * the matrix indices determine the geometry.  Therefore the geometry
 * does not need to be explicitly stored.
 * 
 * 
 * @author jcottam
 *
 * @param <T>
 */
public class DirectMatrix<T> implements Glyphset<Color> {
	private final T[][] matrix;
	private final double xScale, yScale;
	private final ImplicitGlyph<T,Color> colorBy;
	private final boolean nullIsValue;

	
	public DirectMatrix(T[][] matrix, double xScale, double yScale, boolean nullIsValue) {
		this(matrix, xScale, yScale, nullIsValue, new ImplicitGlyphs.AB<T>(null, Color.white, Color.blue));
	}
	
	public DirectMatrix(T[][] matrix, double xScale, double yScale, boolean nullIsValue, ImplicitGlyph<T,Color> colorBy) {
		this.matrix = matrix;
		this.xScale = xScale;
		this.yScale = yScale;
		this.colorBy = colorBy;
		this.nullIsValue = nullIsValue;
	}

	//TODO: Only returns top-left of pixel, not full bounds....
	//TODO: Extend beyond colors for glyph value
	public Collection<? extends Glyph<Color>> intersects(Rectangle2D pixel) {
		long row = Math.round(Math.floor(pixel.getX()/xScale));
		long col = Math.round(Math.floor(pixel.getY()/yScale));

		if (inBounds(row,col)) {
			T v = matrix[(int) row][(int) col];
			if (v == null && !nullIsValue) {return Collections.emptyList();}
			
			Rectangle2D s = new Rectangle2D.Double(row*xScale, col*yScale, xScale, yScale);
			Color c = colorBy.value(v);
			return Collections.singletonList(new SimpleGlyph<Color>(s,c));
		} else {
			return Collections.emptyList();
		}
	}
	private boolean inBounds(long row, long col) {
		return row >=0 && col >= 0
				&& row < matrix.length 
				&& matrix.length > 0 && col < matrix[0].length;
	}

	public boolean isEmpty() {return matrix.length == 0 || matrix[0].length == 0;}

	public long size() {
		if (isEmpty()) {return 0;}
		else {return matrix.length * matrix[0].length;}
	}

	public Rectangle2D bounds() {
		if (isEmpty()) {return new Rectangle2D.Double(0,0,0,0);}
		else {return new Rectangle2D.Double(0,0,xScale*matrix[0].length, yScale*matrix.length);}
	}

	public void add(Glyph<Color> g) {throw new UnsupportedOperationException("Non-extensible glyph set.");}
}
