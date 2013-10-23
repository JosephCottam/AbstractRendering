package ar.app.util;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.Arrays;

import ar.Glyphset;
import ar.glyphsets.MemMapList;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Indexed.Converter;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.DelimitedReader;
import ar.util.MemMapEncoder;
import ar.util.Util;

public class GlyphsetUtils {
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

	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <G,T> Glyphset<G,T> autoLoad(File source, double glyphSize, Glyphset<G,T> glyphs) {
		try {
			DelimitedReader r = new DelimitedReader(source, 0, DelimitedReader.CSV);
			String[] line = r.next();
			int skip;
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
			
			if (glyphs instanceof MemMapList) {
				MemMapList<G,T> list = new MemMapList<>(source, ((MemMapList<G,T>) glyphs).shaper(), ((MemMapList<G,T>) glyphs).valuer());
				System.out.printf("Setup list of %d entries.\n", list.size());
				return list;
			} else {
				int valField = Math.max(colorField, valueField);
				int max = Math.max(xField, valField);
				max = Math.max(max, yField);
				Converter.TYPE[] types = new Converter.TYPE[max+1];
				Arrays.fill(types, Converter.TYPE.X);
				types[xField] = Converter.TYPE.DOUBLE;
				types[yField] = Converter.TYPE.DOUBLE;
				
				if (colorField >=0) {types[colorField] = Converter.TYPE.COLOR;}
				if (valueField >=0) {types[valueField] = Converter.TYPE.DOUBLE;}
				
				Valuer valuer;
				if (valField >=0) {valuer = new Indexed.ToValue<Indexed,T>(valField);}
				else {valuer = new Valuer.Constant<>(Color.red);}
				
				
				return Util.load(
						(Glyphset<Rectangle2D,T>) glyphs, 
						new DelimitedReader(source, skip, DelimitedReader.CSV), 
						new Converter(types),
						new Indexed.ToRect(glyphSize, glyphSize, true, xField, yField), 
						valuer);
			}

		} 
		catch (RuntimeException e) {throw e;}
		catch (Exception e) {throw new RuntimeException(e);}
	}

	public static final <G,V> Glyphset<G,V> memMap(String label, String file, Shaper<G,Indexed> shaper, Valuer<Indexed, V> valuer, int skip, String types) {
		System.out.printf("Memory mapping %s...", label);
		File f = new File(file);

		try {
			long start = System.currentTimeMillis();
			Glyphset<G,V> g = new MemMapList<>(f, shaper, valuer);
			long end = System.currentTimeMillis();
			if (label != null) {System.out.printf("prepared %s entries (%s ms).\n", g.size(), end-start);}
			return g;
		} catch (Exception e) {
			try {
				if (types != null) {
					System.out.println("Error loading.  Attempting re-encode...");
					File source = new File(file.replace(".hbin", ".csv"));
					MemMapEncoder.write(source, skip, f, types.toCharArray());
					return new MemMapList<>(f, shaper, valuer);
				} else {throw e;}
			} catch (Exception ex) {
				System.out.println("Faield to load data.");
				return null;
			}
		}
	}
}
