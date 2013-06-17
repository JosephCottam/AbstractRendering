package ar.test;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.junit.Test;
import ar.util.Util;

public class Utils {
	@Test
	public void initImage() throws Exception {
		innerImageTest(10,11,Color.red);
		innerImageTest(100,200,Color.white);
		innerImageTest(33,12,Color.blue);
	}
	
	public static void innerImageTest(int width, int height, Color color) {
		BufferedImage img = Util.initImage(width, height, color);
		assertEquals("Width", width, img.getWidth());
		assertEquals("Height", height, img.getHeight());
		for (int y=0; y< img.getHeight(); y++) {
			for (int x=0; x<img.getWidth(); x++) {
				assertEquals(color.getRGB(), img.getRGB(x, y));
			}
		}
	}
	
}

