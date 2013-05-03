package ar.glyphsets;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Collections;

import ar.GlyphSet;

public class DirectMatrix<T> implements GlyphSet {
	private final T[][] matrix;
	private final double xScale, yScale;
	private final Painter<T> colorBy;
	private final boolean nullIsValue;

	public static interface Painter<T> {
		public java.awt.Color from(T item);
	}
	
	private static final class IfNull<T> implements Painter<T> {
		private final Color color;
		private final Color nullColor;
		public IfNull(Color c, Color nullColor) {
			this.nullColor = nullColor;
			this.color = c;
		}
		public Color from(T item) {
			if (item != null) {return color;}
			else {return nullColor;}
		}
	}
	
	public DirectMatrix(T[][] matrix, double xScale, double yScale, boolean nullIsValue) {
		this(matrix, xScale, yScale, nullIsValue, new IfNull<T>(Color.blue, Color.white));
	}
	
	public DirectMatrix(T[][] matrix, double xScale, double yScale, boolean nullIsValue, Painter<T> colorBy) {
		this.matrix = matrix;
		this.xScale = xScale;
		this.yScale = yScale;
		this.colorBy = colorBy;
		this.nullIsValue = nullIsValue;
	}


	public Collection<Glyph> containing(Point2D p) {
		long row = Math.round(Math.floor(p.getX()/xScale));
		long col = Math.round(Math.floor(p.getY()/yScale));

		if (inBounds(row,col)) {
			T v = matrix[(int) row][(int) col];
			if (v == null && !nullIsValue) {return Collections.emptyList();}
			
			Rectangle2D s = new Rectangle2D.Double(row*xScale, col*yScale, xScale, yScale);
			Color c = colorBy.from(v);
			return Collections.singletonList(new Glyph(s,c,v));
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

	public int size() {
		if (isEmpty()) {return 0;}
		else {return matrix.length * matrix[0].length;}
	}

	public Rectangle2D bounds() {
		if (isEmpty()) {return new Rectangle2D.Double(0,0,0,0);}
		else {return new Rectangle2D.Double(0,0,xScale*matrix[0].length, yScale*matrix.length);}
	}

	public void add(Glyph g) {throw new UnsupportedOperationException("Non-extensible glyph set.");}
}
