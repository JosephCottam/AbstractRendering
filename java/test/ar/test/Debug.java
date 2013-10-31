package ar.test;

import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;

import ar.util.Util;

public class Debug {
	public static void ASCCIImage(BufferedImage i) {
		StringBuilder b = new StringBuilder();
		for (int x=0;x<i.getWidth(); x++) {
			b.append("[");
			for (int y=0;y<i.getHeight(); y++) {
				if (i.getRGB(x, y) != Util.CLEAR.getRGB()) {
					b.append(" # ");
				} else {
					b.append(" . ");
				}
			}
			b.append("]\n");
		}
		System.out.println(b.toString());
	}
	
	
	public void printPath(GeneralPath p) {
		PathIterator it = p.getPathIterator(new AffineTransform());
		float[] coords = new float[6]; 
		while (!it.isDone()) {
			int type = it.currentSegment(coords);
			System.out.println(typeString(type) + ":" + deepToString(coords));
			it.next();
		}	
	}
	
	private static String typeString(int type) {
		if (type == PathIterator.SEG_CLOSE) {return "close";}
		if (type == PathIterator.SEG_LINETO) {return "line to";}
		if (type == PathIterator.SEG_MOVETO) {return "move to";}
		return "other";
	}
	
	private static String deepToString(float[] coords) {
		StringBuilder b=new StringBuilder();
		for(int i=0; i<coords.length; i++) {
			b.append(coords[i]);
			b.append(", ");
		}
		return b.toString();
	}
}
