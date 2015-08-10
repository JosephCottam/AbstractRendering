package ar.ext.flask;

import static ar.util.Util.argKey;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileOutputStream;

import org.apache.avro.generic.GenericRecord;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.app.components.sequentialComposer.OptionDataset;
import ar.app.components.sequentialComposer.OptionTransfer;
import ar.ext.avro.AggregateSerializer;
import ar.ext.avro.Converters;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.renderers.ThreadpoolRenderer;
import ar.selectors.TouchesPixel;
import ar.util.Util;

/**Entry point for integrating with flask.**/
public class FlaskApp {
	@SuppressWarnings({ "rawtypes", "unchecked"})
	public static <G,I,A> void main(String[] args) throws Exception {
		File cacheFile = new File(Util.argKey(args, "-source", "./flask-aggregates.avsc"));
		File image = new File(Util.argKey(args, "-image", "./flask-output.png"));
		
		int width = Integer.parseInt(argKey(args, "-width", "800"));
		int height = Integer.parseInt(argKey(args, "-height", "800"));
		//String config = argKey(args, "-config", "CENSUS_SYN_PEOPLE");
		String config = argKey(args, "-config", "CENSUS_TRACTS");

		
		OptionDataset source;
		try {
			source = (OptionDataset) OptionDataset.class.getField(config).get(null);
		} catch (NoSuchFieldException | NullPointerException | SecurityException e) {
			throw new IllegalArgumentException("Could not find -config indicated: " + config);
		}

		Aggregator<I,A> aggregator = source.defaultAggregator.aggregator();
		Glyphset<G,I> glyphs = source.glyphset;
		Transfer transfer = OptionTransfer.toTransfer(source.defaultTransfers, null);
		Renderer renderer = new ThreadpoolRenderer();

		Aggregates<A> aggs = null;

		boolean renderMatches = false;
		boolean saveAggregates = false;
		if (cacheFile.exists()) {
			System.out.println("## Found cache file.");
			Valuer<GenericRecord, A> converter = Converters.getDeserialize(aggregator);
			aggs = (Aggregates<A>) AggregateSerializer.deserialize(cacheFile, converter); 
			renderMatches = true;			//TODO: Verify render parameters match (such as zoom level)
		}
		
		if (!renderMatches || !cacheFile.exists()) {
			System.out.println("## Reloading source data.");
			//Render if parameters don't match cached ones..
			saveAggregates = true;
			AffineTransform vt = Util.zoomFit(glyphs.bounds(), width, height);
			Selector s = TouchesPixel.make(glyphs);
			aggs = renderer.aggregate(glyphs, s, aggregator, vt);
		}
		
		Transfer.Specialized<A,Color> ts = transfer.specialize(aggs);
		Aggregates<Color> colors = renderer.transfer(aggs, ts);
		
		System.out.println("## Saving image.");
		Util.writeImage(AggregateUtils.asImage(colors), image);
		
		if (saveAggregates) {
			System.out.println("## Saving aggregates.");
			AggregateSerializer.serialize(aggs, new FileOutputStream(cacheFile));
		}
		//System.exit(0);
		System.out.println("## Done.");
	}
}
