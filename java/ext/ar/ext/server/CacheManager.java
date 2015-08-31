package ar.ext.server;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.avro.generic.GenericRecord;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.Transfer.ItemWise;
import ar.Transfer.Specialized;
import ar.aggregates.AggregateUtils;
import ar.aggregates.implementations.ConstantAggregates;
import ar.aggregates.wrappers.SubsetWrapper;
import ar.ext.avro.AggregateSerializer;
import ar.ext.avro.Converters;
import ar.glyphsets.BoundingWrapper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.renderers.ProgressRecorder;
import ar.renderers.ThreadpoolRenderer;
import static java.lang.String.format;

//TODO: Generalize and move out to a more accessible location...
public class CacheManager implements Renderer {
	private final Path cacheRoot;
	private final int tileSize;
	private final Renderer base;
	
	public CacheManager(File cachedir, int tileSize, Renderer base) {
		this.cacheRoot = cachedir.toPath();
		this.tileSize = tileSize;
		this.base = base;
		
		if (!cachedir.exists()) {cachedir.mkdirs();}
		if (!cachedir.isDirectory()) {throw new IllegalArgumentException("Indicated cache directory exists BUT is not a directory." + cachedir);}
	}
	
	public int tileSize() {return tileSize;}
	

	@Override
	public <I, G, A> Aggregates<A> aggregate(
			Glyphset<? extends G, ? extends I> glyphs, Selector<G> selector,
			Aggregator<I, A> aggregator, AffineTransform viewTransform) {
		return base.aggregate(glyphs, selector, aggregator, viewTransform);
	}

	@Override
	public <I, G, A> Aggregates<A> aggregate(
			Glyphset<? extends G, ? extends I> glyphs, Selector<G> selector,
			Aggregator<I, A> aggregator, AffineTransform viewTransform,
			Function<A, Aggregates<A>> allocator,
			BiFunction<Aggregates<A>, Aggregates<A>, Aggregates<A>> merge) {
		return base.aggregate(glyphs, selector, aggregator, viewTransform, allocator, merge);		
	}

	public <I, G, A> Aggregates<A> aggregate(
			Glyphset<? extends G, ? extends I> glyphs, Selector<G> selector,
			Aggregator<I, A> aggregator, AffineTransform viewTransform,
			String targetId, Rectangle viewport) {
		return this.aggregate(glyphs, selector, aggregator, viewTransform, 
				ThreadpoolRenderer.defaultAllocator(glyphs, viewTransform),
				ThreadpoolRenderer.defaultMerge(aggregator.identity(), aggregator::rollup),
				targetId, viewport);
	}

	
	public <I, G, A> Aggregates<A> aggregate(
			Glyphset<? extends G, ? extends I> glyphs, Selector<G> selector,
			Aggregator<I, A> aggregator, AffineTransform viewTransform,
			Function<A, Aggregates<A>> allocator,
			BiFunction<Aggregates<A>, Aggregates<A>, Aggregates<A>> merge,
			String targetId,
			Rectangle viewport) {
		
		CacheStatus<A> cacheStatus = loadCached(targetId, glyphs.bounds(), aggregator, viewTransform, viewport);
		
		Optional<Aggregates<A>> freshRendered = Optional.empty();
		if (cacheStatus.remaining.isPresent()) {
			AffineTransform gbt = globalBinTransform(glyphs.bounds(), viewTransform);
			try {
				Rectangle2D renderBounds = gbt.createInverse().createTransformedShape(cacheStatus.remaining.get()).getBounds2D();
				Glyphset<? extends G, ? extends I> subset = new BoundingWrapper<>(glyphs, renderBounds);
				freshRendered = Optional.ofNullable(base.aggregate(subset, selector, aggregator, gbt, allocator, merge));
			} catch (NoninvertibleTransformException e) {
				throw new RuntimeException("Error calculating the region to render.", e);
			}
			save(targetId, glyphs.bounds(), aggregator, gbt, freshRendered.orElse(new ConstantAggregates<>(aggregator.identity(), cacheStatus.remaining.get())));
		}

		Aggregates<A> result = AggregateUtils.__unsafeMerge(
									freshRendered.orElse(new ConstantAggregates<>(aggregator.identity())),
									cacheStatus.cached.orElse(new ConstantAggregates<>(aggregator.identity())), 
									aggregator.identity(), 
									aggregator::rollup);
		
		return result;
	}

	@Override
	public <IN, OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Specialized<IN, OUT> t) {return base.transfer(aggregates, t);}

	@Override
	public <IN, OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, ItemWise<IN, OUT> t) {return base.transfer(aggregates, t);}

	@Override
	public ProgressRecorder recorder() {return base.recorder();}
	
	@Override
	//TODO: Manage future work better....
	public void stop() {base.stop();}

	/**Root directory for the view/data/aggregator combination cache.**/
	public Path tilesetPath(String datasetId, Aggregator<?,?> aggregator, AffineTransform vt) {
		//HACK: Using the aggregator class name ignores parameters...and can cause cross-package conflicts...
		return cacheRoot.resolve(datasetId).resolve(aggregator.getClass().getSimpleName()).resolve(Double.toString(vt.getScaleX())).resolve(Double.toString(vt.getScaleY()));
	}

	/**Given a tile bound, what is the file name?**/
	public static File tileFile(Path base, Rectangle r) {
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
	
	
	private static final class CacheStatus<A> {
		/**Aggregates loaded from the cache.**/
		public Optional<Aggregates<A>> cached;
		
		/**Region that still needs to be rendered (in global bin space).**/
		public Optional<Rectangle> remaining;
		public CacheStatus(Aggregates<A> aggs, Optional<Rectangle> remaining) {
			this.cached = Optional.ofNullable(aggs.empty() ? null : aggs);
			this.remaining = remaining;
		}
	}
	
	/**
	 * @param datasetId   Name identifying source data
	 * @param aggregator  Aggregator that will be used		//TODO: Aggregator based transformation in cache load (like CoC->ToCounts)?
	 * @param vt		  View transform applied to source data
	 * @param viewport	  Size of the screen viewport (in screen coordinates) //TODO: Should this be in graphics coordinates? 
	 * @return
	 */
	public <A> CacheStatus<A> loadCached(String datasetId, Rectangle2D glyphsetBounds, Aggregator<?,A> aggregator, AffineTransform vt, Rectangle viewport) {
		Valuer<GenericRecord, A> converter = Converters.getDeserialize(aggregator);
		

		AffineTransform gbt = globalBinTransform(glyphsetBounds, vt);

		Rectangle viewBounds; //viewport in gbt space
		try {viewBounds = gbt.createTransformedShape(vt.createInverse().createTransformedShape(viewport).getBounds2D()).getBounds();}
		catch (NoninvertibleTransformException e) {throw new RuntimeException(e);}

		Rectangle renderBounds = renderBounds(viewBounds);
		System.out.printf("Load: Calc files with %s and %s%n", vt, renderBounds);

		
		
		System.out.println("## Loading cached aggregates.");
		Aggregates<A> combined =  AggregateUtils.make(renderBounds.x, renderBounds.y, renderBounds.x+renderBounds.width, renderBounds.y+renderBounds.height, aggregator.identity());
		Optional<Rectangle> remaining = Optional.empty();
		
		Path tilesetRoot = tilesetPath(datasetId, aggregator, vt);
		for (Rectangle tile: tileBounds(renderBounds)) {
			File f = tileFile(tilesetRoot, tile);  
			System.out.println("loading file " + f.getName());
			if (!f.exists()) {
				System.out.println("## Missing part from cached, aggregating  --- " + f.getName());
				remaining = Optional.of(remaining.isPresent() ? remaining.get().union(tile) : tile);
			} else {
				try {
					Aggregates<A> aggs = AggregateSerializer.deserialize(f, converter);
					combined = AggregateUtils.__unsafeMerge(combined, aggs, aggregator.identity(), aggregator::rollup);
				} catch (Exception e) {
					System.err.println("## Cache located for " + f + ", but error deserializing.");
					e.printStackTrace();
					remaining = Optional.of(remaining.isPresent() ? remaining.get().union(tile) : tile);
				}
			}
		}
		combined = new SubsetWrapper<>(combined, viewBounds);
		return new CacheStatus<>(combined, remaining);
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
	public <A> void save(String datasetId, Rectangle2D glyphsetBounds, Aggregator<?,A> aggregator, AffineTransform vt, Aggregates<A> aggs) {		
		AffineTransform gbt = globalBinTransform(glyphsetBounds, vt);
		Path tilesetRoot = tilesetPath(datasetId, aggregator, vt);
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
			File f = tileFile(tilesetRoot, bound);
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
	
	
	/**Delete the cache directory and everything in it.**/
	public static void clearCache(File cachedir) throws IOException {
		System.out.println("## Clearing the cache.");
		Files.walkFileTree(cachedir.toPath(), 
				new SimpleFileVisitor<Path>(){
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
						Files.deleteIfExists(file);
						return FileVisitResult.CONTINUE;
					}
					
					@Override
					public FileVisitResult postVisitDirectory(Path file, IOException exc) throws IOException {
						Files.deleteIfExists(file);
						return FileVisitResult.CONTINUE;
					}
				});
	}
	
			
	
	/**Same interface as the CacheManager, but NEVER looks at the cache.**/
	public static final class Shim extends CacheManager {
		public Shim(File cachedir, int tileSize, Renderer base) {super(cachedir, tileSize, base);}

		public <I, G, A> Aggregates<A> aggregate(
				Glyphset<? extends G, ? extends I> glyphs, Selector<G> selector,
				Aggregator<I, A> aggregator, AffineTransform viewTransform,
				Function<A, Aggregates<A>> allocator,
				BiFunction<Aggregates<A>, Aggregates<A>, Aggregates<A>> merge,
				String targetId,
				Rectangle viewport) {
			return super.base.aggregate(glyphs, selector, aggregator, viewTransform, allocator, merge);
		}
	}
}
