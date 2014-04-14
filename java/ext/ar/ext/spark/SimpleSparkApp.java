package ar.ext.spark;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Transfer;
import ar.app.components.sequentialComposer.OptionDataset;
import ar.app.components.sequentialComposer.OptionTransfer;
import ar.app.display.TransferDisplay;
import ar.ext.spark.hbin.DataInputRecord;
import ar.ext.spark.hbin.HBINInputFormat;
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

	@SuppressWarnings("rawtypes")
	public static void main(String[] args){
		if (args.length >0) {
			String first = args[0].toLowerCase();
			if (first.equals("-h") || first.equals("-help") || first.equals("--help")) {
				System.err.println("Parameters: -server <server> -in <data.csv> -out <out> -spark <spark-home> -jars <jar:jar...> -partitions <true|false>");
				System.err.println("Parameters are order independent and all have reasonable defaults.");
				System.exit(1);
			}
		}
		
		int width = Integer.parseInt(arg(args, "-width", "500"));
		int height = Integer.parseInt(arg(args, "-height", "500"));
		String host = arg(args, "-server", "local");
		String config = arg(args, "-config", "CENSUS_SYN_PEOPLE");
		String outFile= arg(args, "-out", null);
		String sparkhome = arg(args,  "-spark", System.getenv("SPARK_HOME"));
		String jars[] = arg(args, "-jars", "AR.jar:ARApp.jar:ARExt.jar").split(":");
		RDDRender.MAP_PARTITIONS = Boolean.parseBoolean(arg(args, "-partitions", "false"));
		
		JavaSparkContext ctx = new JavaSparkContext(host, "Abstract-Rendering", sparkhome, jars);
		Shaper<Rectangle2D, Indexed> shaper = new ToRect(.1, .1, false, 2, 3);
		Valuer<Indexed,Integer> valuer = new Valuer.Constant<Indexed,Integer>(1);
		
		
		OptionDataset dataset;
		try {
			dataset= (OptionDataset) OptionDataset.class.getField(config).get(null);
		} catch (
				IllegalAccessException |
				IllegalArgumentException |
				NoSuchFieldException | NullPointerException | SecurityException e) {
			throw new IllegalArgumentException("Could not find -config indicated: " + config);
		}

		
		JavaPairRDD<Long, Indexed> base = ctx.hadoopFile(dataset.sourceFile.getAbsolutePath(), HBINInputFormat.class, Long.class, Indexed.class);
//		JavaRDD<Indexed> base = source.map(new StringToIndexed("\\s*,\\s*"));
//		Aggregator aggregator = source.defaultAggregator().aggregator();
//		Glyphset glyphs = source.dataset();
//		Transfer transfer = OptionTransfer.toTransfer(source.defaultTransfers(), null);

		JavaRDD<String> source = ctx.textFile("");
		JavaRDD<Indexed> base = source.map(new StringToIndexed("\\s*,\\s*"));
		GlyphsetRDD<Rectangle2D, Integer> glyphs = new GlyphsetRDD<>(base.map(new Glypher<>(shaper,valuer)).cache());
		AffineTransform view = Util.zoomFit(glyphs.bounds(), width, height);

 		
 		RDDRender render = new RDDRender();
 		Aggregates<Integer> aggs = render.aggregate(glyphs, TouchesPixel.make(glyphs), new Numbers.Count<Integer>(), view, width, height);

		
		if (outFile == null) {
			TransferDisplay.show("", width, height, aggs, new Numbers.Interpolate<>(new Color(230,230,255), Color.BLUE));
		} else {
			AggregatesToCSV.export(aggs, new File(outFile));
		}
	}
}
