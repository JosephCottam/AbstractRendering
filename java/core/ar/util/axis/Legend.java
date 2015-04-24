package ar.util.axis;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.JLabel;
import javax.swing.JPanel;

import ar.util.Util;

public class Legend {
public static final class ColorSwatch extends JPanel {
		private final Color c;
		public ColorSwatch(Color c) {this.c = c;}		
		@Override public void paintComponent(Graphics g) {
			g.setColor(c);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
		}
	}
	
	/**
	 * Note on colors:
	 *  * Color 0 -- Entry label color (defaults to gra)
	 *  * Color 1 -- Overall label color (defaults to 0)
	 *  * Color 2 -- Outline of examples (defaults to clear)
	 * 
	 * @param axis Descriptor for the axis to draw 
	 * @param g2 Graphics object to draw on
	 * @param bounds Bounding box of the space to draw exemplars, must be in g2's coordinate space 
	 * @param colors -- Optional colors for fine-tuning appearance
	 */
	public static void drawLegend(Descriptor descriptor, Graphics2D g, Rectangle2D bounds, Color... colors) {
		g = (Graphics2D) g.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		Color label = colors.length > 0 ? colors[0] : Color.gray;
		Color title = colors.length > 1 ? colors[1] : label;
		Color outline = colors.length > 2 ? colors[2] : Util.CLEAR; 
		
		//TODO: Font metrics to determine single-item spans (currently hard-coded at 10)
		double low = Double.MAX_VALUE;
		double high = Double.MIN_VALUE;
		double span = 0;
		int callout = 0;
		for (Entry entry: descriptor.entries) {
			if (entry instanceof Entry.Range) {
				double ehigh = ((Entry.Range<?>) entry).high.label.doubleValue();
				double elow = ((Entry.Range<?>) entry).low.label.doubleValue();
				high = Math.max(high, ehigh);
				low = Math.min(low, elow);
				span = span + ((Entry.Range<?>) entry).span();
			} else {
				callout++;
			}
		}
		
		//TODO: Make more flexible by variable sizing the callouts
		double cell = 15;
		double remains = bounds.getHeight()-(callout*cell);
		
		//TODO: Font metrics and center....
		g.setPaint(title);
		
		double cursor =0;
		if (descriptor.label.isPresent()) {
			g.drawString(descriptor.label.get(), (int) bounds.getCenterX(), (int) (bounds.getMinY() + cell));
			cursor = cursor + (cell * 2); //Give a little space under the overall title
		} 
		
		for (Entry entry: descriptor.entries) {
			double step;
			if (entry instanceof Entry.Item) {
				step = cell;
			} else {
				step = (remains-cursor) * (((Entry.Range<?>) entry).span()/span);
			}
			
			Rectangle2D region = new Rectangle2D.Double(bounds.getMinX(), cursor, 10, step);
			entry.draw(g, region, label, outline);				
			cursor = cursor + step;				
		}
	}
	
	public static Dimension measure(Descriptor descriptor, int exemplarWidth, Graphics g) {
		FontMetrics m = g.getFontMetrics();
		int width = 0;
		if (descriptor.label.isPresent()) {width = m.stringWidth(descriptor.label.get());}
		for (Legend.Entry entry: descriptor.entries) {
			width = Math.max(width, entry.width(m));
		}
		Dimension d = new Dimension(width+exemplarWidth*2, 0);
		return d;
	}
	
	
	public static final Descriptor EMPTY = new Descriptor(null);
	
	//TODO: combine legend and axis descriptor.  Think hard about d3 'scales' and how to use that idea in IG in general (complications on the AR side, but the IG side should be straightforward)
	public static final class Descriptor {
		public Optional<String> label;
		public List<Entry> entries;
		public Descriptor(String label, Entry... entries) {
			this.label = Optional.ofNullable(label);
			this.entries = Collections.unmodifiableList(Arrays.asList(entries));
		}
	}

	public static <T> Entry.Item<T> entry(T label, Color value) {return new Entry.Item<>(label, value);}
	public static <T extends Number> Entry.Range<T> entry(Entry.Item<T> high, Entry.Item<T> low) {return new Entry.Range<>(high, low);}
	public static <T extends Number> Entry.Range<T> entry(T lowLabel, Color lowValue, T highLabel, Color highValue) {
		return entry(entry(lowLabel, lowValue), entry(highLabel, highValue));
	}

	public static interface Entry {
		
		/***
		 * @param g			Graphics object to draw on
		 * @param bounds	Total space for legend exemplars
		 * @param label		Label color
		 * @param outline	Outline color
		 */
		public void draw(Graphics2D g, Rectangle2D bounds, Color label, Color outline);
		
		/**How much space to draw the labels of this entry.**/
		public int width(FontMetrics m);
		
		public static final class Range<T extends Number> implements Entry {
			public final Item<T> high;
			public final Item<T> low;
			
			public Range(Item<T> low, Item<T> high) {
				this.high = high;
				this.low = low;
			}
			
			public double span() {return high.label.doubleValue() - low.label.doubleValue();}
			public int width(FontMetrics m) {return Math.max(m.stringWidth(high.label.toString()), m.stringWidth(low.label.toString()));}
			
			@Override
			public void draw(Graphics2D g, Rectangle2D bounds, Color labelColor, Color outlineColor) {
				GradientPaint gradient = new GradientPaint(
						(float) bounds.getCenterX(), (float) bounds.getMinY(), high.color, 
						(float) bounds.getCenterX(), (float) bounds.getMaxY(), low.color);
				g.setPaint(gradient);
				g.fill(bounds);
				
				g.setPaint(outlineColor);
				g.draw(bounds);
				
				g.setPaint(labelColor);
				g.drawString(high.label.toString(), (int) bounds.getMaxX() + 10, (int) bounds.getMinY()+10); 		//TODO: FONT METRICS to align top/center (replace the +10)
				g.drawString(low.label.toString(), (int) bounds.getMaxX() + 10, (int) bounds.getMaxY());				
			}
		}
		
		public static final class Item<T> implements Entry {
			public final T label;
			public final Color color;
			public Item(T label, Color c) {
				this.label = label;
				this.color = c;
			}
			
			public int width(FontMetrics m) {return m.stringWidth(label.toString());}
			
			@Override
			public void draw(Graphics2D g, Rectangle2D bounds, Color labelColor, Color outlineColor) {
				g.setPaint(color);
				g.fill(bounds);
				
				g.setPaint(outlineColor);
				g.draw(bounds);
				
				g.setPaint(labelColor);
				g.drawString(label.toString(), (int) bounds.getMaxX() + 10, (int) bounds.getCenterY() + 5); //TODO: FONT METRICS to align top/center (replace the +10)
			}
	 	}		
	}

	/**Simple panel that defers to the generic Legend drawer.**/
	public static class Panel extends JLabel {
		protected Optional<Legend.Descriptor> desc;
		protected int width;
		protected Color[] colors;
		
		/** @param desc Legend descriptor
		 *  @param width How wide are exemplars?
		 *  @param colors Legend colors
		 */
		public Panel(Descriptor desc, int width, Color... colors) {
			this.desc = Optional.ofNullable(desc);
			this.width = width;
			this.colors = colors;
		}		

		/**Change the descriptor being used.**/
		public void update(Legend.Descriptor desc) {this.desc = Optional.ofNullable(desc);}
		
		@Override public Dimension getPreferredSize() {return minimumSize();}
		
		@Override
		public Dimension minimumSize() {
			if (desc.isPresent()) {return Legend.measure(desc.get(), width, this.getGraphics());}
			else {return new Dimension(0,0);}
		}
		
		@Override
		public void paintComponent(Graphics g) {
			desc.ifPresent(
				desc -> Legend.drawLegend(
							desc, 
							(Graphics2D) g, 
							new Rectangle2D.Double(10, 10, width, this.getHeight()-10), //TODO: Fix those 10s they're arbitrary... 
							colors)	
			);
		}
		
		@Override public String toString() {return "Legend panel for: " + desc.orElse(EMPTY);}
	}
}

