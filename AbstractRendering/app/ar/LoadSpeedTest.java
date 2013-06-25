package ar;

import java.io.File;
import java.io.FilenameFilter;

import ar.glyphsets.*;
import ar.util.GlyphsetLoader;

public class LoadSpeedTest {
	public static void main(String[] args) throws Exception {
		int iterations = args.length >0 ? Integer.parseInt(args[0]) : 10;
		File root = new File("./data/");
		File[] files = root.listFiles(new FilenameFilter(){
			public boolean accept(File dir, String name) {return name.toUpperCase().endsWith(".CSV");}
		});
		
		for (File source: files) {
			long total=0;
			try {
			for (int i=0; i<iterations; i++) {
				long start = System.currentTimeMillis();
				GlyphsetLoader.autoLoad(source, .005, DynamicQuadTree.make(Object.class));
				long end = System.currentTimeMillis();
				System.out.printf("%s, %d, %d\n", source.getName(), end-start, i);
				total += (end-start);
			}
			System.out.printf("\t\t%s (avg), %s\n",source.getName(), total/((double) iterations));
			} catch (Exception e) {System.out.println("Error testing " + source.getName());}
		}		
	}
}
