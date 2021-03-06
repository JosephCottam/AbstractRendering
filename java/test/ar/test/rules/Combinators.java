package ar.test.rules;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.awt.Color;

import org.junit.Test;

import ar.Aggregates;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.renderers.ForkJoinRenderer;
import ar.rules.General;
import ar.rules.combinators.*;

public class Combinators {
	@Test
	public void predicates() {
		Aggregates<Boolean> t = AggregateUtils.make(11,20, true);
		Aggregates<Boolean> f = AggregateUtils.make(11,20, false);
		Aggregates<Boolean> m = AggregateUtils.make(11,20, true);
		m.set(10, 10, false);
		
		Valuer<Boolean, Boolean> isTrue = new Valuer.Equals<>(true);
		Valuer<Boolean, Boolean> isFalse = new Predicates.Not<>(isTrue);
		
		assertTrue(isTrue.apply(t.get(0, 0)));
		assertTrue(isFalse.apply(f.get(0, 0)));
		
		Valuer<Aggregates<? extends Boolean>, Boolean> someTrue = new Predicates.Any<>(isTrue);		
		assertTrue(someTrue.apply(t));
		assertFalse(someTrue.apply(f));
		assertTrue(someTrue.apply(m));

		Valuer<Aggregates<? extends Boolean>, Boolean> allTrue = new Predicates.All<>(isTrue);
		assertTrue(allTrue.apply(t));
		assertFalse(allTrue.apply(f));
		assertFalse(allTrue.apply(m));
		
		Valuer<Aggregates<? extends Boolean>, Boolean>  someFalse = new Predicates.Any<>(isFalse);
		assertFalse(someFalse.apply(t));
		assertTrue(someFalse.apply(f));
		assertTrue(someFalse.apply(m));

		Valuer<Aggregates<? extends Boolean>, Boolean>  allFalse = new Predicates.All<>(isFalse);
		assertFalse(allFalse.apply(t));
		assertTrue(allFalse.apply(f));
		assertFalse(allFalse.apply(m));
	}
	

	@Test
	public void If() {
		Aggregates<Boolean> a = AggregateUtils.make(11, 31, true);
		for (int x=a.lowX(); x < a.highX(); x++) {
			for (int y=a.lowY(); y < a.lowY(); y++) {
				if ((x+y)%2 == 0) {a.set(x, y, false);}
			}
		}
		
		Transfer.Specialized<Boolean,Color> t = new If<>(new Valuer.Constant<Boolean, Boolean>(true), new General.Const<>(Color.red, true), new General.Const<>(Color.black, true)).specialize(a);
		Aggregates<Color> rslt = new ForkJoinRenderer().transfer(a, t);
		
		for (int x=a.lowX(); x < a.highX(); x++) {
			for (int y=a.lowY(); y < a.lowY(); y++) {
				Color ref = a.get(x,y) ? Color.red : Color.black;
				assertThat(String.format("Error at (%d,%d)", x,y), rslt.get(x,y), is(ref));
			}
		}
	}

	@Test
	public void While() {
		Aggregates<Integer> a = AggregateUtils.make(11, 31, 1);
		
		Transfer<Integer,Integer> t1 = new General.TransferFn<>(n -> n+1,0);
		Valuer<Aggregates<? extends Integer>, Boolean> p = new Predicates.All<>(new MathValuers.GT<Integer>(10d));
		Transfer.Specialized<Integer,Integer> t = new While<>(p, t1).specialize(a);
		
		Aggregates<Integer> rslt = new ForkJoinRenderer().transfer(a, t);
		
		assertTrue("Bulk test", p.apply(rslt));
		for (int x=a.lowX(); x < a.highX(); x++) {
			for (int y=a.lowY(); y < a.lowY(); y++) {
				assertThat(String.format("Error at (%d,%d)", x,y), rslt.get(x,y), is(11));
			}
		}		
	}
	
	@Test
	public void Fan() {
		@SuppressWarnings("unchecked")
		Transfer<Integer,Integer>[] ts = new Transfer[10];
		for (int i=0; i<ts.length; i++) {
			final int j = i;
			ts[i] = new General.TransferFn<>(n -> n+j, 0);
		}
		
		Aggregates<Integer> a = AggregateUtils.make(11, 31, 0);
		
				
		Transfer.Specialized<Integer, Integer> t = new Fan<>(0, new Fan.AlignedMerge<>((l,r) -> (l+r)), ts).specialize(a);
		Aggregates<Integer> rslt = new ForkJoinRenderer().transfer(a, t);
		
		Valuer<Aggregates<? extends Integer>, Boolean> p = new Predicates.All<>(new MathValuers.EQ<Integer>(45d));
		assertTrue("Bulk test", p.apply(rslt));
	}
	
	@Test
	public void Split() {
		Transfer<Integer, Integer> left = new General.TransferFn<>(n -> n+1, 0);
		Transfer<Integer, Integer> right = new General.TransferFn<>(n -> n+2, 0);
		
		Aggregates<Integer> a = AggregateUtils.make(11, 31, 0);
				
		Transfer.Specialized<Integer, Integer> t = 
				new Split<Integer, Integer, Integer, Integer>(left, right, 0, (l,r) -> (l+r))
				.specialize(a);
		
		Aggregates<Integer> rslt = new ForkJoinRenderer().transfer(a, t);
		
		Valuer<Aggregates<? extends Integer>, Boolean> p = new Predicates.All<>(new MathValuers.EQ<Integer>(3d));
		assertTrue("Bulk test", p.apply(rslt));
	}

	@Test
	public void Seq() {
		Aggregates<Integer> a = AggregateUtils.make(11, 31, 0);
		
		Transfer<Integer,Integer> t1 = new General.TransferFn<>(n -> n+1, 0);
		Transfer<Integer,Integer> t2 = new General.TransferFn<>(n -> n+2, 0);
		Transfer.Specialized<Integer, Integer> t = new Seq<>(t1,t2).specialize(a);
		
		Aggregates<Integer> rslt = new ForkJoinRenderer().transfer(a, t);
		
		Valuer<Aggregates<? extends Integer>, Boolean> p = new Predicates.All<>(new MathValuers.EQ<Integer>(3d));
		assertTrue("Bulk test", p.apply(rslt));
	}
}
