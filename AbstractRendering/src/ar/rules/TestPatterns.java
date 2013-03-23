package ar.rules;

import java.awt.Color;
import java.awt.geom.AffineTransform;

import ar.Aggregates;
import ar.GlyphSet;
import ar.Reduction;
import ar.Transfer;

public class TestPatterns {
	public static final class IDColor implements Reduction<Color> {
		private final Color c;
		public IDColor(Color c) {this.c=c;}
		public Color at(int x, int y, GlyphSet glyphs, AffineTransform inverseView) {return c;}
	}
	
	public static final class IDTransfer implements Transfer<Color> {
		public Color at(int x, int y, Aggregates<Color> aggregates) {return aggregates.at(x, y);}
	}
	
	public static final class Gradient implements Reduction<Color> {
		private final float width,height;
		public Gradient(int width, int height) {this.width=width; this.height=height;}
		public Color at(int x, int y, GlyphSet glyphs, AffineTransform inverseView) {
			return new Color(x/width, y/height,.5f ,1.0f);
		}

	}
}
