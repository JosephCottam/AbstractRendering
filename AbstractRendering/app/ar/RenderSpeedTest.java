package ar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.JFrame;

import ar.app.components.ARPanel;
import ar.app.util.CSVtoGlyphSet;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;
import ar.glyphsets.*;
import ar.renderers.ParallelGlyphs;
import ar.rules.AggregateReducers;
import ar.util.ImplicitGeometry;

/**Tests the amount of time to render count visualizations.
 * 
 * MUST ENABLE THE SIGNAL IN ARPanel FOR THIS TEST TO WORK CORRECTLY 
 * 
 * **/
public class RenderSpeedTest {
	public static void main(String[] args) throws Exception {
		int iterations = args.length >0 ? Integer.parseInt(args[0]) : 10;
		File root = new File("./data/");
		File[] files = root.listFiles(new FilenameFilter(){
			public boolean accept(File dir, String name) {return name.toUpperCase().endsWith(".HBIN");}
		});
		
		JFrame f = new JFrame();
		f.setLayout(new BorderLayout());
		f.setSize(500,500);
		f.setVisible(true);
		
		WrappedAggregator aggregator = new WrappedAggregator.Count();
		WrappedTransfer transfer = new WrappedTransfer.RedWhiteInterpolate();
		AggregateReducer reduction = new AggregateReducers.Count();
		
		Renderer render = new ParallelGlyphs(1000, reduction);

		for (File source: files) {
			long total=0;
			try {
				//Glyphset glyphs = new MemMapList(source, .005, new ImplicitGeometry.Constant(Color.red));
				Glyphset glyphs = new GenMemMapList(source, new ImplicitGeometry.IndexedToRect(.005, false, 0, 1), new ImplicitGeometry.Constant(Color.red));
				glyphs.bounds();
				for (int i=0; i<iterations; i++) {
					ARPanel panel = new ARPanel(aggregator, transfer, glyphs, render);
					f.add(panel, BorderLayout.CENTER);
					long start = System.currentTimeMillis();
					f.validate();
					panel.zoomFit();
					f.repaint();
					synchronized(panel) {panel.wait();}
					f.remove(panel);
					long end = System.currentTimeMillis();
					System.out.printf("%s, %d, %d\n", source.getName(), end-start, i);
					total += (end-start);
				}
			System.out.printf("\t\t%s (avg), %s\n",source.getName(), total/((double) iterations));
			} catch (Exception e) {
				System.out.println("Error testing " + source.getName());
				e.printStackTrace();
			}
		}		
	}
}
