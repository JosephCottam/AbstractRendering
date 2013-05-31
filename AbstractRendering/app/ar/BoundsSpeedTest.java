package ar;

import java.io.File;
import java.io.FilenameFilter;

import ar.Glyphset;
import ar.glyphsets.*;
import ar.util.ImplicitGlyphs;

public class BoundsSpeedTest {
	public static void main(String[] args) throws Exception {
		int iterations = args.length >0 ? Integer.parseInt(args[0]) : 10;
		File root = new File("./data/");
		File[] files = root.listFiles(new FilenameFilter(){
			public boolean accept(File dir, String name) {
				return name.toUpperCase().endsWith(".HBIN");
			}
		});
		
		for (File source: files) {
			long total=0;
			try {
			for (int i=0; i<iterations; i++) {
				Glyphset<?> glyphs = new MemMapList(source, .1d, new ImplicitGlyphs.Constant<Double>());
				long start = System.currentTimeMillis();
				glyphs.bounds();
				long end = System.currentTimeMillis();
				total += (end-start);
			}
			System.out.printf("\t\t%s (avg), %s\n",source.getName(), total/((double) iterations));
			} catch (Exception e) {System.out.println("Error testing " + source.getName()); e.printStackTrace();}
		}		
	}
}
