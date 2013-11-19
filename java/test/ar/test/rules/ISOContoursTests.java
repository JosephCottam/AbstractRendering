package ar.test.rules;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ar.Aggregates;
import ar.Renderer;
import ar.aggregates.FlatAggregates;
import ar.renderers.ParallelRenderer;
import ar.rules.ISOContours;
import ar.rules.ISOContours.MC_TYPE;


public class ISOContoursTests {
	private static final Renderer RENDERER = new ParallelRenderer();
	
	public Aggregates<Boolean> makeMarchingSquareCase(boolean zz, boolean oz, boolean zo, boolean oo) {
		Aggregates<Boolean> source = new FlatAggregates<>(0,0,2,2,false);
		source.set(0, 0, zz);
		source.set(1, 0, oz);
		source.set(0, 1, zo);
		source.set(1, 1, oo);
		return source;
	}
	
	@Test
	public void ClassifyIndividuals() {
		ISOContours.MCClassifier classifier = new ISOContours.MCClassifier();
		
		assertThat("Emtpy",       classifier.at(1, 1, makeMarchingSquareCase(false,false,false,false)), is(MC_TYPE.empty));
		assertThat("surround",     classifier.at(1, 1, makeMarchingSquareCase(true,true,true,true)), is(MC_TYPE.surround));
		assertThat("diag two", classifier.at(1, 1, makeMarchingSquareCase(true,false,false,true)), is(MC_TYPE.diag_two));
		assertThat("diag one", classifier.at(1, 1, makeMarchingSquareCase(false,true,true,false)), is(MC_TYPE.diag_one));

		assertThat(classifier.at(1, 1, makeMarchingSquareCase(false,false,true,true)), is(MC_TYPE.ui_in));
		assertThat(classifier.at(1, 1, makeMarchingSquareCase(true,true,false,false)), is(MC_TYPE.di_in));
		assertThat(classifier.at(1, 1, makeMarchingSquareCase(true,false,true,false)), is(MC_TYPE.l_in));
		assertThat(classifier.at(1, 1, makeMarchingSquareCase(false,true,false,true)), is(MC_TYPE.r_in));
		
		assertThat(classifier.at(1, 1, makeMarchingSquareCase(false,false,false,true)), is(MC_TYPE.ui_r_in));
		assertThat(classifier.at(1, 1, makeMarchingSquareCase(false,false,true,false)), is(MC_TYPE.ui_l_in));

		assertThat(classifier.at(1, 1, makeMarchingSquareCase(false,true,false,false)), is(MC_TYPE.di_r_in));
		assertThat(classifier.at(1, 1, makeMarchingSquareCase(true,false,false,false)), is(MC_TYPE.di_l_in));
		assertThat(classifier.at(1, 1, makeMarchingSquareCase(true,false,true,true)), is(MC_TYPE.di_r_out));
		assertThat(classifier.at(1, 1, makeMarchingSquareCase(false,true,true,true)), is(MC_TYPE.di_l_out));
		assertThat(classifier.at(1, 1, makeMarchingSquareCase(true,true,false,true)), is(MC_TYPE.ui_l_out));
		assertThat(classifier.at(1, 1, makeMarchingSquareCase(true,true,true,false)), is(MC_TYPE.ui_r_out));
	}
	
	@Test
	public void SaddleContour() {
		int threshold = 3;
		Aggregates<Integer> source = new FlatAggregates<>(0,0,4,4,0);
		source.set(1,1,5);
		source.set(2,2,5);
		
		ISOContours.Single.Specialized<Integer> contour = new ISOContours.Single.Specialized<Integer>(RENDERER, threshold, true, source);
		GeneralPath p = (GeneralPath) contour.contours().get(0).shape();

		GeneralPath p2 = (GeneralPath) p.clone();
		p2.closePath();
		assertEquals("Unequal bounding after closing.", p.getBounds2D(), p2.getBounds2D());
		
		assertFalse(p.contains(new Point(0,0)));
		assertTrue(p.contains(new Point(2,2)));
		assertFalse(p.contains(new Point(2,3)));
		assertFalse(p.contains(new Point(3,2)));
		assertFalse(p.contains(new Point(3,3)));
	}
	
	@Test
	public void SimpleContours() {
		int threshold = 3;
		Aggregates<Integer> source = new FlatAggregates<>(0,0,10,10,0); 
		for (int x=source.lowX()+1; x<source.highX()-2; x++) {
			for (int y=source.lowY()+1; y<source.highY()-2; y++) {
				source.set(x,y,5);
			}
		}
		
		ISOContours.Single.Specialized<Integer> contour = new ISOContours.Single.Specialized<Integer>(RENDERER, threshold, true, source);
		GeneralPath p = (GeneralPath) contour.contours().get(0).shape();

		GeneralPath p2 = (GeneralPath) p.clone();
		p2.closePath();
		assertEquals("Unequal bounding after closing.", p.getBounds2D(), p2.getBounds2D());
		
		for (int x=source.lowX()+2; x<source.highX()-3; x++) {
			for (int y=source.lowX()+2; y<source.highX()-3; y++) {
				assertTrue(String.format("Uncontained point at %d,%d", x,y), p.contains(new Point(x,y)));
			}
		}
		assertFalse(p.contains(new Point(0,0)));
		assertFalse(p.contains(new Point(source.highX()-1, source.highY()-1)));
	}
	

	
	
	@Test
	public void ClassifyAggs() {
		int threshold = 3;
		Aggregates<Integer> source = new FlatAggregates<>(0,0,10,10,0); 
		for (int x=source.lowX()+1; x<source.highX()-2; x++) {
			for (int y=source.lowY()+1; y<source.highY()-2; y++) {
				source.set(x,y,5);
			}
		}
		
		ISOContours.ISOBelow<Integer> trans = new ISOContours.ISOBelow<>(threshold);
		Aggregates<Boolean> isoDivided = RENDERER.transfer(source, trans);
		Aggregates<ISOContours.MC_TYPE> classified = RENDERER.transfer(isoDivided, new ISOContours.MCClassifier());
				
		for (int x=source.lowX(); x<source.highX(); x++) {
			for (int y=source.lowY(); y<source.highY(); y++) {
				if (x == source.lowX()
						|| y == source.lowY()
						|| x == source.highX()-1
						|| y == source.highY()-1) {
					assertThat(String.format("Error at %d,%d", x,y), classified.get(x, y), is(ISOContours.MC_TYPE.empty));
				} else if (x == source.lowX()+1) {
					if (y == source.lowY()+1) {
						assertThat(String.format("Error at %d,%d", x,y), classified.get(x,y), is(ISOContours.MC_TYPE.ui_r_in));						
					} else if (y == source.highY()-2) {
						assertThat(String.format("Error at %d,%d", x,y), classified.get(x,y), is(ISOContours.MC_TYPE.di_r_in));
					} else {
						assertThat(String.format("Error at %d,%d", x,y), classified.get(x,y), is(ISOContours.MC_TYPE.r_in));
					}
				} else if (x == source.highX()-2) {
					if (y == source.lowY()+1) {
						assertThat(String.format("Error at %d,%d", x,y), classified.get(x,y), is(ISOContours.MC_TYPE.ui_l_in));						
					} else if (y == source.highY()-2) {
						assertThat(String.format("Error at %d,%d", x,y), classified.get(x,y), is(ISOContours.MC_TYPE.di_l_in));
					} else {
						assertThat(String.format("Error at %d,%d", x,y), classified.get(x,y), is(ISOContours.MC_TYPE.l_in));
					}	
				} else if (y == source.lowY()+1) {
					assertThat(String.format("Error at %d,%d", x,y), classified.get(x, y), is(ISOContours.MC_TYPE.ui_in));
				} else if (y == source.highY()-2) {
					assertThat(String.format("Error at %d,%d", x,y), classified.get(x, y), is(ISOContours.MC_TYPE.di_in));
				} else { 
					assertThat(String.format("Error at %d,%d", x,y), classified.get(x,y), is(ISOContours.MC_TYPE.surround));
				}
			}
		}

	}
	
	@Test
	public void ISODivide() {
		int threshold = 3;
		Aggregates<Integer> source = new FlatAggregates<>(0,0,10,10,0); 
		for (int x=3; x<6; x++) {
			for (int y=3; y<6; y++) {
				source.set(x,y,5);
			}
		}
		
		ISOContours.ISOBelow<Integer> trans = new ISOContours.ISOBelow<>(threshold);
		Aggregates<Boolean> result = RENDERER.transfer(source, trans);
		for (int x=result.lowX(); x<result.highX(); x++) {
			for (int y=result.lowY(); y<result.highY(); y++) {
				assertThat(String.format("Error at %d,%d", x,y), result.get(x,y), is(source.get(x,y) >= threshold));
			}
		}
	}
	
	@Test
	public void PadAggregates() {
		int pad = 3;
		Aggregates<Integer> base = new FlatAggregates<>(0,0,10,10,5);
		Aggregates<Integer> padded = new ISOContours.PadAggregates<Integer>(base, pad);
		
		assertThat(padded.lowX(), is(base.lowX()-1));
		assertThat(padded.lowY(), is(base.lowY()-1));
		assertThat(padded.highX(), is(base.highX()+1));
		assertThat(padded.highY(), is(base.highY()+1));
		
		for (int x=base.lowX(); x<base.highX(); x++) {
			for (int y=base.lowY(); y<base.highY(); y++) {
				assertThat(padded.get(x+1, y+1), is(base.get(x, y)));
			}
		}
		for (int y=padded.lowY(); y<padded.highY(); y++) {
			assertThat("Error at y of " + y, padded.get(base.lowX()-1,y), is(pad));
			assertThat("Error at y of " + y, padded.get(base.highX()+1,y), is(pad));
		}

		
		for (int x=padded.lowX(); x<padded.highX(); x++) {
			assertThat("Error at x of " + x, padded.get(x,base.lowY()-1), is(pad));
			assertThat("Error at x of " + x, padded.get(x,base.highY()+1), is(pad));
		}
	}
	
	@Test
	public void Flatten() {
		Aggregates<Integer> ones = new FlatAggregates<>(5,5,-1);
		Aggregates<Integer> zeros = new FlatAggregates<>(5,5,-1);
		for (int x=ones.lowX(); x< ones.highX(); x++) {
			for (int y=ones.lowY(); y< ones.highY(); y++) {
				ones.set(x, y, 1);
				zeros.set(x,y, 0);
			}
		}
		
		
		List<Aggregates<Integer>> aggs = Arrays.asList(ones,zeros);
		Aggregates<Integer> combined = ISOContours.LocalUtils.flatten(aggs);
		for (int x=combined.lowX(); x< combined.highX(); x++) {
			for (int y=combined.lowY(); y< combined.highY(); y++) {
				assertEquals(String.format("Error at %d x %d", x,y), (Integer) 0, combined.get(x, y));
			}
		}

		List<Aggregates<Integer>> aggs2 = Arrays.asList(zeros,ones);
		Aggregates<Integer> combined2 = ISOContours.LocalUtils.flatten(aggs2);
		for (int x=combined2.lowX(); x< combined2.highX(); x++) {
			for (int y=combined2.lowY(); y< combined2.highY(); y++) {
				assertEquals(String.format("Error at %d x %d", x,y), (Integer) 1, combined2.get(x, y));
			}
		}

		
	}
}
