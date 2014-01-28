package ar.test.rules;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import java.awt.Color;

import org.junit.Test;

import ar.rules.CategoricalCounts;
import ar.util.Util;

public class CategoricalCountsTests {
	@Test
	public void testCoC() {
		CategoricalCounts<Color> cats = new CategoricalCounts<Color>(Util.COLOR_SORTER);
		int full=0;

		for (int i=1; i<11; i++) {
			for (int j=0; j<i; j++) {
				CategoricalCounts<Color> ncats = cats.extend(new Color(i,0,0), 1);
				assertThat("Expected 'fresh' rle after edit.", ncats, not(cats));
				full++;
				assertThat(ncats.fullSize(), is(full));
				assertThat(cats.fullSize(), is(full-1));
				cats = ncats;
			}
		}
		
		assertEquals("Unexpected number of keys", 10, cats.size());
		assertEquals("Unepxected number sum of entries", 55, cats.fullSize());
		
		for (int i=0; i< cats.size(); i++) {
			Color c = cats.key(i);
			assertEquals("Unexpected key/count pair.", c.getRed(), cats.count(c));
		}
	}
	
	@Test
	public void testCategoricalCountsEquals() {
		CategoricalCounts<String> c1 = new CategoricalCounts<>();
		c1 = c1.extend("Hit", 2);
		
		CategoricalCounts<String> c2 = new CategoricalCounts<>();
		c2 = c2.extend("Hit", 1).extend("Miss", 1);
		
		assertFalse(c1.equals(c2));
	}
	
}
