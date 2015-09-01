package ar.test.ext.avro;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.avro.Schema;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import ar.Aggregates;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.renderers.ForkJoinRenderer;
import ar.rules.CategoricalCounts;
import ar.rules.Categories;
import ar.rules.Numbers;
import ar.selectors.TouchesPixel;
import ar.util.Util;
import ar.aggregates.implementations.ConstantAggregates;
import ar.aggregates.implementations.RefFlatAggregates;
import ar.app.util.GlyphsetUtils;
import ar.ext.avro.AggregateSerializer;
import ar.ext.avro.AggregateSerializer.FORMAT;
import ar.ext.avro.Converters;
import ar.ext.avro.SchemaComposer;
import ar.glyphsets.GlyphList;

public class AvroAggregatesTest {
	public static Aggregates<Integer> count;
	public static Aggregates<CategoricalCounts<Color>> cocs;
	
	@BeforeClass
	public static void load() throws Exception {
		Glyphset<Rectangle2D, Color> glyphs = GlyphsetUtils.autoLoad(new File("../data/circlepoints.csv"), .1, new GlyphList<>());
		Renderer r = new ForkJoinRenderer();
		AffineTransform vt = new AffineTransform(241.4615556310524, 
				0.0, 
				0.0, 
				241.4615556310524,
				238.49100176586487, 
				236.13546883394775);
		Selector<Rectangle2D> s = TouchesPixel.make(glyphs);
		count = r.aggregate(glyphs, s, new Numbers.Count<Object>(), vt);
		cocs = r.aggregate(glyphs, s, new Categories.CountCategories<Color>(Util.COLOR_SORTER), vt);
	}
	
	@Test
	public void inOrderEqOutOrder() throws Exception {
		Aggregates<Integer> ref = new RefFlatAggregates<Integer>(2,2,-1);
		ref.set(0, 0, 11);
		ref.set(0, 1, 12);
		ref.set(1, 0, 21);
		ref.set(1, 1, 22);
		
		File file = new File("./testResults/counts.avro");
		Schema s = new SchemaComposer().addResource("ar/ext/avro/count.avsc").resolved();
		try (OutputStream out = new FileOutputStream(file)) {
			AggregateSerializer.serialize(ref, out, s, new Converters.FromCount(s));
			Aggregates<Integer> res = AggregateSerializer.deserialize(file, new Converters.ToCount());
	
			assertThat(ref.lowX(), is(res.lowX()));
			assertThat(ref.lowY(), is(res.lowY()));
			assertThat(ref.highX(), is(res.highX()));
			assertThat(ref.highY(), is(res.highY()));
			assertThat(ref.defaultValue(), is(res.defaultValue()));
			
			for (int x=ref.lowX(); x<ref.highX(); x++) {
				for (int y=ref.lowY(); y<ref.highY(); y++)
					assertThat(String.format("Value at (%d, %d)",x,y), ref.get(x, y), is(res.get(x, y)));
			}
		}
	}
	
	
	@Test
	public void countsRoundTrip() throws Exception {
		Aggregates<Integer> ref = count;
		File file = new File("./testResults/counts.avro");
		Schema s = new SchemaComposer().addResource("ar/ext/avro/count.avsc").resolved();
		
		try (OutputStream out = new FileOutputStream(file)) {
			AggregateSerializer.serialize(ref, out, s, new Converters.FromCount(s));
			Aggregates<Integer> res = AggregateSerializer.deserialize(file, new Converters.ToCount());
		
			assertThat(ref.lowX(), is(res.lowX()));
			assertThat(ref.lowY(), is(res.lowY()));
			assertThat(ref.highX(), is(res.highX()));
			assertThat(ref.highY(), is(res.highY()));
			assertThat(ref.defaultValue(), is(res.defaultValue()));
			
			for (int x=ref.lowX(); x<ref.highX(); x++) {
				for (int y=ref.lowY(); y<ref.highY(); y++)
					assertThat(String.format("Value at (%d, %d)",x,y), ref.get(x, y), is(res.get(x, y)));
			}
		}
	}
	
	@Test
	public void CountToJSON() throws Exception {
		Aggregates<Integer> ref = count;
		Schema s = new SchemaComposer().addResource("ar/ext/avro/count.avsc").resolved();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		AggregateSerializer.serialize(ref, baos, s, FORMAT.JSON, new Converters.FromCount(s));
		String output = new String(baos.toByteArray(), "UTF-8");
		try (JsonParser p = new JsonFactory().createJsonParser(output)) {		
			ObjectMapper mapper = new ObjectMapper();
			JsonNode n = mapper.readTree(p);
			
			int xOffset = n.get("xOffset").getIntValue();
			int yOffset = n.get("yOffset").getIntValue();
			int highX = n.get("xBinCount").getIntValue() + xOffset;
			int highY = n.get("yBinCount").getIntValue() + yOffset;
			
			assertThat(ref.lowX(), is(xOffset));
			assertThat(ref.lowY(), is(yOffset));
			assertThat(ref.highX(), is(highX));
			assertThat(ref.highY(), is(highY));
		}
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void CoCRoundTrip() throws Exception {
		Aggregates<CategoricalCounts<Color>> ref = cocs;
		
		File file =  new File("./testResults/coc.avro");
		try (OutputStream out = new FileOutputStream(file)) {
			Schema s = new SchemaComposer().addResource(AggregateSerializer.COC_SCHEMA).resolved();
			AggregateSerializer.serialize(ref, out, s, new Converters.FromCoC(s));
			Aggregates<CategoricalCounts<String>> res 
				= AggregateSerializer.deserialize(file, new Converters.ToCoC());
	
			assertThat(ref.lowX(), is(res.lowX()));
			assertThat(ref.lowY(), is(res.lowY()));
			assertThat(ref.highX(), is(res.highX()));
			assertThat(ref.highY(), is(res.highY()));
			assertThat(ref.defaultValue(), is(res.defaultValue()));
			
			for (int x=ref.lowX(); x<ref.highX(); x++) {
				for (int y=ref.lowY(); y<ref.highY(); y++) {
					CategoricalCounts<Color> rref = ref.get(x,y);
					CategoricalCounts<String> rres = res.get(x,y);
					assertThat(String.format("Unequal key count at (%d, %d)", x,y), rref.size(), is(rres.size()));
					
					for (int i=0; i<rres.size();i++) {
						//HACK: Must test string-equality because generic serialization is based on string category key
						assertThat(String.format("Unequal key at %d",i), rref.key(i).toString(), is(rres.key(i)));					
						assertThat(String.format("Unequal count at %d",i), rref.count(i), is(rres.count(i)));					
					}				
				}
			}
		}
	}
	

	@Test
	public void ConstRoundTrip() throws Exception {
		Aggregates<Integer> ref = new ConstantAggregates<>(34, 0,0,10000,10000);
		
		File file =  new File("./testResults/const.avro");
		try (OutputStream out = new FileOutputStream(file)) {
			Schema s = new SchemaComposer().addResource(AggregateSerializer.COUNTS_SCHEMA).resolved();
			AggregateSerializer.serialize(ref, out, s, new Converters.FromCount(s));
			Aggregates<Integer> res = AggregateSerializer.deserialize(file, new Converters.ToCount());
	
			assertThat("Constant aggregates did not deserialize to constant", res, instanceOf(ConstantAggregates.class));
			assertThat(ref.lowX(), is(res.lowX()));
			assertThat(ref.lowY(), is(res.lowY()));
			assertThat(ref.highX(), is(res.highX()));
			assertThat(ref.highY(), is(res.highY()));
			assertThat(ref.defaultValue(), is(res.defaultValue()));
		}
	}
}

