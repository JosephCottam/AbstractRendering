package ar.test.ext;

import static org.junit.Assert.*;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.net.Socket;

import org.junit.BeforeClass;
import org.junit.Test;

import ar.Aggregates;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.app.util.GlyphsetUtils;
import ar.ext.avro.AggregateSerializer;
import ar.ext.server.ARCombiner;
import ar.glyphsets.DynamicQuadTree;
import ar.renderers.ParallelRenderer;
import ar.rules.Numbers;
import ar.selectors.TouchesPixel;
import ar.util.Util;

public class CombinerTests {
	public static Aggregates<Integer> count;

	@BeforeClass
	public static void load() throws Exception {
		Glyphset<Rectangle2D, Object> glyphs = GlyphsetUtils.autoLoad(new File("../data/circlepoints.csv"), .1, DynamicQuadTree.<Rectangle2D, Object>make());
		Selector<Rectangle2D> selector = TouchesPixel.make(glyphs);
		Renderer r = new ParallelRenderer();
		count = r.aggregate(glyphs, selector, new Numbers.Count<>(), Util.zoomFit(glyphs.bounds(), 10, 10).createInverse(), 10,10);
	}
	
	@Test
	public void startStop() throws Exception{
		ARCombiner<Integer> c = new ARCombiner<Integer>("localhost", 8739, new ar.ext.avro.Converters.ToCount(), new Numbers.Count<>());
		c.start();
		if (!c.running()) {Thread.yield();}
		if (!c.running()) {Thread.sleep(1000);}
		assertTrue("Server not started", c.running());
		c.stop();
		assertTrue("Stop signal failed.", !c.running());
	}

	@Test
	public void recieve() throws Exception{
		ARCombiner<Integer> c = new ARCombiner<Integer>("localhost", 8739, new ar.ext.avro.Converters.ToCount(), new Numbers.Count<>());
		c.start();

		Aggregates<Integer> aggs = count;
		
		for (int i=1; i<=10; i++) {
			send(aggs, "localhost", 8739);
			Aggregates<Integer> recvd = c.combined();
			assertEquals("Combined count error.", i,c.count());	
			assertNotNull("Nothing received.", recvd);
			for (int x=aggs.lowX();x<aggs.highX();x++) {
				for (int y=aggs.lowY(); y<aggs.highY();y++) {
					assertEquals(String.format("Unexpected accumulated value at (%d,%d) in round %d",x,y,i), aggs.get(x, y)*i, (int)  recvd.get(x, y));
				}
			}
		}
		
		c.stop();
		assertTrue("Stop signal failed.", !c.running());
	}
	
	public void send(Aggregates<?> aggs, String host, int port) throws Exception {
		try (Socket s = new Socket(host, port)) {
			AggregateSerializer.serialize(aggs, s.getOutputStream());
			s.close();
			Thread.sleep(1000);
		}
	}

	
}
