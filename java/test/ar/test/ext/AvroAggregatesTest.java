package ar.test.ext;

import static org.junit.Assert.*;

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
import ar.glyphsets.DynamicQuadTree;
import ar.renderers.ParallelRenderer;
import ar.rules.CategoricalCounts;
import ar.rules.Categories;
import ar.rules.Numbers;
import ar.selectors.TouchesPixel;
import ar.aggregates.implementations.RefFlatAggregates;
import ar.app.util.GlyphsetUtils;
import ar.ext.avro.AggregateSerializer;
import ar.ext.avro.AggregateSerializer.FORMAT;
import ar.ext.avro.Converters;
import ar.ext.avro.SchemaComposer;

public class AvroAggregatesTest {
	public static Aggregates<Integer> count;
	public static Aggregates<CategoricalCounts.RLE<Color>> rles;
	
	@BeforeClass
	public static void load() throws Exception {
		Glyphset<Rectangle2D, Color> glyphs = GlyphsetUtils.autoLoad(new File("../data/circlepoints.csv"), .1, DynamicQuadTree.<Rectangle2D, Color>make());
		Renderer r = new ParallelRenderer();
		AffineTransform vt = new AffineTransform(241.4615556310524, 
				0.0, 
				0.0, 
				241.4615556310524,
				238.49100176586487, 
				236.13546883394775);
		Selector<Rectangle2D> s = TouchesPixel.make(glyphs);
		count = r.aggregate(glyphs, s, new Numbers.Count<Object>(), vt, 500,500);
		rles = r.aggregate(glyphs, s, new Categories.RunLengthEncode<Color>(), vt, 500,500);
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
	
			assertEquals(ref.lowX(), res.lowX());
			assertEquals(ref.lowY(), res.lowY());
			assertEquals(ref.highX(), res.highX());
			assertEquals(ref.highY(), res.highY());
			assertEquals(ref.defaultValue(), res.defaultValue());
			
			for (int x=ref.lowX(); x<ref.highX(); x++) {
				for (int y=ref.lowY(); y<ref.highY(); y++)
					assertEquals(String.format("Value at (%d, %d)",x,y), ref.get(x, y), res.get(x, y));
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
		
			assertEquals(ref.lowX(), res.lowX());
			assertEquals(ref.lowY(), res.lowY());
			assertEquals(ref.highX(), res.highX());
			assertEquals(ref.highY(), res.highY());
			assertEquals(ref.defaultValue(), res.defaultValue());
			
			for (int x=ref.lowX(); x<ref.highX(); x++) {
				for (int y=ref.lowY(); y<ref.highY(); y++)
					assertEquals(String.format("Value at (%d, %d)",x,y), ref.get(x, y), res.get(x, y));
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
			
			
			assertEquals(ref.lowX(), n.get("xOffset").getIntValue());
			assertEquals(ref.lowY(), n.get("yOffset").getIntValue());
			assertEquals(ref.highX(), n.get("xBinCount").getIntValue());
			assertEquals(ref.highY(), n.get("yBinCount").getIntValue());
		}
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void RLERoundTrip() throws Exception {
		Aggregates<CategoricalCounts.RLE<Color>> ref = rles;
		
		File file =  new File("./testResults/rle.avro");
		try (OutputStream out = new FileOutputStream(file)) {
			Schema s = new SchemaComposer().addResource(AggregateSerializer.COC_SCHEMA).resolved();
			AggregateSerializer.serialize(ref, out, s, new Converters.FromRLE(s));
			Aggregates<CategoricalCounts.RLE<String>> res 
				= AggregateSerializer.deserialize(file, new Converters.ToRLE());
	
			assertEquals(ref.lowX(), res.lowX());
			assertEquals(ref.lowY(), res.lowY());
			assertEquals(ref.highX(), res.highX());
			assertEquals(ref.highY(), res.highY());
			assertEquals(ref.defaultValue(), res.defaultValue());
			
			for (int x=ref.lowX(); x<ref.highX(); x++) {
				for (int y=ref.lowY(); y<ref.highY(); y++) {
					CategoricalCounts.RLE<Color> rref = ref.get(x,y);
					CategoricalCounts.RLE<String> rres = res.get(x,y);
					assertEquals(String.format("Unequal key count at (%d, %d)", x,y), rref.size(), rres.size());
					
					for (int i=0; i<rres.size();i++) {
						//HACK: Must test string-equality because generic serialization is based on string category key
						assertEquals(String.format("Unequal key at %d",i), rref.key(i).toString(), rres.key(i));					
						assertEquals(String.format("Unequal count at %d",i), rref.count(i), rres.count(i));					
					}				
				}
			}
		}
	}
	
	@Test
	public void readOcculusTile() throws Exception {
		String filename = "../data/avroTiles/0/0/0.avro";
		assertTrue("Input file not found.", new File(filename).exists());
		
		Aggregates<Integer> res = AggregateSerializer.deserializeTile(filename, new Converters.ToCount(),0,0,255,255);
		
		assertEquals(0, res.lowX());
		assertEquals(0, res.lowY());
		assertEquals(255, res.highX());
		assertEquals(255, res.highY());
	}
}

