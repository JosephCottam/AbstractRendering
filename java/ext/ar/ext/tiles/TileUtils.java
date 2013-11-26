package ar.ext.tiles;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.avro.generic.GenericRecord;

import ar.Aggregates;
import ar.Aggregator;
import ar.aggregates.AggregateUtils;
import ar.ext.avro.AggregateSerializer;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.renderers.AggregationStrategies;
import ar.rules.General;

public class TileUtils {
	/**Extend a root with the given set of subs.
	 * 
	 * The root is assumed to be a directory.
	 * Subs are mostly sub-directories EXCEPT
	 * the 2nd-to-last one is a filename and the last one is an extension.
	 ***/
	public static File extend(File root, String... subs) {
		String ext =subs[subs.length-1];
		if (ext != null && !ext.equals("")) {
			String[] dirs = Arrays.copyOf(subs, subs.length-1);
			dirs[dirs.length-1] = dirs[dirs.length-1] + subs[subs.length-1];
			subs = dirs;
		}
		return Paths.get(root.toString(), subs).toFile();
	}
	
	/**Given a set of aggregates, rollup one step and output to given root.
	 * Divide into "tiles" per the width/height passed 
	 * 
	 * @param aggs Aggreagte to base items off of. This will be the most-detailed level in the output (e.g. Z-value is level-1)
	 * @param levelRoot Where to place output items.  (This SHOULD include a z-directory.)
	 * @param tileWidth How wide should tiles be made
	 * @param tileHeight How tall should tiles be made
	 */
	public static void makeTiles(Aggregates<?> aggs, File levelRoot, int tileWidth, int tileHeight) throws Exception {
		int cols = (int) Math.ceil((aggs.highX()-aggs.lowX())/(float) tileWidth);
		int rows = (int) Math.ceil((aggs.highY()-aggs.lowY())/(float) tileHeight);
		
		for (int row=0; row<rows; row++) {
			for (int col=0; col<cols; col++) {
				int lowX=(col*tileWidth)+aggs.lowX();
				int lowY=(col*tileHeight)+aggs.lowY();
				Aggregates<?> subset = AggregateUtils.alignedSubset(aggs, lowX, lowY, lowX+tileWidth, lowY+tileHeight);
				
				File target = extend(levelRoot, Integer.toString(col), Integer.toString(row), ".avro");
				target.getParentFile().mkdirs();
				try (OutputStream out = new FileOutputStream(target)) {
					AggregateSerializer.serialize(subset, out);
				}
				
			}
		}
	}
	
	/**Given a set of aggregates, rollup the specified number of levels, output to the 
	 * given root into sub-directories per-level.
	 * 
	 * @param levels How many levels to make
	 * @param aggs Aggreagte to base items off of. This will be the most-detailed level in the output (e.g. Z-value is level-1)
	 * @param red Aggregator to use to do rollup
	 * @param outputRoot Where to place output items.  This SHOULD NOT include a z-directory
	 * @param tileWidth How wide should tiles be made
	 * @param tileHeight How tall should tiles be made
	 */
	public static <A> void makeTileCascae(Aggregates<A> aggs, Aggregator<?,A> red, File outputRoot, int tileWidth, int tileHeight, int levels) throws Exception {
		outputRoot.mkdirs();
		Aggregates<A> running = aggs;
		
		for (int level=levels-1; level>=0; level--) {
			File levelRoot = extend(outputRoot, Integer.toString(level), "");
			makeTiles(running, levelRoot, tileWidth, tileHeight);
			running = AggregationStrategies.verticalRollup(running, red,2);
		}
	}
	
	/**Reload a specified subset of tiles into a single set of aggregates.**/
	public static <A> Aggregates<A> loadTiles(Valuer<GenericRecord, A> converter, Class<A> type, File... files) 
			throws IOException {
		
		Aggregates<A> acc = null;
		Aggregator<A,A> red = new General.Echo<A>(null);
		for (File file: files) {
			Aggregates<A> aggs = AggregateSerializer.deserialize(file, converter);
			acc = AggregationStrategies.horizontalRollup(acc, aggs, red);
		}
		return acc;
	}
}
