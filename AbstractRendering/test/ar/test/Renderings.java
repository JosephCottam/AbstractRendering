package ar.test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.awt.Color;
import java.awt.Image;
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
	
	public BufferedImage image(Renderer r, Glyphset g, Aggregator agg, Transfer t) throws Exception {
		AffineTransform ivt = Util.zoomFit(g.bounds(), width, height);
		Aggregates ser_aggs = r.aggregate(g, agg, ivt.createInverse(), width, height);
		Aggregates ser_trans = r.transfer(ser_aggs, t);
		BufferedImage img = Util.asImage(ser_trans, width, height, Color.white);
		return img;
	}
	
	public void testWith(Glyphset glyphs, Aggregator agg, Transfer t)  throws Exception {
		BufferedImage ref_img =image(new SerialSpatial(), glyphs, agg, t);
		Util.writeImage(ref_img, new File("./testResults/ref.png"));
		
		BufferedImage ser_img = image(new SerialSpatial(), glyphs, agg, t);
		Util.writeImage(ser_img, new File("./testResults/ser.png"));
		assertImageEquals("Serial", ref_img, ser_img);
		
		BufferedImage ps_img = image(new ParallelSpatial(), glyphs, agg, t);
		Util.writeImage(ps_img, new File("./testResults/pg.png"));
		assertImageEquals("Parallel spatial", ref_img, ps_img);
	}
	

	@Test
	public void CheckerboardQuad() throws Exception {
		Glyphset glyphs = GlyphsetUtils.autoLoad(new File("../data/checkerboard.csv"), 1, DynamicQuadTree.make());
		Aggregator agg = new Numbers.Count<>();
		Transfer t = new Numbers.FixedAlpha(Color.white, Color.red, 0, 25.5);
		testWith(glyphs, agg, t);
	}


	@Test
	public void CirclepointsQuad() throws Exception {
		Glyphset glyphs = GlyphsetUtils.autoLoad(new File("../data/circlepoints.csv"), 1, DynamicQuadTree.make());
		Aggregator agg = new Numbers.Count<>();
		Transfer t = new Numbers.FixedAlpha(Color.white, Color.red, 0, 25.5);
		testWith(glyphs, agg, t);
	}

	
	@Test
	public void CirclepointsMemMap() throws Exception {
		Glyphset glyphs = GlyphsetUtils.autoLoad(new File("../data/circlepoints.hbin"), .001, new MemMapList(null, new Indexed.ToRect(.01, 0, 1), new Valuer.Constant(1)));
		Aggregator agg = new Numbers.Count<>();
		Transfer t = new Numbers.FixedAlpha(Color.white, Color.red, 0, 25.5);
		testWith(glyphs, agg, t);
	}

	@Test 
	public void KnownFailing() throws Exception {
		Glyphset glyphs = GlyphsetUtils.autoLoad(new File("../data/checkerboard.csv"), 1, DynamicQuadTree.make());
		Aggregator agg = new Numbers.Count<>();
		Transfer t = new Numbers.FixedAlpha(Color.white, Color.red, 0, 25.5);
		BufferedImage ref_img =image(new SerialSpatial(), glyphs, agg, t);

		BufferedImage pp_img = image(new ParallelGlyphs(), glyphs, agg, t);
		Util.writeImage(pp_img, new File("./testResults/pp.png"));
		assertImageEquals("Known-bad configuration: Parallel glyph", ref_img, pp_img);
		
		
//		BufferedImage pp_img = image(new ParallelGlyphs(), glyphs, agg, t);
//		Util.writeImage(pp_img, new File("./testResults/pp.png"));
//		assertImageEquals("Parallel glyph", ref_img, pp_img);

	}
}
