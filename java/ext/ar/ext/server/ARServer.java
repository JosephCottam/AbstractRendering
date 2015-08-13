package ar.ext.server;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.avro.generic.GenericRecord;

import ar.Aggregates;
import ar.Aggregator;
import ar.Renderer;
import ar.Selector;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.app.components.sequentialComposer.OptionAggregator;
import ar.app.components.sequentialComposer.OptionDataset;
import ar.app.components.sequentialComposer.OptionTransfer;
import ar.ext.avro.AggregateSerializer;
import ar.ext.avro.Converters;
import ar.ext.server.NanoHTTPD.Response.Status;
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.rules.General;
import ar.selectors.TouchesPixel;
import ar.util.HasViewTransform;
import ar.util.Util;
import ar.Glyphset;

public class ARServer extends NanoHTTPD {
	public static final int DEFAULT_PORT = 6582; //In ascii A=65, R=82
	
	private static Map<String, OptionTransfer<?>> TRANSFERS;
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

		
		TRANSFERS.put("Log10", new OptionTransfer() {
			@Override public Transfer transfer(ControlPanel params, Transfer subsequent) {
				MathValuers.Log log = new MathValuers.Log(10);
				Transfer t = new General.TransferFn<Number, Number>(log::apply, 0d);
				return extend(t, subsequent);
			}
			@Override public ControlPanel control(HasViewTransform transformProvider) {return null;}			
		});
		
		TRANSFERS.put("Cuberoot", new OptionTransfer() {
			@Override public Transfer transfer(ControlPanel params, Transfer subsequent) {
				Transfer t = new General.TransferFn<Number, Number>(n -> Math.cbrt(n.doubleValue()), 0d);
				return extend(t, subsequent);
			}
			@Override public ControlPanel control(HasViewTransform transformProvider) {return null;}			
		});
	}
	
	
	private final Path cachedir;
	private final Renderer render = Renderer.defaultInstance();

	public ARServer(String hostname) {this(hostname, DEFAULT_PORT);}
	public ARServer(String hostname, int port) {this(hostname, port, new File("./cache"));}
	public ARServer(String hostname, int port, File cachedir) {
		super(hostname, port);
		this.cachedir = cachedir.toPath();
	}

	@Override 
	public Response serve(String uri, Method method,
			Map<String, String> headers, 
			Map<String, String> parms,
			Map<String, String> files) {
		
		try {
			if (uri.equals("/")) {return help();}
			if (uri.equals("/favicon.ico")) {return new Response(Status.NO_CONTENT, MIME_PLAINTEXT, "");} //TODO: AR favicon? :)
			
			OptionDataset baseConfig = baseConfig(uri);
			
			String format = parms.getOrDefault("format", "png");
			int width = Integer.parseInt(parms.getOrDefault("width", "500"));
			int height = Integer.parseInt(parms.getOrDefault("height", "500"));
			boolean ignoreCached = Boolean.parseBoolean(parms.getOrDefault("ignoreCache", "False"));
			String viewTransTXT = parms.getOrDefault("vt", null);
			
			if (!format.equals("json") && !format.equals("png")) {throw new RuntimeException("Invalid return format: " + format);}
			
			Aggregator<?,?> agg = getAgg(parms.getOrDefault("aggregator", null), baseConfig.defaultAggregator);
			AffineTransform vt = viewTransform(viewTransTXT, baseConfig.glyphset, width, height);
			Transfer transfer = getTransfer(parms.getOrDefault("transfers", null), baseConfig.defaultTransfers);
			//Transfer transfer = Combinators.seq().then(new Categories.ToCount()).then(new Numbers.Interpolate(Color.white, Color.red));

			
			File cacheFile = cacheFile(baseConfig.name, agg, width, height);
			Optional<Aggregates<?>> cached = !ignoreCached ? loadCached(cacheFile, agg, width, height) : Optional.empty();
			Aggregates<?> aggs = cached.orElseGet(() -> aggregate(baseConfig.glyphset, agg, vt));
			if (!cached.isPresent()) {cache(aggs, cacheFile);}
			
			
			System.out.println("## Excuting transfer");
			Transfer.Specialized ts = transfer.specialize(aggs);
			Aggregates<?> post_transfer = render.transfer(aggs, ts);
			//Aggregates<?> post_transfer = aggs;

			Response rslt;
			ByteArrayOutputStream baos = new ByteArrayOutputStream((int) (AggregateUtils.size(aggs)));	//An estimate...png is compressed after all
			if (format.equals("png")) {
				BufferedImage img = AggregateUtils.asImage((Aggregates<? extends Color>) post_transfer); 
				Util.writeImage(img, baos, true);
				rslt = new Response(Status.OK, "png", new ByteArrayInputStream(baos.toByteArray()));
			} else {
				AggregateSerializer.serialize(post_transfer, baos, AggregateSerializer.FORMAT.JSON);
				rslt = new Response(Status.OK, "avro/" + format, new String(baos.toByteArray(), "UTF-8"));
			}
			System.out.println("## Sending response");
			return rslt;
		} catch (Exception e) {
			e.printStackTrace();
			return new Response(Status.ACCEPTED, MIME_PLAINTEXT, "Error:" + e.toString());
		}
	}
	
	public File cacheFile(String datasetId, Aggregator<?,?> agg, int width, int height) {
		String aggId = agg.getClass().getSimpleName();
		String base = Arrays.stream(new String[]{datasetId, aggId, Integer.toString(width), Integer.toString(height)}).collect(Collectors.joining("-"));
		return cachedir.resolve(base + ".avsc").toFile();
	}
	
	//TODO: Add View transform to cache info
	public Optional<Aggregates<?>> loadCached(File cacheFile, Aggregator<?,?> aggregator, int width, int height) {		
		if (!cacheFile.exists()) {return Optional.empty();}
		
		Valuer<GenericRecord, ?> converter = Converters.getDeserialize(aggregator);
		
		try {
			System.out.println("## Loading cached aggregates.");
			Aggregates<?> aggs = AggregateSerializer.deserialize(cacheFile, converter);

			boolean renderMatches = (aggs.highX()-aggs.lowX() == width || aggs.highY()-aggs.lowY() == height);
			if (renderMatches) {return Optional.of(aggs);}
			else {return Optional.empty();}
		} catch (Exception e) {
			System.err.println("## Cache located for " + cacheFile + ", but error deserializing.");
			e.printStackTrace();
			return Optional.empty();
		}
	}

	public Transfer getTransfer(String transferIds, List<OptionTransfer<?>> def) {
		List<OptionTransfer<?>> transfers = def;
		
		if (transferIds!=null && !transferIds.trim().equals("")) {
			transfers = Arrays.stream(transferIds.split(";")).map(this::getTransfer).collect(Collectors.toList());
		} else {
			transfers = new ArrayList<>();
			transfers.addAll(def);
		}
		
		Collections.reverse(transfers);
		Transfer t=null;
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
	
	public Aggregates<?> aggregate(Glyphset glyphs, Aggregator agg, AffineTransform view) {
		Selector<?> s = TouchesPixel.make(glyphs);
		Aggregates<?> aggs = render.aggregate(glyphs, s, agg, view);
		return aggs;
	}
		
	//TODO: Remove dependence on g...'render before zoom' work
	public AffineTransform viewTransform(String vtTXT, Glyphset<?,?> g, int width, int height) {
		if (vtTXT == null) {
			Rectangle2D bounds = g.bounds();
			return Util.zoomFit(bounds, width, height);
		} else {
			String[] parts = vtTXT.split(",");
			double sx = Double.parseDouble(parts[0]);
			double sy = Double.parseDouble(parts[1]);
			double tx = Double.parseDouble(parts[2]);
			double ty = Double.parseDouble(parts[3]);
			AffineTransform vt = new AffineTransform(sx,0,0,sy,tx,ty);
			return vt;
		}
	}
	
	
	public OptionDataset baseConfig(String uri) {
		String config = uri.substring(1).toUpperCase(); //Trim off leading slash
		try {
			return (OptionDataset) OptionDataset.class.getField(config).get(null);
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not find indicated base config: " + config);
		}
	}
		
	public Aggregator<?,?> getAgg(String aggId, OptionAggregator<?,?> def) {
		if (aggId == null || aggId.trim().equals("")) {return def.aggregator();}
		
		try {
			OptionAggregator option = (OptionAggregator) OptionAggregator.class.getField(aggId).get(null);
			return option.aggregator();
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not find indicated aggregator: " + aggId);
		}
	}

	public OptionTransfer getTransfer(String transfer) {
		if (!TRANSFERS.containsKey(transfer)) {throw new IllegalArgumentException("Could not find indicated transfer: " + transfer);}
		return TRANSFERS.get(transfer);
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
	public Collection<String> getAggregators() {return getFieldsOfType(OptionAggregator.class, OptionAggregator.class);}
	public Collection<String> getDatasets() {return getFieldsOfType(OptionDataset.class, OptionDataset.class);}
	
	public Collection<String> getContainedClasses(Class<?> source, Class<?> type) {
		return Arrays.stream(source.getClasses())
				.filter(c -> type.isAssignableFrom(c))
				.map(c -> {
						try {return String.format("<pre>%-20s\t(%s)</pre>", c.getSimpleName(), c.newInstance().toString());}
						
						catch (Exception e) {return "--";}
					})
				.collect(Collectors.toList());
	}
	
	public Collection<String> getFieldsOfType(Class<?> source, Class<?> type) {
		return Arrays.stream(source.getFields())
				.filter(f -> f.getType().equals(type))
				.map(f -> f.getName())
				.collect(Collectors.toList());
	}
	
	private String asList(Collection<String> items, String format) {
		return "<ul>" + items.stream().map(e -> String.format(format, e)).collect(Collectors.joining("\n")) + "</ul>\n\n";
	}
	public Response help() {
		String help = "<H1>AR Server help</H1>"
					+ "Simple interface to the default configurations in the extended AR demo application (ar.app.components.sequentialComposer)<br>"
					+ "The path sets the base configuration, query parameters modify that configuration.<br>"
					+ "URL Format:  host\\base-configuration&...<br><br>"
					+ "\nBase-Configurations: one of --\n" + asList(getDatasets(), "<li><a href='%1$s'>%1$s</a></li>") + "<br><br>" 
					+ "Query Paramters ----------------<br>"
					+ "width/height: Set in pixels<br>"
					+ "format: png, json<br>"
					+ "ignoreCache: True/False -- If set to True, will not laod cached data (may still save it)"
					//+ "vt: Set the view transform as list scale-x, scale-y, translate-x, translate-y"
					//+ "limit: Sets a clip-rectangle as list x,y,w,h;  Will only process data inside the clip"
					+ "aggregator: one of--\n" + asList(getAggregators(), "<li>%s</li>") + "\n\n"
					+ "transfers:  semi-colon separated list of-- \n" + asList(getTransfers(), "<li>%s</li>") + "\n\n";
				
					
		
		return new Response(Status.OK, MIME_HTML, help);
	}
	
	public static void main(String[] args) throws Exception {
		String host = ar.util.Util.argKey(args, "-host", "localhost");
		int port = Integer.parseInt(ar.util.Util.argKey(args, "-port", Integer.toString(DEFAULT_PORT)));
		File cachedir = new File(ar.util.Util.argKey(args, "-cache", "./cache"));
		
		if (!cachedir.exists()) {cachedir.mkdirs();}
		if (!cachedir.isDirectory()) {throw new IllegalArgumentException("Indicated cache directory exists BUT is not a directory." + cachedir);}
		
		ARServer server = new ARServer(host, port, cachedir);
		
		server.start();
		
		System.out.printf("AR Server started on %s:%d%n", host, port);
		while (server.isAlive()) {synchronized(server) {server.wait(10000);;}}
	}
}
