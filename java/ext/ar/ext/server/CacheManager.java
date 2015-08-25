package ar.ext.server;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
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
	

	/**Which tiles are visible in the viewport, given the view transform.**/
	public List<File> files(String datasetId, Aggregator<?,?> aggregator, AffineTransform vt, Rectangle viewport) {
		Path base = root(datasetId, aggregator, vt);
		Rectangle renderBounds = renderBounds(vt, viewport);
		List<File> files = tileBounds(renderBounds).stream().map(r -> tileName(base, r)).collect(toList());
		return files;
	}	

	/**Root directory for the view/data/aggregator combination cache.**/
	public Path root(String datasetId, Aggregator<?,?> aggregator, AffineTransform vt) {
		return cacheRoot.resolve(datasetId).resolve(aggregator.toString()).resolve(Double.toString(vt.getScaleX())).resolve(Double.toString(vt.getScaleY()));
	}

	/**Given a tile bound, what is the file name?**/
	public static File tileName(Path base, Rectangle r) {
		String file = format("%s__%s__%s__%s.avsc", r.x,r.y, r.width, r.height);
		return base.resolve(file).toFile();
	}
	
	public List<Rectangle> tileBounds(Rectangle renderBounds) {
		List<Rectangle> tiles = new ArrayList<>();
		int highX = renderBounds.x+renderBounds.width;
		int highY = renderBounds.y+renderBounds.height;

		for (int x=renderBounds.x; x<highX; x+=tileSize) {
			for (int y=renderBounds.y; y<highY; y+=tileSize) {
				tiles.add(new Rectangle(x,y,tileSize, tileSize));
			}
		}
		return tiles;
	}
	
	public Rectangle renderBounds(AffineTransform vt, Rectangle viewport) {
		Rectangle viewbounds;
		try {viewbounds = vt.createInverse().createTransformedShape(viewport).getBounds();
		} catch (NoninvertibleTransformException e) {throw new RuntimeException("Invalid view transform for cache system.");}
		
		int lowX = (int) (viewbounds.getMinX() - (viewbounds.getMinX()%tileSize));
		int lowY = (int) (viewbounds.getMinY() - (viewbounds.getMinY()%tileSize));
		int highX = (int) (viewbounds.getMaxX() + (tileSize - (viewbounds.getMaxX()%tileSize)));
		int highY = (int) (viewbounds.getMaxY() + (tileSize - (viewbounds.getMaxY()%tileSize)));
		
		return new Rectangle(lowX, lowY, highX-lowX, highY-lowY);
	}
	
	/**
	 * @param datasetId   Name identifying source data
	 * @param aggregator  Aggregator that will be used		//TODO: Aggregator based transformation in cache load (like CoC->ToCounts)?
	 * @param vt		  View transform applied to source data
	 * @param viewport	  Size of the screen viewport (in screen coordinates) //TODO: Should this be in graphics coordinates? 
	 * @return
	 */
	public <A> Optional<Aggregates<A>> loadCached(String datasetId, Aggregator<?,A> aggregator, AffineTransform vt, Rectangle viewport) {
		Valuer<GenericRecord, A> converter = Converters.getDeserialize(aggregator);
		
		List<File> files = files(datasetId, aggregator, vt, viewport);
		Rectangle renderBounds = renderBounds(vt, viewport);
		
		System.out.println("## Loading cached aggregates.");
		Aggregates<A> combined =  AggregateUtils.make(renderBounds.x, renderBounds.y, renderBounds.x+renderBounds.width, renderBounds.y+renderBounds.height, aggregator.identity());
		
		for (File f: files) {
			if (!f.exists()) {
				//TODO: Partial loading from file, partial rendering.
				System.out.println("## Missing part from cached, aggregating");
				return Optional.empty();
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
	
	public <A> void save(String datasetId, Aggregator<?,A> aggregator, AffineTransform vt, Aggregates<A> aggs) {		
		Path base = root(datasetId, aggregator, vt);
		Rectangle renderBounds = AggregateUtils.bounds(aggs);
		List<Rectangle> tileBounds = tileBounds(renderBounds);
		
		System.out.println("## Saving aggregates to cache.");
		for (Rectangle bound: tileBounds) {
			Aggregates<A> tile = new SubsetWrapper<>(aggs, bound);
			File f = tileName(base, bound);
			try {
				AggregateSerializer.serialize(tile, new FileOutputStream(f));
			} catch (IOException e) {
				System.err.println("## Error saving to cache file " + f);
				e.printStackTrace();
			}
		}
		System.out.println("## Cache saved.");
	}

		
}
