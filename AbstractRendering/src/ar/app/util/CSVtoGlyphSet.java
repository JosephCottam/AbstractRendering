package ar.app.util;

import java.awt.Color;
import java.awt.geom.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import ar.GlyphSet;

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
	
	public static GlyphSet load(String filename, int skip, double size, int xField, int yField) {
		CSVtoGlyphSet loader = new CSVtoGlyphSet(filename, skip);
		GlyphSet glyphs = QuadTree.make(10, 0,0,10);
		//GlyphSet glyphs = new GlyphList();
		while (loader.hasNext()) {
			String[] parts = loader.next();
			if (parts == null) {continue;}
			
			double x = Double.parseDouble(parts[xField]);
			double y = Double.parseDouble(parts[yField]);
			Rectangle2D rect = new Rectangle2D.Double(x,y,size,size); 
	        Glyph g = new ar.GlyphSet.Glyph(rect, Color.red);
	        glyphs.add(g);
		}
		System.out.println(glyphs);
		return glyphs;
	}
	
}
