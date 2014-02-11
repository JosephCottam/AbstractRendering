package ar.benchmarks;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FilenameFilter;

import ar.Glyphset;
import ar.glyphsets.*;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Valuer;

public class BoundsSpeed {
	public static void main(String[] args) throws Exception {
		int iterations = args.length >0 ? Integer.parseInt(args[0]) : 10;
		
		File root = new File("../data/");
		File[] files = root.listFiles(new FilenameFilter(){
			@SuppressWarnings("unused")
			public boolean accept(File dir, String name) {
				return name.toUpperCase().endsWith("FULL.HBIN");
			}
		});
		
		for (File source: files) {
			long total=0;
			try {
			for (int i=0; i<iterations; i++) {
				Glyphset<Rectangle2D, Color> glyphs = new MemMapList<>(
							source, 
							new Indexed.ToRect(.1,.1, false, 0, 1), 
							new Valuer.Constant<Indexed,Color>(Color.red));
				long start = System.currentTimeMillis();
				glyphs.bounds();
				long end = System.currentTimeMillis();
				System.out.printf("%s: %d (iter %d)\n", source.getName(), (end-start), i);
				total += (end-start);
				Thread.yield();
			}
			System.out.printf("\t\t%s (avg; %d iters), %s\n",source.getName(), iterations, total/((double) iterations));
			} catch (Exception e) {System.out.println("Error testing " + source.getName()); e.printStackTrace();}
		}		
	
	}
}
