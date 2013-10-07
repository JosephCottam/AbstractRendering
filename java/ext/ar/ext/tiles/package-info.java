/**Tools for working with a tile-server.
 * 
 * The envisioned architecture is that aggregates are stored
 * (via Avro serialization) as tiles.  Multiple tiles
 * can be logically treated as a single aggregate set
 * provided they come from the same aggregation level.
 * 
 * The tools of this package are for creating tile sets
 * at multiple levels of aggregation and for combining save
 * tiles into a single (logical) aggregate set again.
 * 
 * 
 * */
package ar.ext.tiles;