package ar;

import java.awt.Shape;

/**Simple wrapper class glyphs.**/
public interface Glyph<V> {
	public Shape shape();
	public V value();
}