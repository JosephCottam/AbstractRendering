package ar.test.util;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.awt.Color;

import org.junit.Test;

import ar.Aggregates;
import ar.Transfer;
import ar.aggregates.AggregateUtils;
import ar.glyphsets.implicitgeometry.MathValuers;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.rules.General;
import ar.rules.Numbers;
import ar.rules.combinators.*;

public class Combinators {
	@Test
	public void predicates() {
		Aggregates<Boolean> t = AggregateUtils.make(11,20, true);
		Aggregates<Boolean> f = AggregateUtils.make(11,20, false);
		Aggregates<Boolean> m = AggregateUtils.make(11,20, true);
		m.set(10, 10, false);
		
		Predicate<Boolean> isTrue = new Predicate.VPred<>(new Valuer.Equals<>(true));
		Predicate<Boolean> isFalse = new Predicate.Not<>(isTrue);
		
		assertTrue(isTrue.test(t.get(0, 0)));
		assertTrue(isFalse.test(f.get(0, 0)));
		
		Predicate<Aggregates<? extends Boolean>> someTrue = new Predicate.Some<>(isTrue);		
		assertTrue(someTrue.test(t));
		assertFalse(someTrue.test(f));
		assertTrue(someTrue.test(m));

		Predicate<Aggregates<? extends Boolean>> allTrue = new Predicate.All<>(isTrue);
		assertTrue(allTrue.test(t));
		assertFalse(allTrue.test(f));
		assertFalse(allTrue.test(m));
		
		Predicate<Aggregates<? extends Boolean>> someFalse = new Predicate.Some<>(isFalse);
		assertFalse(someFalse.test(t));
		assertTrue(someFalse.test(f));
		assertTrue(someFalse.test(m));

		Predicate<Aggregates<? extends Boolean>> allFalse = new Predicate.All<>(isFalse);
		assertFalse(allFalse.test(t));
		assertTrue(allFalse.test(f));
		assertFalse(allFalse.test(m));
	}
	

	@Test
	public void If() {
		Aggregates<Boolean> a = AggregateUtils.make(11, 31, true);
		for (int x=a.lowX(); x < a.highX(); x++) {
			for (int y=a.lowY(); y < a.lowY(); y++) {
				if ((x+y)%2 == 0) {a.set(x, y, false);}
			}
		}
		
		Transfer.Specialized<Boolean,Color> t = new If<>(new Predicate.True(), new General.Const<>(Color.red, true), new General.Const<>(Color.black, true)).specialize(a);
		Aggregates<Color> rslt = Resources.DEFAULT_RENDERER.transfer(a, t);
		
		for (int x=a.lowX(); x < a.highX(); x++) {
			for (int y=a.lowY(); y < a.lowY(); y++) {
				Color ref = a.get(x,y) ? Color.red : Color.black;
				assertThat(String.format("Error at (%d,%d)", x,y), rslt.get(x,y), is(ref));
			}
		}
	}

	@Test
	public void Fix() {
		Aggregates<Integer> a = AggregateUtils.make(11, 31, 1);
		
		Transfer<Integer,Integer> t1 = new General.ValuerTransfer<>(new MathValuers.AddInt<Integer>(1),0);
		Predicate<Aggregates<? extends Integer>> p = new Predicate.All<>(new Predicate.VPred<>(new MathValuers.GT<Integer>(10)));
		Transfer.Specialized<Integer,Integer> t = new Fix<>(p, t1).specialize(a);
		
		Aggregates<Integer> rslt = Resources.DEFAULT_RENDERER.transfer(a, t);
		
		assertTrue("Bluk test", p.test(rslt));
		for (int x=a.lowX(); x < a.highX(); x++) {
			for (int y=a.lowY(); y < a.lowY(); y++) {
				assertThat(String.format("Error at (%d,%d)", x,y), rslt.get(x,y), is(11));
			}
		}		
	}

	@Test
	public void Diamond() {
		Aggregates<Integer> a = AggregateUtils.make(11, 31, 0);
		
		Transfer<Integer,Integer> t1 = new General.ValuerTransfer<>(new MathValuers.AddInt<Integer>(1),0);
		Transfer<Integer,Integer> t2 = new General.ValuerTransfer<>(new MathValuers.AddInt<Integer>(2),0);
		
		Transfer.Specialized<Integer, Integer> t = new Diamond<>(t1,t2, new Numbers.Count<>()).specialize(a);
		Aggregates<Integer> rslt = Resources.DEFAULT_RENDERER.transfer(a, t);
		
		Predicate<Aggregates<? extends Integer>> p = new Predicate.All<>(new Predicate.VPred<>(new MathValuers.EQ<Integer>(3)));
		assertTrue("Bluk test", p.test(rslt));
	}

	@Test
	public void Seq() {
		Aggregates<Integer> a = AggregateUtils.make(11, 31, 0);
		
		Transfer<Integer,Integer> t1 = new General.ValuerTransfer<>(new MathValuers.AddInt<Integer>(1),0);
		Transfer<Integer,Integer> t2 = new General.ValuerTransfer<>(new MathValuers.AddInt<Integer>(2),0);
		Transfer.Specialized<Integer, Integer> t = new Seq<>(t1,t2).specialize(a);
		
		Aggregates<Integer> rslt = Resources.DEFAULT_RENDERER.transfer(a, t);
		
		Predicate<Aggregates<? extends Integer>> p = new Predicate.All<>(new Predicate.VPred<>(new MathValuers.EQ<Integer>(3)));
		assertTrue("Bluk test", p.test(rslt));
	}
}
