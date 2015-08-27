package ar.ext.server;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.avro.generic.GenericRecord;

import ar.Aggregates;
import ar.Aggregator;
import ar.Renderer;
import ar.Selector;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.aggregates.implementations.ConstantAggregates;
import ar.aggregates.wrappers.SubsetWrapper;
import ar.app.components.sequentialComposer.OptionAggregator;
import ar.app.components.sequentialComposer.OptionDataset;
import ar.app.components.sequentialComposer.OptionTransfer;
import ar.ext.avro.AggregateSerializer;
import ar.ext.avro.Converters;
import ar.ext.lang.BasicLibrary;
import ar.ext.lang.Parser;
import ar.ext.server.NanoHTTPD.Response.Status;
import ar.glyphsets.BoundingWrapper;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.renderers.ProgressRecorder;
import ar.renderers.ThreadpoolRenderer;
import ar.rules.General;
import ar.selectors.TouchesPixel;
import ar.util.HasViewTransform;
import ar.util.Util;
import ar.Glyphset;

public class ARServer extends NanoHTTPD {
	public static final int DEFAULT_PORT = 6582; //In ascii A=65, R=82
	private static Map<String, OptionTransfer<?>> TRANSFERS;

	private final Path cachedir;
	private final Map<Object, Renderer> tasks = new ConcurrentHashMap<>();

	public ARServer(String hostname) {this(hostname, DEFAULT_PORT);}
	public ARServer(String hostname, int port) {this(hostname, port, new File("./cache"));}
	public ARServer(String hostname, int port, File cachedir) {
		super(hostname, port);
		this.cachedir = cachedir.toPath();
	}

	
	public static final AffineTransform flipHorizontal(int height) {
		AffineTransform flip = new AffineTransform();
		flip.translate(0, height/2);
		flip.scale(1, -1);
		flip.translate(0, -height/2);
		return flip;
	}
	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override 
	public Response serve(IHTTPSession session) {
			
		String uri = session.getUri();
		//Map<String, String> headers = session.getHeaders(); 
		Map<String, String> params = session.getParms();
		
		if (uri.equals("/")) {return help();}
		if (uri.equals("/favicon.ico")) {return newFixedLengthResponse(Status.NO_CONTENT, MIME_PLAINTEXT, "");} //TODO: AR favicon? :)
		
		System.out.printf("## Processing request: %s?%s%n", uri, session.getQueryParameterString());
		
		OptionDataset baseConfig = baseConfig(uri);
		
		String format = params.getOrDefault("format", "png");
		int width = Integer.parseInt(params.getOrDefault("width", "500"));
		int height = Integer.parseInt(params.getOrDefault("height", "500"));
		boolean allowStretch = Boolean.parseBoolean(params.getOrDefault("allowStretch", "False"));
		boolean ignoreCached = Boolean.parseBoolean(params.getOrDefault("ignoreCache", "False"));
		Optional<Rectangle2D> selection = Arrays.stream(params.getOrDefault("select", "").split(";|,")).collect(DOUBLE_RECT);
		Optional<List<Point2D>> latlon = Arrays.stream(params.getOrDefault("latlon", "").split(";|,")).collect(POINTS);
		Optional<Rectangle> crop = Arrays.stream(params.getOrDefault("crop", "").split(";|,")).collect(INT_RECT);
		Optional<Rectangle> enhance = Arrays.stream(params.getOrDefault("enhance", "").split(";|,")).collect(INT_RECT);
		Object requesterID = params.getOrDefault("requesterID", Double.toString(Math.random()));
		
		Aggregator<?,?> agg = getAgg(params.getOrDefault("aggregator", null), baseConfig.defaultAggregator);
		Transfer transfer;
		
		try {transfer = params.containsKey("arl") ? parseTransfer(params.get("arl")) : defaultTransfer(baseConfig.defaultTransfers);}
		catch (Exception e) {return newFixedLengthResponse(Status.ACCEPTED, MIME_PLAINTEXT, "Error:" + e.toString());}
		
        long start = System.currentTimeMillis();
        Response rsp;
        try {
        	rsp = execute(format, width, height, agg, transfer, baseConfig, selection, latlon, crop, enhance, requesterID, ignoreCached, allowStretch);
        } finally { 
            long end = System.currentTimeMillis();
            System.out.printf("## Excution time: %d ms%n", (end-start));
        }
        return rsp;
	}
		
	private <G,I,A,OUT> Response execute(String format, int width, int height, 
			Aggregator<I,A> agg,
			Transfer<A,OUT> transfer,
			OptionDataset<G,I> baseConfig,
			Optional<Rectangle2D> selection, Optional<List<Point2D>> latlon, Optional<Rectangle> crop, Optional<Rectangle> enhance, 
			Object requesterID,
			boolean ignoreCached, 
			boolean allowStretch) {
		
		if (tasks.containsKey(requesterID)) {
			System.out.println("## Signaling shutdown to existing session by requester " + requesterID);
			tasks.get(requesterID).stop();
		}
		
		if (!format.equals("json") && !format.equals("png")) {throw new RuntimeException("Invalid return format: " + format);}
		

		try {
			System.out.println("## Loading dataset");
			if (latlon.isPresent()) {
				Rectangle2D bounds;
				if (baseConfig.flags.contains("EPSG:900913")) {
					bounds = DegreesToMeters.from(latlon.get().get(0), latlon.get().get(1));
				} else {
					bounds = new Line2D.Double(latlon.get().get(0), latlon.get().get(1)).getBounds2D();
				}
				selection = Optional.of(bounds);
			}
			
			Glyphset<G,I> glyphs;
			Rectangle2D zoomBounds;
			if (selection.isPresent()) {
				zoomBounds = selection.get();
				ignoreCached = true; //TODO: implement caching logic with selections
			} else {
				zoomBounds = baseConfig.glyphset.bounds();
			}
			
			AffineTransform vt;
			Rectangle2D renderBounds;
			if (!allowStretch) {
				vt = centerFit(zoomBounds, width, height);
				renderBounds = expandSelection(vt, zoomBounds, width, height);
				zoomBounds = renderBounds;
			} else {
				vt = stretchFit(zoomBounds, width, height);
				renderBounds = zoomBounds;
			}
 			
			if (selection.isPresent()) {
				glyphs = new BoundingWrapper<>(baseConfig.glyphset, zoomBounds);
			} else {
				glyphs = baseConfig.glyphset;
			}
			
			Renderer render = new ThreadpoolRenderer(new ProgressRecorder.NOP());
			tasks.put(requesterID, render);
			
			File cacheFile = cacheFile(baseConfig, vt, agg);
			Optional<Aggregates<A>> cached = !ignoreCached ? loadCached(cacheFile, baseConfig, vt, agg) : Optional.empty();
			
			Aggregates<A> aggs;
			try {aggs = cached.orElseGet(() -> aggregate(render, glyphs, agg, vt));}
			catch (Renderer.StopSignaledException e) {return newFixedLengthResponse("Render stopped by signal before completion.");} 
			
			if (aggs == null && selection.isPresent()) {return newFixedLengthResponse("Empty selection, no result.");}
			if (!ignoreCached && !cached.isPresent()) {cache(aggs, cacheFile);} 
			
			System.out.println("## Executing transfer");
 			Aggregates<A> spec_aggs = enhance.isPresent() ? new SubsetWrapper<>(aggs, enhance.get()) : aggs;
			Aggregates<A> target_aggs = crop.isPresent() ? new SubsetWrapper<>(aggs, crop.get()) : aggs; 
			Transfer.Specialized<A,OUT> ts = transfer.specialize(spec_aggs);

			Aggregates<OUT> post_transfer;
			try {post_transfer = render.transfer(target_aggs, ts);}
			catch (Renderer.StopSignaledException e) {return newFixedLengthResponse("Transfer stopped by signal before completion.");} 

			tasks.remove(requesterID);
			
			Rectangle returnBounds = vt.createTransformedShape(renderBounds).getBounds();
			Aggregates<OUT> full_size = new OverlayWrapper<>(
					post_transfer, 
					new ConstantAggregates<>(post_transfer.defaultValue(), 
							returnBounds.x, returnBounds.y, returnBounds.x+returnBounds.width, returnBounds.y+returnBounds.height));
			
			Response rslt;
			ByteArrayOutputStream baos = new ByteArrayOutputStream((int) (AggregateUtils.size(full_size)));	//An estimate...png is compressed after all
			if (format.equals("png")) {
				@SuppressWarnings("unchecked")
				BufferedImage img = AggregateUtils.asImage((Aggregates<Color>) full_size);
				if (baseConfig.flags.contains("NegativeDown")) {
					BufferedImageOp op = new AffineTransformOp(flipHorizontal(img.getHeight()), AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
					BufferedImage src = img;
					img = op.createCompatibleDestImage(src, src.getColorModel());
					op.filter(src, img);
				}
				
				Util.writeImage(img, baos, false);
				rslt = newChunkedResponse(Status.OK, "png", new ByteArrayInputStream(baos.toByteArray()));
			} else {
				AggregateSerializer.serialize(full_size, baos, AggregateSerializer.FORMAT.JSON);
				rslt = newFixedLengthResponse(Status.OK, "avro/" + format, new String(baos.toByteArray(), "UTF-8"));
			}
			System.out.println("## Sending response");
			return rslt;
		} catch (Exception e) {
			e.printStackTrace();
			return newFixedLengthResponse(Status.ACCEPTED, MIME_PLAINTEXT, "Error:" + e.toString());
		}
	}

	/**Zoom fit, but align the center of the bounding region (not top-left, as Util.zoomFit does)**/
	public AffineTransform stretchFit(Rectangle2D content, int width, int height) {
		if (content == null) {return new AffineTransform();}

		double ws = width/content.getWidth();
		double hs = height/content.getHeight();
		double xmargin = width/ws-content.getWidth();
		double ymargin = height/hs-content.getHeight();
		double tx = content.getMinX()-(xmargin/2);
		double ty = content.getMinY()-(ymargin/2);

		AffineTransform t = AffineTransform.getScaleInstance(ws,hs);
		t.translate(-tx,-ty);
		return t;
	}

	
	/**Zoom fit, but align the center of the bounding region (not top-left, as Util.zoomFit does)**/
	public AffineTransform centerFit(Rectangle2D bounds, int width, int height) {
		AffineTransform vt = Util.zoomFit(bounds, width, height);
		Rectangle2D fit = vt.createTransformedShape(bounds).getBounds2D();
		
		
		try {
			Rectangle2D remainder = vt.createInverse().createTransformedShape((new Rectangle2D.Double(0,0, width - fit.getWidth(), height - fit.getHeight()))).getBounds2D();
			double dtx = remainder.getWidth();
			double dty = remainder.getHeight();
			vt.translate(dtx/2, dty/2);
			return vt;
		} catch (Exception e) {throw new RuntimeException("Specified view cannot be realized.");}
	}

	/**Expand the given bounds so it fills width/height region under the given view transform**/
	public Rectangle2D expandSelection(AffineTransform vt, Rectangle2D bounds, int width, int height) {
		Rectangle2D selection = vt.createTransformedShape(bounds).getBounds2D();
		//TODO: Does not account for shear...so not fully general
		try {
			Rectangle2D remainder = vt.createInverse().createTransformedShape((new Rectangle2D.Double(0,0,width-selection.getWidth(), height-selection.getHeight()))).getBounds2D();
			double dtx = remainder.getWidth();
			double dty = remainder.getHeight();
			Rectangle2D newBounds = new Rectangle2D.Double(bounds.getX()-dtx/2, bounds.getY()-dty/2, bounds.getWidth()+dtx, bounds.getHeight()+dty);
			return newBounds;
		} catch (Exception e) {throw new RuntimeException("Specified view cannot be realized.");}
	}
	
	public File cacheFile(OptionDataset<?,?> config, AffineTransform vt, Aggregator<?,?> agg) {
		String datasetId = config.name;
		String aggId = agg.getClass().getSimpleName();
		Rectangle renderBounds = vt.createTransformedShape(config.glyphset.bounds()).getBounds();
		
		int width = (renderBounds.width/10)*10;
		int height = (renderBounds.height/10)*10;
		
		String base = Arrays.stream(new String[]{datasetId, aggId, Integer.toString(width), Integer.toString(height)}).collect(Collectors.joining("-"));
		return cachedir.resolve(base + ".avsc").toFile();
	}
	
	
	//TODO: Add View transform (or derivative) to cache info?
	public <A> Optional<Aggregates<A>> loadCached(File cacheFile, OptionDataset<?,?> baseConfig, AffineTransform vt, Aggregator<?,A> aggregator) {		
		if (!cacheFile.exists()) {return Optional.empty();}
		
		Valuer<GenericRecord, A> converter = Converters.getDeserialize(aggregator);
		
		try {
			System.out.println("## Loading cached aggregates.");
			Aggregates<A> aggs = AggregateSerializer.deserialize(cacheFile, converter);
			
			Rectangle projected = vt.createTransformedShape(baseConfig.glyphset.bounds()).getBounds();

			boolean renderMatches = (aggs.highX()-aggs.lowX() == projected.width|| aggs.highY()-aggs.lowY() == projected.height);
			if (renderMatches) {return Optional.of(aggs);} 
			else {
				System.out.println("## Render match failed, scraping cached aggegates. " + projected);
				return Optional.empty();}
		} catch (Exception e) {
			System.err.println("## Cache located for " + cacheFile + ", but error deserializing.");
			e.printStackTrace();
			return Optional.empty();
		}
	}
		
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Transfer<?,?> defaultTransfer(List<OptionTransfer<?>> def) {
		List<OptionTransfer<?>> transfers = new ArrayList();
		transfers.addAll(def);
		
		Collections.reverse(transfers);
		Transfer<?,?> t=null;
		for (OptionTransfer option: transfers) {
			t = option.transfer(option.control(null), t);
		}
		return t;
	}
	
	
	
	public <A> void cache(Aggregates<A> aggs, File cacheFile) {		
		try {
			System.out.println("## Saving aggregates to cache.");
			AggregateSerializer.serialize(aggs, new FileOutputStream(cacheFile));
		} catch (IOException e) {
			System.err.println("## Error saving to cache file " + cacheFile);
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <A> Aggregates<A> aggregate(Renderer render, Glyphset glyphs, Aggregator agg, AffineTransform view) {
		Selector<?> s = TouchesPixel.make(glyphs);
		Aggregates<A> aggs = render.aggregate(glyphs, s, agg, view);
		return aggs;
	}
			
	@SuppressWarnings({"rawtypes" })
	public OptionDataset baseConfig(String uri) {
		String config = uri.substring(1).toUpperCase(); //Trim off leading slash
		try {
			return (OptionDataset) OptionDataset.class.getField(config).get(null);
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not find indicated base config: " + config);
		}
	}
		
	public Aggregator<?,?> getAgg(String aggId, OptionAggregator<?,?> def) {
		if (aggId == null) {return def.aggregator();}
		aggId = aggId.trim();
		if (aggId.equals("") || aggId.equals("null")) {return def.aggregator();}
		
		try {
			OptionAggregator<?,?> option = (OptionAggregator<?,?>) OptionAggregator.class.getField(aggId).get(null);
			return option.aggregator();
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not find indicated aggregator: " + aggId);
		}
	}

	/**Get an item from the parameters dictionary. 
	 * If it is not present, return an exception with the given error message.**/ 
	public String errorGet(Map<String,String> params, String key) {
		String v = params.get(key);
		if (v != null) {return v;}
		throw new RuntimeException(String.format("Entry not found '%s'", key));
	}

	/**@return collection of known transfer names**/
	public Collection<String> getTransfers() {return TRANSFERS.keySet();}
	
	/**@return collection of known transfer names**/
	public Collection<Field> getAggregators() {return getFieldsOfType(OptionAggregator.class, OptionAggregator.class);}
	public Collection<Field> getDatasets() {return getFieldsOfType(OptionDataset.class, OptionDataset.class);}
	
	public Collection<String> getContainedClasses(Class<?> source, Class<?> type) {
		return Arrays.stream(source.getClasses())
				.filter(c -> type.isAssignableFrom(c))
				.map(c -> {
						try {return String.format("<pre>%-20s\t(%s)</pre>", c.getSimpleName(), c.newInstance().toString());}
						
						catch (Exception e) {return "--";}
					})
				.collect(Collectors.toList());
	}
	
	public static Predicate<Field> loaded(Class<?> source) {
		return f -> {try {return f.get(source) != null;}
						   catch (Throwable e) {return false;}};
	}
	
	public static  String getArl(Field f) {
		 try {return ((OptionDataset<?,?>) f.get(null)).arl;}
		 catch (Throwable e) {return "null";}
	}

	
	public Collection<Field> getFieldsOfType(Class<?> source, Class<?> type) {
		return Arrays.stream(source.getFields())
				.filter(f -> f.getType().equals(type))
				.filter(loaded(source))
				.collect(Collectors.toList());
	}
	
	private String asList(Collection<Field> items, Function<Field, Object[]> toString, String format) {
		return "<ul>" + items.stream().map(toString).map(e -> String.format(format, e)).collect(Collectors.joining("\n")) + "</ul>\n\n";
	}
	public Response help() {
		
		
		String help = "<H1>AR Server help</H1>"
					+ "Simple interface to the default configurations in the extended AR demo application (ar.ap)p.components.sequentialComposer)<br>"
					+ "The path sets the base configuration, query parameters modify that configuration.<br>"
					+ "URL Format:  host\\base-configuration&...<br><br>"
					+ "\nBase-Configurations: one of --\n" + asList(getDatasets(), f->new Object[]{f.getName()}, "<li><a href='%1$s'>%1$s</a></li>") + "<br><br>" 
					+ "Query Paramters ----------------<br>"
					+ "width/height: Set in pixels, directly influencing zoom (as there is it always runs a 'zoom fit')<br>"
					+ "format: either 'png' or 'json'<br>"
					+ "ignoreCache: True/False -- If set to True, will not load cached data (may still save it)<br>"
					+ "allowStretch: True/False -- If True, will render the selection so it fills the width/height, regardless of distortions<br>"
					+ "select: x;y;w;h -- Sets a clip-rectangle as list x,y,w,h on the glyphs in glyph coordinates<br>"
					+ "latlon: x1;y1;x2;y2 -- Sets a clip-rectangle as list by diagonal opposite points in lat/lon coordinates.<br>"
					+ "crop: x;y;w;h -- Sets a clip-rectangle as list x,y,w,h on the aggregates in bin coordinates;  Will only return values in the crop.<br>"
					+ "enhance: x;y;w;h -- Sets a clip-rectangle for specialization in bin coordinates<br><br>"
					+ "select and latlon have may process values outside of the specified bounding rectangles<br><br>"
					+ "arl: AR-language string (see below for details)<br>\n" 
					+ "aggregator: one of--\n" + asList(getAggregators(), f->new Object[]{f.getName()}, "<li>%s</li>") + "\n\n"
					+ "<hr>"
					+ "<h3>AR Language:</h3>"
					+ Parser.basicHelp("<br>") + "<br><br>"
					+ "A few examples (base configurations with the transfer spelled out):\n"
					+ asList(getDatasets(), f->new String[]{f.getName(), getArl(f)},"<li><a href='%1$s?arl=%2$s'>%1$s?arl=%2$s</a></li>") + "<br><br>" 
					+ "Available functions:<br>"
					+ Parser.functionHelp(BasicLibrary.ALL, "<li>%s</li>", "\n");
				
		
		
				
					
		
		return newFixedLengthResponse(Status.OK, MIME_HTML, help);
	}

	public Transfer<?,?> parseTransfer(String source) {
		Parser.TreeNode<?> tree;
		try{tree = Parser.parse(source);}
		catch (Exception e) {
			System.out.println(Parser.tokens(source).stream().collect(Collectors.joining(" -- ")));
			e.printStackTrace();
			throw new RuntimeException(e.getMessage() + "\n While parsing " + source);
		}

		
		try {
			return (Transfer<?,?>) Parser.reify(tree, BasicLibrary.ALL);
		} catch (Throwable e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage() + "\n While processing " + tree.toString());
		}
	}
	
	static {
		TRANSFERS = Arrays.stream(OptionTransfer.class.getClasses())
				.filter(c -> OptionTransfer.class.isAssignableFrom(c))
				.filter(c -> !c.getSimpleName().equals("AutoLegend"))
				.filter(c -> !c.getSimpleName().equals("MathTransfer"))
				.collect(Collectors.toMap(
						c -> c.getSimpleName(), 
						c -> {try {return (OptionTransfer<?>) c.newInstance();}
							  catch (Exception e) {return null;}}));

		TRANSFERS.put("HDInterpolate", TRANSFERS.get("ColorCatInterpolate"));

		
		TRANSFERS.put("Log10", new OptionTransfer<OptionTransfer.ControlPanel>() {
			@SuppressWarnings({"unchecked", "rawtypes"})
			@Override public Transfer transfer(ControlPanel params, Transfer subsequent) {
				MathValuers.Log log = new MathValuers.Log(10);
				Transfer t = new General.TransferFn<Number, Number>(log::apply, 0d);
				return extend(t, subsequent);
			}
			@Override public ControlPanel control(HasViewTransform transformProvider) {return null;}			
		});
		
		TRANSFERS.put("Cuberoot", new OptionTransfer<OptionTransfer.ControlPanel>() {
			@SuppressWarnings({"unchecked", "rawtypes"})
			@Override public Transfer transfer(ControlPanel params, Transfer subsequent) {
				Transfer t = new General.TransferFn<Number, Number>(n -> Math.cbrt(n.doubleValue()), 0d);
				return extend(t, subsequent);
			}
			@Override public ControlPanel control(HasViewTransform transformProvider) {return null;}			
		});
	}
	

	/**Convert values in the latitude, longitude to EPSG:900913 meters system (used by google maps)
	 * 
	 * based on: https://gist.github.com/onderaltintas/6649521
	 */
	public static final class DegreesToMeters {
		private static final double PI360 = Math.PI / 360;
		private static final double PI180 = Math.PI / 180;
		
		public static Rectangle2D from(Rectangle2D degrees) {
			Point2D topLeft = from(new Point2D.Double(degrees.getMaxX(), degrees.getMaxY()));
			Point2D bottomRight = from(new Point2D.Double(degrees.getMinX(), degrees.getMinY()));
			return from(topLeft, bottomRight);
		}
		
		public static Rectangle2D from(Point2D one, Point2D two) {
			Point2D a = from(one);
			Point2D b = from(two);
			return new Line2D.Double(a,b).getBounds2D();			
		}
		
		public static Point2D from(Point2D degrees) {
			final double lat = degrees.getY();
			final double lon = degrees.getX();
			final double x = lon * 20037508.34 / 180;
			double y = Math.log(Math.tan((90 + lat) * PI360)) / (PI180);
	        y = y * 20037508.34 / 180;
	        return new Point2D.Double(x,y);
		}
	}
	
	/**Convert values in the EPSG:900913 meters system (used by google maps) to latitude, longitude.
	 * 
	 * based on: https://gist.github.com/onderaltintas/6649521
	 */
	public static final class MetersToDegrees implements Shaper<Indexed, Point2D> {
		private final int xIdx, yIdx;
		private static final double PI2 = Math.PI / 20037508.34;
		
		public MetersToDegrees(int xIdx, int yIdx) {this.xIdx = xIdx; this.yIdx = yIdx;}
		
		@Override
		public Point2D apply(Indexed t) {
			final double x = ((Number) t.get(xIdx)).doubleValue();
			final double y = ((Number) t.get(yIdx)).doubleValue();
			final double lon = x *  180 / 20037508.34 ;
			final double lat = Math.atan(Math.exp(y * PI2)) * 360 / Math.PI - 90;
			final Point2D rslt = new Point2D.Double(lon, -lat);
			return rslt;
		}
	}
	
	Collector<String, ArrayList<Double>, Optional<List<Point2D>>> POINTS = Collector.of(
			() -> new ArrayList<Double>(), 
			(a, s) -> {if (!s.equals("")) {a.add(Double.parseDouble(s));}}, 
			(a, b) -> {a.addAll(b); return a;}, 
			(ArrayList<Double> a) -> a.size() >= 4 
					? Optional.of(Arrays.asList(new Point2D.Double(a.get(0), a.get(1)), new Point2D.Double(a.get(2), a.get(3)))) 
					: Optional.empty());

	Collector<String, ArrayList<Integer>, Optional<Rectangle>> INT_RECT = Collector.of(
			() -> new ArrayList<Integer>(), 
			(a, s) -> {if (!s.equals("")) {a.add(Integer.parseInt(s));}}, 
			(a, b) -> {a.addAll(b); return a;}, 
			(ArrayList<Integer> a) -> a.size() >= 4 ? Optional.of(new Rectangle(a.get(0), a.get(1), a.get(2), a.get(3))) : Optional.empty());
	
	Collector<String, ArrayList<Double>, Optional<Rectangle2D>> DOUBLE_RECT = Collector.of(
			() -> new ArrayList<Double>(), 
			(a, s) -> {if (!s.equals("")) {a.add(Double.parseDouble(s));}}, 
			(a, b) -> {a.addAll(b); return a;}, 
			(ArrayList<Double> a) -> a.size() >= 4 ? Optional.of(new Rectangle2D.Double(a.get(0), a.get(1), a.get(2), a.get(3))) : Optional.empty());
		
	public static void main(String[] args) throws Exception {
		String host = ar.util.Util.argKey(args, "-host", "localhost");
		int port = Integer.parseInt(ar.util.Util.argKey(args, "-port", Integer.toString(DEFAULT_PORT)));
		File cachedir = new File(ar.util.Util.argKey(args, "-cache", "./cache"));
		
		if (!cachedir.exists()) {cachedir.mkdirs();}
		if (!cachedir.isDirectory()) {throw new IllegalArgumentException("Indicated cache directory exists BUT is not a directory." + cachedir);}
				
		ARServer server = new ARServer(host, port, cachedir);
		
		server.start();
		
		System.out.printf("AR Server started on %s: %d%n", host, port);
		while (server.isAlive()) {synchronized(server) {server.wait(10000);;}}
	}
}
