package ar.ext.spark;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;

import spark.api.java.JavaRDD;
import spark.api.java.JavaSparkContext;
import ar.Aggregates;
import ar.Glyph;
import ar.app.display.TransferDisplay;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.glyphsets.implicitgeometry.Indexed.*;
import ar.rules.Numbers;
import ar.util.AggregatesToCSV;
import ar.util.Util;

/**Main class for driving an ARSpark application.**/
public class SimpleSparkApp {
	private static String arg(String[] args, String flag, String def) {
		flag = flag.toUpperCase();
		for (int i=0; i<args.length; i++) {
			if (args[i].toUpperCase().equals(flag)) {return args[i+1];}
		}
		return def;
	}

	public static void main(String[] args){
		if (args.length == 0) {
			System.err.println("Usage: JavaTC -host <host> -in <data> -out <out> -sh <spark-home> -jars <jars>");
			System.exit(1);
		}
		
		int width = Integer.parseInt(arg(args, "-width", "500"));
		int height = Integer.parseInt(arg(args, "-height", "500"));
		String host = arg(args, "-host", "localhost");
		String inFile = arg(args, "-in", null);
		String outFile= arg(args, "-out", null);
		String sparkhome = arg(args, "-spark", System.getenv("SPARK_HOME"));
		String jars[] = arg(args, "-jars", "").split(":");
		
		JavaSparkContext ctx = new JavaSparkContext(host, "Abstract-Rendering", sparkhome, jars);
		JavaRDD<String> source = ctx.textFile(inFile);
		JavaRDD<Indexed> base = source.map(new StringToIndexed("\\s*,\\s*"));
		Shaper<Rectangle2D, Indexed> shaper = new ToRect(.1, .1, false, 2, 3);
		Valuer<Indexed,Integer> valuer = new Valuer.Constant<Indexed,Integer>(1);

		JavaRDD<Glyph<Rectangle2D, Integer>> glyphs = base.map(new Glypher<>(shaper,valuer)).cache();
 		Rectangle2D contentBounds = RDDRender.bounds(glyphs);
		AffineTransform view = Util.zoomFit(contentBounds, width, height);

 		
 		RDDRender render = new RDDRender();
 		Aggregates<Integer> aggs = render.aggregate(glyphs, new Numbers.Count<>(), view, width, height);

		
		if (outFile == null) {
			TransferDisplay.show("", width, height, aggs, new Numbers.Interpolate<>(new Color(230,230,255), Color.BLUE));
		} else {
			AggregatesToCSV.export(aggs, new File(outFile));
		}
	}
}
