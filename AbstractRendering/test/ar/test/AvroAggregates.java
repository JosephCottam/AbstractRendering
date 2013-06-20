package ar.test;

import static org.junit.Assert.*;

import java.awt.geom.AffineTransform;
import java.io.File;

import org.junit.Test;

import ar.Aggregates;
import ar.Glyphset;
import ar.Renderer;
import ar.glyphsets.DynamicQuadTree;
import ar.renderers.ParallelSpatial;
import ar.rules.Aggregators;
import ar.util.CSVtoGlyphSet;

import ar.ext.avro.AggregateSerailizer;
import ar.ext.avro.CountConverter;

public class AvroAggregates {
	@Test
	public void countsRoundTrip() throws Exception {
		Glyphset<Object> glyphs = CSVtoGlyphSet.autoLoad(new File("../data/circlepoints.csv"), .1, DynamicQuadTree.make());
		Renderer<Object, Integer> r = new ParallelSpatial<>();
		AffineTransform ivt = AffineTransform.getScaleInstance(2/100, 2/100);
		Aggregates<Integer> ref = r.reduce(glyphs, new Aggregators.Count(), ivt, 100,100);
		
		String filename = "./testResults/counts.avro";
		AggregateSerailizer.serializeCounts(ref, filename);
		Aggregates<Integer> res = AggregateSerailizer.deserialize(filename, new CountConverter());

		assertEquals(ref.lowX(), res.lowX());
		assertEquals(ref.lowY(), res.lowY());
		assertEquals(ref.highX(), res.highX());
		assertEquals(ref.highY(), res.highY());
		assertEquals(ref.defaultValue(), res.defaultValue());

	}
	
}

