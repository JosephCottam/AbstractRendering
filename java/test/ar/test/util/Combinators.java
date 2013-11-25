package ar.test.util;

import static org.junit.Assert.*;

import org.junit.Test;

import ar.Aggregates;
import ar.aggregates.AggregateUtils;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.combinators.Predicate;

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
	public void Seq() {
		fail("Not yet implemented");
	}

	@Test
	public void If() {
		fail("Not yet implemented");
	}

	@Test
	public void Fix() {
		fail("Not yet implemented");
	}

	@Test
	public void Diamond() {
		fail("Not yet implemented");
	}

}
