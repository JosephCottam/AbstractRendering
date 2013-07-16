package ar.rules;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.List;

import ar.Aggregator;
import ar.Glyphset;
import ar.rules.Aggregators.Gradient;
import ar.rules.Aggregators.IDColor;
import ar.util.Util;

/**Classes used largely for debugging.
 * May be instructive for reference though.
 * 
 */
public class Debug {

	/**Compute a gradient across the 2D space.  
	 * This class was used largely for debugging; it ignores its inputs. 
	 */
	public static final class Gradient implements Aggregator<Object, Color> {
		private final float width,height;
		public Gradient(int width, int height) {this.width=width; this.height=height;}

		public Color identity() {return Util.CLEAR;}
		public Class<Object> input() {return Object.class;}
		public Class<Color> output() {return Color.class;}
		public boolean equals(Object other) {return other instanceof Gradient;}
		public Color combine(long x, long y, Color left, Object update) {
			return new Color(x/width, y/height,.5f ,1.0f); 
		}

		public Color rollup(List<Color> sources) {
			int r=0,g=0,b=0,a=0;
			for (Color c: sources) {
				r += c.getRed();
				g+=c.getGreen();
				b+=c.getBlue();
				a+=c.getAlpha();
			}
			int n = sources.size();
			return new Color(r/n,g/n,b/n/a/n);
		}

	}

	/**Create a solid fill.  
	 * This class was used largely for debugging; it ignores its inputs. 
	 */
	public static final class IDColor implements Aggregator<Object, Color> {
		private final Color c;
		public IDColor(Color c) {this.c=c;}
		public Color at(Rectangle r, Glyphset<? extends Object> glyphs, AffineTransform inverseView) {return c;}
		public Color identity() {return Util.CLEAR;}
		public Class<Object> input() {return Object.class;}
		public Class<Color> output() {return Color.class;}
		public boolean equals(Object other) {return other instanceof IDColor;}
		@Override
		public Color combine(long x, long y, Color left, Object update) {return c;}
		public Color rollup(List<Color> sources) {
			for (Color c: sources) {if (c!=identity()) {return this.c;}}
			return identity();
		}
	}
}
