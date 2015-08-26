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
	public List<File> files(String datasetId, Aggregator<?,?> aggregator, AffineTransform vt, Rectangle viewport) {
		Path base = root(datasetId, aggregator, vt);
		Rectangle renderBounds = renderBounds(vt, viewport);
		List<File> files = tileBounds(renderBounds).stream().map(r -> tileName(base, r)).collect(toList());
		return files;
	}	

	/**Root directory for the view/data/aggregator combination cache.**/
	public Path root(String datasetId, Aggregator<?,?> aggregator, AffineTransform vt) {
		//HACK: Using the aggregator class name ignores parameters...and can cause cross-package conflicts...
		return cacheRoot.resolve(datasetId).resolve(aggregator.getClass().getSimpleName()).resolve(Double.toString(vt.getScaleX())).resolve(Double.toString(vt.getScaleY()));
	}

	/**Given a tile bound, what is the file name?**/
	public static File tileName(Path base, Rectangle r) {
		String file = format("%s__%s__%s__%s.avsc", r.x,r.y, r.width, r.height);
		return base.resolve(file).toFile();
	}
	
	/**Computes the bounds for tiles in a render space. 
	 * ASSUMES renderBounds is aligned to the tileSize (use renderBounds).**/
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
	
	
	private static double roundTowardZero(double v, double size) {return v - (v%size);}
	private static double roundAwayZero(double v, double size) {return v + (size - (v%size));}
	public Rectangle renderBounds(AffineTransform vt, Rectangle viewport) {
		Rectangle2D viewbounds;	//Graphics coordinates covered by the viewport under the view transform
		Rectangle2D tilebounds; //Size of a tile in graphics coordinates
		
		try {
			AffineTransform ivt = vt.createInverse();
			viewbounds = ivt.createTransformedShape(viewport).getBounds2D();
			tilebounds = ivt.createTransformedShape(new Rectangle(0,0,tileSize, tileSize)).getBounds2D();
		} catch (NoninvertibleTransformException e) {throw new RuntimeException("Invalid view transform for cache system.");}
		
		double lowX = viewbounds.getMinX();
		double lowY = viewbounds.getMinY();
		double highX = lowX + viewbounds.getWidth();
		double highY = lowY + viewbounds.getHeight();
		
		lowX = lowX >=0 ? roundTowardZero(lowX, tilebounds.getWidth()) : roundAwayZero(lowX, -tilebounds.getWidth());
		lowY = lowY >=0 ? roundTowardZero(lowY, tilebounds.getWidth()) : roundAwayZero(lowY, -tilebounds.getWidth());
		highX = highX >=0 ? roundAwayZero(highX, tilebounds.getWidth()) : roundTowardZero(highX, -tilebounds.getWidth());
		highY = highY >=0 ? roundAwayZero(highY, tilebounds.getWidth()) : roundTowardZero(highY, -tilebounds.getWidth());
		
		Rectangle2D graphicsbounds = new Rectangle2D.Double(lowX, lowY, highX-lowX, highY-lowY); //Bounds of the covered tiles in graphics coordinates
		Rectangle renderbounds = vt.createTransformedShape(graphicsbounds).getBounds();
		return renderbounds;
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
		
		System.out.printf("Calc files with %s and %s%n", vt, viewport);
		List<File> files = files(datasetId, aggregator, vt, viewport);
		Rectangle renderBounds = renderBounds(vt, viewport);
		
		System.out.printf("## Loading cached aggregates: %s%n", files.stream().map(f -> f.getName()).collect(joining(",")));
		Aggregates<A> combined =  AggregateUtils.make(renderBounds.x, renderBounds.y, renderBounds.x+renderBounds.width, renderBounds.y+renderBounds.height, aggregator.identity());
		
		for (File f: files) {
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
	
	public <A> void save(String datasetId, Aggregator<?,A> aggregator, AffineTransform vt, Aggregates<A> aggs) {		
		AffineTransform ivt;
		try {ivt = vt.createInverse();}
		catch (NoninvertibleTransformException e1) {throw new RuntimeException("Error inverting view transform.  Values not saved.");}

		
		Path base = root(datasetId, aggregator, vt);
		Rectangle renderBounds = renderBounds(vt, AggregateUtils.bounds(aggs));

		List<Rectangle> tileBounds = tileBounds(renderBounds);
		
		System.out.println("## Saving aggregates to cache.");
		for (Rectangle bound: tileBounds) {
			Aggregates<A> tile = new SubsetWrapper<>(aggs, ivt.createTransformedShape(bound).getBounds());
			File f = tileName(base, bound);
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
