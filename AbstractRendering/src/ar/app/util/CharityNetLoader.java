package ar.app.util;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import ar.GlyphSet;
import ar.GlyphSet.Glyph;
import ar.glyphsets.QuadTree;

public class CharityNetLoader {
	private static double interpolate(double spanMin, double spanMax, double min, double max, double v) {
		return spanMin + ((v/max)*(spanMax-spanMin));
	}
	
	
	public static GlyphSet load(String filename) {
		CSVtoGlyphSet.Reader loader = new CSVtoGlyphSet.Reader(filename, 1);
		
		String[] header = loader.next();
		double maxDate = Integer.parseInt(header[0]);
		double maxState = Integer.parseInt(header[1]);
		final int span=9;
		
		GlyphSet glyphs = QuadTree.make(10000);
		Color olive=new Color(107,142,35);
		
		int count = 0;
		while(loader.hasNext()) {
			count++;
			if (count % 500000 ==0) {System.out.println("\t loaded " + count + " records");}
			String[] parts = loader.next();
			
			if (parts == null || parts.length <2) {continue;}

			int date = Integer.parseInt(parts[0]);
			int state = -Integer.parseInt(parts[1]);
			
			double ddate = interpolate(-span, span, 0, maxDate, date);
			double dstate = interpolate(-span, span, 0, maxState, state);
			
			Rectangle2D r = new Rectangle2D.Double(ddate,dstate,.1,.1);
			Glyph g = new ar.GlyphSet.Glyph(r, olive);
			glyphs.add(g);
		}
		System.out.printf("Read %d entries (items in the dataset %d)\n", count, glyphs.size());
		
		return glyphs;
	}
	
	public static GlyphSet loadNorm(String filename) {
		CSVtoGlyphSet.Reader loader = new CSVtoGlyphSet.Reader(filename, 1);
		
		final int span=1;
		
		GlyphSet glyphs = QuadTree.make(10000);
		Color olive=new Color(107,142,35);
		
		int count = 0;
		while(loader.hasNext()) {
			count++;
			if (count % 500000 ==0) {System.out.println("\t loaded " + count + " records");}
			String[] parts = loader.next();
			
			if (parts == null || parts.length <2) {continue;}

			double date = Double.parseDouble(parts[0]);
			double state = -Double.parseDouble(parts[1]);
			
			Rectangle2D r = new Rectangle2D.Double(date,state,.01,.01);
			Glyph g = new ar.GlyphSet.Glyph(r, olive);
			glyphs.add(g);
		}
		System.out.printf("Read %d entries (items in the dataset %d)\n", count, glyphs.size());
		
		return glyphs;
	}
	
	private static final List<String> STATES = Arrays.asList(new String[]{"AL","AK","AZ","AR","CA","CO","CT","DC","DE","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY"});

	public static GlyphSet loadDirect(String filename) {
		double span = 9;
		GlyphSet glyphs = QuadTree.make(10);
		CSVtoGlyphSet.Reader loader = new CSVtoGlyphSet.Reader(filename, 1);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
		
		long maxDate = Long.MIN_VALUE;
		long minDate = Long.MAX_VALUE;
		
		int count = 0;
		while(loader.hasNext()) {
			count++;
			if (count % 10000 ==0) {System.out.println("\t parsed " + count + " records");}
			
			String[] parts = loader.next();
			if (parts ==null || parts.length != 2) {
				System.out.println("Skipping at " + count);
				continue;
			}
			
			
			int state = STATES.indexOf(parts[1]);	
			long date;
			try {date = df.parse(parts[0]).getTime();}
			catch (ParseException e) {
				System.out.println("Skipping at for date parse: " + count);
				continue;
			}

			if (state < 0) {continue;} //Filter out invalid states			
			maxDate = Math.max(maxDate, date);
			minDate = Math.min(minDate, date);
		}
		
		loader = new CSVtoGlyphSet.Reader(filename, 1);
		final double maxState = STATES.size();
		count = 0;

		Color olive=new Color(107,142,35);
		while(loader.hasNext()) {
			count++;
			if (count % 10000 ==0) {System.out.println("\t loaded " + count + " records");}

			
			String[] parts = loader.next();
			if (parts ==null || parts.length != 2) {continue;}
			
			
			int state = STATES.indexOf(parts[1]);	
			long date;
			try {date = df.parse(parts[0]).getTime();}
			catch (ParseException e) {continue;}
			
			double stateY = interpolate(-span,span, 0, maxState, state);
			double dateX = interpolate(-span, span, minDate, maxDate, date);
			Rectangle2D r = new Rectangle2D.Double(dateX, stateY,1,1);
			glyphs.add(new ar.GlyphSet.Glyph(r, olive));
		}
		
		return glyphs;
	}
}
