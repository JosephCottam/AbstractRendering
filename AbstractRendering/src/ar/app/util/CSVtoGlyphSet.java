package ar.app.util;

import java.awt.Color;
import java.awt.geom.*;
import java.io.BufferedReader;
import java.io.File;
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

		public Reader(File file, int skip) {
			try {
				reader = new BufferedReader(new FileReader(file));
				while (skip-- > 0) {reader.readLine();}
			} catch (IOException e) {throw new RuntimeException("Error intializing glyphset from " + file.getName(), e);}
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
	
	private static boolean isNumber(String s) {
		try {Double.parseDouble(s); return true;}
		catch (Exception e) {return false;}
	}
	
	private static int fieldMatch(final String key, final String[] names, final int defaultValue) {
		int bestMatch = -1;
		boolean exact = false;
		for (int i=names.length-1; i>-0; i--) {
			String name = names[i].toUpperCase();
			if (name.equals(key)) {bestMatch = i; exact = true;}
			else if (!exact && name.contains(key)) {bestMatch = i;}
		}
		if (bestMatch == -1) {return defaultValue;}
		else {return bestMatch;}
	}

	public static GlyphSet autoLoad(File source, double glyphSize, GlyphSet glyphs) {
		try {
			Reader r = new Reader(source, 0);
			String[] line = r.next();
			int skip;
			boolean flipY=true;
			int xField, yField, colorField, valueField;
			if (isNumber(line[0])) {
				xField =0;
				yField =1;
				colorField = line.length >= 3 ? 2 : -1;
				valueField = line.length >= 4 ? 3 : -1;
				skip =0;
			} else {
				xField = fieldMatch("X", line, 0);
				yField = fieldMatch("Y", line, 1);
				colorField = fieldMatch("C", line, -1);
				valueField = fieldMatch("V", line, -1);
				skip =1;
			}
			
			if (glyphs instanceof DirectMatrix) {
				return loadMatrix(
						source, skip, glyphSize, 
						xField, yField, valueField,
						0, new ToInt(), false);
			} else if (glyphs instanceof MemMapList) {
				MemMapList list = new MemMapList(source, glyphSize);
				System.out.printf("Setup list of %d entries.\n", list.size());
				return list;
			} else {
				return load(glyphs, source, skip, glyphSize, flipY, xField, yField, colorField);
			}

		} 
		catch (RuntimeException e) {throw e;}
		catch (Exception e) {throw new RuntimeException(e);}
	}

	public static interface Converter<T> {public T convert(String[] items, int idx, T defaultValue);}
	public static class ToInt implements Converter<Integer> {
		public Integer convert(String[] items, int idx, Integer defaultValue) {
			try {return Integer.parseInt(items[idx]);}
			catch (Exception e) {return defaultValue;}
		}
	}
	
	//Loads a matrix from a file.  Assumes the first line tells the matrix dimensions
	@SuppressWarnings("unchecked")
	public static <T> DirectMatrix<T> loadMatrix(File file, int skip, double size, 
			int rowField, int colField, int valueField, 
			T defaultValue, Converter<T> converter,
			boolean nullIsValue) {
		
		Reader loader = new Reader(file, 0);
		String[] header = loader.next();
		int rows = Integer.parseInt(header[1]);
		int cols = Integer.parseInt(header[2]);
		Object[][] matrix = new Object[rows][cols];
		
		while (skip>0) {loader.next(); skip--;}
		int count = 0;
		while (loader.hasNext()) {
			String[] line = loader.next();
			if (line == null) {continue;}
			
			int row = Integer.parseInt(line[rowField]);
			int col = Integer.parseInt(line[colField]);
			T value = valueField >=0 ? converter.convert(line, valueField, defaultValue) : defaultValue;
			matrix[row][col] = value;
			count++;
		}
		
		System.out.printf("Read %d entries into a %d x %d matrix.\n", count, rows, cols);
		return new DirectMatrix<T>((T[][]) matrix,size,size, nullIsValue);
	}


	public static GlyphSet load(final GlyphSet glyphs, File file, int skip, double size, boolean flipy, int xField, int yField, int colorField) {
		Reader loader = new Reader(file, skip);
		final int yflip = flipy?-1:1;
		int count =0;

		while (loader.hasNext()) {
			String[] parts = loader.next();
			if (parts == null) {continue;}
			if (skip >0) {skip--; continue;}

			double x = Double.parseDouble(parts[xField]);
			double y = Double.parseDouble(parts[yField]) * yflip;
			Rectangle2D rect = new Rectangle2D	.Double(x,y,size,size);
			Color color;
			if (colorField >=0) {
				try {
					color = ColorNames.byName(parts[colorField], Color.red);
				} catch (Exception e) {throw new RuntimeException("Error loading color: " + parts[colorField]);}
			} else {color = Color.RED;}

			Glyph g = new ar.GlyphSet.Glyph(rect, color);
			try {glyphs.add(g);}
			catch (Exception e) {throw new RuntimeException("Error loading item number " + count, e);}
			count++;
			//if (count % 100000 == 0) {System.out.println(System.currentTimeMillis() + " -- Loaded: " + count);}
		}

		//The check below causes an issue if memory is tight...the check has a non-trivial overhead on some glyphset types
		if (count != glyphs.size()) {throw new RuntimeException(String.format("Error loading data; Read and retained glyph counts don't match (%s read vs %s retained).", count, glyphs.size()));}
		System.out.printf("Read %d entries\n", count);

		return glyphs;
	}

}
