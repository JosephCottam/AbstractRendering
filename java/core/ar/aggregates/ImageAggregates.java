package ar.aggregates;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Iterator;

import ar.Aggregates;

/**Set of color aggregates backed by a buffered image.**/
public class ImageAggregates implements Aggregates<Color> {
	private BufferedImage img;
	private final int[] colors;
	private final Color background;
	private final int lowX, lowY, highX, highY;

	public ImageAggregates(int lowX, int lowY, Color background, BufferedImage img) {
		this(lowX, lowY, lowX+img.getWidth(), lowY+img.getHeight(), background, img);
	}

	public ImageAggregates(int lowX, int lowY, int highX, int highY, Color background) {
		this(lowX, lowY, highX, highY, background, new BufferedImage(highX-lowX, highY-lowY, BufferedImage.TYPE_INT_ARGB));
	}

	private ImageAggregates(int lowX,int lowY, int highX, int highY, Color background, BufferedImage img) {
		this.background = background;
		this.lowX = lowX;
		this.lowY = lowY;
		this.highX = highX;
		this.highY = highY;
		
		int size = (highX-lowX)*(highY-lowY);
		this.colors = new int[size];
	}

	public Color get(int x, int y) {
		if (x<lowX || x >=highX || y<lowY || y>=highY) {return background;}
		return new Color(colors[idx(x,y)]);
	}

	public void set(int x, int y, Color val) {
		if (x<lowX || x >=highX || y<lowY || y>=highY) {return;}
		colors[idx(x,y)] = val.getRGB();
		img = null;
	}

	public Iterator<Color> iterator() {return new Iterator2D<>(this);};
	public Color defaultValue() {return background;}
	public int lowX() {return lowX;}
	public int lowY() {return lowY;}
	public int highX() {return highX;}
	public int highY() {return highY;}
	public BufferedImage image() {
		if (img != null) {return img;}
		
		int w = highX-lowX;
		int h = highY-lowY;
		img = new BufferedImage(w,h, BufferedImage.TYPE_INT_ARGB);
		img.setRGB(0, 0, w, h, colors, 0, w);
		
		return img;
	}

	private final int idx(int x,int y) {
		int idx = ((highX-lowX)*(y-lowY))+(x-lowX);
		return idx;
	}
}
