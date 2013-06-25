package ar.test;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.Arrays;

import org.apache.avro.Schema;
import org.junit.Test;

import ar.Aggregates;
import ar.Glyphset;
import ar.Renderer;
import ar.glyphsets.DynamicQuadTree;
import ar.renderers.ParallelSpatial;
import ar.rules.Aggregators;
import ar.rules.Aggregators.RLE;
import ar.util.GlyphsetLoader;

import ar.ext.avro.AggregateSerailizer;
import ar.ext.avro.Converters;
import ar.ext.avro.SchemaComposer;

public class AvroAggregates {
	@Test
	public void countsRoundTrip() throws Exception {
		Glyphset<Object> glyphs = GlyphsetLoader.autoLoad(new File("../data/circlepoints.csv"), .1, DynamicQuadTree.make(Object.class));
		Renderer r = new ParallelSpatial();
		AffineTransform ivt = new AffineTransform(241.4615556310524, 
				0.0, 
				0.0, 
				241.4615556310524,
				238.49100176586487, 
				236.13546883394775).createInverse();
		Aggregates<Integer> ref = r.reduce(glyphs, new Aggregators.Count(), ivt, 500,500);
		
		String filename ="./testResults/counts.avro";
		Schema s = new SchemaComposer().addResource("ar/ext/avro/counts.avsc").resolved();
		AggregateSerailizer.serialize(ref, filename, s, new Converters.FromCount(s));
		Aggregates<Integer> res = AggregateSerailizer.deserialize(filename, new Converters.ToCount());

		assertEquals(ref.lowX(), res.lowX());
		assertEquals(ref.lowY(), res.lowY());
		assertEquals(ref.highX(), res.highX());
		assertEquals(ref.highY(), res.highY());
		assertEquals(ref.defaultValue(), res.defaultValue());
		
		for (int x=ref.lowX(); x<ref.highX(); x++) {
			for (int y=ref.lowY(); y<ref.highY(); y++)
				assertEquals(String.format("Value at (%d, %d)",x,y), ref.at(x, y), res.at(x, y));
		}

	}
	
	
	
	@Test
	public void RLERoundTrip() throws Exception {
		Glyphset<Color> glyphs = (Glyphset<Color>) GlyphsetLoader.autoLoad(new File("../data/circlepoints.csv"), .1, DynamicQuadTree.make(Color.class));
		Renderer r = new ParallelSpatial();
		AffineTransform ivt = new AffineTransform(241.4615556310524, 
				0.0, 
				0.0, 
				241.4615556310524,
				238.49100176586487, 
				236.13546883394775).createInverse();
		Aggregates<RLE> ref = r.reduce(glyphs, new Aggregators.RLEColor(true, false), ivt, 500,500);
		
		String filename ="./testResults/rle.avro";
		Schema s = new SchemaComposer().addResource("ar/ext/avro/rle.avsc").resolved();
		AggregateSerailizer.serialize(ref, filename, s, new Converters.FromRLE(s));
		Aggregates<RLE> res = AggregateSerailizer.deserialize(filename, new Converters.ToRLE());

		assertEquals(ref.lowX(), res.lowX());
		assertEquals(ref.lowY(), res.lowY());
		assertEquals(ref.highX(), res.highX());
		assertEquals(ref.highY(), res.highY());
		assertEquals(ref.defaultValue(), res.defaultValue());
		
		for (int x=ref.lowX(); x<ref.highX(); x++) {
			for (int y=ref.lowY(); y<ref.highY(); y++) {
				RLE rref = ref.at(x,y);
				RLE rres = res.at(x,y);
				assertEquals(String.format("Unequal key count at (%d, %d)", x,y), rres.size(), rref.size());
				assertEquals("Unequal counts.", rref.counts,  rres.counts);
				
				//HACK: Must test string-equality because generic serialization is based on string category key
				assertEquals(Arrays.deepToString(rres.keys.toArray()), Arrays.deepToString(rref.keys.toArray()));
			}
		}

	}
}

