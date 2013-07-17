package ar.test.rules;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import java.awt.Color;

import org.junit.Test;

import ar.rules.CategoricalCounts;
import ar.util.Util;

public class CategoricalCountsTests {

	
	private void testItems(CategoricalCounts<Color> cats) {
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
	public void testRLE() {
		CategoricalCounts.RLE<Color> rle = new CategoricalCounts.RLE<Color>();
		testItems(rle);
	}
	
	@Test
	public void testCoC() {
		CategoricalCounts.CoC<Color> cats = new CategoricalCounts.CoC<Color>(Util.COLOR_SORTER);
		testItems(cats);

	}
	
}
