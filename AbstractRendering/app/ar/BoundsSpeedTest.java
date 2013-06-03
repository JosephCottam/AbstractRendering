package ar;

import java.io.File;
import java.io.FilenameFilter;

import ar.Glyphset;
import ar.glyphsets.*;
import ar.util.ImplicitGeometry;

public class BoundsSpeedTest {
	public static void main(String[] args) throws Exception {
		int iterations = args.length >0 ? Integer.parseInt(args[0]) : 10;
		
		File root = new File("./data/");
		File[] files = root.listFiles(new FilenameFilter(){
			public boolean accept(File dir, String name) {
				return name.toUpperCase().endsWith("FULL.HBIN");
			}
		});
		
		for (File source: files) {
			long total=0;
			try {
			for (int i=0; i<iterations; i++) {
				Glyphset<?> glyphs = new PointMemMapList(source, .1, new ImplicitGeometry.Constant<Double>());
//					Glyphset<?> glyphs = new GenMemMapList(source, 
//							new ImplicitGeometry.IndexedToRect(.1, false, 0, 1), 
//							new ImplicitGeometry.Constant<Double>());
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
