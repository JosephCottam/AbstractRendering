package ar.ext.tiles;

import java.io.File;

import ar.Aggregates;

/**
 * @author jcottam
 *
 */
public class TileUtils {
	/**Given a set of aggregates, rollup one step and ouptut to given root.
	 * Divide into "tiles" per the width/height passed 
	 * 
	 * @param aggs Aggreagte to base items off of. This will be the most-detailed level in the output (e.g. Z-value is level-1)
	 * @param ouptutRoot Where to place output items.  This SHOULD include a z-directory
	 * @param tileWidth How wide should tiles be made
	 * @param tileHeight How tall should tiles be made
	 */
	public void makeTiles(Aggregates<?> aggs, File outputRoot, int tileWidth, int tileHeight) {
		
	}
	
	
	/**
	 * @param levels How many levels to make
	 * @param aggs Aggreagte to base items off of. This will be the most-detailed level in the output (e.g. Z-value is level-1)
	 * @param ouptutRoot Where to place output items.  This SHOULD NOT include a z-directory
	 * @param tileWidth How wide should tiles be made
	 * @param tileHeight How tall should tiles be made
	 */
	public void makeTileCascae(int levels, Aggregates<?> aggs, File ouptutRoot, int tileWidht, int tileHeight) {
		
	}
}
