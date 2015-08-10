package ar.benchmarks;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.Arrays;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.Transfer;
import ar.Transfer.Specialized;
import ar.aggregates.implementations.RefFlatAggregates;
import ar.app.components.sequentialComposer.OptionDataset;
import ar.app.components.sequentialComposer.OptionTransfer;
import ar.app.util.GlyphsetUtils;
import ar.glyphsets.*;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Valuer.Constant;
import ar.glyphsets.implicitgeometry.Indexed.ToRect;
import ar.renderers.ForkJoinRenderer;
import ar.renderers.SerialRenderer;
import ar.rules.Advise;
import ar.rules.Categories;
import ar.rules.CategoricalCounts;
import ar.rules.Numbers;
import ar.selectors.TouchesPixel;
import ar.util.Util;

/**Tests the amount of time to render specific configurations.
 * **/
@SuppressWarnings("unused")
public class RenderSpeed {
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) throws Exception {
		int iterations = Integer.parseInt(Util.argKey(args, "-iters", "10"));
		int cores = Integer.parseInt(Util.argKey(args, "-p", Integer.toString(Runtime.getRuntime().availableProcessors())));
		String config = Util.argKey(args, "-config", "CENSUS_SYN_PEOPLE");
		String rend = Util.argKey(args, "-rend", "parallel").toUpperCase();
		int width = Integer.parseInt(Util.argKey(args, "-width", "800"));
		int height = Integer.parseInt(Util.argKey(args, "-height", "800"));
		boolean header = Boolean.valueOf(Util.argKey(args, "-header", "true"));
		int tasksPerThread = Integer.parseInt(Util.argKey(args,"-tasksMult", "-1"));
		int synPoints = Integer.parseInt(Util.argKey(args,"-pc", "-1"));

		cores = cores > 0 ? cores : ForkJoinRenderer.RENDER_POOL_SIZE;
		tasksPerThread = tasksPerThread > 0 ? tasksPerThread : ForkJoinRenderer.RENDER_THREAD_LOAD;
		
		OptionDataset.SYNTHETIC = synPoints > 0 ? OptionDataset.syntheticPoints(synPoints) : OptionDataset.SYNTHETIC;

		
		OptionDataset source;
		try {
			source = (OptionDataset) OptionDataset.class.getField(config).get(null);
		} catch (NoSuchFieldException | NullPointerException | SecurityException e) {
			throw new IllegalArgumentException("Could not find -config indicated: " + config);
		}

		
		Aggregator aggregator = source.defaultAggregator.aggregator();
		Glyphset glyphs = source.glyphset;
		Transfer transfer = OptionTransfer.toTransfer(source.defaultTransfers, null);
	
		
		Renderer render;
		if (rend.startsWith("PARALLEL")) {
			render = new ForkJoinRenderer();
		} else if (rend.startsWith("SERIAL")) {
			render = new SerialRenderer();
		} else {
			throw new IllegalArgumentException("Renderer type not known: " + rend);
		}
		
		AffineTransform vt = Util.zoomFit(glyphs.bounds(), width, height);
		Selector s = TouchesPixel.make(glyphs);
		long taskCount = cores * tasksPerThread;
		
		if (header) {
			System.out.println("source, elapse/avg agg, elapse/avg trans, iter num, width, height, renderer, cores, tasks (max)");
		}
		
		try {
			for (int i=0; i<iterations; i++) {
				long start = System.currentTimeMillis();
				Aggregates<Integer> aggs = render.aggregate(glyphs, s, aggregator, vt);
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

