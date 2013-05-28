package ar.util;

import java.awt.Shape;

import ar.GlyphSet.Glyph;

public class SimpleGlyph<V> implements Glyph<V>{
	private static int IDCOUNTER=0;
	
	public final Shape shape;
	public final V value;
	public final Integer id = IDCOUNTER++;
	
	public SimpleGlyph(Shape shape) {this(shape, null);}
	public SimpleGlyph(Shape shape, V value) {
		this.shape=shape; 
		this.value = value;
	}
	
	public boolean equals(Object other) {
		return (other instanceof SimpleGlyph) &&
				id.equals(((SimpleGlyph<?>) other).id);
	}
	public int hashCode() {return id;}
	public Shape shape() {return shape;}
	public V value() {return value;}
}
