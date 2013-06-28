package ar.test.glyphsets;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;

import ar.Glyphset;
import ar.glyphsets.WrappedCollection;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;

public class TestWrappedCollection {
	
	@Test
	public void toQuadTree() {
		Collection data = data();
		Glyphset g = WrappedCollection.toQuadTree(data, shaper(), valuer(), Color.class);
		
		assertNotNull(g);
		assertEquals("Size mismatch.", data.size(), g.size());
	}

	@Test
	public void wrapList() {
		ArrayList<Indexed> data = data();
		Glyphset g = WrappedCollection.wrap(data, shaper(), valuer(), Color.class);
		
		assertNotNull(g);
		assertEquals("Size mismatch.", data.size(), g.size());
		assertEquals("Failed to identify list.", WrappedCollection.List.class, g.getClass());
		
		Glyphset.RandomAccess ra = (Glyphset.RandomAccess) g;
		
		for (int i=0; i<data.size(); i++) {assertEquals(ra.get(i).value(), data.get(i).get(2));}		
	}

	@Test
	public void wrapNonlist() {
		Collection<Indexed> data = new HashSet<Indexed>();
		data.addAll(data());
		
		Glyphset g = WrappedCollection.wrap(data, shaper(), valuer(), Color.class);
		
		assertNotNull(g);
		assertEquals("Size mismatch.", data.size(), g.size());
		assertFalse("Incorrectly identified list.", g.getClass() == WrappedCollection.List.class);
	}

	public Shaper shaper() {return new Indexed.ToRect(1, 0, 1);}
	public Valuer valuer() {return new Indexed.ToValue<Object,Object>(2);}
	
	public ArrayList<Indexed> data() {
		ArrayList<Indexed> values = new ArrayList();
		for (int i=0; i< 100; i++) {
			int[] array = new int[]{i,i,i};
			values.add(new Indexed.ArrayWrapper(array));
		}
		return values;
	}

}
