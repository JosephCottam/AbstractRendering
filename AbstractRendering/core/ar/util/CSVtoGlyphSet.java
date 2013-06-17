package ar.util;

import java.awt.Color;
import java.awt.geom.*;
import java.io.File;

import ar.Glyphset;
import ar.app.util.ColorNames;
import ar.glyphsets.*;

import static ar.Glyphset.Glyph;

public class CSVtoGlyphSet {
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

	public static Glyphset autoLoad(File source, double glyphSize, Glyphset glyphs) {
		try {
			DelimitedReader r = new DelimitedReader(source, 0, DelimitedReader.CSV);
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
				MemMapList list = new MemMapList(source, ((MemMapList) glyphs).shaper(), ((MemMapList) glyphs).valuer());
				System.out.printf("Setup list of %d entries.\n", list.size());
				return list;
			} else {
				return load(glyphs, source, skip, glyphSize, flipY, xField, yField, colorField, valueField);
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
		
		DelimitedReader loader = new DelimitedReader(file, 0, DelimitedReader.CSV);
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


	public static Glyphset load(final Glyphset glyphs, File file, int skip, double size, boolean flipy, int xField, int yField, int colorField, int valueField) {
		DelimitedReader loader = new DelimitedReader(file, skip, DelimitedReader.CSV);
		final int yflip = flipy?-1:1;
		int count =0;

		while (loader.hasNext()) {
			String[] parts = loader.next();
			if (parts == null) {continue;}

			double x = Double.parseDouble(parts[xField]);
			double y = Double.parseDouble(parts[yField]) * yflip;
			Rectangle2D rect = new Rectangle2D	.Double(x,y,size,size);
			Object value;
			if (colorField >=0) {
				try {
					value = ColorNames.byName(parts[colorField], Color.red);
				} catch (Exception e) {throw new RuntimeException("Error loading color: " + parts[colorField]);}
			} else if (valueField > 0) {
				value = parts[valueField];
			} else {value = Color.RED;}

			Glyph<Color> g = new SimpleGlyph(rect, value);
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
