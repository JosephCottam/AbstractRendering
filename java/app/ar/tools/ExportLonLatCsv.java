package ar.tools;
import static ar.app.components.sequentialComposer.OptionDataset.*;

import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import ar.Glyph;
import ar.Glyphset;

public class ExportLonLatCsv {
	public static void main(String[] args) throws FileNotFoundException {
		Glyphset<Point2D, Character> glyphs = CENSUS_SYN_PEOPLE.glyphset;
		PrintStream s = new PrintStream("test.csv");
		s.println("Lon,Lat,Race");
		int counter = 0;
		System.out.println("Started!");
		for (Glyph<Point2D, Character> g: glyphs) {
			s.printf("%s,%s,%s%n", g.shape().getX(), g.shape().getY(), g.info());
			if (counter%1000000 ==0) {System.out.print(".");}
			counter++;
		}
		System.out.println("DONE!");
		s.close();
	}
}
