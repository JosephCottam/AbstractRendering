package ar.aggregates;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Iterator;

import ar.Aggregates;

/**Set of color aggregates backed by a buffered image.**/
public class ImageAggregates implements Aggregates<Color> {
	private final BufferedImage img;
	private final Color background;
	private final int lowX, lowY, highX, highY;

	public ImageAggregates(int lowX, int lowY, Color background, BufferedImage img) {
		this(lowX, lowY, lowX+img.getWidth(), lowY+img.getHeight(), background, img);
	}

	public ImageAggregates(int lowX, int lowY, int highX, int highY, Color background) {
		this(lowX, lowY, highX, highY, background, new BufferedImage(highX-lowX, highY-lowY, BufferedImage.TYPE_INT_ARGB));

		Graphics g = img.createGraphics();
		g.setColor(background);
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		g.dispose();
	}

	private ImageAggregates(int lowX,int lowY, int highX, int highY, Color background, BufferedImage img) {
		this.img = img;
		this.background = background;
		this.lowX = lowX;
		this.lowY = lowY;
		this.highX = highX;
		this.highY = highY;
	}

	public Color get(int x, int y) {
		if (x<lowX || x >=highX || y<lowY || y>=highY) {return background;}
		return new Color(img.getRGB(x-lowX, y-lowY), true);
	}

	public void set(int x, int y, Color val) {
		if (x<lowX || x >=highX || y<lowY || y>=highY) {return;}
		img.setRGB(x-lowX, y-lowY, val.getRGB());
	}

	public Iterator<Color> iterator() {return new Iterator2D<>(this);};
	public Color defaultValue() {return background;}
	public int lowX() {return lowX;}
	public int lowY() {return lowY;}
	public int highX() {return highX;}
	public int highY() {return highY;}
	public BufferedImage image() {return img;}
}
