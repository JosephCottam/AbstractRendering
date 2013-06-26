package ar.ext.server;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ar.AggregateReducer;
import ar.Aggregates;
import ar.Aggregator;
import ar.Renderer;
import ar.Transfer;
import ar.app.util.Wrapped;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;
import ar.ext.avro.AggregateSerailizer;
import ar.ext.server.NanoHTTPD.Response.Status;
import ar.glyphsets.implicitgeometry.Indexed.ToValue;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.glyphsets.implicitgeometry.Valuer.Binary;
import ar.renderers.ParallelGlyphs;
import ar.renderers.ParallelSpatial;
import ar.rules.AggregateReducers;
import ar.util.GlyphsetLoader;
import ar.util.Util;
import ar.Glyphset;


public class ARServer extends NanoHTTPD {
	private static Map<String, Transfer> TRANSFERS = 
			load(Transfer.class, 
					WrappedTransfer.class, 
					new Valuer<WrappedTransfer,String>() {public String value(WrappedTransfer o) {return o.getClass().getSimpleName();}},
					new Valuer<WrappedTransfer,Transfer>() {public Transfer value(WrappedTransfer o) {return o.op();}});
	private static Map<String, Aggregator> AGGREGATORS = 
			load(Aggregator.class, 
					WrappedAggregator.class, 
					new Valuer<WrappedAggregator,String>() {public String value(WrappedAggregator o) {return o.getClass().getSimpleName();}},
					new Valuer<WrappedAggregator, Aggregator>() {public Aggregator value(WrappedAggregator o) {return o.op();}});
	private static Map<Class<?>, AggregateReducer> REDUCERS = 
			load(AggregateReducer.class, 
					AggregateReducers.class, 
					new Valuer<AggregateReducer, Class<?>>() {public Class value(AggregateReducer o) {return ((AggregateReducer) o).left();}},
					new Valuer.IdentityValuer<AggregateReducer>());
	private static Map<String, Glyphset<?>> DATASETS = new HashMap<>();
	
	static {
		DATASETS.put("CIRCLEPOINTS", GlyphsetLoader.load("Scatterplot", "../data/circlepoints.csv", .1));
		DATASETS.put("BOOST", GlyphsetLoader.memMap("BGL Memory", "../data/MemVisScaledB.hbin", .001, .001, true, new ToValue<>(2, new Binary<Integer,Color>(0, Color.BLUE, Color.RED)), 1, "ddi"));
	}
	
	private static <K,WV,V> Map<K,V> load(Class<V> type, Class<?> index, Valuer<WV,K> makeKey, Valuer<WV,V> valuer) {
		Map<K,V> instances = new HashMap<>();
		Class<?>[] items = index.getClasses();

		for (Class<?> item: items) {
			if (!Wrapped.class.isAssignableFrom(item)) {continue;}
			try {
				Constructor<WV> c = (Constructor<WV>) item.getConstructor();
				WV wv = c.newInstance();
				V v = valuer.value(wv);
				K k = makeKey.value(wv);
				instances.put(k,v);
			} catch (Exception e) {continue;}
		}
		return instances;
	}
	
	
	public ARServer(String hostname, int port) {
		super(hostname, port);
	}

	public Response serve(String uri, Method method,
			Map<String, String> headers, Map<String, String> parms,
			Map<String, String> files) {
		
		try {
			String datasetID = errorGet(parms, "data");
			String aggID = safeGet(parms, "aggregate", "count");
			String transferIDS = safeGet(parms, "transfers", ""); 
			String format = safeGet(parms, "format", "json");
			int width = Integer.parseInt(safeGet(parms, "width", "500"));
			int height = Integer.parseInt(safeGet(parms, "format", "500"));
			String viewTransTXT = safeGet(parms, "vt", null);
			
			if (!format.equals("json") && !format.equals("binary")) {throw new RuntimeException("Invalid return format: " + format);}
			
			Glyphset<?> dataset = loadData(datasetID);
			Aggregator<?,?> agg = getAgg(aggID);
			List<Transfer<?,?>> transfers = getTransfers(transferIDS);
			AffineTransform vt = viewTransform(viewTransTXT, dataset, width, height);
			
			validate(dataset, agg, transfers);
			Aggregates<?> aggs = execute(dataset, agg, transfers, vt, width, height);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			AggregateSerailizer.serialize(aggs, baos, AggregateSerailizer.FORMAT.JSON);
			Response response = new Response(Status.OK, "avro/" + format, new String(baos.toByteArray(), "UTF-8"));
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			return new Response(Status.ACCEPTED, MIME_PLAINTEXT, "Error:" + e.toString());
		}
	}

	
	public Aggregates<?> execute(Glyphset<?> glyphs, Aggregator agg, List<Transfer<?,?>> trans, AffineTransform inverseView, int width, int height) {
		Renderer r;
		if (glyphs instanceof Glyphset.RandomAccess<?>) {
			AggregateReducer red = REDUCERS.get(agg.output());
			if (red == null) {throw new RuntimeException("Could not find aggregate reducer for type " + agg.output());}
			r = new ParallelGlyphs(red);
		} else {
			r = new ParallelSpatial();
		}
		Aggregates aggs = r.reduce(glyphs, agg, inverseView, width, height);
		for (Transfer t: trans) {
			aggs = r.transfer(aggs, t);
		}
		return aggs;
	}
	
	/**Ensure that the requested information is consistent.
	 * Essentially making sure that the input/output types all line up.
	 * 
	 * @param glyphs
	 * @param aggs
	 * @param trans
	 */
	public void validate(Glyphset<?> glyphs, Aggregator<?,?> aggs, List<Transfer<?,?>> trans) {
		Class<?> root = glyphs.valueType();
		if (!aggs.input().isAssignableFrom(root)) {
			throw new RuntimeException(String.format("Aggregator incompatible with glyphset (%s vs %s).", aggs.input(), root));
		}
		Class<?> prior = aggs.output();
		for (Transfer<?,?> t: trans) {
			if (!t.input().isAssignableFrom(prior)) {
				throw new RuntimeException(String.format("Transfer incompatible with prior (%s vs %s).", t.input(), prior));
			}
			prior = t.output();
		}
	}
	
	
	public AffineTransform viewTransform(String vtTXT, Glyphset<?> g, int width, int height) {
		if (vtTXT == null) {
			Rectangle2D bounds = g.bounds();
			return Util.zoomFit(bounds, width, height);
		} else {
			String[] parts = vtTXT.split(",");
			Double sx = Double.parseDouble(parts[0]);
			Double sy = Double.parseDouble(parts[1]);
			Double tx = Double.parseDouble(parts[2]);
			Double ty = Double.parseDouble(parts[3]);
			AffineTransform vt = new AffineTransform(sx,0,0,sy,tx,ty);
			return vt;
		}
	}
	
	public Glyphset<?> loadData(String id) {
		Glyphset<?> d = DATASETS.get(id.toUpperCase());
		if (d == null) {throw new RuntimeException("Dataset not found: " + id);}
		return d;
	}
	
	public Aggregator<?,?> getAgg(String aggID) {
		Aggregator<?,?> agg = AGGREGATORS.get(aggID); 
		if (agg==null) {throw new RuntimeException("Aggregator not found: " + aggID);}
		return agg;
	}
	
	public List<Transfer<?,?>> getTransfers(String transfers) {
		String[] trans = transfers.split(";");
		List<Transfer<?,?>> ts = new ArrayList<>(trans.length);
		for (String tID:trans) {
			Transfer<?,?> t = TRANSFERS.get(tID);
			if (t==null) {throw new RuntimeException("Transfer not found: " + tID);}
			ts.add(t);
		}
		return ts;
	}
	
	/**Get an item from the parameters dictionary. 
	 * If it is not present, return the default value.**/ 
	public String safeGet(Map<String,String> params, String key, String def) {
		String v = params.get(key);
		if (v != null) {return v;}
		return def;
	}
	

	/**Get an item from the parameters dictionary. 
	 * If it is not present, return an execption with the given error message.**/ 
	public String errorGet(Map<String,String> params, String key) {
		String v = params.get(key);
		if (v != null) {return v;}
		throw new RuntimeException(String.format("Entry not found '%s'", key));
	}

	
	public Collection<String> getAggregators() {return AGGREGATORS.keySet();}
	public Collection<String> getTransfers() {return TRANSFERS.keySet();}
	
	
	private static String get(String[] args, String flag, String def) {
		flag = flag.toUpperCase();
		for (int i=0; i<args.length; i++) {
			if (args[i].toUpperCase().equals(flag)) {return args[i+1];}
		}
		return def;
	}
	
	public static void main(String[] args) throws Exception {
		String host = get(args, "-host", "localhost");
		int port = Integer.parseInt(get(args, "-port", "8080"));
		ARServer server = new ARServer(host, port);
		
		server.start();
		
		System.out.printf("AR Server started on %s:%d", host, port);
		Object hold = new Object();

		synchronized(hold) {hold.wait();}
	}
}
