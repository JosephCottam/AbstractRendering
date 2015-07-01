package ar.test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.Test;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Selector;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.app.util.GlyphsetUtils;
import ar.glyphsets.GlyphList;
import ar.glyphsets.MemMapList;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.renderers.*;
import ar.rules.Numbers;
import ar.selectors.TouchesPixel;
import ar.util.Util;


/**Test all glyphset type/rendering pairs...even the silly/slow ones.
 * WARNING:  Some of these configurations are a bad idea, but we test them anyway.  They take a long time for their image size.
 * @author jcottam
 *
 */
public class Renderings {

	/**Check image equality**/ 
	public static void assertImageEquals(String msg, BufferedImage ref, BufferedImage res) {		
		for (int x = 0; x<res.getWidth() && x < ref.getWidth(); x++) {
			for (int y=0; y<res.getHeight() && y < ref.getWidth(); y++) {
				assertThat(String.format(msg + " (%d,%d)", x, y), new Color(res.getRGB(x, y),true), is(new Color(ref.getRGB(x, y) ,true)));
			}
		}
	}

	/**Create an image.**/
	public <G,V,A> BufferedImage image(Renderer r, Glyphset<G,V> g, Aggregator<V,A> agg, Transfer<? super A,Color> t) throws Exception {
		final int WIDTH = 100;
		final int HEIGHT = 100;
		
		AffineTransform vt = Util.zoomFit(g.bounds(), WIDTH, HEIGHT);
		Selector<G> selector = TouchesPixel.make(g);
		Aggregates<A> aggs = r.aggregate(g, selector, agg, vt);
		Transfer.Specialized<? super A,Color> t2 = t.specialize(aggs);
		Aggregates<Color> imgAggs = r.transfer(aggs, t2);
		BufferedImage img = AggregateUtils.asImage(imgAggs, WIDTH, HEIGHT, Color.white);
		return img;
	}
	
	public <G,V,A> void testWith(String test, Glyphset<G,V> glyphs, Aggregator<V,A> agg, Transfer<? super A,Color> t)  throws Exception {
		Renderer r = new SerialRenderer();
		BufferedImage ref_img = image(r, glyphs, agg, t);
		Util.writeImage(ref_img, new File(String.format("./testResults/%s/ref.png", test)));		
		
		r = new SerialRenderer();
		BufferedImage ser_img = image(r, glyphs, agg, t);
		Util.writeImage(ser_img, new File(String.format("./testResults/%s/Serial.png", test)));
		
		r = new ForkJoinRenderer();
		BufferedImage pg_img = image(r, glyphs, agg, t);
		Util.writeImage(pg_img, new File(String.format("./testResults/%s/ForkJoin.png", test)));
		
		r = new ThreadpoolRenderer();
		BufferedImage tp_img = image(r, glyphs, agg, t);
		Util.writeImage(tp_img, new File(String.format("./testResults/%s/Threadpool.png", test)));

		assertImageEquals("Serial", ref_img, ser_img);
		assertImageEquals("Fork/Join", ref_img, pg_img);
		assertImageEquals("Threadpool", ref_img, tp_img);
	}
	

	@Test
	public void CheckerboardQuad() throws Exception {
		Glyphset<Rectangle2D, Object> glyphs = GlyphsetUtils.autoLoad(new File("../data/checkerboard.csv"), 1, new GlyphList<>());
		Aggregator<Object, Integer> agg = new Numbers.Count<>();
		Transfer<Number, Color> t = new Numbers.FixedInterpolate<>(Color.white, Color.red, 0, 25.5);
		testWith("checker_quad", glyphs, agg, t);
	}


	@Test
	public void CirclepointsQuad() throws Exception {
		Glyphset<Rectangle2D, Object> glyphs = GlyphsetUtils.autoLoad(new File("../data/circlepoints.csv"), 1, new GlyphList<>());
		Aggregator<Object, Integer> agg = new Numbers.Count<>();
		Transfer<Number, Color> t = new Numbers.FixedInterpolate<>(Color.white, Color.red, 0, 25.5);
		testWith("circle_quad", glyphs, agg, t);
	}

	
	@Test
	public void CirclepointsMemMap() throws Exception {
		Glyphset<Point2D, Object> glyphs = GlyphsetUtils.autoLoad(
				new File("../data/circlepoints.hbin"), 
				.001, 
				new MemMapList<Point2D, Object>(
						null, 
						new Indexed.ToPoint(false, 0, 1), 
						new Valuer.Constant<Indexed, Object>(1)));
		
		Aggregator<Object, Integer> agg = new Numbers.Count<>();
		Transfer<Number, Color> t = new Numbers.FixedInterpolate<>(Color.white, Color.red, 0, 25.5);
		testWith("circle_mem", glyphs, agg, t);
	}

	
}
