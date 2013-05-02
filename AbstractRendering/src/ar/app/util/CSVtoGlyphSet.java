package ar.app.util;

import java.awt.Color;
import java.awt.geom.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import ar.GlyphSet;
import ar.glyphsets.*;

import static ar.GlyphSet.Glyph;

public class CSVtoGlyphSet {
	public static class Reader {
		private BufferedReader reader;
		private final Pattern splitter = Pattern.compile("\\s*,\\s*");

		public Reader(String filename, int skip) {
			try {
				reader = new BufferedReader(new FileReader(filename));
				while (skip-- > 0) {reader.readLine();}
			} catch (IOException e) {throw new RuntimeException("Error intializing glyphset from " + filename, e);}
		}
		
		protected String[] next() {
			String line = null;
			try {line = reader.readLine();}
			catch (Exception e) {return done();}
			if (line == null) {return done();}
			else {return splitter.split(line);}
		}
		
		//Always returns null...
		protected String[] done() {
			try {reader.close();}
			catch (IOException e) {throw new RuntimeException(e);}
			finally {reader = null;}
			return null;
		}
		protected boolean hasNext() {return reader != null;}
	}
	
	public static GlyphSet load(String filename, int skip, double size, boolean flipy, int xField, int yField, int colorField) {
		GlyphSet glyphs = DynamicQuadTree.make(10);
		//GlyphSet glyphs = MultiQuadTree.make(10, 0,0,12);
		//GlyphSet glyphs = SingleHomedQuadTree.make(100, 0,0,10);
		//GlyphSet glyphs = new GlyphList();
    //
		Reader loader = new Reader(filename, skip);
    final int yflip = flipy?-1:1;
		int count =0;
		
    while (loader.hasNext()) {
			String[] parts = loader.next();
			if (parts == null) {continue;}
			
			double x = Double.parseDouble(parts[xField]);
			double y = Double.parseDouble(parts[yField]) * yflip;
			Rectangle2D rect = new Rectangle2D.Double(x,y,size,size);
			Color color;
			if (colorField >=0) {
				try {
					color = ColorNames.byName(parts[colorField], Color.red);
				} catch (Exception e) {throw new RuntimeException("Error loading color: " + parts[colorField]);}
			} else {color = Color.RED;}
			
	        Glyph g = new ar.GlyphSet.Glyph(rect, color);
	        glyphs.add(g);
	        count++;
		}
		
		if (count != glyphs.size()) {throw new RuntimeException(String.format("Error loading data; Read and retained glyph counts don't match (%s read vs %s retained).", count, glyphs.size()));}
		System.out.printf("Read %d entries (items in the dataset %d)\n", count, glyphs.size());
		
		//System.out.println(glyphs);

		return glyphs;
	}
	
}
