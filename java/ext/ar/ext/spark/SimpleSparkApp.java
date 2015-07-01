package ar.ext.spark;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import ar.Aggregates;
import ar.Selector;
import ar.app.display.TransferDisplay;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.glyphsets.implicitgeometry.Indexed.*;
import ar.rules.Numbers;
import ar.selectors.TouchesPixel;
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
		if (args.length >0) {
			String first = args[0].toLowerCase();
			if (first.equals("-h") || first.equals("-help") || first.equals("--help")) {
				System.err.println("Parameters: -host <host> -in <data.csv> -out <out> -sh <spark-home> -jars <jars>");
				System.err.println("Parameters are order independent and all have reasonable defaults.");
				System.exit(1);
			}
		}
		
		int width = Integer.parseInt(arg(args, "-width", "500"));
		int height = Integer.parseInt(arg(args, "-height", "500"));
		String host = arg(args, "-host", "local");
		String inFile = arg(args, "-in", "../data/circlepoints.csv");
		String outFile= arg(args, "-out", null);
		String sparkhome = arg(args,  "-spark", System.getenv("SPARK_HOME"));
		String jars[] = arg(args, "-jars", "AR.jar:ARApp.jar:ARExt.jar").split(":");
		
		JavaSparkContext ctx = new JavaSparkContext(host, "Abstract-Rendering", sparkhome, jars);
		JavaRDD<String> source = ctx.textFile(inFile);
		JavaRDD<Indexed> base = source.map(new StringToIndexed("\\s*,\\s*"));
		Shaper<Indexed, Rectangle2D> shaper = new ToRect(.1, .1, false, 2, 3);
		Valuer<Indexed,Integer> valuer = new Valuer.Constant<Indexed,Integer>(1);

		GlyphsetRDD<Rectangle2D, Integer> glyphs = new GlyphsetRDD<>(base.map(new Glypher<>(shaper, valuer)));
		AffineTransform view = Util.zoomFit(glyphs.bounds(), width, height);
		Selector<Rectangle2D> selector = TouchesPixel.make(glyphs.exemplar().shape().getClass());
		
 		RDDRender render = new RDDRender();
 		Aggregates<Integer> aggs = render.aggregate(glyphs, selector, new Numbers.Count<Integer>(), view);

		
		if (outFile == null) {
			TransferDisplay.show("", width, height, aggs, new Numbers.Interpolate<>(new Color(230,230,255), Color.BLUE));
		} else {
			AggregatesToCSV.export(aggs, new File(outFile));
		}
	}
}
