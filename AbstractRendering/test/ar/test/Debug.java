package ar.test;

import java.awt.Color;
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
}
