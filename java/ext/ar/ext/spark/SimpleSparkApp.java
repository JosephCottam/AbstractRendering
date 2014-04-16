package ar.ext.spark;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;

import scala.Tuple2;
import ar.Aggregates;
import ar.Aggregator;
import ar.Selector;
import ar.Transfer;
import ar.app.components.sequentialComposer.OptionDataset;
import ar.app.components.sequentialComposer.OptionTransfer;
import ar.app.display.TransferDisplay;
import ar.ext.spark.hbin.DataInputRecord;
import ar.ext.spark.hbin.HBINInputFormat;
import ar.glyphsets.implicitgeometry.Indexed;
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
	public static <G,I,A> void main(String[] args) throws IOException{
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
		String config = arg(args, "-config", "BOOST_MEMORY");
		String outFile= arg(args, "-out", null);
		String sparkhome = arg(args,  "-spark", System.getenv("SPARK_HOME"));
		String jars[] = arg(args, "-jars", "AR.jar:ARApp.jar:ARExt.jar").split(":");
		boolean partition = Boolean.parseBoolean(arg(args, "-partitions", "true"));
		
		JavaSparkContext ctx = new JavaSparkContext(host, "Abstract-Rendering", sparkhome, jars);
		
		
		OptionDataset<G,I> dataset;
		try {
			dataset= (OptionDataset) OptionDataset.class.getField(config).get(null);
		} catch (
				IllegalAccessException |
				IllegalArgumentException |
				NoSuchFieldException | NullPointerException | SecurityException e) {
			throw new IllegalArgumentException("Could not find -config indicated: " + config);
		}

		
		JavaRDD<Indexed> base;
		if (!dataset.sourceFile.getName().endsWith(".csv")) {
			JavaPairRDD<LongWritable, DataInputRecord> source = ctx.hadoopFile(dataset.sourceFile.getAbsolutePath(), HBINInputFormat.class, LongWritable.class, DataInputRecord.class);
			base = (JavaRDD<Indexed>) (JavaRDD) source.map(new Function<Tuple2<LongWritable, DataInputRecord>, DataInputRecord>() {
				public DataInputRecord call(Tuple2<LongWritable, DataInputRecord> pair) throws Exception {return pair._2;}
			});
		} else {
			JavaRDD<String> source = ctx.textFile(dataset.sourceFile.getCanonicalPath());
			base = source.map(new StringToIndexed("\\s*,\\s*"));
		}

		Glypher<G,I> glypher = new Glypher<>(dataset.shaper,dataset.valuer);
		GlyphsetRDD<G, I> glyphs = new GlyphsetRDD<>(base.map(glypher), true, partition);
		AffineTransform view = Util.zoomFit(glyphs.bounds(), width, height);
 		Selector selector = TouchesPixel.make(glyphs.exemplar().shape().getClass());

		Aggregator<I, A> aggregator = (Aggregator<I, A>) dataset.defaultAggregator.aggregator();
		Transfer transfer = OptionTransfer.toTransfer(((OptionDataset) dataset).defaultTransfers, null);
		
 		RDDRender render = new RDDRender();

 		long start = System.currentTimeMillis();
 		Aggregates<A> aggs = render.aggregate(glyphs, selector, aggregator, view, width, height);
 		long end = System.currentTimeMillis();
		
		if (outFile == null) {
			TransferDisplay.show("", width, height, aggs, transfer);
		} else {
			AggregatesToCSV.export(aggs, new File(outFile));
		}
	}
}
