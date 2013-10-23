package ar;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

import ar.aggregates.AggregateUtils;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;
import ar.glyphsets.*;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Valuer.Constant;
import ar.glyphsets.implicitgeometry.Indexed.ToRect;
import ar.renderers.ParallelGlyphs;
import ar.rules.General;
import ar.rules.Numbers;
import ar.util.AggregatesToCSV;
import ar.util.Util;

@SuppressWarnings("unused")
public class BatchExport {
	private static String arg(String[] args, String flag, String def) {
		flag = flag.toUpperCase();
		for (int i=0; i<args.length; i++) {
			if (args[i].toUpperCase().equals(flag)) {return args[i+1];}
		}
		return def;
	}
	
	public static void main(String[] args) throws Exception {
		String[] widths = arg(args, "-width", "400").split(",");
		String[] heights = arg(args, "-height", "400").split(",");
		String source = arg(args, "-data", "../data/circlepoints.hbin");
		String outPattern = arg(args, "-out", "./result%s.csv");
		double size = Double.parseDouble(arg(args, "-size", ".1"));
		
		if (widths.length != heights.length) {
			System.err.println("Must provide same number of widths as heights\n"); 
			System.exit(-1);
		}
		if (outPattern.split("%").length !=2) {
			System.err.println("Output must be a format pattern with exactly one format variable.");
			System.exit(-2);
		}
		
		Aggregator<Object,Integer> aggregator = new WrappedAggregator.Count().op();
			
		Renderer render = new ParallelGlyphs();
		Glyphset<Rectangle2D, Color> glyphs = new MemMapList<Rectangle2D, Color>(
					new File(source), 
					new ToRect(size, size, false, 0, 1), 
					new Constant<Indexed,Color>(Color.red));
		glyphs.bounds(); //Force bounds calc to only happen once...hopefully

		try {
			for (int i=0; i< widths.length; i++) {
				int width = Integer.parseInt(widths[i]);
				int height = Integer.parseInt(heights[i]);
				System.out.printf("Processing %s at %dx%d\n", source, width, height);
				AffineTransform ivt = Util.zoomFit(glyphs.bounds(), width, height).createInverse();
				Aggregates<Integer> aggs = render.aggregate(glyphs, aggregator, ivt, width, height);
				String filepart = String.format("%dx%d", width, height);
				String filename = String.format(outPattern, filepart);
				System.out.printf("\t Writing to %s\n", filename);
				if (filename.endsWith("csv")) {
					AggregatesToCSV.export(aggs, new File(filename));
				} else {
					//Transfer<Number, Color> t = new WrappedTransfer.FixedAlpha().op();
					//Transfer<Number, Color> t = new WrappedTransfer.DrawDarkVar().op();
					//Transfer<Number, Color> t = new WrappedTransfer.RedWhiteLinear().op();
					Transfer<Number, Color> t = new WrappedTransfer.OverUnder().op();
					Transfer.Specialized<Number, Color> ts = t.specialize(aggs);
					Aggregates<Color> colors = render.transfer(aggs, ts);
					BufferedImage img = AggregateUtils.asImage(colors, width, height, Color.white);
					Util.writeImage(img, new File(filename));
				}
				System.out.printf("\t Done!\n", filename);
			}
		} catch (Exception e) {
			System.out.println("Error testing " + source);
			e.printStackTrace();
		}
		System.exit(0);
	}
}
