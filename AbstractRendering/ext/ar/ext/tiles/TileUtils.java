package ar.ext.tiles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.avro.generic.GenericRecord;

import ar.AggregateReducer;
import ar.Aggregates;
import ar.aggregates.FlatAggregates;
import ar.ext.avro.AggregateSerializer;
import ar.glyphsets.implicitgeometry.Valuer;

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
				Aggregates<?> subset = subset(aggs, lowX, lowY, lowX+tileWidth, lowY+tileHeight);
				
				File target = extend(levelRoot, Integer.toString(col), Integer.toString(row), ".avro");
				target.getParentFile().mkdirs();
				OutputStream out = new FileOutputStream(target);
				
				AggregateSerializer.serialize(subset, out);
				
			}
		}
	}
	
	/**Create a new aggregate set that is a subset of the old aggregate set.
	 * 
	 * The new aggregate set will have the same indices as the old aggregate set
	 * (so 100,100 in the old one will have the same value as 100,100 in the old),
	 * however the new aggregate set will not necessarily have the same lowX/lowY or highX/highY
	 * as the old set.  The new aggregate set will have the same default value as the old.
	 */
	public static  <A> Aggregates<A> subset(Aggregates<A> source, int lowX, int lowY, int highX, int highY) {
		Aggregates<A> target = new FlatAggregates<A>(lowX, lowY, highX, highY, source.defaultValue());
		for (int x=lowX; x<highX; x++) {
			for (int y=lowY; y<highY; y++) {
				target.set(x, y, source.at(x, y));
			}
		}
		return target;
	}
	
	
	/**Given a set of aggregates, rollup the specified number of levels, output to the 
	 * given root into sub-directories per-level.
	 * 
	 * @param levels How many levels to make
	 * @param aggs Aggreagte to base items off of. This will be the most-detailed level in the output (e.g. Z-value is level-1)
	 * @param ouptutRoot Where to place output items.  This SHOULD NOT include a z-directory
	 * @param tileWidth How wide should tiles be made
	 * @param tileHeight How tall should tiles be made
	 */
	public static <A> void makeTileCascae(Aggregates<A> aggs, AggregateReducer<A,A,A> red, File outputRoot, int tileWidth, int tileHeight, int levels) throws Exception {
		outputRoot.mkdirs();
		Aggregates<A> running = aggs;
		
		for (int level=levels-1; level>=0; level--) {
			File levelRoot = extend(outputRoot, Integer.toString(level), "");
			makeTiles(running, levelRoot, tileWidth, tileHeight);
			running = AggregateReducer.Strategies.foldUp(running, red);
		}
	}
	
	/**Reload a specified subset of tiles into a single set of aggregates.**/
	public static <A> Aggregates<A> loadTiles(Valuer<GenericRecord, A> converter, Class<A> type, File... files) 
			throws FileNotFoundException {
		
		Aggregates<A> acc = null;
		AggregateReducer<A,A,A> red = new CopyReducer<A>(type);
		for (File file: files) {
			Aggregates<A> aggs = AggregateSerializer.deserialize(file, converter);
			acc = AggregateReducer.Strategies.foldLeft(acc, aggs, red);
		}
		return acc;
	}
}
