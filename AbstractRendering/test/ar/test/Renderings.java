package ar.test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;

import org.junit.Test;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;
import ar.app.util.GlyphsetUtils;
import ar.glyphsets.DynamicQuadTree;
import ar.glyphsets.MemMapList;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.renderers.*;
import ar.rules.Numbers;
import ar.util.Util;


/**Test all glyphset type/rendering pairs...even the silly/slow ones.
 * WARNING:  Some of these configurations are a bad idea, but we test them anyway.  They take a long time for their image size.
 * @author jcottam
 *
 */
public class Renderings {
	private final int width = 100;
	private final int height = 100;

	/**Check image equality**/ 
	public static void assertImageEquals(String msg, BufferedImage ref, BufferedImage res) {
		assertThat(res.getWidth(), is(ref.getWidth()));
		assertThat(res.getHeight(), is(ref.getHeight()));
		for (int x = 0; x<res.getWidth(); x++) {
			for (int y=0; y<res.getHeight(); y++) {
				assertThat(String.format(msg + "(%d,%d)", x, y), res.getRGB(x, y), is(ref.getRGB(x, y)));
			}
		}
	}
	
	public <V,A> BufferedImage image(Renderer r, Glyphset<V> g, Aggregator<V,A> agg, Transfer<? super A,Color> t) throws Exception {
		AffineTransform ivt = Util.zoomFit(g.bounds(), width, height);
		Aggregates<A> ser_aggs = r.aggregate(g, agg, ivt.createInverse(), width, height);
		t.specialize(ser_aggs);
		Aggregates<Color> ser_trans = r.transfer(ser_aggs, t);
		BufferedImage img = Util.asImage(ser_trans, width, height, Color.white);
		return img;
	}
	
	public <V,A> void testWith(String test, Glyphset<V> glyphs, Aggregator<V,A> agg, Transfer<? super A,Color> t)  throws Exception {
		BufferedImage ref_img =image(new SerialSpatial(), glyphs, agg, t);
		Util.writeImage(ref_img, new File(String.format("./testResults/%s/ref.png", test)));
		
		BufferedImage ser_img = image(new SerialSpatial(), glyphs, agg, t);
		Util.writeImage(ser_img, new File(String.format("./testResults/%s/ser.png", test)));
		assertImageEquals("Serial", ref_img, ser_img);
		
		BufferedImage ps_img = image(new ParallelSpatial(), glyphs, agg, t);
		Util.writeImage(ps_img, new File(String.format("./testResults/%s/ps.png", test)));
		assertImageEquals("Parallel spatial", ref_img, ps_img);
		
		BufferedImage pg_img = image(new ParallelGlyphs(), glyphs, agg, t);
		Util.writeImage(pg_img, new File(String.format("./testResults/%s/pg.png", test)));
		assertImageEquals("Parallel glyphs", ref_img, ps_img);
	}
	

	@Test
	public void CheckerboardQuad() throws Exception {
		Glyphset<Object> glyphs = GlyphsetUtils.autoLoad(new File("../data/checkerboard.csv"), 1, DynamicQuadTree.make());
		Aggregator<Object, Integer> agg = new Numbers.Count<>();
		Transfer<Number, Color> t = new Numbers.FixedInterpolate(Color.white, Color.red, 0, 25.5);
		testWith("checker_quad", glyphs, agg, t);
	}


	@Test
	public void CirclepointsQuad() throws Exception {
		Glyphset<Object> glyphs = GlyphsetUtils.autoLoad(new File("../data/circlepoints.csv"), 1, DynamicQuadTree.make());
		Aggregator<Object, Integer> agg = new Numbers.Count<>();
		Transfer<Number, Color> t = new Numbers.FixedInterpolate(Color.white, Color.red, 0, 25.5);
		testWith("circle_quad", glyphs, agg, t);
	}

	
	@Test
	public void CirclepointsMemMap() throws Exception {
		Glyphset<Object> glyphs = GlyphsetUtils.autoLoad(
				new File("../data/circlepoints.hbin"), 
				.001, 
				new MemMapList<Object>(
						null, 
						new Indexed.ToRect(.01, 0, 1), 
						new Valuer.Constant<Indexed, Object>(1)));
		
		Aggregator<Object, Integer> agg = new Numbers.Count<>();
		Transfer<Number, Color> t = new Numbers.FixedInterpolate(Color.white, Color.red, 0, 25.5);
		testWith("checker_mem", glyphs, agg, t);
	}

	
}
