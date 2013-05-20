package ar.app;

import java.awt.Color;
import java.io.File;
import java.io.FilenameFilter;

import ar.glyphsets.MemMapList;
import ar.glyphsets.Painter;

public class BoundSpeedTest {
	public static void main(String[] args) throws Exception {
		int iterations = args.length >0 ? Integer.parseInt(args[0]) : 10;
		File root = new File("./data/");
		File[] files = root.listFiles(new FilenameFilter(){
			public boolean accept(File dir, String name) {return name.toUpperCase().endsWith("XY.HBIN");}
		});
		
		for (File source: files) {
			long total=0;
			try {
				for (int i=0; i<iterations; i++) {
					MemMapList glyphs = new MemMapList(source, .1d, new Painter.Constant<Double>(Color.RED));
					long start = System.currentTimeMillis();
					glyphs.bounds();
					long end = System.currentTimeMillis();
					System.out.printf("%s, %d, %d\n", source.getName(), end-start, i);
					total += (end-start);
				}
			System.out.printf("\t\t%s (avg), %s\n",source.getName(), total/((double) iterations));
			} catch (Exception e) {System.out.println("Error testing " + source.getName());}
		}		
	}
}
