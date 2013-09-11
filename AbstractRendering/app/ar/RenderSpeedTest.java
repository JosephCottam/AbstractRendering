package ar;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.File;

import ar.app.util.GlyphsetUtils;
import ar.app.util.WrappedAggregator;
import ar.glyphsets.*;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Valuer.Constant;
import ar.glyphsets.implicitgeometry.Indexed.ToRect;
import ar.renderers.ParallelGlyphs;
import ar.renderers.ParallelSpatial;
import ar.renderers.SerialSpatial;
import ar.util.Util;

/**Tests the amount of time to render count visualizations.
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
		int task = Integer.parseInt(arg(args, "-task", "100000"));
		String rend = arg(args, "-rend", "glyph").toUpperCase();
		String source = arg(args, "-data", "../data/circlepoints.hbin");
		int width = Integer.parseInt(arg(args, "-width", "500"));
		int height = Integer.parseInt(arg(args, "-height", "500"));
		boolean header = Boolean.valueOf(arg(args, "-header", "true"));
		Aggregator<Object,Integer> aggregator = new WrappedAggregator.Count().op();
		//Transfer<Integer,Color> transfer = new WrappedTransfer.RedWhiteLinear().op();
	
		ParallelGlyphs.THREAD_POOL_SIZE = cores;
		ParallelSpatial.THREAD_POOL_SIZE = cores;
		
		Renderer render;
		Glyphset<Color> glyphs;
		if (rend.startsWith("GLYPH")) {
			render = new ParallelGlyphs(task);
			glyphs = new MemMapList<Color>(
						new File(source), 
						new ToRect(.005, .005, false, 0, 1), 
						new Constant<Indexed,Color>(Color.red));
		} else if (rend.startsWith("PIXEL")) {
			render = new ParallelSpatial(task);
			glyphs = GlyphsetUtils.autoLoad(new File(source), .005, DynamicQuadTree.<Color>make()); 
		} else if (rend.startsWith("SPIXEL")) {
			render = new SerialSpatial();
			glyphs = GlyphsetUtils.autoLoad(new File(source), .005, DynamicQuadTree.<Color>make()); 
		} else {
			throw new IllegalArgumentException("Renderer type not known: " + rend);
		}
		glyphs.bounds(); //Force bounds calc to only happen once...hopefully
		AffineTransform ivt = Util.zoomFit(glyphs.bounds(), width, height).createInverse();
		
		if (header) {
			System.out.println("source, elapse/avg, iter num, renderer, cores, task-size");
		}
		
		long total=0;
		try {
			for (int i=0; i<iterations; i++) {
				long start = System.currentTimeMillis();
				Aggregates<Integer> aggs = render.aggregate(glyphs, aggregator, ivt, width, height);
				long end = System.currentTimeMillis();
				aggs.get(0, 0);
				System.out.printf("%s, %d, %d, %s, %d, %d\n", source, end-start, i, rend, cores, task);
				System.out.flush();
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
