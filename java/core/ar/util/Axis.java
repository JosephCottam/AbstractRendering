package ar.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import ar.Glyphset;

public class Axis {

	/**Describes a pair of axes.**/
	public static class Descriptor<X,Y> {
		public final AxisDescriptor<X> x;
		public final AxisDescriptor<Y> y;
		public Descriptor(AxisDescriptor<X> x, AxisDescriptor<Y> y) {
			this.x = x;
			this.y = y;
		}
	}
	
	/**Describes an axis.
	 * 
	 * The 'seeds' are value/location pairs along the axis.
	 * The 'interpolate' function is used to modify the list of seeds to fill in the axis. 
	 * **/
	public static final class AxisDescriptor<T> {
		public final Map<T, Double> seeds;
		public final Interpolate<T> interpolate;
		public final String label;
		
		public AxisDescriptor(final String label, final Map<T, Double> seeds, final Interpolate<T> interpolate) {
			this.label = label;
			this.seeds = seeds;
			this.interpolate = interpolate;
		}
	}
	
	public static final <T> AxisDescriptor<T> empty() {return new AxisDescriptor<T>("", Collections.<T,Double>emptyMap(), new Discrete<T>());}
	
	/**Given a set of seeds, produce a new set of seeds of the requested target size.**/
	public static interface Interpolate<T> {
		public Map<T, Double> interpolate(Map<T,Double> seeds, int targetSize);
	}
	
	/**If seeds is not a SortedMap, returns the input value unchanged. 
	 * If seeds is a SortedMap, returns a strided selection of the seeds.
	 * If forceLast is true, will return a set of targetSize+1 to ensure the last item is present.
	 * **/
	public static class Discrete<T> implements Interpolate<T> {
		public final boolean forceLast;
		
		public Discrete() {this(false);}
		public Discrete(boolean forceLast) {this.forceLast = forceLast;}
 		
		@Override
		public Map<T, Double> interpolate(Map<T, Double> seeds, int targetSize) {
			if (!(seeds instanceof SortedMap)) {return seeds;}
			
			SortedMap<T, Double> rslt = new TreeMap<T, Double>();
			int tick =0;
			Map.Entry<T,Double> last = null;
			for (Map.Entry<T,Double> e: seeds.entrySet()) {
				last = e;
				if (tick ==0) {rslt.put(e.getKey(), e.getValue());}
				tick = (tick+1)%targetSize;
			}
			
			if (forceLast && last != null) {rslt.put(last.getKey(), last.getValue());}
			
			return rslt;
		}		
	}
		
	//TODO: Add switch for log 
	//TODO: Add the 'nice' ticks logic 
	//TODO: Add logic for non-double keys
	public static class LinearSmooth implements Interpolate<Double> {

		@Override
		public Map<Double, Double> interpolate(Map<Double, Double> seeds, int targetSize) {
			double min, max;
			double high, low;

			SortedMap<Double, Double> m;
			if (seeds instanceof SortedMap) {
				m = (SortedMap<Double,Double>) seeds;
			} else {
				m = new TreeMap<>();
				m.putAll(seeds);								
			}
			
			min = m.firstKey();
			low = m.get(min);
			max = m.lastKey();
			high = m.get(max);
			
			double keySpan = max-min; //HACK!!
			double keyStride = keySpan/(targetSize-1);
			double valSpan = high-low;
			double valStride = valSpan/(targetSize-1);
			
			Map<Double, Double> rslt = new TreeMap<>();
			for (int i=0; i<targetSize; i++) {
				rslt.put(min+(keyStride*i), low+(valStride*i));
			}
			rslt.put(max, high);
			return rslt;
		}
		
	}
	
	/**Produce descriptors indicating literal positions.
	 * DOES NOT reflect backing data, just the bounding box of the projection.
	 * **/
	public static Descriptor<?, ?> coordinantDescriptors(Glyphset<?,?> glyphs) {
		Rectangle2D bounds = glyphs.bounds();
		return new Descriptor<>(linearDescriptor("", bounds.getMinX(), bounds.getMaxX(), 10, true),
					    		linearDescriptor("", bounds.getMinY(), bounds.getMaxY(), 10, true));
	}
	
	public static <T extends Number> AxisDescriptor<T> linearDescriptor(String label, double low, double high, int samples, boolean continuous) {
		Map<Number, Double> rslt = continuous ? new TreeMap<Number, Double>() : new HashMap<Number, Double>();
		Interpolate<?> interp = continuous ? new LinearSmooth() : new Discrete<Long>();
		
		for (int i=0; i<samples+1; i++) {
			Number val;
			if (continuous) {val = new Double(low + ((high-low)/samples)*i);}
			else {val = Math.round(low + ((high-low)/samples)*i);}
			rslt.put(val, val.doubleValue());
		}
		
		return new AxisDescriptor<>(label, (Map<T, Double>) rslt, (Interpolate<T>) interp);
	}
	
	/**Create an evenly-spaced categorical axis.**/
	public static AxisDescriptor<String> categoricalDescriptor(String label, double low, double high, String... labels) {
		int positions = labels.length;
		Map<String, Double> rslt = new HashMap<String, Double>();
		
		double gap = low + ((high-low)/(positions*2));
		for (int i=0; i<positions; i++) {
			Double val = gap + gap*i*2;
			rslt.put(labels[i], val);
		}
		return new AxisDescriptor<>(label, rslt, new Discrete<String>());
	}
	
	
	/**Merge two descriptors.  
	 * 
	 * NOTE: Assumes the interpolate from 'first' is the one to carry forward.
	 */
	public <T> AxisDescriptor<T> merge(AxisDescriptor<T> first, AxisDescriptor<T> second) {
		Map<T, Double> combined = new HashMap<>();
		combined.putAll(first.seeds);
		combined.putAll(second.seeds);
		
		String label;
		if (first.label == second.label) {label = first.label;}
		else {label = first.label + "/" + second.label;}
		
		return new AxisDescriptor<>(label, combined, first.interpolate);
	}
	
	
    //---------------------------------------------------- Rendering ----------------------------------------------------
	

	public static void drawAxes(Axis.Descriptor<?,?> axes, Graphics2D g2, AffineTransform viewTransform, Rectangle2D screenBounds) {
		
		Object restore_anti_alias = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

		drawAxis(axes.x, g2, viewTransform, screenBounds, true);
		drawAxis(axes.y, g2, viewTransform, screenBounds, false);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, restore_anti_alias);

	}
	
	/**How far off of the line should labels be placed?**/
	public static float LABEL_OFFSET = 10;
	
	/**How far from the line should the tick go in the direction AWAY from the label?**/
	public static float TICK_AWAY = 3;
	
	/**How far from the line should the tick go in the direction TOWARDS the label?**/
	public static float TICK_TOWARD = 6;
	
	/**How many pixels high should the axes be given?**/
	public static int AXIS_SPACE = 100;
	
	private static final void drawAxis(AxisDescriptor<?> axis, Graphics2D g2, AffineTransform viewTransform, Rectangle2D screenBounds, boolean isX) {
		g2.setColor(Color.GRAY);
		double max=Double.NEGATIVE_INFINITY, min=Double.POSITIVE_INFINITY;		
		for (Map.Entry<?,Double> e:axis.seeds.entrySet()) {
			Double val = e.getValue();
			
			drawLine(val, val, TICK_TOWARD, TICK_AWAY, g2, viewTransform, screenBounds, isX);
			drawLabel(e.getKey(), val, val, LABEL_OFFSET, g2, viewTransform, screenBounds, isX);

			max = Math.max(max, val);
			min = Math.min(min, val);
		}
		
		drawLine(min, max, 0,0, g2, viewTransform, screenBounds, isX);		
		drawLabel(axis.label, min, max, LABEL_OFFSET*5, g2, viewTransform, screenBounds, isX); //TODO: The '5' is a magic number...remove it by doing some whole-axis analysis
	}
	
	/**Draws text at the given position.
	 * Text is drawn in unscaled space, but positioning is done with respect to the view transform.
	 * This is an interpretation of Bertin-style 'point' implantation, applied to text.
	 */
	private static final void drawLabel(Object label, double val1, double val2, double offset, Graphics2D g2, AffineTransform vt, Rectangle2D screenBounds, boolean isX) {
		AffineTransform restore = g2.getTransform();
		g2.setTransform(new AffineTransform());
		
		String labelText;
		if (label instanceof Integer || label instanceof Long) {
			labelText = String.format("%,d", label);
		} else if (label instanceof Number) {
			labelText = String.format("%.3f", label);
		} else {
			labelText = label.toString();
		}
		
		Rectangle2D stringBounds = g2.getFontMetrics().getStringBounds(labelText, g2); 

		Point2D p1, p2;
		AffineTransform t = new AffineTransform(vt);
		if (isX) {
			t.scale(1,1/vt.getScaleY());
			t.translate(0, -vt.getTranslateY()+screenBounds.getHeight()-AXIS_SPACE);

			p1 = new Point2D.Double(val1, offset);
			p2 = new Point2D.Double(val2, offset);
		} else {
			t.scale(1/vt.getScaleX(), 1);
			t.translate(-vt.getTranslateX()+AXIS_SPACE,0);

			p1 = new Point2D.Double(offset, val1);
			p2 = new Point2D.Double(offset, val2);			
		}
		
		t.transform(p1, p1);
		t.transform(p2, p2);
		
		double x,y;
		if (isX) {
			x = (p1.getX()+p2.getX())/2 - (stringBounds.getHeight()/4); //HACK: Divide by 4????  It just looks better...
			y = Math.min(p1.getY(), p2.getY()) + offset;
			t = AffineTransform.getTranslateInstance(x, y);
			t.rotate(Math.PI/2);
		} else {
			x = Math.min(p1.getX(), p2.getX()) - (stringBounds.getWidth()+offset+offset); //HACK: TWICE!!! Not sure why...
			y = (p1.getY()+p2.getY())/2 + (stringBounds.getHeight()/4); //HACK: Divide by 4????  It just looks better...
			t = AffineTransform.getTranslateInstance(x, y);
		}

		g2.setTransform(t);
		g2.drawString(labelText, 0,0);
		g2.setTransform(restore);
	}
	
	/**Draws a line between two points.  The line is always drawn in unscaled space, 
	 * so the line thickness is not affect by the view transform BUT the points are scaled to match 
	 * the view transform.  Otherwise said, this method achieves Bertin-style 'line' implantation.   
	 */
	private static final void drawLine(double val1, double val2, double toward, double away, Graphics2D g2, AffineTransform vt, Rectangle2D screenBounds, boolean isX) {
		AffineTransform t = new AffineTransform(vt);
		Point2D p1, p2;
		if (isX) {
			t.scale(1, 1/vt.getScaleY());
			t.translate(0, -vt.getTranslateY()+screenBounds.getHeight()-AXIS_SPACE);
			p1 = new Point2D.Double(val1, toward);
			p2 = new Point2D.Double(val2, -away);
		} else {
			t.scale(1/vt.getScaleX(), 1);
			t.translate(-vt.getTranslateX()+AXIS_SPACE,0);
			p1 = new Point2D.Double(away, val1);
			p2 = new Point2D.Double(-toward, val2);
		}
		
		t.transform(p1, p1);
		t.transform(p2, p2);
		
		AffineTransform restore = g2.getTransform();
		g2.setTransform(new AffineTransform());
		Line2D l = new Line2D.Double(p1, p2);
		g2.draw(l);
		g2.setTransform(restore);
	}	
}
