package ar.ext.server;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ar.Aggregates;
import ar.Aggregator;
import ar.Renderer;
import ar.Selector;
import ar.Transfer;
import ar.ext.avro.AggregateSerializer;
import ar.ext.server.NanoHTTPD.Response.Status;
import ar.glyphsets.DynamicQuadTree;
import ar.glyphsets.MemMapList;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Indexed.Converter.TYPE;
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.glyphsets.implicitgeometry.Indexed.ToValue;
import ar.glyphsets.implicitgeometry.Valuer.Binary;
import ar.renderers.ParallelRenderer;
import ar.rules.Categories;
import ar.rules.Debug;
import ar.rules.General;
import ar.rules.Numbers;
import ar.rules.combinators.Chain;
import ar.rules.combinators.Seq;
import ar.selectors.TouchesPixel;
import ar.util.DelimitedReader;
import ar.util.Util;
import ar.Glyphset;



public class ARServer extends NanoHTTPD {
	private static Map<String, Transfer<?,?>> TRANSFERS = new HashMap<String,Transfer<?,?>>();
	private static Map<String, Aggregator<?,?>> AGGREGATORS = new HashMap<String,Aggregator<?,?>>();
	private static Map<String, Glyphset<?,?>> DATASETS = new HashMap<String, Glyphset<?,?>>();
	
	static {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Glyphset circlepoints = Util.load(
				DynamicQuadTree.<Rectangle2D, Integer>make(),
				new DelimitedReader(new File( "../data/circlepoints.csv"), 1, DelimitedReader.CSV),
				new Indexed.Converter(TYPE.X, TYPE.X, TYPE.DOUBLE, TYPE.DOUBLE, TYPE.INT),
				new Indexed.ToRect(1, 2, 3),
				new Indexed.ToValue(4, new Valuer.ToInt<Object>()));
		
		@SuppressWarnings({"unchecked", "rawtypes"})
		Glyphset boost = new MemMapList<>(
				new File("../data/MemVisScaled.hbin"),
				new Indexed.ToRect(.001, .001, true, 0, 1), 
				new ToValue(2, new Binary<Integer,Color>(0, Color.BLUE, Color.RED)));
		
		DATASETS.put("CIRCLEPOINTS", circlepoints);
		DATASETS.put("BOOST", boost);
		
		
		TRANSFERS.put("RedWhiteLinear", new Numbers.Interpolate<>(new Color(255,0,0,38), Color.red));
		TRANSFERS.put("RedWhiteLog", new Seq<Number, Double, Color>(
											new General.ValuerTransfer<>(new MathValuers.Log<>(10, false, true), 0d), 
											new Numbers.Interpolate<Double>(new Color(255,0,0,38), Color.red, Color.white)));
		TRANSFERS.put("Alpha10", new Numbers.FixedInterpolate(Color.white, Color.red, 0, 25.5));
		TRANSFERS.put("AlphaMin", new Numbers.FixedInterpolate(Color.white, Color.red, 0, 255));
		TRANSFERS.put("Present", new General.Present<Integer,Color>(Color.red, Color.white));
		TRANSFERS.put("90Percent", new Categories.KeyPercent<Color>(.9, Color.blue, Color.white, Color.blue, Color.red));
		TRANSFERS.put("25Percent", new Categories.KeyPercent<Color>(.25, Color.blue, Color.white, Color.blue, Color.red));
		TRANSFERS.put("Echo", new General.Echo<Color>(Util.CLEAR));
		TRANSFERS.put("HDAlpha", new Categories.HighAlpha(Color.white, .1, false));
		TRANSFERS.put("HDAlphaLog", new Categories.HighAlpha(Color.white, .1, true));
		TRANSFERS.put("Gradient", new Debug.Gradient());
						
		AGGREGATORS.put("Blue",new General.Const<>(Color.BLUE));
		AGGREGATORS.put("First", new Categories.First());
		AGGREGATORS.put("Last", new General.Last<>(null));
		AGGREGATORS.put("Count", new Numbers.Count<Object>());
		AGGREGATORS.put("RLEColor", new Categories.RunLengthEncode<Color>());
		AGGREGATORS.put("RLEUnsortColor", new Categories.CountCategories<Color>());
	}
	
	public ARServer(String hostname) {this(hostname, 8739);}
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
			
			Glyphset<?,?> dataset = loadData(datasetID);
			Aggregator<?,?> agg = getAgg(aggID);
			List<Transfer<?,?>> transfers = getTransfers(transferIDS);
			AffineTransform vt = viewTransform(viewTransTXT, dataset, width, height);
			
			Aggregates<?> aggs = execute(dataset, agg, transfers, vt, width, height);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			AggregateSerializer.serialize(aggs, baos, AggregateSerializer.FORMAT.JSON);
			Response response = new Response(Status.OK, "avro/" + format, new String(baos.toByteArray(), "UTF-8"));
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			return new Response(Status.ACCEPTED, MIME_PLAINTEXT, "Error:" + e.toString());
		}
	}

	
	/**Execute the passed aggregator and list of transfers.
	 * This is inherently not statically type-safe, so it may produce type errors at runtime.  
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" }) 
	public Aggregates<?> execute(Glyphset<?,?> glyphs, Aggregator agg, List<Transfer<?,?>> transfers, AffineTransform view, int width, int height) {
		Renderer r = new ParallelRenderer();
		
		Selector s = TouchesPixel.make(glyphs);
		Aggregates aggs = r.aggregate(glyphs, s, agg, view, width, height);
		Transfer transfer = new Chain(transfers.toArray(new Transfer[transfers.size()]));
		Transfer.Specialized ts = transfer.specialize(aggs);
		Aggregates<?> rslt = r.transfer(aggs, ts);
		return rslt;
	}
	
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
	
	public Glyphset<?,?> loadData(String id) {
		Glyphset<?,?> d = DATASETS.get(id.toUpperCase());
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
		List<Transfer<?,?>> ts = new ArrayList<Transfer<?,?>>(trans.length);
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

	
	/**@return Collection of known aggregator names**/
	public Collection<String> getAggregators() {return AGGREGATORS.keySet();}
	
	/**@return collection of known transfer names**/
	public Collection<String> getTransfers() {return TRANSFERS.keySet();}
	
	
	private static String arg(String[] args, String flag, String def) {
		flag = flag.toUpperCase();
		for (int i=0; i<args.length; i++) {
			if (args[i].toUpperCase().equals(flag)) {return args[i+1];}
		}
		return def;
	}
	
	public static void main(String[] args) throws Exception {
		String host = arg(args, "-host", "localhost");
		int port = Integer.parseInt(arg(args, "-port", "8080"));
		ARServer server = new ARServer(host, port);
		
		server.start();
		
		System.out.printf("AR Server started on %s:%d", host, port);
		Object hold = new Object();

		synchronized(hold) {hold.wait();}
	}
}
