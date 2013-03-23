package ar.app.util;

import java.awt.Color;
import java.awt.geom.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import ar.GlyphSet;
import ar.glyphsets.MultiQuadTree;

import static ar.GlyphSet.Glyph;

public class CSVtoGlyphSet {
	private BufferedReader reader;
	private final Pattern splitter = Pattern.compile("\\s*,\\s*");
		
	private CSVtoGlyphSet(String filename, int skip) {
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
	
	public static GlyphSet load(String filename, int skip, double size, int xField, int yField, int colorField) {
		CSVtoGlyphSet loader = new CSVtoGlyphSet(filename, skip);
		GlyphSet glyphs = MultiQuadTree.make(10, 0,0,10);
		//GlyphSet glyphs = QuadTree.make(100, 0,0,10);
		//GlyphSet glyphs = new GlyphList();
		int count =0;
		while (loader.hasNext()) {
			String[] parts = loader.next();
			if (parts == null) {continue;}
			
			double x = Double.parseDouble(parts[xField]);
			double y = Double.parseDouble(parts[yField]);
			Rectangle2D rect = new Rectangle2D.Double(x,y,size,size);
			Color color;
			if (colorField >=0) {
				try {
					color = (Color) Color.class.getField(parts[colorField].toUpperCase()).get(null);
				} catch (Exception e) {throw new RuntimeException("Error loading color: " + parts[colorField]);}
			} else {color = Color.RED;}
			
	        Glyph g = new ar.GlyphSet.Glyph(rect, color);
	        glyphs.add(g);
	        count++;
		}
		
		if (count != glyphs.size()) {throw new RuntimeException("Error loading data; Read and retained glyph counts don't match.");}
		System.out.printf("Read %d entries\n", count);
		
		//System.out.println(glyphs);

		return glyphs;
	}
	
}
