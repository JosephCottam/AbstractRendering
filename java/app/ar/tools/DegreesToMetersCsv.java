package ar.tools;

import static ar.app.components.sequentialComposer.OptionDataset.CENSUS_TRACTS;
import ar.glyphsets.implicitgeometry.Cartography;
import ar.rules.CategoricalCounts;
import ar.util.Util;

import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import ar.Glyph;
import ar.Glyphset;

public class DegreesToMetersCsv {
	public static void main(String[] args) throws FileNotFoundException {
		Glyphset<Point2D, CategoricalCounts<Character>> glyphs = CENSUS_TRACTS.glyphset;
		PrintStream s = new PrintStream(Util.argKey(args, "-out", "tracts.csv"));
		s.println("x,y,Race,Count");
		int counter = 0;
		System.out.println("Started!");
		for (Glyph<Point2D, CategoricalCounts<Character>> g: glyphs) {
			Point2D degrees = g.shape();
			Point2D meters = Cartography.DegreesToMeters.from(degrees);
			s.printf("%s,%s,%s,%s%n", meters.getX(), meters.getY(), g.info().key(0), g.info().count(0));
			if (counter%1000000 ==0) {System.out.print(".");}
			counter++;
		}
		System.out.println("DONE!");
		s.close();
	}
}
