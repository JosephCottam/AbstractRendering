package ar.ext.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import ar.glyphsets.implicitgeometry.Indexed;

//TODO: FIGURE OUT HOW TO DO 1-TO-MANY MORE EFFICIENLY IN IMPLICIT GEOMETRY
public class BroadcastEntries {
	public static Collection<Indexed> broadcast(Object key, Iterable<?> values) {
		return broadcast(key, values, new ArrayList<Indexed>());
	}
	
	/**
	 * Creates an entry for each value in the collection, associated with
	 * the passed key.  An index is also included in the resulting entries 
	 * that reflects the iteration order.
	 * 
	 * @param key Item to associate with all other items
	 * @param values Values to asssociate with the key
	 * @param acc place to put each result
	 * @return The acc passed in, with new entries in the form (key, cal, idx)
	 */
	public static Collection<Indexed> broadcast(Object key, Iterable<?> values, Collection<Indexed> acc) {
		int i=0;
		for (Object val: values) {
			Indexed item = new Indexed.ArrayWrapper(new Object[]{key, val, i});
			acc.add(item);
			i++;
		}
		return acc;
	}
	
	/**Broadcast each key in the map to the values of associated with it in the map.
	 * 
	 * @param values
	 * @return
	 */
	public static Collection<Indexed> broadcastAll(Map<Object, Iterable<?>> values) {
		List<Indexed> acc = new ArrayList<Indexed>();
		for(Object key: values.keySet()) {
			broadcast(key, values.get(key), acc);
		}
		return acc;
	}
}
