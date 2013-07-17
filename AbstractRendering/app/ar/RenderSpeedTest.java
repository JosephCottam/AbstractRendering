package ar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;

import javax.swing.JFrame;

import ar.app.components.ARPanel;
import ar.app.util.GlyphsetUtils;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;
import ar.glyphsets.*;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Valuer.Constant;
import ar.glyphsets.implicitgeometry.Indexed.ToRect;
import ar.renderers.ParallelGlyphs;
import ar.renderers.ParallelSpatial;
import ar.renderers.SerialSpatial;

/**Tests the amount of time to render count visualizations.
 * 
 * MUST ENABLE THE SIGNAL IN ARPanel FOR THIS TEST TO WORK CORRECTLY 
 * 
 * **/
public class RenderSpeedTest {
	private static String arg(String[] args, String flag, String def) {
		flag = flag.toUpperCase();
		for (int i=0; i<args.length; i++) {
			if (args[i].toUpperCase().equals(flag)) {return args[i+1];}
		}
		return def;
	}
	
	public static void main(String[] args) throws Exception {
		int iterations = Integer.parseInt(arg(args, "-iters", "10"));
		int cores = Integer.parseInt(arg(args, "-p", Integer.toString(Runtime.getRuntime().availableProcessors())));
		int task = Integer.parseInt(arg(args, "-task", "100000000"));
		String rend = arg(args, "-rend", "glyph").toUpperCase();
		String source = arg(args, "-data", "../data/circlepoints.hbin");
		
		Aggregator<?,?> aggregator = new WrappedAggregator.Count().op();
		Transfer<?,?> transfer = new WrappedTransfer.RedWhiteLinear().op();
	
		ARPanel.PERF_MON = true;
		ParallelGlyphs.THREAD_POOL_SIZE = cores;
		ParallelSpatial.THREAD_POOL_SIZE = cores;
		
		Renderer render;
		Glyphset<Color> glyphs;
		if (rend.startsWith("GLYPH")) {
			render = new ParallelGlyphs(task);
			glyphs = new MemMapList<Color>(
						new File(source), 
						new ToRect(.005, .005, false, 0, 1), 
						new Constant<Indexed>(Color.red), Color.class);
		} else if (rend.startsWith("PIXEL")) {
			render = new ParallelSpatial(task);
			glyphs = GlyphsetUtils.load(null, source, .005);
		} else if (rend.startsWith("SPIXEL")) {
			render = new SerialSpatial();
			glyphs = GlyphsetUtils.load(null, source, .005);
		} else {
			throw new IllegalArgumentException("Renderer type not known: " + rend);
		}
		glyphs.bounds(); //Force bounds calc to only happen once...hopefully
		
		JFrame f = new JFrame();
		f.setLayout(new BorderLayout());
		f.setSize(500,500);
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	
		
		System.out.println("source, elapse/avg, iter num, renderer, cores, task-size");
		long total=0;
		try {
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
				System.out.printf("%s, %d, %d, %s, %d, %d\n", source, end-start, i, rend, cores, task);
				total += (end-start);
			}
			System.out.printf("%s (avg), %s, n/a, %s, %d, %d\n",source, total/((double) iterations), rend, cores, task);
		} catch (Exception e) {
			System.out.println("Error testing " + source);
			e.printStackTrace();
		}
		System.exit(0);
	}
}
