package ar.test.glyphsets;

import static org.junit.Assert.*;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;

import ar.Glyphset;
import ar.glyphsets.WrappedCollection;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;

public class WrappedCollectionTests {
	@Test
	public void wrapList() {
		ArrayList<Indexed> data = data();
		Glyphset<Rectangle2D, Object> g = WrappedCollection.wrap(data, shaper(), valuer());
		
		assertNotNull(g);
		assertEquals("Size mismatch.", data.size(), g.size());
		assertEquals("Failed to identify list.", WrappedCollection.List.class, g.getClass());
		
		Glyphset.RandomAccess<Rectangle2D, Object> ra = (Glyphset.RandomAccess<Rectangle2D, Object>) g;
		
		for (int i=0; i<data.size(); i++) {assertEquals(ra.get(i).info(), data.get(i).get(2));}		
	}

	@Test
	public void wrapNonlist() {
		Collection<Indexed> data = new HashSet<Indexed>();
		data.addAll(data());
		
		Glyphset<Rectangle2D, Object> g = WrappedCollection.wrap(data, shaper(), valuer());
		
		assertNotNull(g);
		assertEquals("Size mismatch.", data.size(), g.size());
		assertFalse("Incorrectly identified list.", g.getClass() == WrappedCollection.List.class);
	}

	public Shaper<Indexed, Rectangle2D> shaper() {return new Indexed.ToRect(1, 0, 1);}
	public Valuer<Indexed,Object> valuer() {return new Indexed.ToValue<Object,Object>(2);}
	
	public ArrayList<Indexed> data() {
		ArrayList<Indexed> values = new ArrayList<Indexed>();
		for (int i=0; i< 100; i++) {
			Integer[] array = new Integer[]{i,i,i};
			values.add(new Indexed.ArrayWrapper(array));
		}
		return values;
	}

}
