package ar.benchmarks;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.File;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.Transfer;
import ar.Transfer.Specialized;
import ar.aggregates.implementations.RefFlatAggregates;
import ar.app.components.Presets;
import ar.app.util.GlyphsetUtils;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;
import ar.glyphsets.*;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Valuer.Constant;
import ar.glyphsets.implicitgeometry.Indexed.ToRect;
import ar.renderers.ParallelRenderer;
import ar.renderers.SerialRenderer;
import ar.rules.Advise;
import ar.rules.Categories;
import ar.rules.CategoricalCounts.CoC;
import ar.rules.CategoricalCounts.RLE;
import ar.rules.Numbers;
import ar.selectors.TouchesPixel;
import ar.util.Util;

/**Tests the amount of time to render specific configurations.
 * **/
@SuppressWarnings("unused")
public class RenderSpeed {
	private static String arg(String[] args, String flag, String def) {
		flag = flag.toUpperCase();
		for (int i=0; i<args.length; i++) {
			if (args[i].toUpperCase().equals(flag)) {return args[i+1];}
		}
		return def;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) throws Exception {
		int iterations = Integer.parseInt(arg(args, "-iters", "10"));
		int cores = Integer.parseInt(arg(args, "-p", Integer.toString(Runtime.getRuntime().availableProcessors())));
		String config = arg(args, "-config", "USCensusPopulationLinear");
		String rend = arg(args, "-rend", "parallel").toUpperCase();
		int width = Integer.parseInt(arg(args, "-width", "800"));
		int height = Integer.parseInt(arg(args, "-height", "800"));
		boolean header = Boolean.valueOf(arg(args, "-header", "true"));
		
		Presets.Preset source = null;
		for (Class clss: Presets.class.getClasses()) {
			if (clss.getSimpleName().equals(config)) {
				source = (Presets.Preset) clss.getConstructor().newInstance();
			}
		}
		if (source == null) {throw new IllegalArgumentException("Could not find -config indicated: " + config);}

		
		Aggregator aggregator = source.aggregator();
		Transfer transfer = source.transfer();
		Glyphset glyphs = source.glyphset();
	
		ParallelRenderer.THREAD_POOL_PARALLELISM = cores > 0 ? cores : ParallelRenderer.THREAD_POOL_PARALLELISM;
		
		Renderer render;
		if (rend.startsWith("PARALLEL")) {
			render = new ParallelRenderer();
		} else if (rend.startsWith("SERIAL")) {
			render = new SerialRenderer();
		} else {
			throw new IllegalArgumentException("Renderer type not known: " + rend);
		}
		
		AffineTransform vt = Util.zoomFit(glyphs.bounds(), width, height);
		Selector s = TouchesPixel.make(glyphs);
		long taskCount = glyphs.size()/render.taskSize(glyphs);
		
		if (header) {
			System.out.println("source, elapse/avg agg, elapse/avg trans, iter num, width, height, renderer, cores, tasks");
		}
		
		try {
			for (int i=0; i<iterations; i++) {
				long start = System.currentTimeMillis();
				Aggregates<Integer> aggs = render.aggregate(glyphs, s, aggregator, vt, width, height);
				long end = System.currentTimeMillis();
				long aggTime = end-start;

				start = System.currentTimeMillis();
				Transfer.Specialized<Number,Color> ts = transfer.specialize(aggs);
				Aggregates<Color> colors = render.transfer(aggs, ts);
				end = System.currentTimeMillis();
				long transTime = end-start;

				aggs.get(0, 0);
				colors.get(0, 0);
				System.out.printf("%s, %d, %d, %d, %d, %d, %s, %d, %d\n", source, aggTime, transTime, i, width, height, rend, cores, taskCount);
				System.out.flush();
			}
		} catch (Exception e) {
			System.out.println("Error testing " + source);
			e.printStackTrace();
		}
		System.exit(0);
	}
}

