package ar.ext.server;

import java.awt.Color;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ar.Aggregator;
import ar.Transfer;
import ar.app.util.Wrapped;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;
import ar.glyphsets.implicitgeometry.Indexed.ToValue;
import ar.glyphsets.implicitgeometry.Valuer.Binary;
import ar.util.GlyphsetLoader;
import ar.Glyphset;


public class ARServer extends NanoHTTPD {
	private static Map<String, Transfer> TRANSFERS = load(Transfer.class, WrappedTransfer.class);
	private static Map<String, Aggregator> AGGREGATORS = load(Aggregator.class, WrappedAggregator.class);
	private static Map<String, Glyphset> DATASETS = new HashMap();
	
	static {
		DATASETS.put("CIRCLEPOINTS", GlyphsetLoader.load("Scatterplot", "../data/circlepoints.csv", .1));
		DATASETS.put("BOOST", GlyphsetLoader.memMap("BGL Memory", "../data/MemVisScaledB.hbin", .001, .001, true, new ToValue<>(2, new Binary<Integer,Color>(0, Color.BLUE, Color.RED)), 1, "ddi"));
	}
	
	private static <T> Map<String, T> load(Class<T> type, Class<?> index) {
		Map<String, T> instances = new HashMap<>();
		Class<?>[] items = index.getClasses();

		for (Class<?> item: items) {
			if (!Wrapped.class.isAssignableFrom(item)) {continue;}
			try {
				Constructor<?> c = item.getConstructor();
				Object t = c.newInstance();
				instances.put(item.getSimpleName(), ((T) ((Wrapped) t).op()));
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
			String typeID = safeGet(parms, "format", "json");
			
			Glyphset<?> dataset = loadData(datasetID);
			Aggregator<?,?> agg = getAgg(aggID);
			List<Transfer<?,?>> transfers = getTransfers(transferIDS);
			
			validate(dataset, agg, transfers);
			
			
			// TODO Auto-generated method stub
			String response = String.format("DS: %s\nAgg: %s\nTrans: %s\nType:%s", datasetID, aggID, transferIDS,typeID);		
			return new Response(response);
		} catch (Exception e) {
			return new Response("Error:" + e.toString());
		}
	}

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
}
