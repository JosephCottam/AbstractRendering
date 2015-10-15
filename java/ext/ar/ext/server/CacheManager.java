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
import ar.aggregates.wrappers.CompositeWrapper;
import ar.aggregates.wrappers.SubsetWrapper;
import ar.app.components.sequentialComposer.OptionDataset;
import ar.ext.avro.AggregateSerializer;
import ar.ext.avro.Converters;
import ar.glyphsets.BoundingWrapper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.renderers.ProgressRecorder;
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
			Rectangle2D baseframe,
			String targetId, Rectangle viewport) {
		return this.aggregate(glyphs, selector, aggregator, viewTransform, 
				Renderer.simpleMerge(aggregator.identity(), aggregator::rollup),
				baseframe,
				targetId, viewport);
	}

	
	public <I, G, A> Aggregates<A> aggregate(
			Glyphset<? extends G, ? extends I> glyphs, Selector<G> selector,
			Aggregator<I, A> aggregator, AffineTransform viewTransform,
			BiFunction<Aggregates<A>, Aggregates<A>, Aggregates<A>> merge,
			Rectangle2D baseframe,
			String targetId,
			Rectangle viewport) {
		
		AffineTransform gbt = globalBinTransform(baseframe, viewTransform);
		CacheStatus<A> cacheStatus = loadCached(targetId, aggregator, viewTransform, gbt, viewport);
		
		
		Optional<Aggregates<A>> freshRendered = Optional.empty();
		if (cacheStatus.remaining.isPresent()) {
			System.out.println("## Rendering tile(s) from source " + cacheStatus.remaining.get());
			
			Rectangle2D renderBounds;
			try {renderBounds = gbt.createInverse().createTransformedShape(cacheStatus.remaining.get()).getBounds2D();} 
			catch (NoninvertibleTransformException e) {throw new RuntimeException("Error calculating the region to render.", e);}

			Glyphset<? extends G, ? extends I> subset = renderBounds.contains(glyphs.bounds()) 
													? glyphs
													: new BoundingWrapper<>(glyphs, renderBounds);
			Function<A, Aggregates<A>> allocator = Renderer.simpleAllocator(subset, gbt);

			System.out.println("## Aggregating for source " + subset.bounds());
			freshRendered = Optional.ofNullable(base.aggregate(subset, selector, aggregator, gbt, allocator, merge));
				
				
			
			//Expand aggregates back out to full tile sizes
			Aggregates<A> toSave = new ConstantAggregates<>(aggregator.identity(), cacheStatus.remaining.get());
			toSave = freshRendered.isPresent() 
									? new CompositeWrapper<>(freshRendered.get(), toSave, new CompositeWrapper.LeftBiased<>(freshRendered.get().defaultValue()))
									: toSave;
			save(targetId, aggregator, gbt, toSave);
		}

		Rectangle gbtViewport = AffineTransform.getTranslateInstance(-gbt.getTranslateX(), -gbt.getTranslateY()).createTransformedShape(viewport).getBounds();
		Aggregates<A> result = AggregateUtils.__unsafeMerge(
									freshRendered.orElse(new ConstantAggregates<>(aggregator.identity())),
									cacheStatus.cached.orElse(new ConstantAggregates<>(aggregator.identity(), gbtViewport)), 
									aggregator.identity(), 
									aggregator::rollup);

		int dx = (int) (gbt.getTranslateX() - viewTransform.getTranslateX());
		int dy = (int) (gbt.getTranslateY() - viewTransform.getTranslateY());
		result = new SubsetWrapper<>(new ShiftWrapper<>(result, dx, dy), viewport);
		
		return result;
	}

	@Override
	public <IN, OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, Specialized<IN, OUT> t) {return base.transfer(aggregates, t);}

	@Override
	public <IN, OUT> Aggregates<OUT> transfer(Aggregates<? extends IN> aggregates, ItemWise<IN, OUT> t) {return base.transfer(aggregates, t);}

	@Override
	public ProgressRecorder recorder() {return base.recorder();}
	
	@Override
	public void stop() {base.stop();}

	/**Root directory for the view/data/aggregator combination cache.
	 * 
	 * HACK: Trims the scale to 4 decimal places to forestall rounding error that was making tiles not found A LOT of the time even on simple navigation
	 * 
	 * **/
	public Path tilesetPath(String datasetId, Aggregator<?,?> aggregator, AffineTransform vt) {
		//HACK: Using the aggregator class name ignores parameters...and can cause cross-package conflicts...
		String sx = String.format("%.6f", vt.getScaleX());
		String sy = String.format("%.6f", vt.getScaleY());
		return cacheRoot.resolve(datasetId).resolve(aggregator.getClass().getSimpleName()).resolve(sx).resolve(sy);
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
		gbt.translate(-datasetBounds.getMinX(), -datasetBounds.getMinY());
		return gbt;
	}
	
	
	private static final class CacheStatus<A> {
		/**Aggregates loaded from the cache.**/
		public Optional<Aggregates<A>> cached;
		
		/**Region that still needs to be rendered (in global bin space).**/
		public Optional<Rectangle> remaining;
		public CacheStatus(Aggregates<A> aggs, Optional<Rectangle> remaining) {
			this.cached = Optional.ofNullable((aggs == null || aggs.empty()) ? null : aggs);
			this.remaining = remaining;
		}
	}
	
	/**
	 * TODO: Is aggregator conversion post-load valuable (like CoC->ToCounts)? Would enable a smaller on-disk cache....but might be too narrow to bother with. 
	 * 
	 * @param datasetId   Name identifying source data
	 * @param aggregator  Aggregator that will be used
	 * @param vt		  View transform applied to source data
	 * @param gbt		  Global bin transform		
	 * @param viewport	  Size of the screen viewport (in screen coordinates) 
	 * @return
	 */
	public <A> CacheStatus<A> loadCached(String datasetId, Aggregator<?,A> aggregator, AffineTransform vt, AffineTransform gbt, Rectangle viewport) {
		Valuer<GenericRecord, A> converter = converter(datasetId);
		aggregator.identity();
		
		Rectangle viewBounds; //viewport in gbt space
		try {viewBounds = gbt.createTransformedShape(vt.createInverse().createTransformedShape(viewport).getBounds2D()).getBounds();}
		catch (NoninvertibleTransformException e) {throw new RuntimeException(e);}

		Rectangle renderBounds = renderBounds(viewBounds);
		
		System.out.println("## Loading cached aggregates.");
		Aggregates<A> combined = AggregateUtils.make(renderBounds.x, renderBounds.y, renderBounds.x+renderBounds.width, renderBounds.y+renderBounds.height, aggregator.identity());
		boolean combinedEmpty = true;
		Optional<Rectangle> remaining = Optional.empty();
		
		Path tilesetRoot = tilesetPath(datasetId, aggregator, gbt);
		for (Rectangle tile: tileBounds(renderBounds)) {
			File f = tileFile(tilesetRoot, tile);  
			if (!f.exists()) {
				System.out.println("## Missing part from cached, aggregating  --- " + f.getName());
				remaining = Optional.of(remaining.isPresent() ? remaining.get().union(tile) : tile);
			} else {
				try {
					Aggregates<A> aggs = AggregateSerializer.deserialize(f, converter);
					combined = AggregateUtils.__unsafeMerge(combined, aggs, aggregator.identity(), aggregator::rollup);
					combinedEmpty = false;
				} catch (Exception e) {
					System.err.println("## Cache located for " + f + ", but error deserializing.");
					e.printStackTrace();
					remaining = Optional.of(remaining.isPresent() ? remaining.get().union(tile) : tile);
				}
			}
		}
		
		return new CacheStatus<>(combinedEmpty ? null : combined, remaining);
	}
	
	//HACK: TODO: Figure out how to auto-record the CoC key type...
	@SuppressWarnings("unchecked")
	private static final <A> Valuer<GenericRecord, A> converter(String datasetId) {
		if (OptionDataset.BOOST_MEMORY != null && datasetId.equals(OptionDataset.BOOST_MEMORY.name)) {return (Valuer<GenericRecord, A>) new Converters.ToCoCString();}
		else if (OptionDataset.KIVA != null && datasetId.equals(OptionDataset.KIVA.name)) {return (Valuer<GenericRecord, A>) new Converters.ToCount();}
		else if (OptionDataset.SYNTHETIC != null && datasetId.equals(OptionDataset.SYNTHETIC.name)) {return (Valuer<GenericRecord, A>) new Converters.ToCount();}
		else if (OptionDataset.CENSUS_NY_SYN_PEOPLE != null && datasetId.equals(OptionDataset.CENSUS_NY_SYN_PEOPLE.name)) {return (Valuer<GenericRecord, A>) new Converters.ToCoCChar();}
		else if (OptionDataset.CENSUS_SYN_PEOPLE != null && datasetId.equals(OptionDataset.CENSUS_SYN_PEOPLE.name)) {return (Valuer<GenericRecord, A>) new Converters.ToCoCChar();}
		else if (OptionDataset.CENSUS_TRACTS != null && datasetId.equals(OptionDataset.CENSUS_TRACTS.name)) {return (Valuer<GenericRecord, A>) new Converters.ToCoCChar();}
		else if (OptionDataset.GDELT_YEAR != null && datasetId.equals(OptionDataset.GDELT_YEAR.name)) {return (Valuer<GenericRecord, A>) new Converters.ToCoCInteger();}
		else if (OptionDataset.TAXI_DROPOFF != null && datasetId.equals(OptionDataset.TAXI_DROPOFF.name)) {return (Valuer<GenericRecord, A>) new Converters.ToCount();}
		else if (OptionDataset.TAXI_PICKUP != null && datasetId.equals(OptionDataset.TAXI_PICKUP.name)) {return (Valuer<GenericRecord, A>) new Converters.ToCount();}
		else {throw new IllegalArgumentException("Cannot load from cache because root type could not be discerned");}
	}
	
	/**Save out a set of aggregates into tiles.
	 * 
	 * Assumes that the aggregates cover full tiles (easily done using the renderBounds method).
	 * 
	 * TODO: More efficiently save constant aggregates so it doesn't have to reload large swaths of single values
	 * 
	 * @param base Source dataset
	 * @param aggregator Aggregator used to build the aggregates (influences the cache path)
	 * @param gbt View transform used to render the aggregates (used to align the aggregates to the global grid)
	 * @param aggs Aggregates to save (MUST be full tiles aligned to the tile grid)
	 *
	 * **/
	public <A> void save(String datasetId, Aggregator<?,A> aggregator,  AffineTransform gbt, Aggregates<A> aggs) {		
		Path tilesetRoot = tilesetPath(datasetId, aggregator, gbt);
		
		Rectangle aggregateBounds = AggregateUtils.bounds(aggs);		
		List<Rectangle> tileBounds = tileBounds(aggregateBounds);
		
		System.out.println("## Saving aggregates to cache.");
		for (Rectangle bound: tileBounds) {
			Aggregates<A> tile = new SubsetWrapper<>(aggs, bound);
			File f = tileFile(tilesetRoot, bound);
			System.out.printf("##    Saving %s to %s%n", bound, f.getName());
			try {
				f.getParentFile().mkdirs();
				AggregateSerializer.serialize(tile, new FileOutputStream(f));
			} catch (Exception e) {
				System.err.println("## Error saving to cache file " + f);
				e.printStackTrace();
			}
		}
		System.out.println("## Cache saved.");
	}
	
	
	/**Delete the cache directory and everything in it.**/
	public static void clearCache(final File cachedir) throws IOException {
		System.out.println("## Clearing the cache.");
		if (!cachedir.exists()) {return;}
		
		Files.walkFileTree(cachedir.toPath(), 
				new SimpleFileVisitor<Path>(){
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
						Files.deleteIfExists(file);
						return FileVisitResult.CONTINUE;
					}
					
					@Override
					public FileVisitResult postVisitDirectory(Path file, IOException exc) throws IOException {
						if (!Files.isSameFile(file, cachedir.toPath())) {Files.deleteIfExists(file);} //Delete everything EXCEPT the cache directory itself
						return FileVisitResult.CONTINUE;
					}
				});
	}
	
			
	
	/**Same interface as the CacheManager, but NEVER looks at the cache (just uses the base renderer).**/
	public static final class Shim extends CacheManager {
		public Shim(File cachedir, int tileSize, Renderer base) {super(cachedir, tileSize, base);}

		@Override
		public <I, G, A> Aggregates<A> aggregate(
				Glyphset<? extends G, ? extends I> glyphs, Selector<G> selector,
				Aggregator<I, A> aggregator, AffineTransform viewTransform,
				BiFunction<Aggregates<A>, Aggregates<A>, Aggregates<A>> merge,
				Rectangle2D baseframe,
				String targetId,
				Rectangle viewport) {

			AffineTransform gbt = globalBinTransform(baseframe, viewTransform);
			Function<A, Aggregates<A>> allocator = Renderer.simpleAllocator(glyphs, gbt);
			Aggregates<A> aggs = super.base.aggregate(glyphs, selector, aggregator, gbt, allocator, merge);

			int dx = (int) (gbt.getTranslateX() - viewTransform.getTranslateX());
			int dy = (int) (gbt.getTranslateY() - viewTransform.getTranslateY());
			return new SubsetWrapper<>(new ShiftWrapper<>(aggs, dx, dy), viewport);
		}
	}
}
