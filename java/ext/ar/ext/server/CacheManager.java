package ar.ext.server;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.avro.generic.GenericRecord;

import ar.Aggregates;
import ar.Aggregator;
import ar.aggregates.AggregateUtils;
import ar.aggregates.wrappers.SubsetWrapper;
import ar.app.components.sequentialComposer.OptionDataset;
import ar.ext.avro.AggregateSerializer;
import ar.ext.avro.Converters;
import ar.glyphsets.implicitgeometry.Valuer;
import static java.lang.String.format;
import static java.util.stream.Collectors.*;

//TODO: This should probably live somewhere else so other systems can use it (like the composer app)...maybe in the ext.avro or core.util package?
public class CacheManager {
	private final Path cacheRoot;
	private final int tileSize;

	public CacheManager(File cachedir, int tileSize) {
		this.cacheRoot = cachedir.toPath();
		this.tileSize = tileSize;
	}
	
	public int tileSize() {return tileSize;}
	
	
	/**Which tiles are visible in the viewport, given the view transform.**/
	public List<File> tileFiles(String datasetId, Aggregator<?,?> aggregator, AffineTransform vt, Rectangle renderBounds) {
		Path base = root(datasetId, aggregator, vt);
		List<File> files = tileBounds(renderBounds).stream().map(r -> tileName(base, r)).collect(toList());
		return files;
	}	

	/**Root directory for the view/data/aggregator combination cache.**/
	private Path root(String datasetId, Aggregator<?,?> aggregator, AffineTransform vt) {
		//HACK: Using the aggregator class name ignores parameters...and can cause cross-package conflicts...
		return cacheRoot.resolve(datasetId).resolve(aggregator.getClass().getSimpleName()).resolve(Double.toString(vt.getScaleX())).resolve(Double.toString(vt.getScaleY()));
	}

	/**Given a tile bound, what is the file name?**/
	private static File tileName(Path base, Rectangle r) {
		String file = format("%s__%s__%s__%s.avsc", r.x,r.y, r.width, r.height);
		return base.resolve(file).toFile();
	}
	
	/**Computes the bounds for tiles in a render space. 
	 * ASSUMES renderBounds is aligned to the tileSize (use renderBounds).**/
	public List<Rectangle> tileBounds(Rectangle renderBounds) {
		List<Rectangle> tiles = new ArrayList<>();
		int highX = (int) renderBounds.getMaxX();
		int highY = (int) renderBounds.getMaxY();

		for (int x=renderBounds.x; x<highX; x+=tileSize) {
			for (int y=renderBounds.y; y<highY; y+=tileSize) {
				tiles.add(new Rectangle(x,y,tileSize, tileSize));
			}
		}
		return tiles;
	}
	
	
	private static int roundTowardZero(int v, int size) {return v - (v%size);}
	private static int roundAwayZero(int v, int size) {return v + (size - (v%size));}
	
	/**Compute the bounds of all of the tiles covered by the requested viewport
	 * @param viewbounds -- in GLOBAL BIN SPACE.
	 * **/
	public Rectangle renderBounds(Rectangle viewbounds) {		
		int lowX = viewbounds.x;
		int lowY = viewbounds.y;
		int highX = viewbounds.x + viewbounds.width;
		int highY = viewbounds.y + viewbounds.height;
		
		lowX = lowX >=0 ? roundTowardZero(lowX, tileSize) : roundAwayZero(lowX, -tileSize);
		lowY = lowY >=0 ? roundTowardZero(lowY, tileSize) : roundAwayZero(lowY, -tileSize);
		highX = highX >=0 ? roundAwayZero(highX, tileSize) : roundTowardZero(highX, -tileSize);
		highY = highY >=0 ? roundAwayZero(highY, tileSize) : roundTowardZero(highY, -tileSize);
		
		return new Rectangle(lowX, lowY, highX-lowX, highY-lowY);
	}
	

	/**Transform that provides global bin coordinates that align with the current view transform.
	 * 
	 * Tiles are defined in terms of global bins, which are in term defined in terms of scaling and translating the whole dataset.
	 * NOTE: The overall system based on gbt assumes that the min X/Y of the glyphset remain constant.
	 * 
	 * @param datasetBounds -- Full bounds of the source data
	 * @param vt -- View transform for the current rendering.
	 * **/
	public static AffineTransform globalBinTransform(Rectangle2D datasetBounds, AffineTransform vt) {
		AffineTransform gbt = AffineTransform.getScaleInstance(vt.getScaleX(), vt.getScaleY()); 
		gbt.translate(datasetBounds.getMinX(), -datasetBounds.getMinY());
		return gbt;
	}
	
	/**
	 * @param datasetId   Name identifying source data
	 * @param aggregator  Aggregator that will be used		//TODO: Aggregator based transformation in cache load (like CoC->ToCounts)?
	 * @param vt		  View transform applied to source data
	 * @param viewport	  Size of the screen viewport (in screen coordinates) //TODO: Should this be in graphics coordinates? 
	 * @return
	 */
	public <A> Optional<Aggregates<A>> loadCached(OptionDataset<?,?> base, Aggregator<?,A> aggregator, AffineTransform vt, Rectangle viewport) {
		Valuer<GenericRecord, A> converter = Converters.getDeserialize(aggregator);
		

		AffineTransform gbt = globalBinTransform(base.glyphset.bounds(), vt);

		Rectangle viewbounds; //viewport in gbt space
		try {viewbounds = gbt.createTransformedShape(vt.createInverse().createTransformedShape(viewport).getBounds2D()).getBounds();}
		catch (NoninvertibleTransformException e) {throw new RuntimeException(e);}

		Rectangle renderBounds = renderBounds(viewbounds);
		System.out.printf("Load: Calc files with %s and %s%n", vt, renderBounds);

		List<File> files = tileFiles(base.name, aggregator, vt, renderBounds);
		
		
		System.out.println("## Loading cached aggregates.");
		Aggregates<A> combined =  AggregateUtils.make(renderBounds.x, renderBounds.y, renderBounds.x+renderBounds.width, renderBounds.y+renderBounds.height, aggregator.identity());
		
		for (File f: files) {
			System.out.println("loading file " + f.getName());
			if (!f.exists()) {
				System.out.println("## Missing part from cached, aggregating  --- " + f.getName()); 
				return Optional.empty(); //TODO: Return partially loaded aggregates (and bounds on unloaded parts)
			}

			try {
				Aggregates<A> aggs = AggregateSerializer.deserialize(f, converter);
				combined = AggregateUtils.__unsafeMerge(combined, aggs, aggregator.identity(), aggregator::rollup);
			} catch (Exception e) {
				System.err.println("## Cache located for " + f + ", but error deserializing.");
				e.printStackTrace();
				return Optional.empty();
			}
		}
		return Optional.of(combined);
	}
	
	/**Save out a set of aggregates into tiles.
	 * 
	 * Assumes that the aggregates cover full tiles (easily done using the renderBounds method).
	 * 
	 * @param base Source dataset
	 * @param aggregator Aggregator used to build the aggregates (influences the cache path)
	 * @param vt View transform used to render the aggregates (used to align the aggregates to the global grid)
	 * @param aggs Aggregates to save
	 *
	 * **/
	public <A> void save(OptionDataset<?,?> base, Aggregator<?,A> aggregator, AffineTransform vt, Aggregates<A> aggs) {		
		AffineTransform gbt = globalBinTransform(base.glyphset.bounds(), vt);
		Path root = root(base.name, aggregator, vt);
		Rectangle aggregateBounds = AggregateUtils.bounds(aggs);
		
		//Compensate for existing translation...
		double tx = gbt.getTranslateX()-vt.getTranslateX();
		double ty = gbt.getTranslateY()-vt.getTranslateY();
		
		//Align rendered space to global bins, expand to full tiles and calculate tiles
		Rectangle renderBounds = AffineTransform.getTranslateInstance(tx,ty).createTransformedShape(aggregateBounds).getBounds();
		renderBounds = renderBounds(renderBounds);
		List<Rectangle> tileBounds = tileBounds(renderBounds);
		System.out.printf("Save: Calc files with %s and %s%n", vt, renderBounds);

		
		System.out.println("## Saving aggregates to cache.");
		for (Rectangle bound: tileBounds) {
			Aggregates<A> tile = new SubsetWrapper<>(aggs, bound);
			File f = tileName(root, bound);
			System.out.printf("##    Saving %s to %s%n", bound, f.getName());
			try {
				f.getParentFile().mkdirs();
				AggregateSerializer.serialize(tile, new FileOutputStream(f));
			} catch (IOException e) {
				System.err.println("## Error saving to cache file " + f);
				e.printStackTrace();
			}
		}
		System.out.println("## Cache saved.");
	}

		
}
