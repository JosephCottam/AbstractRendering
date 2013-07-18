package ar;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.File;

import ar.app.util.AggregatesToCSV;
import ar.app.util.WrappedAggregator;
import ar.glyphsets.*;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Valuer.Constant;
import ar.glyphsets.implicitgeometry.Indexed.ToRect;
import ar.renderers.ParallelGlyphs;
import ar.util.Util;

public class BatchExport {
	private static String arg(String[] args, String flag, String def) {
		flag = flag.toUpperCase();
		for (int i=0; i<args.length; i++) {
			if (args[i].toUpperCase().equals(flag)) {return args[i+1];}
		}
		return def;
	}
	
	public static void main(String[] args) throws Exception {
		String[] rawRes = arg(args, "-res", "400").split(",");
		String source = arg(args, "-data", "../data/circlepoints.hbin");
		String outPrefix = arg(args, "-out", "./result");
		double size = Double.parseDouble(arg(args, "-size", ".1"));
		
		Aggregator<Object,Integer> aggregator = new WrappedAggregator.Count().op();
			
		Renderer render = new ParallelGlyphs();
		Glyphset<Color> glyphs = new MemMapList<Color>(
					new File(source), 
					new ToRect(size, size, false, 0, 1), 
					new Constant<Indexed>(Color.red), Color.class);
		glyphs.bounds(); //Force bounds calc to only happen once...hopefully

		try {
			for (String rres: rawRes) {
				int res = Integer.parseInt(rres);
				System.out.printf("Processing %s at %d\n", source, res);
				AffineTransform ivt = Util.zoomFit(glyphs.bounds(), res, res).createInverse();
				Aggregates aggs = render.reduce(glyphs, aggregator, ivt, res, res);
				String filename = String.format("%s_%d.csv", outPrefix, res);
				System.out.printf("\t Writing to %s\n", filename);
				AggregatesToCSV.export(aggs, new File(filename));
				System.out.printf("\t Done!\n", filename);
			}
		} catch (Exception e) {
			System.out.println("Error testing " + source);
			e.printStackTrace();
		}
		System.exit(0);
	}
}
