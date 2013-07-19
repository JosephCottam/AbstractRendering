package ar.rules;

import java.awt.Color;
import java.util.List;

import ar.Aggregator;
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
}
