package ar.aggregates.implementations;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Iterator;

import ar.Aggregates;
import ar.aggregates.Iterator2D;

/**Set of colors, with extra tools for creating images.**/
public class ColorAggregates extends IntegerBackingAggregates implements Aggregates<Color> {
	private final Color background;

	public ColorAggregates(int lowX, int lowY, Color background, BufferedImage img) {
		this(lowX, lowY, lowX+img.getWidth(), lowY+img.getHeight(), background, img);
	}

	public ColorAggregates(int lowX, int lowY, int highX, int highY, Color background) {
		this(lowX, lowY, highX, highY, background, null);
	}

	private ColorAggregates(int lowX,int lowY, int highX, int highY, Color background, BufferedImage img) {
		super(lowX, lowY, highX, highY, background.getRGB());
		this.background = background;
		if (img != null) {
			//TODO: Implement initializing aggregates off of an image.
			throw new UnsupportedOperationException("Can't initialize off of an image...yet...");
		}
	}

	public Color get(int x, int y) {return new Color(super.getInt(x, y), true);}
	public void set(int x, int y, Color val) {
		super.set(x, y, val.getRGB());
	}
	public Iterator<Color> iterator() {return new Iterator2D<>(this);};
	public Color defaultValue() {return background;}

	public BufferedImage image() {
		int w = highX-lowX;
		int h = highY-lowY;
		if (w<=0 && h<=0) {return null;}
		
		BufferedImage img = new BufferedImage(w,h, BufferedImage.TYPE_INT_ARGB);
		img.setRGB(0, 0, w, h, values, 0, w);
		
		return img;
	}
}
